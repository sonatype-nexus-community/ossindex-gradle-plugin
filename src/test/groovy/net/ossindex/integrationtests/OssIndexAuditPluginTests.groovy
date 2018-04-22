package net.ossindex.integrationtests

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class OssIndexAuditPluginTests extends Specification {
    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile("build.gradle")
    }

    def "a project with no dependencies should not fail"() {
        given: "A project with no dependencies"
        buildFile << """
            plugins {
                id 'net.ossindex.audit'
                id 'java'
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("audit")
                .withPluginClasspath()
                .build()

        then:
        result.task(":audit").outcome.is(SUCCESS)
    }

    def "a project with vulnerable dependencies should fail"() {
        given: "A project with vulnerable dependencies"
        buildFile << """
            plugins {
                id 'net.ossindex.audit'
                id 'java'
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                compile 'org.grails:grails:2.0.1'
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("audit")
                .withPluginClasspath()
                .buildAndFail()

        then:
        result.task(":audit").outcome.is(FAILED)
        result.output.contains("vulnerabilities found")
    }

    def "a project with vulnerable dependencies which should continue on error should pass"() {
        given: "A project with vulnerable dependencies"
        buildFile << """
            plugins {
                id 'net.ossindex.audit'
                id 'java'
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                compile 'org.grails:grails:2.0.1'
            }
            
            audit {
                failOnError = false
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("audit")
                .withPluginClasspath()
                .build()

        then:
        result.task(":audit").outcome.is(SUCCESS)
        result.output.contains("vulnerabilities found")
    }

    def "a project which ignores a specific artifact and version should pass"() {
        given: "A project which ignores the vulnerability with specific version"
        buildFile << """
            plugins {
                id 'net.ossindex.audit'
                id 'java'
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                compile 'org.grails:grails:2.0.1'
            }
            
            audit {
                ignore = [ 'org.grails:grails:2.0.1' ]
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("audit")
                .withPluginClasspath()
                .build()

        then:
        result.task(":audit").outcome.is(SUCCESS)
    }

    def "a project which ignores a specific artifact and all versions should pass"() {
        given: "A project which ignores the vulnerability with all versions"
        buildFile << """
            plugins {
                id 'net.ossindex.audit'
                id 'java'
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                compile 'org.grails:grails:2.0.1'
            }
            
            audit {
                ignore = [ 'org.grails:grails' ]
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("audit")
                .withPluginClasspath()
                .build()

        then:
        result.task(":audit").outcome.is(SUCCESS)
    }

    def "a project which ignores a specific artifact but has another vulnerability should fail"() {
        given: "A project with 2 vulnerable artifacts and one ignore"
        buildFile << """
            plugins {
                id 'net.ossindex.audit'
                id 'java'
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                compile 'org.grails:grails:2.0.1'
                compile 'com.squareup.okhttp3:mockwebserver:3.7.0'
            }
            
            audit {
                ignore = [ 'org.grails:grails' ]
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("audit")
                .withPluginClasspath()
                .buildAndFail()

        then:
        result.task(":audit").outcome.is(FAILED)
        // This is only valid so long as no new vulnerabilities are found in these packages.
        result.output.contains("2 unignored (of 10 total) vulnerabilities found")
    }
}


