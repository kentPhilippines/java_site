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
        sudo ln -sfn $(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
    elif [[ -f /etc/debian_version ]]; then
        sudo apt-get update
        # 移除旧版本 Java（如果存在）
        sudo apt-get remove -y openjdk*
        # 安装 JDK 17
        sudo apt-get install -y openjdk-17-jdk
        # 设置 JDK 17 为默认版本
        sudo update-alternatives --set java /usr/lib/jvm/java-17-openjdk-amd64/bin/java
        sudo update-alternatives --set javac /usr/lib/jvm/java-17-openjdk-amd64/bin/javac
    elif [[ -f /etc/redhat-release ]]; then
        # 移除旧版本 Java（如果存在）
        sudo yum remove -y java*
        # 安装 JDK 17
        sudo yum install -y java-17-openjdk-devel
        # 设置 JDK 17 为默认版本
        sudo alternatives --set java java-17-openjdk.x86_64
        sudo alternatives --set javac java-17-openjdk.x86_64
    fi

    # 验证 Java 版本
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$java_version" != "17" ]; then
        echo "错误：Java 版本不是 17，当前版本是 $java_version"
        exit 1
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
        # 配置 Maven 使用 JDK 17
        sudo sh -c 'echo "JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64" >> /etc/maven/mavenrc'
    elif [[ -f /etc/redhat-release ]]; then
        sudo yum install -y maven
        # 配置 Maven 使用 JDK 17
        sudo sh -c 'echo "JAVA_HOME=/usr/lib/jvm/java-17-openjdk" >> /etc/maven/mavenrc'
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







