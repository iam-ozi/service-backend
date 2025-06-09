pipeline {
  agent any

  environment {
    IMAGE_TAG = "${env.BUILD_NUMBER}"
    DOCKERHUB_CREDENTIALS = credentials('dockerhub-creds') // Jenkins credentials ID for DockerHub
    SONAR_TOKEN = credentials('sonarcloud-token') // Jenkins credentials ID for SonarCloud token
    NEXUS_CREDS = credentials('nexus-creds') // Jenkins credentials ID for Nexus user/pass
    NEXUS_HOST = 'http://54.159.41.107:8081'
  }

  stages {
    stage('Checkout') {
      steps {
        git branch: 'main', url: 'https://github.com/iam-ozi/service-backend.git'
      }
    }

    stage('Build Java Backend') {
      when {
        branch 'main'
      }
      steps {
        dir('java-app') {
          sh './mvnw clean package -DskipTests'
          sh 'cp target/*.jar ../builds/java-ap.jar'
        }
      }
    }

    stage('Build Node Backend') {
      when {
        branch 'main'
      }
      steps {
        dir('node-app') {
          sh 'npm install'
          sh 'tar -czf ../builds/node-app.tar.gz .'
        }
      }
    }

    stage('SonarCloud Analysis') {
      when {
        branch 'main'
      }
      steps {
        withEnv(["SONAR_TOKEN=${SONAR_TOKEN}"]) {
          sh 'sonar-scanner -Dproject.settings=sonar-project.properties'
        }
      }
    }

    stage('Publish to Nexus') {
      when {
        branch 'main'
      }
      steps {
        withCredentials([usernamePassword(credentialsId: 'nexus-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
          sh '''
            mkdir -p builds
            # Upload Java JAR
            curl -v -u $NEXUS_USER:$NEXUS_PASS \
              --upload-file builds/java-ap.jar \
              $NEXUS_HOST/repository/maven-releases/com/example/java-ap/${BUILD_NUMBER}/java-ap-${BUILD_NUMBER}.jar

            # Upload Node archive
            curl -v -u $NEXUS_USER:$NEXUS_PASS \
              --upload-file builds/node-app.tar.gz \
              $NEXUS_HOST/repository/raw-hosted/node-app-${BUILD_NUMBER}.tar.gz
          '''
        }
      }
    }

    stage('Build Docker Images') {
      when {
        branch 'main'
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
        branch 'main'
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
      cleanWs()
    }
  }
}
