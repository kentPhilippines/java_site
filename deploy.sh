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

# 添加Nginx相关变量
NGINX_CONF_DIR="${WORK_DIR}/nginx/conf"
NGINX_LOGS_DIR="${WORK_DIR}/nginx/logs"
NGINX_TEMP_DIR="${WORK_DIR}/nginx/temp"
NGINX_CERTS_DIR="${WORK_DIR}/certs"

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
        apt-get remove -y openjdk* maven nginx
        apt-get autoremove -y
    elif [ -f /etc/redhat-release ]; then
        # CentOS/RHEL系统
        yum remove -y java-* maven nginx
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

# 检查并安装Git
check_and_install_git() {
    if ! command -v git &> /dev/null; then
        echo -e "${YELLOW}Git未安装，正在安装...${NC}"
        
        # 检测操作系统类型
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            if command -v brew &> /dev/null; then
                brew install git
            else
                echo -e "${RED}请先安装Homebrew，然后重新运行此脚本${NC}"
                echo "安装Homebrew命令: /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
                exit 1
            fi
        elif [[ -f /etc/debian_version ]]; then
            # Debian/Ubuntu
            sudo apt-get update
            sudo apt-get install -y git
        elif [[ -f /etc/redhat-release ]]; then
            # CentOS/RHEL
            sudo yum install -y git
        else
            echo -e "${RED}不支持的操作系统，请手动安装Git${NC}"
            exit 1
        fi
        
        # 验证安装
        if ! command -v git &> /dev/null; then
            echo -e "${RED}Git安装失败${NC}"
            exit 1
        fi
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
    mkdir -p ${LOG_DIR}
    
    # 创建Nginx相关目录
    mkdir -p ${NGINX_CONF_DIR}/conf.d
    mkdir -p ${NGINX_LOGS_DIR}
    mkdir -p ${NGINX_TEMP_DIR}
    mkdir -p ${NGINX_CERTS_DIR}
    
    # 设置目录权限
    chmod -R 755 ${WORK_DIR}
    chmod -R 777 ${LOG_DIR}  # 确保日志目录可写
    
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
    
    # 停止Nginx服务
    if command -v nginx &> /dev/null; then
        echo -e "${GREEN}停止Nginx服务...${NC}"
        systemctl stop nginx || true
    fi
}

# 启动服务
start_service() {
    echo -e "${GREEN}启动服务...${NC}"
    
    # 确保日志目录存在
    mkdir -p ${LOG_DIR}
    chmod -R 777 ${LOG_DIR}
    
    # 启动Java应用
    nohup java -jar ${JAR_FILE} \
        --spring.profiles.active=prod \
        > ${LOG_DIR}/app.log 2>&1 &
    
    # 等待Java应用启动
    echo -e "${GREEN}等待Java应用启动...${NC}"
    sleep 10
    
    # 启动Nginx
    echo -e "${GREEN}启动Nginx...${NC}"
    nginx -c /opt/java_site/nginx/conf/nginx.conf
    
    # 检查Nginx状态
    if pgrep nginx > /dev/null; then
        echo -e "${GREEN}Nginx已成功启动${NC}"
        # 检查端口监听状态
        if netstat -tlpn | grep -q ':80.*nginx'; then
            echo -e "${GREEN}Nginx成功监听80端口${NC}"
        else
            echo -e "${RED}警告: Nginx未能监听80端口${NC}"
            echo -e "${YELLOW}检查Nginx错误日志...${NC}"
            tail -n 50 /opt/java_site/nginx/logs/error.log
        fi
    else
        echo -e "${RED}Nginx启动失败${NC}"
        echo -e "${YELLOW}检查Nginx错误日志...${NC}"
        tail -n 50 /opt/java_site/nginx/logs/error.log
    fi
    
    # 显示Nginx进程状态
    ps aux | grep nginx
    
    # 显示端口监听状态
    netstat -tlpn | grep nginx
}

