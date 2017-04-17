package net.ossindex.integrationtests

import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.junit.Assert.assertEquals

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
        assertEquals(result.task(":audit").outcome, SUCCESS)
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
        result.output.contains("vulnerabilities found!")
    }
}

