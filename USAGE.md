# Usage

## Requirements

Import the library in Jenkins. From __Manage__ page, go to __Configure System__
Under section __Global Pipeline Libraries__, add a new library with name
`jenkins-pipelayer`, default version `master`, modern scm git https://github.com/SAP/jenkins-pipelayer.git

<details><summary>alternative: configure the library from a script</summary>
<p>

Execute the below script from jenkins console `https://<jenkins-url>/script`

```groovy
import jenkins.model.*
import jenkins.plugins.git.GitSCMSource
import org.jenkinsci.plugins.workflow.libs.*
  
createIfMissing('jenkins-pipelayer', 'https://github.com/SAP/jenkins-pipelayer.git', 'master')

def createIfMissing(String libName, String gitUrl, String defaultVersion) {
    println 'Add Library ' + libName + ', from repository ' + gitUrl + ' with version ' + defaultVersion
    GitSCMSource gitScmSource = new GitSCMSource(null, gitUrl, '', 'origin', '+refs/heads/*:refs/remotes/origin/*', '*', '', true)
    LibraryConfiguration lib = new LibraryConfiguration(libName, new SCMSourceRetriever(gitScmSource))
    lib.defaultVersion = defaultVersion
    lib.implicit = false
    lib.allowVersionOverride = true

    GlobalLibraries globalLibraries = GlobalLibraries.get()
    List libs = new ArrayList(globalLibraries.libraries)

    boolean exists = false
    for (LibraryConfiguration libConfig : libs) {
        if (libConfig.name == libName) {
            exists = true
            break
        }
    }

    if (!exists) {
        libs.add(lib)
        globalLibraries.setLibraries(libs)
        globalLibraries.save()
        println 'Library ' + libName + ' added'
    }
}
```

</p>
</details>

approve from page __In Process Script Approval__:

```groovy
method groovy.lang.GroovyObject getProperty java.lang.String
method groovy.lang.GroovyObject invokeMethod java.lang.String java.lang.Object
method jenkins.model.Jenkins getItemByFullName java.lang.String
method jenkins.model.ParameterizedJobMixIn$ParameterizedJob isDisabled
staticMethod jenkins.model.Jenkins getInstance
staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods println java.lang.Object java.lang.Object
```

<details><summary>recommended alternative: approve these using a script</summary>
<p>

Execute the below script from jenkins console `https://<jenkins-url>/script`

```groovy
import org.jenkinsci.plugins.scriptsecurity.scripts.*

def scriptApproval = ScriptApproval.get()

[
    'method groovy.lang.GroovyObject getProperty java.lang.String',
    'method groovy.lang.GroovyObject invokeMethod java.lang.String java.lang.Object',
    'method jenkins.model.Jenkins getItemByFullName java.lang.String',
    'method jenkins.model.ParameterizedJobMixIn$ParameterizedJob isDisabled',
    'staticMethod jenkins.model.Jenkins getInstance',
    'staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods println java.lang.Object java.lang.Object',
    'new groovy.json.JsonSlurperClassic'
].each { signature ->
    scriptApproval.approveSignature(signature)
}

scriptApproval.save()
```

This approval method is recommended. Indeed if you don't use the script, whenever you run a step that uses one of these methods, your pipeline will fail, you will have to go to **In-process Script Approval** page, approve the method, restart your pipeline and repeat these steps until everything is approved.

</p>
</details>

At first run of `generateJobs` step, approve the seed job.

## Pipeline step usage

### Generate jobs

To generate jobs from jobs or templates configuration files, use `generateJobs`.

Usage:

```groovy
    generateJobs {
        gitConfigJenkinsBranch = 'master'
        gitRemoteUrl = 'git-uri'
    }
```
- **gitConfigJenkinsBranch** [mandatory]: the branch on which the jobs are located.

- **gitRemoteUrl** [mandatory]: the url to the repository is used to set the project url property of the job. This set Github link in jenkins menu to redirect to the repository.

- **path**: path to your files (either jobs or templates configuration files) [defaults: 'config/\*.properties' for templates otherwise 'jobs/\*\*/\*.groovy'].

- **destination**: where to create the jobs. empty is at jenkins root, if not, will create a folder.

- **useTemplate**: specifies wether you want to use templates. If so, it will process files in `config/*.properties` by default.

- **withContent**: specifies wether you want to use your files contents as groovy scripts for the jobs. If not set, groovy scripts will be fetched from GitHub when running the jobs. This parameter is useless if you use templates.

