#!/bin/bash

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 应用配置
APP_NAME="java_site"
JAR_FILE="target/${APP_NAME}-0.0.1-SNAPSHOT.jar"
WORK_DIR="/opt/${APP_NAME}"
LOG_DIR="${WORK_DIR}/logs"
JAVA_VERSION="11"

# 检查并安装Java
install_java() {
    echo -e "${YELLOW}开始安装JDK ${JAVA_VERSION}...${NC}"
    
    # 检测系统类型
    if [ -f /etc/debian_version ]; then
        # Debian/Ubuntu系统
        apt-get update
        apt-get install -y openjdk-${JAVA_VERSION}-jdk
    elif [ -f /etc/redhat-release ]; then
        # CentOS/RHEL系统
        yum install -y java-${JAVA_VERSION}-openjdk-devel
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS系统
        if ! command -v brew &> /dev/null; then
            echo -e "${RED}请先安装Homebrew${NC}"
            echo "安装命令: /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
            exit 1
        fi
        brew install openjdk@${JAVA_VERSION}
        sudo ln -sfn $(brew --prefix)/opt/openjdk@${JAVA_VERSION}/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-${JAVA_VERSION}.jdk
    else
        echo -e "${RED}不支持的操作系统${NC}"
        exit 1
    fi
}

# 检查Java环境
check_java() {
    if ! command -v java &> /dev/null; then
        echo -e "${YELLOW}未检测到Java环境，开始安装...${NC}"
        install_java
    fi
    
    # 验证Java版本
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    required_version="${JAVA_VERSION}"
    
    if [[ $java_version == *"$required_version"* ]]; then
        echo -e "${GREEN}Java版本符合要求: $java_version${NC}"
    else
        echo -e "${YELLOW}当前Java版本($java_version)不符合要求(需要版本$required_version)，开始安装...${NC}"
        install_java
        
        # 再次验证
        java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
        if [[ $java_version != *"$required_version"* ]]; then
            echo -e "${RED}Java安装失败，请手动安装JDK ${JAVA_VERSION}${NC}"
            exit 1
        fi
    fi
}

# 创建目录
create_directories() {
    echo -e "${GREEN}创建必要目录...${NC}"
    mkdir -p ${WORK_DIR}/{logs,config,certs,data}
    chmod -R 755 ${WORK_DIR}
    chmod -R 777 ${LOG_DIR}
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
    nohup java -jar ${JAR_FILE} \
        --spring.profiles.active=prod \
        --server.port=9090 \
        > ${LOG_DIR}/app.log 2>&1 &
    
    sleep 5
    if pgrep -f ${APP_NAME} > /dev/null; then
        echo -e "${GREEN}服务启动成功${NC}"
    else
        echo -e "${RED}服务启动失败，请检查日志${NC}"
        tail -n 50 ${LOG_DIR}/app.log
        exit 1
    fi
}

# 创建管理脚本
create_manage_script() {
    cat > ${WORK_DIR}/manage.sh << 'EOF'
#!/bin/bash
case "$1" in
    start)
        ./deploy.sh
        ;;
    stop)
        pid=$(pgrep -f java_site)
        if [ ! -z "$pid" ]; then
            kill $pid
            echo "服务已停止"
        else
            echo "服务未运行"
        fi
        ;;
    restart)
        $0 stop
        sleep 2
        $0 start
        ;;
    status)
        pid=$(pgrep -f java_site)
        if [ ! -z "$pid" ]; then
            echo "服务正在运行，PID: $pid"
        else
            echo "服务未运行"
        fi
        ;;
    log)
        tail -f logs/app.log
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status|log}"
        exit 1
esac
EOF
    chmod +x ${WORK_DIR}/manage.sh
}

# 主函数
main() {
    echo -e "${GREEN}开始部署 ${APP_NAME}${NC}"
    
    # 检查Java环境
    check_java
    
    # 检查JAR文件
    if [ ! -f "${JAR_FILE}" ]; then
        echo -e "${RED}错误: JAR文件不存在 ${JAR_FILE}${NC}"
        exit 1
    fi
    
    # 创建目录
    create_directories
    
    # 复制文件
    cp ${JAR_FILE} ${WORK_DIR}/
    
    # 停止现有服务
    stop_service
    
    # 启动服务
    cd ${WORK_DIR}
    start_service
    
    # 创建管理脚本
    create_manage_script
    
    echo -e "${GREEN}部署完成！${NC}"
    echo -e "${YELLOW}使用 ./manage.sh {start|stop|restart|status|log} 管理服务${NC}"
}

# 执行主函数
main