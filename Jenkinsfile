pipeline {
  agent any

  environment {
    IMAGE_TAG = "${env.BUILD_NUMBER}" // ⬅️ Used to tag Docker images
    SONAR_TOKEN = credentials('sonarcloud-token')
    NEXUS_HOST = 'http://54.144.217.147:8081'
    AWS_REGION = 'us-east-1' // ⬅️ Replace with your actual AWS region
    EKS_CLUSTER = 'eks-demo-cluster' // ⬅️ Replace with your EKS cluster name
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Detect Changes') {
      steps {
        script {
          def changedFiles = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim().split('\n')
          env.HAS_JAVA_CHANGES = changedFiles.any { it.startsWith('service-backend/java-app/') }.toString()
          env.HAS_NODE_CHANGES = changedFiles.any { it.startsWith('service-backend/node-app/') }.toString()
        }
      }
    }

    stage('Build Java Backend') {
      when { expression { env.HAS_JAVA_CHANGES == 'true' } }
      steps {
        dir('service-backend/java-app') {
          sh '''
            ./mvnw clean package -DskipTests
            mkdir -p ../../builds
            cp target/*.jar ../../builds/java-ap.jar
          '''
        }
      }
    }

    stage('Build Node Backend') {
      when { expression { env.HAS_NODE_CHANGES == 'true' } }
      steps {
        dir('service-backend/node-app') {
          sh '''
            npm install
            mkdir -p ../../builds
            tar -czf ../../builds/node-app.tar.gz .
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
          script {
            if (env.HAS_JAVA_CHANGES == 'true') {
              sh '''
                curl -v -u $NEXUS_USER:$NEXUS_PASS \
                  --upload-file builds/java-ap.jar \
                  $NEXUS_HOST/repository/maven-releases/com/example/java-ap/${BUILD_NUMBER}/java-ap-${BUILD_NUMBER}.jar
              '''
            }
            if (env.HAS_NODE_CHANGES == 'true') {
              sh '''
                curl -v -u $NEXUS_USER:$NEXUS_PASS \
                  --upload-file builds/node-app.tar.gz \
                  $NEXUS_HOST/repository/raw-hosted/node-app-${BUILD_NUMBER}.tar.gz
              '''
            }
          }
        }
      }
    }

    stage('Build Docker Images') {
      steps {
        script {
          if (env.HAS_JAVA_CHANGES == 'true') {
            docker.build("iamozi2025/java-ap:${IMAGE_TAG}", 'service-backend/java-app')
          }
          if (env.HAS_NODE_CHANGES == 'true') {
            docker.build("iamozi2025/node-app:${IMAGE_TAG}", 'service-backend/node-app')
          }
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
          script {
            sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'

            if (env.HAS_JAVA_CHANGES == 'true') {
              sh 'docker push iamozi2025/java-ap:${BUILD_NUMBER}'
            }
            if (env.HAS_NODE_CHANGES == 'true') {
              sh 'docker push iamozi2025/node-app:${BUILD_NUMBER}'
            }

            sh 'docker logout'
          }
        }
      }
    }

    stage('Helm Deploy to EKS') {
      when {
        expression {
          env.BRANCH_NAME == 'main' || env.GIT_BRANCH == 'origin/main'
        }
      }
      steps {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-credentials-id']]) {
          script {
            // ⬇️ Configure EKS access using AWS CLI (requires Jenkins node with AWS CLI + kubectl + helm installed)
            sh '''
              aws eks --region ${AWS_REGION} update-kubeconfig --name ${EKS_CLUSTER}
            '''

            if (env.HAS_JAVA_CHANGES == 'true') {
              sh '''
                helm upgrade --install java-ap ./my-helm-charts/java-app \
                  --namespace backend --create-namespace \
                  --set image.repository=iamozi2025/java-ap \
                  --set image.tag=${BUILD_NUMBER}
              '''
            }

            if (env.HAS_NODE_CHANGES == 'true') {
              sh '''
                helm upgrade --install node-app ./my-helm-charts/node-app \
                  --namespace backend --create-namespace \
                  --set image.repository=iamozi2025/node-app \
                  --set image.tag=${BUILD_NUMBER}
              '''
            }
          }
        }
      }
    }
  }

  post {
    always {
      cleanWs() // ⬅️ Clean workspace after every run to save disk space
    }
  }
}
