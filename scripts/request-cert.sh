#!/bin/bash

# 检查参数
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <domain>"
    exit 1
fi

DOMAIN=$1
EMAIL="admin@example.com"  # 默认邮箱
WEBROOT="/var/lib/letsencrypt/.well-known/acme-challenge"
CERT_DIR="certs/$DOMAIN"

# 创建必要的目录
mkdir -p "$WEBROOT"
mkdir -p "$CERT_DIR"

# 设置权限
chmod -R 755 "$WEBROOT"

# 运行certbot并捕获输出
certbot_output=$(certbot certonly \
    --webroot \
    --non-interactive \
    --agree-tos \
    --email "$EMAIL" \
    --domain "$DOMAIN" \
    --webroot-path "$WEBROOT" \
    --preferred-challenges http \
    --force-renewal \
    --debug-challenges 2>&1)

echo "$certbot_output"

# 检查是否包含验证错误
if echo "$certbot_output" | grep -q "Expected"; then
    # 提取期望的验证内容
    expected_content=$(echo "$certbot_output" | grep "Expected" | sed 's/.*Expected "\([^"]*\)".*/\1/')
    token=$(echo "$expected_content" | cut -d'.' -f1)
    
    if [ ! -z "$token" ] && [ ! -z "$expected_content" ]; then
        echo "写入验证文件: $WEBROOT/$token"
        echo "$expected_content" > "$WEBROOT/$token"
        chmod 644 "$WEBROOT/$token"
        
        # 重新运行certbot
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
    fi
fi

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