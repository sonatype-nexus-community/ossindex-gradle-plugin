# ossindex-gradle-plugin
Audits a [gradle](https://gradle.org/) project using the [OSS Index REST API v2.0](https://ossindex.net) to identify known vulnerabilities in its dependencies.

Requirements
-------------

* Gradle
* An internet connection with access to https://ossindex.net 

Usage
-----

TODO (the plugin is yet to be released)

### Success output
This will run the OSS Index Auditor against the applicable maven project. A successful
scan finding no errors will look something like this:

![Success](docs/NoVulnerabilities.PNG)

### Error output
If a vulnerability is found that might impact your project, the output will resemble the
following, where the package and vulnerability details depends on what is identified.

![Failure](docs/Vulnerabilities.PNG)


### Dependency tree of vulnerabilities
If the `--info` flag is provided to gradle it will output a dependency tree which shows the transitive dependencies which have vulnerabilities.

![Tree](docs/Tree.png)

Report output
-------------

TODO

Disable fail on error
------------------------

TODO

Ignore vulnerability for package(s)
-----------------------------------

TODO
