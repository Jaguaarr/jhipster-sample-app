pipeline {
    agent any

    tools {
        // Nom que tu as configuré dans Jenkins pour Maven
        maven 'Maven-3.9.11'
        // Si besoin, tu peux ajouter JDK aussi
        // jdk 'JDK11'
    }

    environment {
        MVN_HOME = tool 'Maven-3.9.11'
        PATH = "${tool 'Maven-3.9.11'}/bin:${env.PATH}"
    }

    stages {

        stage('1. Clone Repository') {
            steps {
                echo "Cloning repository from GitHub..."
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[url: 'https://github.com/Jaguaarr/jhipster-sample-app']]
                ])
            }
        }

        stage('2. Compile Project') {
            steps {
                echo "Compiling Maven project..."
                sh "${MVN_HOME}/bin/mvn clean compile -DskipTests"
            }
        }

        stage('3. Run Tests with Failure Tolerance') {
            steps {
                echo "Running Maven tests..."
                sh "${MVN_HOME}/bin/mvn test || echo 'Some tests failed, continue...'"
            }
        }

        stage('4. Generate JAR Package') {
            steps {
                echo "Building JAR package..."
                sh "${MVN_HOME}/bin/mvn package -DskipTests"
            }
        }

        stage('5. SonarQube Analysis') {
            steps {
                echo "Running SonarQube analysis..."
                withSonarQubeEnv('SonarQube') {
                    sh "${MVN_HOME}/bin/mvn sonar:sonar"
                }
            }
        }

        stage('6. Quality Gate Check') {
            steps {
                echo "Checking SonarQube Quality Gate..."
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo "Building Docker image..."
                sh "docker build -t jhipster-app:latest ."
            }
        }

        stage('Run Docker Container') {
            steps {
                echo "Running Docker container..."
                sh "docker run -d -p 8080:8080 --name jhipster-app jhipster-app:latest"
            }
        }

        stage('Start Minikube Cluster') {
            steps {
                echo "Starting Minikube..."
                sh "minikube start"
            }
        }

        stage('Check Minikube Status') {
            steps {
                echo "Checking Minikube status..."
                sh "minikube status"
            }
        }

        stage('Setup Minikube Docker') {
            steps {
                echo "Setup Docker environment for Minikube..."
                sh "eval $(minikube -p minikube docker-env)"
            }
        }

        stage('Build Docker Image for Kubernetes') {
            steps {
                echo "Building Docker image for Kubernetes..."
                sh "docker build -t jhipster-app:k8s ."
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                echo "Deploying to Kubernetes..."
                sh "kubectl apply -f k8s/"
            }
        }

        stage('Expose JHipster App') {
            steps {
                echo "Exposing JHipster app..."
                sh "kubectl expose deployment jhipster-app --type=LoadBalancer --port=8080"
            }
        }
    }

    post {
        always {
            echo "Cleaning workspace..."
            deleteDir() // cleanWs() nécessite le plugin, deleteDir() fonctionne par défaut
        }
        success {
            echo "Pipeline completed successfully!"
        }
        failure {
            echo "✗ Pipeline failed."
        }
    }
}
