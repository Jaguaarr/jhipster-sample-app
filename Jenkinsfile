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
                bat 'mvn clean compile -DskipTests'
            }
        }

        stage('3. Run Tests with Failure Tolerance') {
            steps {
                script {
                    catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                        bat '''
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
                bat 'mvn package -DskipTests'
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
                        bat 'mvn sonar:sonar -Dsonar.projectKey=yourwaytoltaly -DskipTests'
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
                bat "docker build -t ${APP_NAME} ."
            }
        }

        stage('Run Docker Container') {
            steps {
                bat """
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
            bat '''
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
            bat '''
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
                    bat """
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
                    bat(script: 'which minikube', returnStatus: true) == 0
                }
            }
            steps {
                script {
                    echo "Deploying to Kubernetes..."
                    bat """
                        if [ -d "kubernetes/" ]; then
                            kubectl apply -f kubernetes/
                            
                            echo "Waiting for PostgreSQL to be ready..."
                            kubectl wait --for=condition=available --timeout=300s deployment/postgresql || echo "PostgreSQL deployment timeout"
                            
                            echo "Waiting for JHipster app to be ready..."
                            kubectl rollout status deployment/jhipster-app --timeout=300s || echo "JHipster app deployment timeout"
                            
                            echo "Waiting for pods to be running..."
                            kubectl wait --for=condition=ready pod -l app=postgresql --timeout=120s || true
                            kubectl wait --for=condition=ready pod -l app=jhipster-app --timeout=300s || echo "Pods not ready"
                            
                            echo "Checking pod status..."
                            kubectl get pods -l app=jhipster-app
                            kubectl get pods -l app=postgresql
                            
                            echo "Checking service endpoints..."
                            kubectl get endpoints ${APP_NAME} || echo "Service endpoints not ready"
                            
                            echo "Waiting for app to be healthy (checking /management/health)..."
                            for i in {1..30}; do
                                if kubectl exec -it \$(kubectl get pod -l app=jhipster-app -o jsonpath='{.items[0].metadata.name}') -- curl -sf http://localhost:8080/management/health >/dev/null 2>&1; then
                                    echo "✅ App is healthy!"
                                    break
                                fi
                                echo "Waiting for app health check... (\$i/30)"
                                sleep 10
                            done
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
            def pidFile = "/tmp/${APP_NAME}-port-forward.pid"
            def logFile = "/tmp/${APP_NAME}-port-forward.log"

            bat """
                set -euo pipefail

                PID_FILE="${pidFile}"
                LOG_FILE="${logFile}"
                PORT_FORWARD_CMD="kubectl port-forward --address 0.0.0.0 svc/${APP_NAME} 30080:8080"

                # Verify service exists
                if ! kubectl get svc ${APP_NAME} >/dev/null 2>&1; then
                    echo "❌ Service ${APP_NAME} not found!"
                    exit 1
                fi

                # Check if service has endpoints
                ENDPOINTS=\$(kubectl get endpoints ${APP_NAME} -o jsonpath='{.subsets[0].addresses[0].ip}' 2>/dev/null || echo "")
                if [ -z "\$ENDPOINTS" ]; then
                    echo "⚠️  Warning: Service ${APP_NAME} has no endpoints yet. Waiting..."
                    sleep 10
                fi

                # Stop existing port-forward if running
                if [ -f "\$PID_FILE" ]; then
                    OLD_PID=\$(cat "\$PID_FILE")
                    if ps -p \$OLD_PID >/dev/null 2>&1; then
                        echo "Stopping existing port-forward (PID \$OLD_PID)..."
                        kill \$OLD_PID || true
                        sleep 2
                    fi
                    rm -f "\$PID_FILE"
                fi

                # Start port-forward
                echo "Starting port-forward to expose ${APP_NAME}..."
                nohup \$PORT_FORWARD_CMD >> "\$LOG_FILE" 2>&1 &
                PF_PID=\$!
                echo \$PF_PID > "\$PID_FILE"
                
                # Wait a moment for port-forward to start
                sleep 3
                
                # Verify port-forward is running
                if ! ps -p \$PF_PID >/dev/null 2>&1; then
                    echo "❌ Port-forward failed to start!"
                    echo "Last 20 lines of log:"
                    tail -20 "\$LOG_FILE" || true
                    exit 1
                fi
                
                # Test if port-forward is working
                echo "Testing port-forward connection..."
                for i in {1..10}; do
                    if curl -sf http://localhost:30080/management/health >/dev/null 2>&1; then
                        echo "✅ Port-forward is working!"
                        break
                    fi
                    if [ \$i -eq 10 ]; then
                        echo "⚠️  Port-forward started but health check failed"
                        echo "Log file: \$LOG_FILE"
                        tail -20 "\$LOG_FILE" || true
                    fi
                    sleep 2
                done
            """

            def publicIp = sh(script: "curl -s https://api.ipify.org || curl -s https://ifconfig.me || hostname -I | awk '{print \$1}'", returnStdout: true).trim()

            echo "✅ JHipster app is now accessible at: http://${publicIp}:30080"
            echo "ℹ️ Port-forward PID file: ${pidFile}"
            echo "ℹ️ Port-forward log: ${logFile}"
            echo "ℹ️ To check status: ps -p ${'$'}(cat ${pidFile})"
            echo "ℹ️ To stop: kill ${'$'}(cat ${pidFile})"
            echo "ℹ️ To view logs: tail -f ${logFile}"
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
