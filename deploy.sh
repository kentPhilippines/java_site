#!/bin/bash

# 设置工作目录
WORK_DIR="/opt/java_site"
JAR_NAME="java_site.jar"
JAVA_HOME="${WORK_DIR}/jdk-17"
MAVEN_HOME="${WORK_DIR}/maven"
PATH="${JAVA_HOME}/bin:${MAVEN_HOME}/bin:$PATH"

# 下载工具函数
download_file() {
    local url=$1
    local output=$2
    if command -v curl >/dev/null 2>&1; then
        curl -L -o "$output" "$url"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$output" "$url"
    else
        echo "需要 curl 或 wget 来下载文件"
        exit 1
    fi
}

# 创建工作目录
create_directories() {
    echo "创建工作目录..."
    sudo mkdir -p ${WORK_DIR}
    sudo chown -R $(whoami):$(whoami) ${WORK_DIR}
}

# 安装 Java
install_java() {
    echo "安装 JDK 17..."
    if [ ! -d "${JAVA_HOME}" ]; then
        cd ${WORK_DIR}
        # 下载 OpenJDK 17
        if [[ "$(uname -m)" == "x86_64" ]]; then
            download_file "https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz" "jdk17.tar.gz"
        else
            download_file "https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-aarch64_bin.tar.gz" "jdk17.tar.gz"
        fi
        tar -xzf jdk17.tar.gz
        rm jdk17.tar.gz
        mv jdk-17* jdk-17
    fi

    # 验证 Java 版本
    ${JAVA_HOME}/bin/java -version
    if [ $? -ne 0 ]; then
        echo "Java 安装失败"
        exit 1
    fi
}

# 安装 Maven
install_maven() {
    echo "安装 Maven..."
    if [ ! -d "${MAVEN_HOME}" ]; then
        cd ${WORK_DIR}
        download_file "https://dlcdn.apache.org/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz" "maven.tar.gz"
        tar -xzf maven.tar.gz
        rm maven.tar.gz
        mv apache-maven-* maven
        
        # 创建 Maven 配置文件
        mkdir -p ${MAVEN_HOME}/conf
        cat > ${MAVEN_HOME}/conf/settings.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <localRepository>${WORK_DIR}/maven/repository</localRepository>
    <mirrors>
        <mirror>
            <id>aliyun</id>
            <mirrorOf>central</mirrorOf>
            <name>Aliyun Maven Mirror</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>
</settings>
EOF
    fi

    # 验证 Maven 安装
    ${MAVEN_HOME}/bin/mvn -version
    if [ $? -ne 0 ]; then
        echo "Maven 安装失败"
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
    
    # 设置 JAVA_HOME 和 MAVEN_HOME
    export JAVA_HOME=${JAVA_HOME}
    export MAVEN_HOME=${MAVEN_HOME}
    export PATH=${JAVA_HOME}/bin:${MAVEN_HOME}/bin:$PATH
    
    # 执行构建
    ${MAVEN_HOME}/bin/mvn clean package -DskipTests \
        -Dmaven.home=${MAVEN_HOME} \
        -Djavax.net.ssl.trustStore=${JAVA_HOME}/lib/security/cacerts
    
    if [ $? -eq 0 ]; then
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
    ${JAVA_HOME}/bin/java -jar ${JAR_NAME} --spring.profiles.active=prod > app.log 2>&1 &
    echo "服务已启动，日志文件：${WORK_DIR}/app.log"
}

# 检查必要的工具
check_requirements() {
    command -v curl >/dev/null 2>&1 || command -v wget >/dev/null 2>&1 || { echo "需要安装 curl 或 wget"; exit 1; }
    command -v tar >/dev/null 2>&1 || { echo "需要安装 tar"; exit 1; }
    command -v git >/dev/null 2>&1 || { echo "需要安装 Git 但未找到，正在安装..."; install_git; }
}

# 主函数
main() {
    check_requirements
    create_directories
    install_java
    install_maven
    clone_source
    build_project
    stop_service
    start_service
}

# 执行主函数
main







