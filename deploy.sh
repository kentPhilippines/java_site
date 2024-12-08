#!/bin/bash

# 设置变量
APP_NAME="java_site"
REPO_URL="https://github.com/kentPhilippines/java_site.git"
BRANCH="main"
WORK_DIR="/opt/${APP_NAME}"
LOG_DIR="${WORK_DIR}/logs"
JAR_FILE="${WORK_DIR}/target/${APP_NAME}-0.0.1-SNAPSHOT.jar"

# Maven相关配置
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

# 检查并安装Maven
install_maven() {
    if [ ! -f "${MVN_CMD}" ]; then
        echo -e "${YELLOW}Maven未安装，开始安装...${NC}"
        
        # 创建Maven目录
        mkdir -p ${MAVEN_HOME}
        
        # 下载Maven
        echo -e "${GREEN}下载Maven...${NC}"
        wget ${MAVEN_DOWNLOAD_URL} -O /tmp/${MAVEN_TAR}
        
        # 解压Maven
        echo -e "${GREEN}解压Maven...${NC}"
        tar -xzf /tmp/${MAVEN_TAR} -C /tmp
        mv /tmp/apache-maven-${MAVEN_VERSION}/* ${MAVEN_HOME}/
        
        # 清理临时文件
        rm -f /tmp/${MAVEN_TAR}
        rm -rf /tmp/apache-maven-${MAVEN_VERSION}
        
        # 配置Maven环境变量
        echo -e "${GREEN}配置Maven环境变量...${NC}"
        echo "export MAVEN_HOME=${MAVEN_HOME}" > /etc/profile.d/maven.sh
        echo 'export PATH=$MAVEN_HOME/bin:$PATH' >> /etc/profile.d/maven.sh
        source /etc/profile.d/maven.sh
        
        echo -e "${GREEN}Maven安装完成${NC}"
    fi
}

# 检查Java环境
check_java() {
    if ! command -v java &> /dev/null; then
        echo -e "${RED}错误: 未安装Java${NC}"
        exit 1
    fi
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo -e "${GREEN}Java版本: $java_version${NC}"
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
    if [ ! -d "${WORK_DIR}" ]; then
        git clone -b ${BRANCH} ${REPO_URL} ${WORK_DIR}
    else
        cd ${WORK_DIR}
        git fetch origin
        git reset --hard origin/${BRANCH}
    fi
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}代码拉取失败${NC}"
        exit 1
    fi
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
    mkdir -p ${WORK_DIR}/{logs,config}
    echo -e "${GREEN}创建工作目录: ${WORK_DIR}${NC}"
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
    cd ${WORK_DIR}
    nohup java -jar ${JAR_FILE} \
        --spring.profiles.active=prod \
        > ${LOG_DIR}/app.log 2>&1 &

    # 等待服务启动
    echo -e "${GREEN}等待服务启动...${NC}"
    for i in {1..30}; do
        if curl -s http://localhost:80/actuator/health > /dev/null; then
            echo -e "${GREEN}服务已成功启动${NC}"
            break
        fi
        if [ $i -eq 30 ]; then
            echo -e "${RED}服务启动超时，请检查日志${NC}"
            tail -n 50 ${LOG_DIR}/app.log
            exit 1
        fi
        echo -n "."
        sleep 1
    done
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