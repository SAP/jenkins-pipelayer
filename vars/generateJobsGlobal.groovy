#!/usr/bin/env groovy

import groovy.transform.InheritConstructors
import com.sap.corydoras.Parser

@InheritConstructors
class NoTemplateException extends Exception {}

private processTemplate(file) {
    def properties = readProperties file: file.path
    if (!properties['jenkins.job.template']) {
        throw new NoTemplateException()
    }
    def filePath = properties['jenkins.job.template']
    def fileContent = sh returnStdout: true, script: "cat ${filePath}"
    properties.each { key, value ->
        fileContent = fileContent.replace(/{{${key}}}/, value)
    }

    // note: we comment the first line in case a shebang is present
    fileContent = "//template: ${filePath}  properties: ${file}" + fileContent

    def fileName = properties['jenkins.job.name']
    if (!fileName) {
        fileName = filePath.replaceFirst(~/\.[^\.]+$/, '').split('/')[-1]
    }

    return [filePath, fileName, fileContent]
}

def call(String path, String destination, commit, additionalParameters) {
    arrFiles = []
    def parser = new Parser()

    def resourcesDestination = ''

    if (!path) {
        if (additionalParameters.useTemplate) {
            path = 'config/*.properties'
        } else {
            path = 'jobs/**/*.groovy'
        }
    }

    if (!commit || !commit['GIT_URL'] || !commit['GIT_BRANCH']) {
        error 'Cannot generate Jobs. Job must be triggered by a commit.\nIf you are running a multibranch job. Run Scan Multibranch Pipeline Now'
        return
    }

    //copy src to jenkins
    if (additionalParameters.copySrc) {
        resourcesDestination = "$JENKINS_HOME/job_resources/$destination"
        sh "mkdir -p $resourcesDestination"
        sh "cp -r * $resourcesDestination"
    }

    findFiles(glob: path).each { file ->

        def name = ''
        def filePath = ''
        def fileContent = ''

        if (additionalParameters.useTemplate) {
            try {
                (filePath, name, fileContent) = processTemplate(file)
                if (resourcesDestination) {
                    fileContent = fileContent.replace(/{{resources.directory}}/, resourcesDestination)
                }
            } catch (NoTemplateException exception) {
                println "You did not specify a template in $file.path, pass"
            }
        } else {
            fileContent = sh returnStdout: true, script: "cat ${file.path}"
            filePath = file.path
            name = parser.getBaseName(file.name)
        }

        arrFiles << [
            path: filePath,
            name: name,
            displayName: parser.getDisplayName(fileContent, filePath),
            description: parser.getDescription(fileContent, filePath),
            triggers: parser.getTriggers(fileContent, filePath),
            parameters: parser.getParameters(fileContent, filePath),
            authorizations: parser.getAuthorizations(fileContent, filePath),
            environmentVariables: parser.getEnvironmentVariables(fileContent, filePath),
            author: sh(returnStdout: true, script: "git log --format=%an ${filePath} | tail -1").trim(),
            content: additionalParameters.withContent || additionalParameters.useTemplate ? fileContent : ''
        ]
    }

    def targetFile = 'seed/jobs.groovy'
    def jobDefinition = libraryResource "com/sap/corydoras/${targetFile}"
    writeFile file: targetFile, text: jobDefinition

    jobDsl removedJobAction: 'DELETE',
            removedViewAction: 'DELETE',
            targets: targetFile,
            unstableOnDeprecation: true,
            additionalParameters: [
                pipelineJobs: arrFiles,
                props: [
                    basePath: destination,
                    gitRemoteUrl: "${commit.GIT_URL}",
                    gitConfigJenkinsBranch: "${commit.GIT_BRANCH.replaceAll(/^origin\//, '')}"
                ]
            ],
            sandbox: true
}
