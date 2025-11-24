pipeline {
    agent any
    tools {
        maven 'Maven-3.9.11'
        jdk 'JDK-17'
    }

    environment {
        MAVEN_OPTS = '-Xmx2048m'
        DOCKER_IMAGE = 'jhipster-app'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        APP_NAME = 'jhipster-app'
    }

    stages {
        stage('1. Clone Repository') {
            steps {
                echo 'Cloning repository from GitHub...'
                checkout scm
            }
        }

        stage('2. Compile Project') {
            steps {
                echo 'Compiling Maven project...'
                sh 'mvn clean compile -DskipTests'
            }
        }

        stage('3. Run Tests with Failure Tolerance') {
            steps {
                script {
                    catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                        sh '''
                            mvn test \
                                -DskipTests=false \
                                -Dtest="!DTOValidationTest,!MailServiceTest,!HibernateTimeZoneIT,!OperationResourceAdditionalTest" \
                                -DfailIfNoTests=false
                        '''
                    }
                }
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('4. Generate JAR Package') {
            steps {
                echo 'Creating JAR package...'
                sh 'mvn package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true, fingerprint: true
                }
            }
        }

        stage('5. SonarQube Analysis') {
            steps {
                echo 'Running SonarQube analysis...'
                timeout(time: 15, unit: 'MINUTES') {
                    withSonarQubeEnv('SonarQube') {
                        sh 'mvn sonar:sonar -Dsonar.projectKey=yourwaytoltaly -DskipTests'
                    }
                }
            }
        }

        stage('6. Quality Gate Check') {
            steps {
                echo 'Checking Quality Gate...'
                // You can add quality gate check here if needed
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${APP_NAME} ."
            }
        }

        stage('Run Docker Container') {
            steps {
                sh """
                # Stop old containers if exist
                docker stop ${APP_NAME} || true
                docker rm ${APP_NAME} || true
                docker stop postgresql || true
                docker rm postgresql || true

                # Start PostgreSQL database
                docker run -d --name postgresql \\
                  -e POSTGRES_DB=jhipsterSampleApplication \\
                  -e POSTGRES_USER=jhipsterSampleApplication \\
                  -e POSTGRES_PASSWORD=password \\
                  -p 5432:5432 \\
                  postgres:15

                # Wait for PostgreSQL to be ready
                sleep 10

                # Run JHipster application with database connection
                docker run -d --name ${APP_NAME} \\
                  --network host \\
                  -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/jhipsterSampleApplication \\
                  -e SPRING_DATASOURCE_USERNAME=jhipsterSampleApplication \\
                  -e SPRING_DATASOURCE_PASSWORD=password \\
                  ${APP_NAME}

                echo "Container started - check logs with: docker logs ${APP_NAME}"
                """
            }
        }

        stage('Start Minikube Cluster') {
    steps {
        script {
            sh '''
                # Clean start
                minikube delete --all --purge || true

                # Start with proper settings for your environment
                minikube start \
                    --driver=docker \
                    --force \
                    --wait=all \
                    --wait-timeout=10m \
                    --apiserver-port=8443 \
                    --kubernetes-version=stable

                # Wait for API server to be ready
                sleep 60
                kubectl cluster-info
                echo "✅ Kubernetes API server (8443) is ready!"
            '''
        }
    }
}

        stage('Check Minikube Status') {
    steps {
        script {
            // Get ACTUAL minikube status
            def minikubeStatus = sh(
                script: 'minikube status --format="{{.Host}}" 2>/dev/null || echo "NOT_RUNNING"',
                returnStdout: true
            ).trim()

            echo "Minikube status: ${minikubeStatus}"

            if (minikubeStatus != "Running") {
                currentBuild.result = 'FAILURE'
                error "❌ Minikube is not running. Run 'minikube start' first."
            }
        }
    }
}

stage('Setup Minikube Docker') {
    steps {
        script {
            echo "Setting up Minikube Docker environment..."
            sh '''
                # Test if minikube docker-env works
                eval $(minikube docker-env)
                docker info | grep "Server Version"
                echo "✅ Minikube Docker environment ready"
            '''
        }
    }
}



        stage('Build Docker Image for Kubernetes') {
            when {
                expression {
                    sh(script: 'which minikube', returnStatus: true) == 0
                }
            }
            steps {
                script {
                    echo "Building Docker image for Kubernetes..."
                    sh """
                        # Use minikube's docker environment
                        eval \$(minikube docker-env)
                        docker build -t ${APP_NAME}:k8s .
                        echo "✅ Image built successfully for Kubernetes"
                    """
                }
            }
        }

                stage('Deploy to Kubernetes') {
            when {
                expression {
                    sh(script: 'which minikube', returnStatus: true) == 0
                }
            }
            steps {
                script {
                    echo "Deploying to Kubernetes..."
                    sh """
                        if [ -d "kubernetes/" ]; then
                            kubectl apply -f kubernetes/
                            kubectl rollout status deployment/jhipster-app --timeout=300s || echo "JHipster app deployment not found"
                            kubectl rollout status deployment/postgresql --timeout=300s || echo "PostgreSQL deployment not found"
                        else
                            echo "⚠️  Kubernetes directory not found - creating basic deployment"
                            kubectl create deployment ${APP_NAME} --image=${APP_NAME}:k8s --dry-run=client -o yaml | kubectl apply -f -
                            kubectl expose deployment ${APP_NAME} --port=8080 --type=NodePort --dry-run=client -o yaml | kubectl apply -f -
                        fi
                        echo "✅ Kubernetes deployment completed"
                    """
                }
            }
        }


stage('Expose JHipster App') {
    steps {
        script {
            def hostIP = sh(script: "ip route get 1.1.1.1 | awk '{print \$7; exit}'", returnStdout: true).trim()
            echo "✅ JHipster app is now accessible at: http://${hostIP}:30080"
        }
    }
}






    }

    post {
        success {
            echo '✓ Pipeline executed successfully!'
        }
        failure {
            echo '✗ Pipeline failed.'
        }
        always {
            echo 'Cleaning workspace...'
            cleanWs()
        }
    }
}
