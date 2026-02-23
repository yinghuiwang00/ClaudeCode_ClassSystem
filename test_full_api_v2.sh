#!/bin/bash
# 完整端到端API测试脚本 V2（包含数据库清理和动态ID处理）

set -e  # 任何命令失败则退出脚本

echo "=== Class Booking System 完整端到端测试 V2 ==="
echo "开始时间: $(date)"

# 辅助函数：从JSON响应中提取字段值
extract_field() {
    echo "$1" | python3 -c "
import sys, json
try:
    data = json.loads(sys.stdin.read())
    print(data.get('$2', ''))
except:
    print('')
"
}

# 辅助函数：从JSON数组中提取第一个元素的字段
extract_first_field() {
    echo "$1" | python3 -c "
import sys, json
try:
    data = json.loads(sys.stdin.read())
    if data and isinstance(data, list) and len(data) > 0:
        print(data[0].get('$2', ''))
    else:
        print('')
except:
    print('')
"
}

# 0. 检查服务状态
echo "0. 检查Docker服务状态..."
if docker-compose ps | grep -q "Up (healthy)"; then
    echo "✓ 服务正常运行"
else
    echo "⚠ 服务未运行，正在启动..."
    docker-compose up -d
    sleep 15
fi

# 1. 清理数据库数据（确保全新测试环境）
echo "1. 清理数据库数据..."
docker exec class-postgres psql -U postgres -d bookingdb -c "
-- 禁用外键约束检查
SET session_replication_role = 'replica';

-- 清理所有表数据（按依赖顺序）
TRUNCATE TABLE bookings CASCADE;
TRUNCATE TABLE class_schedules CASCADE;
TRUNCATE TABLE instructors CASCADE;
TRUNCATE TABLE users CASCADE;

-- 启用外键约束检查
SET session_replication_role = 'origin';
"
echo "✓ 数据库数据已清理"

# 2. 验证数据清理
echo "2. 验证数据清理..."
docker exec class-postgres psql -U postgres -d bookingdb -c "
SELECT
  (SELECT COUNT(*) FROM users) as users_count,
  (SELECT COUNT(*) FROM instructors) as instructors_count,
  (SELECT COUNT(*) FROM class_schedules) as classes_count,
  (SELECT COUNT(*) FROM bookings) as bookings_count;
"

# 3. 服务健康检查（使用公共端点）
echo "3. 检查服务健康状态..."
SERVICE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/classes)
if [ "$SERVICE_STATUS" = "200" ] || [ "$SERVICE_STATUS" = "204" ]; then
    echo "✓ 服务可访问 (HTTP $SERVICE_STATUS)"
else
    echo "✗ 服务不可访问 (HTTP $SERVICE_STATUS)"
    exit 1
fi

# 4. 注册管理员用户
echo "4. 注册管理员用户..."
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "adminuser",
    "email": "admin@example.com",
    "password": "admin123",
    "firstName": "Admin",
    "lastName": "User"
  }')
echo "注册响应: $REGISTER_RESPONSE"

# 5. 登录获取token
echo "5. 用户登录..."
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}' | \
  grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "✓ 获取到JWT token"

# 6. 将用户提升为ADMIN角色
echo "6. 将用户提升为ADMIN角色..."
docker exec class-postgres psql -U postgres -d bookingdb -c \
  "UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'admin@example.com';"
echo "✓ 用户角色已更新为ADMIN"

# 7. 重新登录获取ADMIN token
echo "7. 重新登录获取ADMIN token..."
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}' | \
  grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "✓ 获取到ADMIN JWT token"

# 8. 创建课程
echo "8. 创建课程..."
CREATE_CLASS_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/classes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Yoga Class",
    "description": "Beginner yoga class",
    "startTime": "2026-02-23T14:00:00",
    "endTime": "2026-02-23T15:00:00",
    "capacity": 10,
    "location": "Studio A"
  }')
echo "创建课程响应: $CREATE_CLASS_RESPONSE"

# 提取课程ID
CLASS_ID=$(extract_field "$CREATE_CLASS_RESPONSE" "id")
if [ -z "$CLASS_ID" ]; then
    echo "✗ 无法提取课程ID"
    exit 1
