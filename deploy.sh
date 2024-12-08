#!/bin/bash

# 设置变量
APP_NAME="java_site"
REPO_URL="https://github.com/kentPhilippines/java_site.git"  # 替换为你的仓库地址
BRANCH="main"  # 替换为你的分支名
WORK_DIR="/opt/java_site"
JAR_FILE="${WORK_DIR}/target/${APP_NAME}-0.0.1-SNAPSHOT.jar"
APP_PORT=443
HTTP_PORT=80
JAVA_VERSION="11"
MAVEN_VERSION="3.8.4"

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 检查是否为root用户
check_root() {
    if [ "$EUID" -ne 0 ]; then
        echo -e "${RED}请使用root权限运行此脚本${NC}"
        echo "使用方法: sudo bash $0"
        exit 1
    fi
}

# 检查并安装必要的工具
install_tools() {
    echo -e "${GREEN}检查并安装必要的工具...${NC}"
    if [ -f /etc/debian_version ]; then
        # Debian/Ubuntu系统
        apt-get update
        apt-get install -y curl wget lsof net-tools
    elif [ -f /etc/redhat-release ]; then
        # CentOS/RHEL系统
        yum install -y curl wget lsof net-tools
    fi
}

# 安装Java
install_java() {
    echo -e "${GREEN}检查Java环境...${NC}"
    if ! command -v java &> /dev/null; then
        echo -e "${YELLOW}未检测到Java，开始安装...${NC}"
        if [ -f /etc/debian_version ]; then
            # Debian/Ubuntu系统
            apt-get update
            apt-get install -y openjdk-${JAVA_VERSION}-jdk
        elif [ -f /etc/redhat-release ]; then
            # CentOS/RHEL系统
            yum install -y java-${JAVA_VERSION}-openjdk-devel
        fi
    fi
    
    # 验证Java安装
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Java安装成功，版本: $java_version${NC}"
    else
        echo -e "${RED}Java安装失败${NC}"
        exit 1
    fi
}

# 配置系统参数
configure_system() {
    echo -e "${GREEN}配置系统参数...${NC}"
    
    # 配置文件描述符限制
    if ! grep -q "* soft nofile 65535" /etc/security/limits.conf; then
        echo "* soft nofile 65535" >> /etc/security/limits.conf
        echo "* hard nofile 65535" >> /etc/security/limits.conf
    fi
    
    # 配置系统参数
    cat > /etc/sysctl.d/99-java-app.conf << EOF
net.ipv4.tcp_fin_timeout = 30
net.ipv4.tcp_keepalive_time = 1200
net.ipv4.tcp_max_syn_backlog = 8192
net.ipv4.tcp_tw_reuse = 1
net.core.somaxconn = 65535
EOF
    sysctl -p /etc/sysctl.d/99-java-app.conf
}

# 创建服务用户
create_service_user() {
    echo -e "${GREEN}创建服务用户...${NC}"
    if ! id -u javaapp &>/dev/null; then
        useradd -m -s /bin/bash javaapp  # 创建用户并创建home目录
        usermod -a -G javaapp javaapp
    fi
    
    # 设置目录权限
    mkdir -p ${WORK_DIR}/{logs,cert,target}
    chown -R javaapp:javaapp ${WORK_DIR}
    chmod -R 755 ${WORK_DIR}
}

# 检查并安装Git
install_git() {
    echo -e "${GREEN}检查Git环境...${NC}"
    if ! command -v git &> /dev/null; then
        echo -e "${YELLOW}未检测到Git，开始安装...${NC}"
        if [ -f /etc/debian_version ]; then
            apt-get update
            apt-get install -y git
        elif [ -f /etc/redhat-release ]; then
            yum install -y git
        fi
    fi
}

# 安装Maven
install_maven() {
    echo -e "${GREEN}检查Maven环境...${NC}"
    if ! command -v mvn &> /dev/null; then
        echo -e "${YELLOW}未检测到Maven，开始安装...${NC}"
        if [ -f /etc/debian_version ]; then
            # Debian/Ubuntu系统
            apt-get update
            apt-get install -y maven
        elif [ -f /etc/redhat-release ]; then
            # CentOS/RHEL系统
            yum install -y maven
        fi
    fi
    
    # 验证Maven安装
    mvn_version=$(mvn -version | head -n 1)
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Maven安装成功: $mvn_version${NC}"
    else
        echo -e "${RED}Maven安装失败${NC}"
        exit 1
    fi
}

