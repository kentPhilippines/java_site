#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 配置信息
APP_NAME="java_site"
JAR_NAME="site-0.0.1-SNAPSHOT.jar"
JDK_VERSION="17"
REQUIRED_MEMORY="1024"  # 最小内存要求(MB)
APP_PORT=9099          # 应用端口
WORK_DIR="/opt/${APP_NAME}"  # 工作目录
DATA_DIR="${WORK_DIR}/data"  # 数据目录
LOG_DIR="${WORK_DIR}/logs"   # 日志目录
BACKUP_DIR="${WORK_DIR}/backup"  # 备份目录
TEMP_DIR="${WORK_DIR}/temp"  # 临时目录
PID_FILE="${WORK_DIR}/app.pid"  # PID文件

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查系统环境
check_system() {
    log_info "检查系统环境..."
    
    # 检查操作系统
    if [[ "$(uname)" != "Linux" && "$(uname)" != "Darwin" ]]; then
        log_error "不支持的操作系统: $(uname)"
        exit 1
    fi
    
    # 检查内存
    total_mem=$(free -m | awk '/^Mem:/{print $2}')
    if [ $total_mem -lt $REQUIRED_MEMORY ]; then
        log_error "内存不足: 需要${REQUIRED_MEMORY}MB, 当前${total_mem}MB"
        exit 1
    fi
    
    # 检查磁盘空间
    free_space=$(df -m . | awk 'NR==2 {print $4}')
    if [ $free_space -lt 1024 ]; then
        log_warn "可用磁盘空间不足1GB"
    fi
}

# 检查并安装JDK
check_java() {
    log_info "检查Java环境..."
    
    if type -p java >/dev/null; then
        current_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [ "$current_version" -ge "$JDK_VERSION" ]; then
            log_info "Java版本符合要求: $current_version"
            return
        fi
    fi
    
    log_info "安装JDK ${JDK_VERSION}..."
    if [ "$(uname)" == "Darwin" ]; then
        brew install openjdk@${JDK_VERSION}
    elif [ -f /etc/debian_version ]; then
        apt-get update
        apt-get install -y openjdk-${JDK_VERSION}-jdk
    elif [ -f /etc/redhat-release ]; then
        yum install -y java-${JDK_VERSION}-openjdk
    else
        log_error "不支持的Linux发行版"
        exit 1
    fi
}

# 创建目录结构
create_directories() {
    log_info "创建目录结构..."
    mkdir -p ${WORK_DIR} ${DATA_DIR} ${LOG_DIR} ${BACKUP_DIR} ${TEMP_DIR}
    chmod -R 755 ${WORK_DIR}
}

# 停止旧进程
stop_app() {
    log_info "停止旧进程..."
    if [ -f ${PID_FILE} ]; then
        pid=$(cat ${PID_FILE})
        if kill -0 ${pid} 2>/dev/null; then
            kill ${pid}
            sleep 5
            if kill -0 ${pid} 2>/dev/null; then
                log_warn "进程未能正常停止，强制终止..."
                kill -9 ${pid}
            fi
        fi
        rm -f ${PID_FILE}
    fi
}

# 备份
backup() {
    log_info "创建备份..."
    backup_time=$(date +%Y%m%d_%H%M%S)
    backup_path="${BACKUP_DIR}/${backup_time}"
    mkdir -p ${backup_path}
    
    if [ -f ${WORK_DIR}/${JAR_NAME} ]; then
        cp ${WORK_DIR}/${JAR_NAME} ${backup_path}/
    fi
    
    if [ -d ${DATA_DIR} ]; then
        cp -r ${DATA_DIR} ${backup_path}/
    fi
    
    # 保留最近7天的备份
    find ${BACKUP_DIR} -maxdepth 1 -type d -mtime +7 -exec rm -rf {} \;
    
    log_info "备份完成: ${backup_path}"
}

# 清理旧文件
cleanup() {
    log_info "清理旧文件..."
    find ${LOG_DIR} -type f -name "*.log" -mtime +30 -delete
    find ${TEMP_DIR} -type f -mtime +1 -delete
}

# 部署新版本
deploy() {
    log_info "部署新版本..."
    if [ ! -f ${JAR_NAME} ]; then
        log_error "JAR文件不存在: ${JAR_NAME}"
        exit 1
    fi
    
    cp ${JAR_NAME} ${WORK_DIR}/
    chmod 755 ${WORK_DIR}/${JAR_NAME}
}

# 启动应用
start_app() {
    log_info "启动应用..."
    cd ${WORK_DIR}
    
    # JVM参数
    JAVA_OPTS="-server -Xms512m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_DIR}/heap.dump"
    # GC参数
    JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    # 系统参数
    JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.active=prod -Dserver.port=${APP_PORT}"
    # 日志参数
    JAVA_OPTS="${JAVA_OPTS} -Dlogging.file.path=${LOG_DIR}"
    
    nohup java ${JAVA_OPTS} -jar ${JAR_NAME} > ${LOG_DIR}/startup.log 2>&1 &
    echo $! > ${PID_FILE}
    
    # 等待应用启动
    for i in {1..30}; do
        if curl -s http://localhost:${APP_PORT}/health >/dev/null; then
            log_info "应用启动成功"
            return
        fi
        sleep 2
    done
    log_error "应用启动超时"
    exit 1
}

# 检查应用状态
check_status() {
    if [ -f ${PID_FILE} ]; then
        pid=$(cat ${PID_FILE})
        if kill -0 ${pid} 2>/dev/null; then
            log_info "应用正在运行，PID: ${pid}"
            return 0
        fi
    fi
    log_warn "应用未运行"
    return 1
}

# 显示帮助信息
show_help() {
    echo "用法: $0 {start|stop|restart|status|deploy}"
    echo "  start   - 启动应用"
    echo "  stop    - 停止应用"
    echo "  restart - 重启应用"
    echo "  status  - 查看应用状态"
    echo "  deploy  - 部署新版本"
}

# 主函数
main() {
    case "$1" in
        start)
            check_system
            check_java
            create_directories
            start_app
            ;;
        stop)
            stop_app
            ;;
        restart)
            stop_app
            start_app
            ;;
        status)
            check_status
            ;;
        deploy)
            check_system
            check_java
            create_directories
            stop_app
            backup
            cleanup
            deploy
            start_app
            ;;
        *)
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"