# 配置Nginx
setup_nginx() {
    echo -e "${GREEN}配置Nginx...${NC}"
    
    # 安装Nginx
    if [ -f /etc/debian_version ]; then
        apt-get update
        apt-get install -y nginx
    elif [ -f /etc/redhat-release ]; then
        yum install -y nginx
    fi
    
    # 创建Nginx目录结构
    mkdir -p /opt/java_site/nginx/{conf,logs,temp}
    
    # 复制配置文件
    cp -r nginx/conf/* /opt/java_site/nginx/conf/
    cp nginx/conf/conf.d/default.conf /opt/java_site/nginx/conf/conf.d/
    
    # 如果mime.types不存在，从系统复制一份
    if [ ! -f /opt/java_site/nginx/conf/mime.types ]; then
        if [ -f /etc/nginx/mime.types ]; then
            cp /etc/nginx/mime.types /opt/java_site/nginx/conf/mime.types
        fi
    fi
    
    # 设置正确的权限
    chown -R www-data:www-data /opt/java_site/nginx
    chmod -R 755 /opt/java_site/nginx
    chmod -R 700 /opt/java_site/nginx/conf
    
    # 设置证书目录权限
    mkdir -p /opt/java_site/certs
    chown -R www-data:www-data /opt/java_site/certs
    chmod -R 755 /opt/java_site/certs
    
    # 确保nginx用户可以访问
    usermod -aG www-data nginx || true
    chmod -R g+rx /opt/java_site/certs
    
    # 停止已运行的Nginx
    systemctl stop nginx || true
    killall nginx || true
    
    # 创建Nginx服务配置
    cat > /etc/systemd/system/nginx.service << EOF
[Unit]
Description=nginx - high performance web server
Documentation=https://nginx.org/en/docs/
After=network-online.target remote-fs.target nss-lookup.target
Wants=network-online.target

[Service]
Type=forking
PIDFile=/opt/java_site/nginx/logs/nginx.pid
ExecStartPre=/usr/sbin/nginx -t -c /opt/java_site/nginx/conf/nginx.conf
ExecStart=/usr/sbin/nginx -c /opt/java_site/nginx/conf/nginx.conf
ExecReload=/bin/kill -s HUP \$MAINPID
ExecStop=/bin/kill -s TERM \$MAINPID
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF

    # 重新加载systemd
    systemctl daemon-reload
    
    # 删除默认的Nginx配置
    rm -f /etc/nginx/sites-enabled/default
    rm -f /etc/nginx/conf.d/default.conf
    
    # 创建软链接
    ln -sf /opt/java_site/nginx/conf/nginx.conf /etc/nginx/nginx.conf
    
    # 测试配置
    nginx -t -c /opt/java_site/nginx/conf/nginx.conf
    
    echo -e "${GREEN}Nginx配置完成${NC}"
}

# 检查并安装certbot
check_and_install_certbot() {
    echo -e "${YELLOW}检查certbot...${NC}"
    if ! command -v certbot &> /dev/null; then
        echo -e "${YELLOW}certbot未安装，开始安装...${NC}"
        if [ -f /etc/debian_version ]; then
            # Debian/Ubuntu系统
            apt-get update
            apt-get install -y certbot
        elif [ -f /etc/redhat-release ]; then
            # CentOS/RHEL系统
            yum install -y epel-release
            yum install -y certbot
        else
            echo -e "${RED}警告: 不支持的操作系统，请手动安装certbot${NC}"
            return 1
        fi
        
        # 验证安装
        if ! command -v certbot &> /dev/null; then
            echo -e "${RED}警告: certbot安装失败${NC}"
            return 1
        fi
        echo -e "${GREEN}certbot安装成功${NC}"
    else
        echo -e "${GREEN}certbot已安装${NC}"
    fi
    return 0
}

# 主函数
main() {
    echo -e "${GREEN}开始部署 ${APP_NAME}${NC}"
    check_java
    check_maven
    check_and_install_git
    check_and_install_certbot
    create_directories
    fetch_code
    build_code
    setup_nginx
    stop_service
    start_service
    echo -e "${GREEN}部署完成！${NC}"
    echo -e "${YELLOW}Java应用运行在9090端口${NC}"
    echo -e "${YELLOW}Nginx运行在80和443端口${NC}"
}

# 执行主函数
main