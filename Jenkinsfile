pipeline {
  agent any

  environment {
    IMAGE_TAG = "${env.BUILD_NUMBER}"
    SONAR_TOKEN = credentials('sonarcloud-token')
    NEXUS_HOST = 'http://52.23.171.1:8081'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build Java Backend') {
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
          env.BRANCH_NAME == 'main' || env.GIT_BRANCH == 'origin/main'
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
          env.BRANCH_NAME == 'main' || env.GIT_BRANCH == 'origin/main'
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
          env.BRANCH_NAME == 'main' || env.GIT_BRANCH == 'origin/main'
        }
      }
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh '''
            echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
            docker push iamozi2025/java-ap:${BUILD_NUMBER}
            docker push iamozi2025/node-app:${BUILD_NUMBER}
            docker logout
          '''
        }
      }
    }
  }

  post {
    always {
      cleanWs()
    }
  }
}