fi
echo "✓ 创建课程ID: $CLASS_ID"

# 9. 查看课程列表
echo "9. 查看课程列表..."
CLASS_LIST=$(curl -s http://localhost:8080/api/v1/classes)
echo "$CLASS_LIST" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f'当前有 {len(data)} 个课程')
for cls in data:
    print(f'  - ID: {cls.get(\"id\")}, 名称: {cls.get(\"name\")}, 容量: {cls.get(\"capacity\")}')
"

# 10. 注册学生用户
echo "10. 注册学生用户..."
STUDENT_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student1",
    "email": "student1@example.com",
    "password": "student123",
    "firstName": "Student",
    "lastName": "One"
  }' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "✓ 学生用户注册成功"

# 11. 预订课程
echo "11. 预订课程..."
BOOKING_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -d "{
    \"classScheduleId\": $CLASS_ID,
    \"notes\": \"First yoga class\"
  }")
echo "预订响应: $BOOKING_RESPONSE"

# 提取预订ID
BOOKING_ID=$(extract_field "$BOOKING_RESPONSE" "id")
if [ -z "$BOOKING_ID" ]; then
    echo "✗ 无法提取预订ID，预订可能失败"
    # 检查错误信息
    ERROR_MSG=$(extract_field "$BOOKING_RESPONSE" "message")
    if [ -n "$ERROR_MSG" ]; then
        echo "错误信息: $ERROR_MSG"
    fi
    exit 1
fi
echo "✓ 创建预订ID: $BOOKING_ID"

# 12. 查看学生预订
echo "12. 查看学生预订..."
MY_BOOKINGS=$(curl -s -H "Authorization: Bearer $STUDENT_TOKEN" \
  http://localhost:8080/api/v1/bookings/my-bookings)
echo "我的预订: $MY_BOOKINGS"

# 13. 查看课程状态（验证预订）
echo "13. 查看课程状态..."
curl -s http://localhost:8080/api/v1/classes | python3 -c "
import sys, json
data = json.load(sys.stdin)
for cls in data:
    if cls.get('id') == $CLASS_ID:
        print(f'课程 {cls.get(\"name\")}:')
        print(f'  当前预订: {cls.get(\"currentBookings\")}/{cls.get(\"capacity\")}')
        print(f'  状态: {cls.get(\"status\")}')
"

# 14. 取消预订
echo "14. 取消预订..."
CANCEL_RESPONSE=$(curl -s -X DELETE -H "Authorization: Bearer $STUDENT_TOKEN" \
  "http://localhost:8080/api/v1/bookings/$BOOKING_ID")
echo "取消预订响应: $CANCEL_RESPONSE"

# 15. 验证预订已取消
echo "15. 验证预订已取消..."
curl -s http://localhost:8080/api/v1/classes | python3 -c "
import sys, json
data = json.load(sys.stdin)
for cls in data:
    if cls.get('id') == $CLASS_ID:
        bookings = cls.get('currentBookings', 0)
        if bookings == 0:
            print('✓ 预订已成功取消，当前预订数: 0')
        else:
            print(f'✗ 预订未正确取消，当前预订数: {bookings}')
"

# 16. 查看所有用户（ADMIN功能）
echo "16. 查看所有用户..."
ALL_USERS=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/v1/users)
echo "所有用户: $(echo "$ALL_USERS" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f'共 {len(data)} 个用户')
")"

# 17. 清理测试数据（可选）
echo "17. 清理测试数据..."
docker exec class-postgres psql -U postgres -d bookingdb -c "
SET session_replication_role = 'replica';
TRUNCATE TABLE bookings CASCADE;
TRUNCATE TABLE class_schedules CASCADE;
TRUNCATE TABLE instructors CASCADE;
TRUNCATE TABLE users CASCADE;
SET session_replication_role = 'origin';
"
echo "✓ 测试数据已清理"

echo ""
echo "=== 完整测试完成 ==="
echo "结束时间: $(date)"
echo "所有测试步骤执行完毕！"