# 拉取代码
fetch_code() {
    echo -e "${GREEN}拉取代码...${NC}"
    if [ ! -d "${WORK_DIR}" ]; then
        git clone ${REPO_URL} ${WORK_DIR}
        cd ${WORK_DIR}
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

# 编译代码
build_code() {
    echo -e "${GREEN}编译代码...${NC}"
    cd ${WORK_DIR}
    mvn clean package -DskipTests
    
    if [ ! -f "${JAR_FILE}" ]; then
        echo -e "${RED}编译失败${NC}"
        exit 1
    fi
}

# 主函数
main() {
    echo -e "${GREEN}开始部署 ${APP_NAME}${NC}"
    
    # 检查root权限
    check_root
    
    # 安装基础工具
    install_tools
    
    # 安装Git
    install_git
    
    # 安装Java
    install_java
    
    # 安装Maven
    install_maven
    
    # 拉取代码
    fetch_code
    
    # 编译代码
    build_code
    
    # 配置系统
    configure_system
    
    # 创建服务用户
    create_service_user
    
    # 检查端口占用
    check_port() {
        if lsof -i:$1 > /dev/null 2>&1; then
            echo -e "${YELLOW}警告: 端口 $1 已被占用${NC}"
            read -p "是否释放该端口？(y/n) " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                echo -e "${YELLOW}尝试释放端口...${NC}"
                fuser -k $1/tcp
                sleep 2
            else
                echo -e "${RED}端口 $1 被占用，部署终止${NC}"
                exit 1
            fi
        fi
    }
    
    check_port $APP_PORT
    check_port $HTTP_PORT
    
    # 检查系统资源
    total_mem=$(free -m | awk '/^Mem:/{print $2}')
    if [ $total_mem -lt 1024 ]; then
        echo -e "${YELLOW}警告: 系统内存小于1GB，可能影响服务性能${NC}"
    fi
    
    # 设置JVM参数
    if [ $total_mem -gt 4096 ]; then
        JAVA_OPTS="-Xms1024m -Xmx2048m"
    else
        JAVA_OPTS="-Xms512m -Xmx1024m"
    fi
    
    # 停止现有服务
    pid=$(pgrep -f ${APP_NAME})
    if [ ! -z "$pid" ]; then
        kill ${pid}
        sleep 3
        if ps -p ${pid} > /dev/null; then
            kill -9 ${pid}
        fi
    fi
    
    # 启动服务
    echo -e "${GREEN}启动服务...${NC}"
    cd ${WORK_DIR}
    chown javaapp:javaapp ${JAR_FILE}
    su - javaapp -c "cd ${WORK_DIR} && \
        nohup java $JAVA_OPTS -jar ${JAR_FILE} \
        --server.port=$APP_PORT \
        --server.http.port=$HTTP_PORT \
        --spring.profiles.active=prod \
        > ${WORK_DIR}/logs/app.log 2>&1 &"
    
    # 等待服务启动
    echo -e "${GREEN}等待服务启动...${NC}"
    for i in {1..60}; do  # 增加等待时间到60秒
        if curl -s http://localhost:$HTTP_PORT/actuator/health > /dev/null; then
            echo -e "${GREEN}服务已成功启动${NC}"
            break
        fi
        if [ $i -eq 60 ]; then
            echo -e "${RED}服务启动超时，请检查日志${NC}"
            echo -e "${YELLOW}最后50行日志：${NC}"
            tail -n 50 ${WORK_DIR}/logs/app.log
            exit 1
        fi
        echo -n "."
        sleep 1
    done
    
    # 创建管理脚本和说明文件
    create_management_scripts
    
    echo -e "${GREEN}部署完成！${NC}"
    echo -e "${GREEN}使用说明已保存到 README.txt${NC}"
    echo -e "${GREEN}请执行 ./manage.sh status 查看服务状态${NC}"
}

# 创建管理脚本和说明文件
create_management_scripts() {
    cat > ${WORK_DIR}/manage.sh << 'EOF'
#!/bin/bash
case "$1" in
    deploy)
        sudo bash deploy.sh
        ;;
    update)
        cd /opt/java_site
        git pull
        sudo bash deploy.sh
        ;;
    start)
        sudo bash deploy.sh
        ;;
    stop)
        pid=$(pgrep -f java_site)
        if [ ! -z "$pid" ]; then
            sudo kill $pid
            echo "服务已停止"
        else
            echo "服务未运行"
        fi
        ;;
    restart)
        bash $0 stop
        sleep 2
        bash $0 start
        ;;
    status)
        pid=$(pgrep -f java_site)
        if [ ! -z "$pid" ]; then
            echo "服务正在运行 (PID: $pid)"
            echo "内存使用:"
            ps -o pid,ppid,%cpu,%mem,cmd -p $pid
        else
            echo "服务未运行"
        fi
        ;;
    log)
        tail -f ${WORK_DIR}/logs/app.log
        ;;
    *)
        echo "用法: $0 {deploy|update|start|stop|restart|status|log}"
        exit 1
        ;;
esac
EOF

    chmod +x ${WORK_DIR}/manage.sh
    
    # 创建说明文件
    cat > ${WORK_DIR}/README.txt << EOF
===== ${APP_NAME} 服务管理说明 =====
1. 首次部署：
   sudo bash deploy.sh

2. 日常管理命令：
   更新代码：./manage.sh update
   部署服务：./manage.sh deploy
   启动服务：./manage.sh start
   停止服务：./manage.sh stop
   重启服务：./manage.sh restart
   查看状态：./manage.sh status
   查看日志：./manage.sh log

3. 服务配置：
   部署目录：${WORK_DIR}
   HTTPS端口：$APP_PORT
   HTTP端口：$HTTP_PORT
   日志位置：logs/app.log

4. 系统要求：
   - Linux系统（Ubuntu/CentOS等）
   - 最小内存：1GB
   - Git
   - Java ${JAVA_VERSION}
   - Maven ${MAVEN_VERSION}

5. 注意事项：
   - 首次部署需要root权限
   - 确保端口未被占用
   - 如遇问题，请查看日志文件

6. 目录说明：
   - logs/: 日志目录
   - cert/: 证书目录
   - target/: 编译输出目录

7. 常见问题：
   Q: 服务无法启动
   A: 检查日志文件 logs/app.log

   Q: 端口被占用
   A: 使用 sudo lsof -i:端口号 查看占用进程

   Q: 代码更新失败
   A: 检查Git配置和网络连接

==================
EOF
}

# 执行主函数
main