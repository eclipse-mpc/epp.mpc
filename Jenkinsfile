def boolean useCredentials = false

def targetPlatformToJavaVersionMap = [
  'staging' : '25',
  '2025-12' : '21',
  '2024-12' : '21',
]

def targetPlatforms = targetPlatformToJavaVersionMap.keySet() as List

pipeline {
  agent {
    label 'centos-latest'
   }

  options {
    timestamps()
    timeout(time: 45, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '10'))
    disableConcurrentBuilds(abortPrevious: true)
  }

  tools {
    maven 'apache-maven-latest'
    jdk 'temurin-jdk25-latest'
  }

  parameters {
    choice(
      name: 'BUILD_TYPE',
      choices: ['nightly', 'milestone', 'release'],
      description: '''
        Choose the type of build.
        Note that a release build will not promote the build, but rather will promote the most recent milestone build.
        '''
    )

    choice(
      name: 'TARGET_PLATFORM',
      choices: targetPlatforms,
      description: '''
        Choose the named target platform against which to compile and test.
        This is relevant only for nightly and milestone builds.
        '''
    )

    booleanParam(
      name: 'PROMOTE',
      defaultValue: true,
      description: 'Whether to promote the build to the download server.'
    )
  }

  stages {
    stage('Display Parameters') {
      steps {
        script {
          env.BUILD_TYPE = params.BUILD_TYPE
          env.TARGET_PLATFORM =params.TARGET_PLATFORM
          env.JAVA_VERSION = targetPlatformToJavaVersionMap[params.TARGET_PLATFORM]
          if ((env.BRANCH_NAME == 'master' || env.BRANCH_NAME == null) && env.TARGET_PLATFORM == 'staging') {
            useCredentials = true
            if (params.PROMOTE) {
              env.PROMOTE = true
            } else {
              env.PROMOTE = false
            }
          } else {
            useCredentials = false
            env.PROMOTE = false
          }

          def description = """
BUILD_TYPE=${env.BUILD_TYPE}
BRANCH_NAME=${env.BRANCH_NAME}
TARGET_PLATFORM=${env.TARGET_PLATFORM}
JAVA_VERSION=${env.JAVA_VERSION}
PROMOTE=${env.PROMOTE}
""".trim()
          echo description
          currentBuild.description = description.replace("\n", "<br/>")
        }
      }
    }

    stage('Build') {
      steps {
        script {
          wrap([$class: 'Xvnc', useXauthority: true]) {
            if (useCredentials) {
              sshagent(['projects-storage.eclipse.org-bot-ssh']) {
                mvn()
              }
            } else {
              mvn()
            }
          }
        }
      }
    }
  }

  post {
    always {
      archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/repository/**,**/target/screenshots/**'
      junit '**/target/surefire-reports/*.xml'
      recordIssues publishAllIssues: true, tools: [mavenConsole(), java(), eclipse(), javaDoc()]
    }

    failure {
      mail to: 'ed.merks@gmail.com',
      subject: "[mpc] Build Failure ${currentBuild.fullDisplayName}",
      mimeType: 'text/html',
      body: "Project: ${env.JOB_NAME}<br/>Build Number: ${env.BUILD_NUMBER}<br/>Build URL: <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a>"
    }

    fixed {
      mail to: 'ed.merks@gmail.com',
      subject: "[mpc] Back to normal ${currentBuild.fullDisplayName}",
      mimeType: 'text/html',
      body: "Project: ${env.JOB_NAME}<br/>Build Number: ${env.BUILD_NUMBER}<br/>Build URL: <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a>"
    }

    cleanup {
      deleteDir()
    }
  }
}

def void mvn() {
  sh '''
    pwd
    if [[ $PROMOTE == false ]]; then
      sign_argument=''
    else
      sign_argument='-Prelease,promote'
    fi
    mvn \
      --no-transfer-progress \
      -Dorg.eclipse.justj.p2.manager.build.url=$JOB_URL \
      -Dbuild.type=$BUILD_TYPE \
      -Dgit.commit=$GIT_COMMIT \
      -DskipTests=false \
      -Dtarget-platform=${TARGET_PLATFORM} \
      -Djava-version=${JAVA_VERSION} \
      -Dmaven.repo.local=$WORKSPACE/.m2/repository \
      -Dtycho.surefire.timeout=720 \
      $sign_argument \
      clean \
      verify
    '''
}