pipeline {
    agent any

    environment {
        // Docker 镜像仓库（可改为 Harbor/阿里云镜像仓库）
        DOCKER_REGISTRY = 'registry.example.com/open-iot'
        IMAGE_TAG = "${BUILD_NUMBER}"

        // 后端服务列表
        BACKEND_SERVICES = 'gateway-service tenant-service device-service connect-service data-service'
    }

    tools {
        maven 'Maven 3.9'
        jdk 'JDK 21'
        nodejs 'NodeJS 18'
    }

    stages {
        stage('Checkout') {
            steps {
                echo '📥 拉取代码...'
                checkout scm
                sh 'git log -1 --pretty=format:"%h - %s (%an)"'
            }
        }

        stage('Build Backend') {
            steps {
                echo '🔨 构建后端服务...'
                dir('backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'backend/*/target/*.jar', fingerprint: true
                }
            }
        }

        stage('Build Frontend') {
            steps {
                echo '🔨 构建前端...'
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Unit Tests') {
            steps {
                echo '🧪 运行单元测试...'
                dir('backend') {
                    sh 'mvn test'
                }
            }
            post {
                always {
                    junit 'backend/*/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                echo '🐳 构建 Docker 镜像...'
                script {
                    // 构建后端服务镜像
                    env.BACKEND_SERVICES.split(' ').each { service ->
                        sh """
                            docker build \
                                -t ${DOCKER_REGISTRY}/${service}:${IMAGE_TAG} \
                                -t ${DOCKER_REGISTRY}/${service}:latest \
                                -f backend/${service}/Dockerfile \
                                backend/${service}
                        """
                    }

                    // 构建前端镜像
                    sh """
                        docker build \
                            -t ${DOCKER_REGISTRY}/frontend:${IMAGE_TAG} \
                            -t ${DOCKER_REGISTRY}/frontend:latest \
                            -f frontend/Dockerfile \
                            frontend
                    """
                }
            }
        }

        stage('Push Images') {
            when {
                branch 'main'
            }
            steps {
                echo '📤 推送镜像到仓库...'
                script {
                    // 登录镜像仓库
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-registry',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh "docker login ${DOCKER_REGISTRY} -u ${DOCKER_USER} -p ${DOCKER_PASS}"
                    }

                    // 推送所有镜像
                    env.BACKEND_SERVICES.split(' ').each { service ->
                        sh "docker push ${DOCKER_REGISTRY}/${service}:${IMAGE_TAG}"
                        sh "docker push ${DOCKER_REGISTRY}/${service}:latest"
                    }

                    sh "docker push ${DOCKER_REGISTRY}/frontend:${IMAGE_TAG}"
                    sh "docker push ${DOCKER_REGISTRY}/frontend:latest"
                }
            }
        }

        stage('Deploy to Dev') {
            when {
                branch 'develop'
            }
            steps {
                echo '🚀 部署到开发环境...'
                sh '''
                    cd infrastructure/docker
                    docker-compose up -d
                '''
            }
        }

        stage('Deploy to Prod') {
            when {
                branch 'main'
            }
            steps {
                echo '🚀 部署到生产环境...'
                input '确认部署到生产环境?'
                sh '''
                    cd infrastructure/docker
                    docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
                '''
            }
        }
    }

    post {
        always {
            echo '🧹 清理工作空间...'
            cleanWs()
        }
        success {
            echo '✅ 流水线执行成功!'
        }
        failure {
            echo '❌ 流水线执行失败!'
        }
    }
}
