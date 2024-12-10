#!/bin/bash

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 检查root权限
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}请使用root权限运行此脚本${NC}"
    echo "使用方法: sudo bash $0"
    exit 1
fi

echo -e "${YELLOW}开始清理所有环境...${NC}"

# 停止相关服务
echo -e "${GREEN}停止相关服务...${NC}"
pkill -f java_site
pkill -f java
systemctl stop maven || true
systemctl stop nginx || true

# 卸载Java
echo -e "${GREEN}卸载Java...${NC}"
if [ -f /etc/debian_version ]; then
    # Debian/Ubuntu系统
    apt-get remove -y openjdk* java* default-jdk default-jre
    apt-get purge -y openjdk* java* default-jdk default-jre
elif [ -f /etc/redhat-release ]; then
    # CentOS/RHEL系统
    yum remove -y java* 
    yum remove -y *openjdk*
fi

# 卸载Maven
echo -e "${GREEN}卸载Maven...${NC}"
if [ -f /etc/debian_version ]; then
    apt-get remove -y maven
    apt-get purge -y maven
elif [ -f /etc/redhat-release ]; then
    yum remove -y maven
fi

# 卸载Nginx
echo -e "${GREEN}卸载Nginx...${NC}"
if [ -f /etc/debian_version ]; then
    apt-get remove -y nginx
    apt-get purge -y nginx
elif [ -f /etc/redhat-release ]; then
    yum remove -y nginx
fi

# 删除Java相关目录
echo -e "${GREEN}删除Java相关目录...${NC}"
rm -rf /usr/lib/jvm/*
rm -rf /usr/java/*
rm -rf /opt/java
rm -f /etc/profile.d/java.sh
rm -f /etc/alternatives/java
rm -f /etc/alternatives/javac

# 删除Maven相关目录
echo -e "${GREEN}删除Maven相关目录...${NC}"
rm -rf /opt/maven
rm -rf /usr/share/maven
rm -f /etc/profile.d/maven.sh

# 删除Nginx相关目录
echo -e "${GREEN}删除Nginx相关目录...${NC}"
rm -rf /etc/nginx
rm -rf /var/log/nginx
rm -rf /var/cache/nginx
rm -rf /usr/share/nginx
rm -f /etc/systemd/system/nginx.service

# 删除项目相关目录
echo -e "${GREEN}删除项目相关目录...${NC}"
rm -rf /opt/java_site
rm -rf ~/java_site
rm -rf /var/lib/java_site
rm -f /etc/systemd/system/java-site.service

# 清理包管理器缓存
echo -e "${GREEN}清理包管理器缓存...${NC}"
if [ -f /etc/debian_version ]; then
    apt-get clean
    apt-get autoremove -y
elif [ -f /etc/redhat-release ]; then
    yum clean all
    yum autoremove -y
fi

# 清理环境变量
echo -e "${GREEN}清理环境变量...${NC}"
sed -i '/JAVA_HOME/d' /etc/profile
sed -i '/MAVEN_HOME/d' /etc/profile
source /etc/profile

# 重新加载systemd
systemctl daemon-reload

# 验证清理结果
echo -e "${GREEN}验证清理结果...${NC}"
java -version > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${RED}警告: Java仍然存在${NC}"
else
    echo -e "${GREEN}Java已完全删除${NC}"
fi

mvn -v > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${RED}警告: Maven仍然存在${NC}"
else
    echo -e "${GREEN}Maven已完全删除${NC}"
fi

nginx -v > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${RED}警告: Nginx仍然存在${NC}"
else
    echo -e "${GREEN}Nginx已完全删除${NC}"
fi

echo -e "${GREEN}环境清理完成！${NC}"
echo -e "${YELLOW}请重新登录终端以确保环境变量更新${NC}" 