#!/bin/bash

# 检查参数
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <domain> <email>"
    exit 1
fi

DOMAIN=$1
EMAIL=$2
WEBROOT="/var/lib/letsencrypt/.well-known/acme-challenge"
CERT_DIR="certs/$DOMAIN"

# 创建必要的目录
mkdir -p "$WEBROOT"
mkdir -p "$CERT_DIR"

# 设置权限
chmod -R 755 "$WEBROOT"

# 申请证书
certbot certonly \
    --webroot \
    --non-interactive \
    --agree-tos \
    --email "$EMAIL" \
    --domain "$DOMAIN" \
    --webroot-path "$WEBROOT" \
    --preferred-challenges http \
    --force-renewal \
    --debug-challenges

# 检查certbot执行结果
if [ $? -ne 0 ]; then
    echo "证书申请失败"
    exit 1
fi

# 复制证书文件到应用目录
CERTBOT_DIR="/etc/letsencrypt/live/$DOMAIN"
if [ -d "$CERTBOT_DIR" ]; then
    cp "$CERTBOT_DIR/fullchain.pem" "$CERT_DIR/"
    cp "$CERTBOT_DIR/privkey.pem" "$CERT_DIR/"
    cp "$CERTBOT_DIR/chain.pem" "$CERT_DIR/"
    chmod 644 "$CERT_DIR"/*.pem
    echo "证书文件已复制到 $CERT_DIR"
else
    echo "找不到证书目录: $CERTBOT_DIR"
    exit 1
fi

# 重新加载Nginx配置
nginx -s reload

echo "证书申请完成"
exit 0 