#!/bin/bash

# 设置变量
APP_NAME="java_site"
REPO_URL="https://github.com/kentPhilippines/java_site.git"
BRANCH="main"
WORK_DIR="/opt/${APP_NAME}"
LOG_DIR="${WORK_DIR}/logs"
JAR_FILE="${WORK_DIR}/target/${APP_NAME}-0.0.1-SNAPSHOT.jar"
JAVA_VERSION="11"
JAVA_HOME="/opt/java"
MAVEN_VERSION="3.9.5"
MAVEN_HOME="/opt/maven"
MAVEN_DOWNLOAD_URL="https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
MAVEN_TAR="apache-maven-${MAVEN_VERSION}-bin.tar.gz"
MVN_CMD="${MAVEN_HOME}/bin/mvn"

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 清理环境
clean_environment() {
    echo -e "${YELLOW}开始清理环境...${NC}"
    
    # 停止现有服务
    pid=$(pgrep -f ${APP_NAME})
    if [ ! -z "$pid" ]; then
        kill -9 ${pid}
    fi
    
    # 删除工作目录
    if [ -d "${WORK_DIR}" ]; then
        rm -rf ${WORK_DIR}
        echo -e "${GREEN}已删除工作目录${NC}"
    fi
    
    # 删除Java环境
    if [ -d "${JAVA_HOME}" ]; then
        rm -rf ${JAVA_HOME}
        rm -f /etc/profile.d/java.sh
        echo -e "${GREEN}已删除Java环境${NC}"
    fi
    
    # 删除Maven环境
    if [ -d "${MAVEN_HOME}" ]; then
        rm -rf ${MAVEN_HOME}
        rm -f /etc/profile.d/maven.sh
        echo -e "${GREEN}已删除Maven环境${NC}"
    fi
    
    # 清理系统级Java和Maven（根据系统类型）
    if [ -f /etc/debian_version ]; then
        # Debian/Ubuntu系统
        apt-get remove -y openjdk* maven
        apt-get autoremove -y
    elif [ -f /etc/redhat-release ]; then
        # CentOS/RHEL系统
        yum remove -y java-* maven
        yum autoremove -y
    fi
    
    # 刷新环境变量
    source /etc/profile
    
    echo -e "${GREEN}环境清理完成${NC}"
}

