pipeline {
	agent {
		label 'ubuntu-latest'
	}
	triggers {
		githubPush()
	}
	options {
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
	}
	tools {
		maven 'apache-maven-latest'
		jdk   'temurin-jdk21-latest'
	}
	stages {
		stage('Build') {
			steps {
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh "mvn clean verify -fae"
				}
			}
			post {	
				always {
					junit '**/target/surefire-reports/*.xml'
					archiveArtifacts artifacts: '**/target/repository/**,**/target/screenshots/**'
					recordIssues publishAllIssues: true, tools: [mavenConsole(), java(), eclipse(), javaDoc()]
				}
			}
		}
	}
}
