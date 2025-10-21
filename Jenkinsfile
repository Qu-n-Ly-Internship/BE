pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'minhp205/internship-be'
        DOCKER_CREDENTIALS_ID = 'dockerhub-credentials'
    }

    stages {
        stage('Checkout') {
            steps {
                echo '🔹 Cloning source code...'
                git branch: 'main', url: 'https://github.com/Qu-n-Ly-Internship/BE.git'
            }
        }

        stage('Build JAR') {
            steps {
                echo '🔹 Building Spring Boot project...'
                // Nếu mvnw chưa được cấp quyền, chạy chmod
                bat '''
                if exist mvnw (
                    echo Running Maven Wrapper...
                    call mvnw clean package -DskipTests
                ) else (
                    echo Running global Maven...
                    mvn clean package -DskipTests
                )
                '''
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                script {
                    // build Docker image
                    def image = docker.build("${DOCKER_IMAGE}:latest")

                    // push lên Docker Hub
                    docker.withRegistry('https://index.docker.io/v1/', "${DOCKER_CREDENTIALS_ID}") {
                        image.push('latest')
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                echo '🚀 Deploying container...'
                bat '''
                docker stop internship-be || echo Container not running
                docker rm internship-be || echo Container not found
                docker run -d -p 8090:8080 --name internship-be minhp205/internship-be:latest
                '''
            }
        }
    }

    post {
        success {
            echo '✅ Deployment successful!'
        }
        failure {
            echo '❌ Build failed!'
        }
    }
}