# 检查并安装Maven
install_maven() {
    if [ ! -f "${MVN_CMD}" ]; then
        echo -e "${YELLOW}Maven未安装，开始安装...${NC}"
        
        # 创建Maven目录
        mkdir -p ${MAVEN_HOME}
        
        # 下载Maven
        echo -e "${GREEN}下载Maven...${NC}"
        wget "${MAVEN_DOWNLOAD_URL}" -P /tmp/
        
        # 检查下载是否成功
        if [ ! -f "/tmp/${MAVEN_TAR}" ]; then
            echo -e "${RED}Maven下载失败${NC}"
            exit 1
        fi
        
        # 解压Maven
        echo -e "${GREEN}解压Maven...${NC}"
        tar -xzf "/tmp/${MAVEN_TAR}" -C /tmp/
        
        # 检查解压是否成功
        if [ ! -d "/tmp/apache-maven-${MAVEN_VERSION}" ]; then
            echo -e "${RED}Maven解压失败${NC}"
            exit 1
        fi
        
        # 移动文件
        mv "/tmp/apache-maven-${MAVEN_VERSION}"/* ${MAVEN_HOME}/
        
        # 清理临时文件
        rm -f "/tmp/${MAVEN_TAR}"
        rm -rf "/tmp/apache-maven-${MAVEN_VERSION}"
        
        # 配置Maven环境变量
        echo -e "${GREEN}配置Maven环境变量...${NC}"
        echo "export MAVEN_HOME=${MAVEN_HOME}" > /etc/profile.d/maven.sh
        echo 'export PATH=$MAVEN_HOME/bin:$PATH' >> /etc/profile.d/maven.sh
        source /etc/profile.d/maven.sh
        
        # 验证安装
        if [ ! -f "${MVN_CMD}" ]; then
            echo -e "${RED}Maven安装失败${NC}"
            exit 1
        fi
        
        echo -e "${GREEN}Maven安装完成${NC}"
    fi
}

# 检查并安装Java
install_java() {
    echo -e "${YELLOW}JDK未安装，开始安装...${NC}"
    
    # 根据系统类型安装JDK
    if [ -f /etc/debian_version ]; then
        # Debian/Ubuntu系统
        apt-get update
        apt-get install -y openjdk-${JAVA_VERSION}-jdk
        
        # 设置JAVA_HOME
        JAVA_HOME="/usr/lib/jvm/java-${JAVA_VERSION}-openjdk-amd64"
        
    elif [ -f /etc/redhat-release ]; then
        # CentOS/RHEL系统
        yum install -y java-${JAVA_VERSION}-openjdk-devel
        
        # 设置JAVA_HOME
        JAVA_HOME="/usr/lib/jvm/java-${JAVA_VERSION}-openjdk"
    fi
    
    # 验证JAVA_HOME
    if [ ! -d "${JAVA_HOME}" ]; then
        echo -e "${RED}错误: JAVA_HOME目录不存在: ${JAVA_HOME}${NC}"
        exit 1
    fi
    
    # 配置Java环境变量
    echo "export JAVA_HOME=${JAVA_HOME}" > /etc/profile.d/java.sh
    echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/profile.d/java.sh
    source /etc/profile.d/java.sh
    
    # 验证javac
    if ! command -v javac &> /dev/null; then
        echo -e "${RED}错误: javac命令不可用${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}JDK安装完成: $(javac -version)${NC}"
}

# 检查Java环境
check_java() {
    # 检查是否安装了JDK
    if ! command -v javac &> /dev/null; then
        install_java
    fi
    
    # 验证Java版本
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    javac_version=$(javac -version 2>&1)
    
    echo -e "${GREEN}Java版本: $java_version${NC}"
    echo -e "${GREEN}JDK版本: $javac_version${NC}"
    
    # 验证JAVA_HOME
    if [ ! -d "${JAVA_HOME}" ]; then
        echo -e "${RED}错误: JAVA_HOME未正确设置${NC}"
        install_java
    fi
    
    # 再次验证javac
    if ! command -v javac &> /dev/null; then
        echo -e "${RED}错误: JDK安装失败${NC}"
        exit 1
    fi
}

# 检查Maven环境
check_maven() {
    install_maven
    ${MVN_CMD} -v > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        echo -e "${RED}错误: Maven安装或配置失败${NC}"
        exit 1
    fi
    mvn_version=$(${MVN_CMD} -v | head -n 1)
    echo -e "${GREEN}Maven版本: $mvn_version${NC}"
}

# 检查Git环境
check_git() {
    if ! command -v git &> /dev/null; then
        echo -e "${RED}错误: 未安装Git${NC}"
        exit 1
    fi
    git_version=$(git --version)
    echo -e "${GREEN}Git版本: $git_version${NC}"
}

# 拉取代码
fetch_code() {
    echo -e "${GREEN}拉取代码...${NC}"
    
    # 确保工作目录存在
    mkdir -p ${WORK_DIR}
    
    # 如果目录不为空，先清空
    if [ "$(ls -A ${WORK_DIR})" ]; then
        rm -rf ${WORK_DIR}/*
    fi
    
    # 克隆代码
    echo -e "${GREEN}克隆代码: ${REPO_URL}${NC}"
    git clone -b ${BRANCH} ${REPO_URL} ${WORK_DIR}
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}代码拉取失败${NC}"
        echo -e "${RED}请检查：${NC}"
        echo -e "1. 仓库地址是否正确: ${REPO_URL}"
        echo -e "2. 分支名称是否正确: ${BRANCH}"
        echo -e "3. 网络连接是否正常"
        exit 1
    fi
    
    # 切换到工作目录
    cd ${WORK_DIR}
    
    echo -e "${GREEN}代码拉取成功${NC}"
}

# 编译打包
build_code() {
    echo -e "${GREEN}开始编译打包...${NC}"
    cd ${WORK_DIR}
    ${MVN_CMD} clean package -DskipTests
    
    if [ ! -f "${JAR_FILE}" ]; then
        echo -e "${RED}编译打包失败${NC}"
        exit 1
    fi
    echo -e "${GREEN}编译打包完成${NC}"
}

# 创建目录
create_directories() {
    echo -e "${GREEN}创建必要目录...${NC}"
    
    # 创建工作目录和子目录
    mkdir -p ${WORK_DIR}/{logs,config,cache}
    
    # 设置目录权限
    chmod -R 755 ${WORK_DIR}
    
    # 如果使用非root用户运行服务，需要修改目录所有者
    if [ -n "$SUDO_USER" ]; then
        chown -R $SUDO_USER:$SUDO_USER ${WORK_DIR}
    fi
    
    echo -e "${GREEN}目录创建完成: ${WORK_DIR}${NC}"
}

# 停止服务
stop_service() {
    echo -e "${GREEN}停止现有服务...${NC}"
    pid=$(pgrep -f ${APP_NAME})
    if [ ! -z "$pid" ]; then
        kill ${pid}
        sleep 3
        if ps -p ${pid} > /dev/null; then
            kill -9 ${pid}
        fi
    fi
}

# 启动服务
start_service() {
    echo -e "${GREEN}启动服务...${NC}"
    
    # 确保日志目录存在
    mkdir -p ${LOG_DIR}
    
    cd ${WORK_DIR}
    JAVA_CMD="${JAVA_HOME}/bin/java"
    
    # 启动服务
    nohup ${JAVA_CMD} -jar ${JAR_FILE} \
        --spring.profiles.active=prod \
        > ${LOG_DIR}/app.log 2>&1 &

    # 等待服务启动
    echo -e "${GREEN}等待服务启动...${NC}"
    for i in {1..30}; do
        if [ -f "${LOG_DIR}/app.log" ]; then
            echo -e "${GREEN}服务已启动，日志文件已创建${NC}"
            break
        fi
        if [ $i -eq 30 ]; then
            echo -e "${RED}服务启动超时，请检查${NC}"
            exit 1
        fi
        echo -n "."
        sleep 1
    done
    
    # 显示最新日志
    if [ -f "${LOG_DIR}/app.log" ]; then
        echo -e "${GREEN}最新日志内容：${NC}"
        tail -n 50 ${LOG_DIR}/app.log
    else
        echo -e "${RED}警告：日志文件未创建${NC}"
    fi
}

# 创建管理脚本
create_manage_script() {
    cat > ${WORK_DIR}/manage.sh << EOF
#!/bin/bash
MVN_CMD="${MVN_CMD}"

case "\$1" in
    start)
        bash deploy.sh
        ;;
    stop)
        pid=\$(pgrep -f ${APP_NAME})
        if [ ! -z "\$pid" ]; then
            kill \$pid
            echo "服务已停止"
        else
            echo "服务未运行"
        fi
        ;;
    restart)
        bash \$0 stop
        sleep 2
        bash \$0 start
        ;;
    status)
        pid=\$(pgrep -f ${APP_NAME})
        if [ ! -z "\$pid" ]; then
            echo "服务正在运行 (PID: \$pid)"
            echo "内存使用:"
            ps -o pid,ppid,%cpu,%mem,cmd -p \$pid
        else
            echo "服务未运行"
        fi
        ;;
    update)
        cd ${WORK_DIR}
        git pull
        \${MVN_CMD} clean package -DskipTests
        bash \$0 restart
        ;;
    log)
        tail -f ${LOG_DIR}/app.log
        ;;
    *)
        echo "用法: \$0 {start|stop|restart|status|update|log}"
        exit 1
        ;;
esac
EOF
    chmod +x ${WORK_DIR}/manage.sh
}

# 主函数
main() {
    echo -e "${GREEN}开始部署 ${APP_NAME}${NC}"
    check_java
    check_maven
    check_git
    create_directories
    fetch_code
    build_code
    stop_service
    start_service
    create_manage_script
    echo -e "${GREEN}部署完成！${NC}"
    echo -e "${GREEN}使用 ./manage.sh {start|stop|restart|status|update|log} 管理服务${NC}"
}

# 执行主函数
main