- **copySrc**: specifies wheter you want to copy your project's files to Jenkins. If yes, files will be copied to `$JENKINS_HOME/job_resources/{{destination}}`. The full path to this destination can be retrieved in templates with `{{sources.directory}}`. `useTemplate` option must be set to true.

- **localPath**: the process working directory. ie: your jobs are in a folder ```./devops/config``` and you execute this step from within a ```./devops/Jenkinsfile```.

### Approve scripts

You need to add step `approveScripts` to approve generated scripts when `useTemplate = true` parameter is used.
You could also do it manually from page *Manage Jenkins -> In-process Script Approval* 

`approveScripts` looks for all non approved scripts and methods and approve them.

Use it like this:

```groovy
    stage('approve') {
        steps {
            approveScripts 'jenkins_user', this
        }
    }
```

In this example, `jenkins_user` is a jenkins credential id from <https://<your_jenkins>/credentials/>
The credential must be of type "username and password" and be a valid jenkins user.

### Host shared library on filesystem

Jenkins Shared libraries are usually hosted on scm. This method is handy but presents two drawbacks:

- Each time a job builds, a request is made to Github
- checkout of the libary may fail because of a timeout

With `generateJobs` and its property `useTemplate` we get rid of the call to github everytime the pipeline runs.
With `libToFs` we get rid of the last calls that does not concern the code we want to test.
`libToFs` uses the same logic `generateJobs`'s property`copySrc` uses. It stores the library to the File System.
The library is cloned and added to jenkins "Configure System" page as `file:///path/to/library`.
If the library is already present, `libToFs` will just fetch the new sources

```groovy
    stage('library to filesystem') {
        steps {
            libToFs 'jenkins_user', this, [
                'jenkins-pipelayer': 'https://github.com/SAP/jenkins-pipelayer.git'
            ]
        }
    }
```

Note: `git` must be installed on master node.
you can specify a branch in the form of https://<giturl>.git@<branch>

`jenkins_user` is credentials of a user on jenkins. same as approveScripts step.

## Template Engine

### Create a template

Templates are pipelines that contains variables in the form of `{{key}}`
This syntax is inspired by the logic less template engine [mustache](https://mustache.github.io), but does only sypport the variable replacement functionnality.

example of template:

```
/*
    My Job Description
*/
pipeline {
    agent any
    parameters {
        booleanParam(name: 'HCP_HOST', defaultValue: "{{hcp.host}}")
    }
    stage('dummy') {
        steps {
            println params.HCP_HOST
        }
    }
}
```

### Use the template

Create or modify a property file in `config` folder.
Set property `jenkins.job.template` to the pipeline template you want to use.
Set a name for the job with property `jenkins.job.name`


example of property file `./config/my-dymmy-job.properties`:

```
hcp.host=int.sap.hana.ondemand.com
jenkins.job.name=dummy-job
jenkins.job.pipeline=templates/my-dummy-job.groovy
```

you can also set `jenkins.job.destination` to import the job with name `jenkins.job.name` to folder `jenkins.job.destination`

`jenkins.job.template` also supports ant pattern to get multiple files.
If there are multiple files, `jenkins.job.name` won't be used.

An other method to set a job name is to write a comment as follow in the pipeline file:
`//@jobName=my-generated-job`

you can also act on the display name of the job with:
`//@DisplayName=my cool name`

Note: remember the comment `My Job Description` we wrote at the top of our pipeline? The description of the generated job in jenkins is set based on the first comment of the form

```
/*
    <comment>
*/
```

## How it is made possible

Jenkins-pipelayer uses JobDSL to create pipelines jobs from declarative pipelines

The JobDSL seed is located in `resources` folder.

To help you understand this project, have a look at folder `sample`.

To understand how jobdsl works [have a look at the documentation](https://github.com/jenkinsci/job-dsl-plugin/wiki/Tutorial---Using-the-Jenkins-Job-DSL). You could also go see the [jobdsl api documentation](https://jenkinsci.github.io/job-dsl-plugin/)

## Known limitations

 - Pipeline keyword `jobDsl` is called, therefore you cannot cummulate multiple time the generation step within the same job
   For instance, if you call `generateMultiPipeline` before `generateJobs`, only jobs generated with `generateJobs` will remain
 - Don't execute these methods on a docker. A jenkins workspace must exist. This lib uses `writeFile` pipeline keyword
 - There is a `jobDsl` limitation regarding the update of jobs. If the job is running at high frequency (every time a build stops, a new one starts) you might need to disable it in order to update it with jobdsl
 - When adding a multi-choice parameter to a properties file for a pipeline, be sure to DOUBLE escape the \n:
 `choice.values=value1\\nvalue2\\nvalue3\\nvalue4`
 

