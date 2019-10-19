pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        git 'https://github.com/bonapeti/traktor.git'
      }
    }
  }
  environment {
    BOO = 'BAA'
  }
}