pipeline {
	agent {
		label 'ubuntu-latest'
	}

	triggers {
		githubPush()
	}

	options {
		timeout(time: 20, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'10'))
		disableConcurrentBuilds(abortPrevious: false)
	}

	tools {
		jdk   'temurin-jdk21-latest'
		maven 'apache-maven-latest'
	}

	stages {
		stage('Build') {
			steps {
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh '''
						set -euo pipefail

						MAVEN_OPTS="${MAVEN_OPTS:-}"
						MAVEN_OPTS+=" -Djava.security.egd=file:/dev/./urandom" # https://stackoverflow.com/questions/58991966/what-java-security-egd-option-is-for/59097932#59097932
						MAVEN_OPTS+=" -Dorg.slf4j.simpleLogger.showDateTime=true -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss,SSS" # https://stackoverflow.com/questions/5120470/how-to-time-the-different-stages-of-maven-execution/49494561#49494561
						MAVEN_OPTS+=" -Xmx1024m -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Dhttps.protocols=TLSv1.3,TLSv1.2"
						export MAVEN_OPTS
						echo "MAVEN_OPTS: $MAVEN_OPTS"

						mvn clean verify \
							--errors \
							--update-snapshots \
							--batch-mode \
							--show-version \
							--fail-at-end \
							--no-transfer-progress \
							-Declipse.p2.mirrors=false \
							-Djgit.dirtyWorkingTree=warning \
							-Dtycho.localArtifacts=ignore \
							-Dsurefire.rerunFailingTestsCount=3 \
							-Dsurefire.timeout=1500 \
							-Dtarget-platform=latest.target
						'''
				}
			}
			post {
				always {
					archiveArtifacts artifacts: '**/target/repository/**,**/target/screenshots/**'
					junit '**/target/surefire-reports/TEST-*.xml'
					recordIssues publishAllIssues: true, tools: [mavenConsole(), java(), eclipse(), javaDoc()]
				}
			}
		}
	}
}
