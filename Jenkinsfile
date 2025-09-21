pipeline {
  agent any

  environment {
    // 你可以改成 main/master 或你实际的分支
    GIT_BRANCH = 'master'
    // Spring Boot 最终 JAR 的名称（下方 Maven 命令将重命名）
    APP_JAR = 'app.jar'
  }

  triggers {
    // 结合 GitHub Webhook 使用（见后文），也可保留轮询兜底
    pollSCM('H/5 * * * *') // 每 5 分钟兜底检测
  }

  options {
    // 保留日志、超时等可按需配置
    timestamps()
  }

  stages {
    stage('Checkout') {
      steps {
        checkout([$class: 'GitSCM',
          branches: [[name: "*/${env.GIT_BRANCH}"]],
          userRemoteConfigs: [[
            url: 'https://github.com/goodboy95/AINovel.git'
          ]]
        ])
      }
    }

    stage('Frontend Build (Node 18)') {
      agent {
        docker {
          // Node 18 LTS，避免污染宿主机
          image 'node:18'
          args '-u root:root' // 允许容器里写文件
        }
      }
      steps {
        dir('frontend') {
          sh '''
            set -e
            npm ci
            npm run build
          '''
        }
      }
    }

    stage('Backend Build (Maven + Java 21)') {
      agent {
        docker {
          image 'maven:3.9-eclipse-temurin-21'
          args '-u root:root'
        }
      }
      steps {
        dir('backend') {
          sh '''
            set -e
            # 如需 -DskipTests 可加上
            mvn -B -DskipTests clean package
            # 将最终 JAR 统一命名为 app.jar，供 docker-compose 映射使用
            JAR_FILE=$(ls target/*.jar | head -n 1)
            cp "$JAR_FILE" target/app.jar
          '''
        }
      }
      post {
        always {
          archiveArtifacts artifacts: 'backend/target/app.jar', fingerprint: true
        }
      }
    }

    stage('Deploy with docker-compose') {
      steps {
        sh '''
          set -e
          docker compose version >/dev/null 2>&1 || docker-compose version >/dev/null 2>&1 || {
            echo "需要 docker compose 或 docker-compose 命令"
            exit 1
          }

          # 优先使用 docker compose（v2），否则回退 docker-compose（v1）
          if command -v docker >/dev/null && docker compose version >/dev/null 2>&1; then
            COMPOSE="docker compose"
          else
            COMPOSE="docker-compose"
          fi

          # 以当前仓库根目录的 docker-compose.yml 为准
          $COMPOSE down || true
          $COMPOSE up -d
          $COMPOSE ps
        '''
      }
    }
  }

  post {
    success {
      echo "部署成功：后端已在宿主机 12345 端口提供服务。"
    }
    failure {
      echo "部署失败：请查看 Jenkins 控制台日志。"
    }
  }
}
