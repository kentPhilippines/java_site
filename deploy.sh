#!/bin/bash

# 设置工作目录
WORK_DIR="/opt/java_site"
JAR_NAME="java_site.jar"

# 检查是否安装了必要的软件
check_requirements() {
    command -v java >/dev/null 2>&1 || { echo "需要安装 Java 但未找到，正在安装..."; install_java; }
    command -v git >/dev/null 2>&1 || { echo "需要安装 Git 但未找到，正在安装..."; install_git; }
    command -v mvn >/dev/null 2>&1 || { echo "需要安装 Maven 但未找到，正在安装..."; install_maven; }
}

# 安装 Java
install_java() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install openjdk@17
    elif [[ -f /etc/debian_version ]]; then
        sudo apt-get update
        sudo apt-get install -y openjdk-17-jdk
    elif [[ -f /etc/redhat-release ]]; then
        sudo yum install -y java-17-openjdk-devel
    fi
}

# 安装 Git
install_git() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install git
    elif [[ -f /etc/debian_version ]]; then
        sudo apt-get update
        sudo apt-get install -y git
    elif [[ -f /etc/redhat-release ]]; then
        sudo yum install -y git
    fi
}

# 安装 Maven
install_maven() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install maven
    elif [[ -f /etc/debian_version ]]; then
        sudo apt-get update
        sudo apt-get install -y maven
    elif [[ -f /etc/redhat-release ]]; then
        sudo yum install -y maven
    fi
}

# 创建工作目录
create_directories() {
    echo "创建工作目录..."
    sudo mkdir -p ${WORK_DIR}
    sudo chown -R $(whoami):$(whoami) ${WORK_DIR}
}

# 拉取源码
clone_source() {
    echo "正在拉取源码..."
    cd ${WORK_DIR}
    if [ ! -d "source" ]; then
        git clone https://github.com/kentPhilippines/java_site.git source
        cd source
    else
        cd source
        git pull
    fi
}

# 构建项目
build_project() {
    echo "正在构建项目..."
    cd ${WORK_DIR}/source
    
    # 检查是否在正确的目录
    if [ ! -f "pom.xml" ]; then
        echo "错误：未找到 pom.xml 文件，请确保在正确的项目目录中"
        exit 1
    fi
    
    # 执行构建
    if mvn clean package -DskipTests; then
        echo "构建成功"
        cp target/*.jar ${WORK_DIR}/${JAR_NAME}
    else
        echo "构建失败"
        exit 1
    fi
}

# 停止旧服务
stop_service() {
    echo "正在停止旧服务..."
    pid=$(ps -ef | grep ${JAR_NAME} | grep -v grep | awk '{print $2}')
    if [ -n "$pid" ]; then
        kill -15 $pid
        sleep 5
    fi
}

# 启动服务
start_service() {
    echo "正在启动服务..."
    cd ${WORK_DIR}
    nohup java -jar ${JAR_NAME} --spring.profiles.active=prod > app.log 2>&1 &
    echo "服务已启动，日志文件：${WORK_DIR}/app.log"
}

# 主函数
main() {
    check_requirements
    create_directories
    clone_source
    build_project
    stop_service
    start_service
}

# 执行主函数
main







