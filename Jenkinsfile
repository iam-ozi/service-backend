pipeline {
  agent any

  environment {
    IMAGE_TAG = "${env.BUILD_NUMBER}"
    DOCKERHUB_CREDENTIALS = credentials('dockerhub-creds')
    SONAR_TOKEN = credentials('sonarcloud-token')
    NEXUS_CREDS = credentials('nexus-creds')
    NEXUS_HOST = 'http://13.217.230.87:8081'
  }

  stages {
    stage('Checkout') {
      steps {
        git branch: 'main', url: 'https://github.com/iam-ozi/service-backend.git'
      }
    }

    stage('Build Java Backend') {
      when {
        expression {
          env.GIT_BRANCH == 'origin/main' || env.BRANCH_NAME == 'main'
        }
      }
      steps {
        dir('java-app') {
          sh '''
            ./mvnw clean package -DskipTests
            mkdir -p ../builds
            cp target/*.jar ../builds/java-ap.jar
          '''
        }
      }
    }

    stage('Build Node Backend') {
      when {
        expression {
          env.GIT_BRANCH == 'origin/main' || env.BRANCH_NAME == 'main'
        }
      }
      steps {
        dir('node-app') {
          sh '''
            npm install
            mkdir -p ../builds
            tar -czf ../builds/node-app.tar.gz .
          '''
        }
      }
    }

    stage('SonarCloud Analysis') {
      when {
        expression {
          env.GIT_BRANCH == 'origin/main' || env.BRANCH_NAME == 'main'
        }
      }
      steps {
        withEnv(["SONAR_TOKEN=${SONAR_TOKEN}"]) {
          sh 'sonar-scanner -Dproject.settings=sonar-project.properties'
        }
      }
    }

    stage('Publish to Nexus') {
      when {
        expression {
          env.GIT_BRANCH == 'origin/main' || env.BRANCH_NAME == 'main'
        }
      }
      steps {
        withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
          sh '''
            curl -v -u $NEXUS_USER:$NEXUS_PASS \
              --upload-file builds/java-ap.jar \
              $NEXUS_HOST/repository/maven-releases/com/example/java-ap/${BUILD_NUMBER}/java-ap-${BUILD_NUMBER}.jar

            curl -v -u $NEXUS_USER:$NEXUS_PASS \
              --upload-file builds/node-app.tar.gz \
              $NEXUS_HOST/repository/raw-hosted/node-app-${BUILD_NUMBER}.tar.gz
          '''
        }
      }
    }

    stage('Build Docker Images') {
      when {
        expression {
          env.GIT_BRANCH == 'origin/main' || env.BRANCH_NAME == 'main'
        }
      }
      steps {
        script {
          docker.build("iamozi2025/java-ap:${IMAGE_TAG}", 'java-app')
          docker.build("iamozi2025/node-app:${IMAGE_TAG}", 'node-app')
        }
      }
    }

    stage('Push Docker Images') {
      when {
        expression {
          env.GIT_BRANCH == 'origin/main' || env.BRANCH_NAME == 'main'
        }
      }
      steps {
        script {
          docker.withRegistry('', DOCKERHUB_CREDENTIALS) {
            docker.image("iamozi2025/java-ap:${IMAGE_TAG}").push()
            docker.image("iamozi2025/node-app:${IMAGE_TAG}").push()
          }
        }
      }
    }
  }

  post {
    always {
      script {
        cleanWs()
      }
    }
  }
}
