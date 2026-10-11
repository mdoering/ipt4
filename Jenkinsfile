// GBIF's release job for IPT4: builds, tests and pushes the image of the given version. Run by hand from Jenkins.
// The credential ids are those of GBIF's Jenkins; check them before the first release.
pipeline {
  agent any
  parameters {
    string(name: 'VERSION', description: 'Release version, e.g. 4.0.0-RC1')
    booleanParam(name: 'LATEST', defaultValue: false, description: 'Also tag the image as latest')
  }
  tools {
    jdk 'OpenJDK25'
    maven 'Maven 3.9'
  }
  stages {
    stage('Version') {
      steps {
        sh "mvn -B versions:set -DnewVersion=${params.VERSION} -DgenerateBackupPoms=false"
      }
    }
    stage('Build and test') {
      steps {
        sh 'mvn -B verify'
        sh "node --test 'ipt-app/src/test/js/*.test.js'"
      }
    }
    stage('Image') {
      steps {
        sh "tools/docker-smoke.sh gbif/ipt:${params.VERSION}"
      }
    }
    stage('Push') {
      steps {
        withDockerRegistry([url: 'https://docker.gbif.org', credentialsId: 'gbif-docker']) {
          sh "docker tag gbif/ipt:${params.VERSION} docker.gbif.org/ipt:${params.VERSION} && docker push docker.gbif.org/ipt:${params.VERSION}"
        }
        withDockerRegistry([credentialsId: 'gbif-dockerhub']) {
          sh "docker push gbif/ipt:${params.VERSION}"
          script {
            if (params.LATEST) {
              sh "docker tag gbif/ipt:${params.VERSION} gbif/ipt:latest && docker push gbif/ipt:latest"
            }
          }
        }
      }
    }
  }
}
