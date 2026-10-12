// GBIF's release job for IPT4: builds, tests and pushes the image of the given version to Docker Hub as gbif/ipt.
// Run by hand from Jenkins.
// The GitHub release workflow (.github/workflows/release.yml, started by pushing a tag v<version>) does the same;
// a final version (no suffix) is also tagged latest. The credential ids are those of GBIF's Jenkins; check them
// before the first release.
pipeline {
  agent any
  parameters {
    string(name: 'VERSION', description: 'Release version, e.g. 4.0.0-RC1')
  }
  tools {
    jdk 'OpenJDK25'
    maven 'Maven 3.9'
  }
  stages {
    stage('Version') {
      steps {
        sh 'tools/set-version.sh "$VERSION"'
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
        sh 'tools/docker-smoke.sh "gbif/ipt:$VERSION"'
      }
    }
    stage('Push') {
      steps {
        withDockerRegistry([credentialsId: 'gbif-dockerhub']) {
          sh 'tools/push-image.sh gbif/ipt "$VERSION"'
        }
      }
    }
  }
}
