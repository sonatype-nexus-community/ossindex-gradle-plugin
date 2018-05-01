package net.ossindex.integrationtests

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ExclusionTests extends Specification
{
  @Rule
  final TemporaryFolder testProjectDir = new TemporaryFolder()
  File buildFile

  def setup() {
    buildFile = testProjectDir.newFile("build.gradle")
  }

  def "a project which ignores two specific vulnerabilities"() {
    given: "A project with 2 vulnerable artifacts with two vulnerabilities ignored from one"
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
                exclusion {
                    vid = "8396562328"
                }
                exclusion {
                    vid = "8402763844"
                }
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
      result.output.contains("8 unignored (of 10 total) vulnerabilities found")
  }

  def "a project which ignores a specific artifact but has another vulnerability should fail"() {
    given: "A project with 2 vulnerable artifacts and one package exclusion"
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
                exclusion {
                    packages = [ 'org.grails:grails' ]
                }
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

  def "a project which ignores a vulnerable artifact and a vulnerability besides"() {
    given: "A project with 2 vulnerable artifacts and two package exclusions"
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
                exclusion {
                    packages = [ 'org.grails:grails' ]
                }
                exclusion {
                    vid = '8396562328'
                }
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
      result.output.contains("1 unignored (of 10 total) vulnerabilities found")
  }

  def "a project which ignores two vulnerable artifacts"() {
    given: "A project with 2 vulnerable artifacts and two package exclusions"
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
                exclusion {
                    packages = [ 'org.grails:grails' ]
                }
                exclusion {
                    packages = [ 'org.bouncycastle:bcprov-jdk15on:1.50' ]
                }
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
      // This is only valid so long as no new vulnerabilities are found in these packages.
      result.output.contains("0 unignored (of 10 total) vulnerabilities found")
  }

  def "a project which ignores a different version of an artifact"() {
    given: "A project with 2 vulnerable artifacts, excluding a different version of an artifact"
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
                exclusion {
                    packages = [ 'org.grails:grails:1.5.3' ]
                }
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
      result.output.contains("10 unignored (of 10 total) vulnerabilities found")
  }

  def "a project which ignores vulnerabilities through a parent package"() {
    given: "A project with 2 vulnerable artifacts, excluding a parent of a vulnerable package"
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
                exclusion {
                    vid = '8396562328'
                    packages = [ 'com.squareup.okhttp3:mockwebserver:3.7.0' ]
                }
                exclusion {
                    vid = '8402763844'
                    packages = [ 'groupid:artifactid:1.50' ]
                }
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
      result.output.contains("9 unignored (of 10 total) vulnerabilities found")
  }
}
