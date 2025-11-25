pipeline {
    agent any

    tools {
        // Nom de votre Maven installé dans Jenkins
        maven 'Maven-3.9.11'
        // Java doit être installé et configuré si nécessaire
        jdk 'JDK-17'
    }

    environment {
        PROJECT_NAME = "jhipster-sample-app"
    }

    stages {
        stage('Checkout SCM') {
            steps {
                echo "Cloning repository..."
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[url: 'https://github.com/Jaguaarr/jhipster-sample-app']]
                ])
            }
        }

        stage('Compile Project') {
            steps {
                echo "Compiling Maven project..."
                sh 'mvn clean compile'
            }
        }

        stage('Run Tests') {
            steps {
                echo "Running tests..."
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package JAR') {
            steps {
                echo "Generating JAR package..."
                sh 'mvn package'
            }
        }

        stage('SonarQube Analysis') {
            environment {
                SONAR_TOKEN = credentials('sonar-token') // Assurez-vous de configurer ce token dans Jenkins
            }
            steps {
                echo "Running SonarQube Analysis..."
                withSonarQubeEnv('SonarQube') {
                    sh "mvn sonar:sonar -Dsonar.login=$SONAR_TOKEN"
                }
            }
        }

        stage('Quality Gate Check') {
            steps {
                timeout(time: 1, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo "Building Docker image..."
                sh "docker build -t ${PROJECT_NAME}:latest ."
            }
        }

        stage('Run Docker Container') {
            steps {
                echo "Running Docker container..."
                sh "docker run -d --name ${PROJECT_NAME} -p 8080:8080 ${PROJECT_NAME}:latest"
            }
        }

        stage('Start Minikube') {
            steps {
                echo "Starting Minikube..."
                sh 'minikube start'
            }
        }

        stage('Setup Minikube Docker') {
            steps {
                echo "Setting Docker environment for Minikube..."
                sh 'eval $(minikube -p minikube docker-env)'
            }
        }

        stage('Build Docker Image for Kubernetes') {
            steps {
                echo "Building Docker image for Kubernetes..."
                sh "docker build -t ${PROJECT_NAME}:k8s ."
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                echo "Deploying app to Kubernetes..."
                sh "kubectl apply -f k8s/deployment.yaml"
            }
        }

        stage('Expose JHipster App') {
            steps {
                echo "Exposing service..."
                sh "kubectl expose deployment ${PROJECT_NAME} --type=NodePort --port=8080"
            }
        }
    }

    post {
        always {
            echo "Cleaning workspace..."
            deleteDir()
        }
        success {
            echo "Pipeline completed successfully!"
        }
        failure {
            echo "Pipeline failed!"
        }
    }
}
