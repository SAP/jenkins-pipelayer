# Jenkins Pipelayer

Pipeline job generation made easy

## Description

This Jenkins Library introduces :

- a new way to import jobs programmatically into jenkins.
  Basically it is a parser that converts Declarative Pipelines to JobDSL Declarative Pipelines
  This embed a template engine to generage multiple jobs from one Jenkinsfile template (or more) parametarized by property files
- a clever way to reduce drastically the numbers of calls made by github by your pipelines
  this library automates the process of storing and updating a shared library on jenkins filesystem

## Use cases

- Reduce the number of jobs to maintain
- develop pipelines without the need configure jobs on jenkins
- Handle multiple microservices and their jenkinsfile from one repo. Meta repository is handy when the project you work on has too many microservices and was not designed with deployment in mind.

## Usage

### Import the library to Jenkins

From the __Configure System__ administration page, add the library <https://github.com/SAP/jenkins-pipelayer.git>, name it 
`jenkins-pipelayer` for convenience

Have a look at [USAGE.md](https://github.com/SAP/jenkins-pipelayer/blob/master/USAGE.md#requirements) for a detailed procedure of the import.

### Import the library in a Jenkins job

```groovy
@Library('jenkins-pipelayer')_
```

### Use the library

The library adds the folowing steps to Jenkins pipeline dsl:

- [__generateJobs__](https://github.com/SAP/jenkins-pipelayer/blob/master/USAGE.md#generate-jobs) => Parse pipeline dsl, translate it to jobdsl and create a jenkins job
- [__approveScripts__](https://github.com/SAP/jenkins-pipelayer/blob/master/USAGE.md#approve-scripts) => this step approves jobs generated from templates on jenkins
- [__libToFs__](https://github.com/SAP/jenkins-pipelayer/blob/master/USAGE.md#host-shared-library-on-filesystem) => particularly useful to reduce the number of queries to github

Have a look at [USAGE.md](https://github.com/SAP/jenkins-pipelayer/blob/master/USAGE.md) for a description of the steps introduced by Jenkins Pipelayer.

The template engines introduced by step `generateJobs` can create and manage jobs based on the same template parameterized by multiple property files.

For instance, the following two property files would generate job1 and job2, where job 1 displays `example.com` and job2 displays `site.com`

```
// ./config/myjob1.properties
hcp.host=example.com

// ./config/myjob2.properties
hcp.host=site.com

// mytemplate.groovy
    steps {
            println "{{hcp.host}}"
    }
```

see the usage documentation here for the [template engine](https://github.com/SAP/jenkins-pipelayer/blob/master/USAGE.md#template-engine)

We made samples to help you understand how to use the library. Please check out the [sample](https://github.com/SAP/jenkins-pipelayer/tree/master/sample) folder

## Contribute

Install the [requirements](https://github.com/SAP/jenkins-pipelayer/blob/master/USAGE.md#requirements). Run lint and tests with command:

```
gradlew codenarcMain test
```

Have a look at our [Contribution Guidelines](https://github.com/SAP/jenkins-pipelayer/blob/master/CONTRIBUTING.md), there is more info there.

## License

Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved. This file is licensed under the Apache Software License, Version 2.0 except as noted otherwise in the [LICENSE](https://github.com/SAP/jenkins-pipelayer/blob/master/LICENSE) file.
