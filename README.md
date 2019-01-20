# ossindex-gradle-plugin
Audits a [gradle](https://gradle.org/) project using the [OSS Index REST API v3](https://ossindex.sonatype.org/rest) to identify known vulnerabilities in its dependencies.

New Release Notes
-------------

**This release uses the new OSS Index v3 API**. There are a few differences of note:

* Vulnerability IDs have changed, they are now UUIDs instead of long integers.
  These should not change again: sorry for the inconvenience.
* The new API has rate limiting, which is higher for authenticated users. Many
  users should be fine running unauthenticated, but if you start running
  into rate limit issues this can easily be resolved by getting a free OSS Index
  account and providing credentials (discussed below).
* We have added results caching in an attempt to reduce server hits (and thus
  reduce rate limit problems). Setting the cache location is described below.

Table of Contents
-----------------

  * [New Release Notes](#new-release-notes)
  * [Requirements](#requirements)
  * [Usage](#usage)
    + [Installing](#installing)
    + [Running the audit](#running-the-audit)
    + [Success output](#success-output)
    + [Error output](#error-output)
    + [Credentials](#credentials)
    + [Prevent error on exceeding rate limit](#prevent-error-on-exceeding-rate-limit)
    + [Dependency tree of vulnerabilities](#dependency-tree-of-vulnerabilities)
    + [Reporting in a Jenkins Pipeline](#reporting-in-a-jenkins-pipeline)
    + [Stages](#stages)
    + [Report Output](#report-output)
  * [Disable fail on error](#disable-fail-on-error)
  * [Ignore: Simple vulnerability management](#ignore--simple-vulnerability-management)
  * [Exclusions: Advanced vulnerability management](#-alpha--exclusions--advanced-vulnerability-management)
  * [Cache](#cache)

Requirements
-------------

* Gradle
* An internet connection with access to https://ossindex.net

Usage
-----

### Installing

(NOTE: Versions < 1.0 are considered preview)

The plugin is available at the [Gradle plugin repository](https://plugins.gradle.org/plugin/net.ossindex.audit).

To use it either place (replace `THEVERSION` with the correct version)

```
plugins {
  id "net.ossindex.audit" version "THEVERSION"
}
```

in your `build.gradle`.

If you use an older Gradle version you can use

```
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.net.ossindex:ossindex-gradle-plugin:THEVERSION"
  }
}

apply plugin: "net.ossindex.audit"
```

### Running the audit

To run the audit standalone specify the task `audit`:

`gradle audit`

To use it before compiling write

`compile.dependsOn audit`

into your buildscript.

### Success output
This will run the OSS Index Auditor against the applicable maven project. A successful
scan finding no errors will look something like this:

![Success](docs/NoVulnerabilities.PNG)


### Error output
If a vulnerability is found that might impact your project, the output will resemble the
following, where the package and vulnerability details depends on what is identified.

![Failure](docs/Vulnerabilities.PNG)

### Credentials

The OSS Index API is rate limited. In many cases the limit is more than
sufficient, however in heavier use cases an increased limit might be desired.
This can be attained by creating a user account at
[OSS Index](https://ossindex.sonatype.org) and supplying the username and `token`
to the plugin. The `token` can be retrieved from the OSS Index settings page
of the user.

As you don't want credentials stored in a source repository, you use the
gradle properties file to specify the cache folder. In the gradle properties,
write this:

```
ossindexUser=user@example.com
ossindexToken=ef40752eeb642ba1c3df1893d270c6f9fb7ab9e1
```

In your build.gradle file, add the following.

```
audit {
        user = "$ossindexUser"
        token = "$ossindexToken"
    }
```

The credentialed rate limit is 64 requests per hour, where each request can fetch
information for up to 128 packages. (Strictly speaking, you actually have 64 requests which
replenish at a rate of one per minute).

### Prevent error on exceeding rate limit
It may be that you know you are going to exceed the rate limit, but you want
a run to complete without failing. This is done by setting the "rateLimitAsError"
option.

```
audit {
        rateLimitAsError = false
    }
```

By default the cache resets every 12 hours, which means if you exceed the
credentialled limit, you can run multiple times a day (eg. once an hour) and cached packages
WILL NOT be rechecked on the server and therefore rate limiting will not
be affected by those packages.

### Dependency tree of vulnerabilities
If the `--info` flag is provided to gradle it will output a dependency tree which shows the transitive dependencies which have vulnerabilities.

![Tree](docs/Tree.PNG)

### Reporting in a Jenkins Pipeline

The gradle plugin supports writing out test reports in the correct XML format for the
[Jenkins JUnit Reporting Plugin](https://wiki.jenkins.io/display/JENKINS/JUnit+Plugin).
To switch on this reporting, set the path to the report in you project's build.gradle file using the **"junitReport"** element like so:

```
audit {
        failOnError = false
        ignore = [ 'ch.qos.logback:logback-core' ]
        junitReport = "./ossindex/junitReport.xml"
    }
```

This would create the file in an /ossindex folder in the project root.

To access this using the JUnit plugin in a Jenkins pipeline:

```
    stage('OSSIndex Scan') {
        steps {
            // Run the audit
            sh "./gradlew --no-daemon --stacktrace audit"
        }
        post {
            always {
                // Tell junit plugin to use report
                junit '**/ossindex/junitReport.xml'
                // Fail stage if '<failure' found in report
                sh "[[ ! \$(grep '<failure' ./ossindex/junitReport.xml) ]]"
            }
        }
    }
```

NOTE: The junit plugin uses a slightly different syntax to reference the path.

The example code creates a stage in the pipeline, best put between checkout and compile, to run the ossindex
scan and then run the reporting plugin.

The line:
```
    sh "[[ ! \$(grep '<failure' ./ossindex/junitReport.xml) ]]"
```

Ensures that the build fails if any failures are reported.

Set
```
    failOnError = false
```
As failOnError is true by default and will cause the scan to exit on the first failure, instead of finding them all.

### Stages

![Typical Pipeline Stage](docs/pipeline_stages.png)

### Report Output

![Typical Report](docs/example_report.png)

Disable fail on error
------------------------

To let the build continue when vulnerabilities are found you can override the `failOnError` property:

```
audit {
    failOnError = false
}
```

Ignore: Simple vulnerability management
---------------------------------------

To ignore vulnerabilities from specific artifacts you can specify the artifacts on two ways:

Ignore a specific version:
```
audit {
    ignore = [ 'org.dependency:thelibrary:1.0.0' ]
}
```

Ignore a specific artifact (all versions):
```
audit {
    ignore = [ 'org.dependency:thelibrary' ]
}
```

`ignore` is a list which makes it possible to ignore multiple artifacts:

```
audit {
    ignore = [ 'org.dependency:thelibrary', 'net.awesomelibs:anotherlib:1.0.0' ]
}
```

Exclusions: Advanced vulnerability management
-----------------------------------------------------
Exclusions provide a similar task as "ignore", but with more expressiveness.

Ignore all vulnerabilities in a specific package version. This ignores only vulnerabilities
directly in the specified package.

```
audit {
    exclusion {
        packages = [ 'org.dependency:thelibrary:1.0.0' ]
    }
}
```

Ignore all vulnerabilities in a specific package. This ignores only vulnerabilities
directly in the specified package.

```
audit {
    exclusion {
        packages = [ 'org.dependency:thelibrary' ]
    }
}
```

Ignore a specific vulnerability. Some vulnerabilities are assigned to multiple
packages. This will ignore *all instances* of this vulnerability in any package.

```
audit {
    exclusion {
        id = '96a6fc01-c9c1-4098-94e5-c30f4052a3d4'
    }
}
```

Ignore a specific vulnerability in a specific package version's dependencies. Note that this
vulnerability does not necessarily need to belong to the exact package, but be
somewhere in the dependency tree under the package.

As vulnerabilities are assigned to "vulnerable packages", including a the vulnerable
package in this way will ignore the vulnerability for *anyone* who depends on this
package version.

Instead you can specify a parent package which does not express the vulnerability or
otherwise mitigates the problem, which other packages which include the vulnerable
package will still report the vulnerability.

```
audit {
    exclusion {
        packages = [ 'org.dependency:thelibrary:1.0.0' ]
        id = '96a6fc01-c9c1-4098-94e5-c30f4052a3d4'
    }
}
```

Ignore a specific vulnerability belonging to a specific package's dependencies
(any version).  Note that this
vulnerability does not necessarily need to belong to the exact package, but be
somewhere in the dependency tree under the package.

As vulnerabilities are assigned to "vulnerable packages", including a the vulnerable
package in this way will ignore the vulnerability for *anyone* who depends on this
package.

Instead you can specify a parent package which does not express the vulnerability or
otherwise mitigates the problem, which other packages which include the vulnerable
package will still report the vulnerability.

```
audit {
    exclusion {
        packages = [ 'org.dependency:thelibrary' ]
        vid = '96a6fc01-c9c1-4098-94e5-c30f4052a3d4'
    }
}
```

Ignore a specific vulnerability belonging to a dependency path that has multiple
packages that MUST be in the path. This can handle more complex situations.

For example: The same vulnerability can affect both package 'A' and 'B'. Our code includes
'A' as a dependency of 'Z'.

By setting up the exclusion using **both** 'A' and 'Z' we exclude the vulnerability
only in the situation where it is found in package 'A' when included by 'Z'.

The vulnerability will still be reported if:

* We include package 'B' anywhere
* We include package 'A' as a dependency of any other package

```
audit {
    exclusion {
        packages = [ 'org.parent.package:theParent', 'org.dependency:vulnerablePackage ]
        vid '96a6fc01-c9c1-4098-94e5-c30f4052a3d4'
    }
}
```

Cache
-----

In order to reduce round trips to OSS Index (as there is rate limiting), a
local cache file is used. By default it is in the `.ossindex` directory
in the users home folder. This location can be overloaded.

```
audit {
    cache = "/tmp/ossindex.cache"
}
```
