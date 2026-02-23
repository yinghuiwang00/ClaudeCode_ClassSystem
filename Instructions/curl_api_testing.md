# Class Booking System - cURL API 测试指南

本指南提供了使用 cURL 命令行工具测试 Class Booking System REST API 的详细方法。

## 环境准备

确保服务正在运行：
```bash
# 检查服务状态
docker-compose ps

# 如果服务未运行，启动服务
docker-compose up -d

# 等待服务完全启动
sleep 10
```

## 基础测试流程

### 0. 清理数据库数据（可选，确保全新测试环境）

在开始测试前，如果需要确保数据库处于全新状态，可以清理所有数据：

**注意**：本指南默认使用 PostgreSQL 数据库（生产环境配置）。如果使用 H2 内存数据库，数据在应用重启后会自动清除，无需手动清理。

```bash
# 清理所有表数据（PostgreSQL）
# 注意：需要确保容器正在运行且名称为 class-postgres
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

# 验证数据已清理
docker exec class-postgres psql -U postgres -d bookingdb -c "
SELECT
  (SELECT COUNT(*) FROM users) as users_count,
  (SELECT COUNT(*) FROM instructors) as instructors_count,
  (SELECT COUNT(*) FROM class_schedules) as classes_count,
  (SELECT COUNT(*) FROM bookings) as bookings_count;
"

# 或者使用更简单的方法：重启容器并清理数据卷
# docker-compose down -v
# docker-compose up -d
# sleep 10
```

**注意**：清理操作会删除所有现有数据，请在测试环境中谨慎使用。

### 1. 用户注册（公共端点）

```bash
# 注册新用户
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "testuser@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'

# 响应示例：
# {"token":"eyJhbGciOiJ...","type":"Bearer","email":"testuser@example.com","username":"testuser","role":"ROLE_USER"}
```

**重要字段**：
- `username`：必需，3-50字符
- `email`：必需，有效邮箱格式
- `password`：必需，至少6字符
- `firstName`：必需
- `lastName`：必需

### 2. 用户登录（公共端点）

```bash
# 用户登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "password123"
  }'

# 提取JWT token（自动化脚本使用）
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@example.com","password":"password123"}' | \
  grep -o '"token":"[^"]*"' | cut -d'"' -f4)
```

### 3. 查看课程列表（公共端点）

```bash
# 查看所有课程
curl http://localhost:8080/api/v1/classes

# 查看可用课程
curl "http://localhost:8080/api/v1/classes?availableOnly=true"

# 查看特定状态的课程
curl "http://localhost:8080/api/v1/classes?status=SCHEDULED"
```

### 4. 获取当前用户信息（需认证）

```bash
# 使用JWT token获取用户信息
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/users/me

# 查看详细响应头信息
curl -i -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/users/me
```

## 管理员/教练功能测试

### 5. 创建课程（需 ADMIN/INSTRUCTOR 角色）

```bash
# 首先将用户角色更新为ADMIN（通过数据库）
docker exec class-postgres psql -U postgres -d bookingdb -c \
  "UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'testuser@example.com';"

# 重新登录获取新token
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@example.com","password":"password123"}' | \
  grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 创建课程
curl -X POST http://localhost:8080/api/v1/classes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Yoga Class",
    "description": "Beginner yoga class",
    "startTime": "2026-02-23T10:00:00",
    "endTime": "2026-02-23T11:00:00",
    "capacity": 20,
    "location": "Studio A"
  }'
```

**课程字段说明**：
- `name`：课程名称（必需）
- `startTime`：开始时间（必需，必须为未来时间）
- `endTime`：结束时间（必需）
- `capacity`：容量（必需，正整数）
- `instructorId`：可选教练ID

### 6. 更新课程信息

```bash
# 更新课程
curl -X PUT http://localhost:8080/api/v1/classes/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Advanced Yoga",
    "capacity": 25
  }'
```

### 7. 取消课程

```bash
# 取消课程（将状态改为CANCELLED）
curl -X DELETE http://localhost:8080/api/v1/classes/1 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 用户预订功能测试

### 8. 预订课程（需认证）

```bash
# 注册学生用户
STUDENT_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student1",
    "email": "student1@example.com",
    "password": "password123",
    "firstName": "Student",
    "lastName": "One"
  }' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 预订课程
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -d '{
    "classScheduleId": 1,
    "notes": "First yoga class"
  }'
```

### 9. 查看我的预订

```bash
# 查看当前用户的预订
curl -H "Authorization: Bearer $STUDENT_TOKEN" \
  http://localhost:8080/api/v1/bookings/my-bookings
```

### 10. 取消预订

```bash
# 取消预订
curl -X DELETE -H "Authorization: Bearer $STUDENT_TOKEN" \
  http://localhost:8080/api/v1/bookings/1

# 验证预订已取消（查看课程可用位置）
curl http://localhost:8080/api/v1/classes | \
  grep -o '"currentBookings":[0-9]*'
```

## 管理员管理功能

### 11. 查看所有用户（ADMIN only）

```bash
# 获取所有用户列表
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/v1/users
```

### 12. 查看所有预订（ADMIN only）

```bash
# 获取所有预订列表
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/v1/bookings
```

## 综合测试脚本

### 自动化测试流程

```bash
#!/bin/bash
# 自动化API测试脚本

echo "=== Class Booking System API 测试 ==="

# 1. 服务健康检查
echo "1. 检查服务健康状态..."
curl -f http://localhost:8080/actuator/health && echo "✓ 服务健康"

# 2. 注册用户
echo "2. 注册测试用户..."
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "apitest",
    "email": "apitest@example.com",
    "password": "test123",
    "firstName": "API",
    "lastName": "Test"
  }')
echo "注册响应: $(echo $REGISTER_RESPONSE | grep -o '"username":"[^"]*"')"

# 3. 用户登录
echo "3. 用户登录..."
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"apitest@example.com","password":"test123"}' | \
  grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "✓ 获取到JWT token"

# 4. 查看用户信息
echo "4. 查看用户信息..."
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users/me | \
  grep -o '"username":"[^"]*"' && echo "✓ 用户信息获取成功"

# 5. 查看课程列表
echo "5. 查看课程列表..."
CLASS_COUNT=$(curl -s http://localhost:8080/api/v1/classes | \
  python3 -c "import sys,json;print(len(json.load(sys.stdin)))")
echo "当前有 $CLASS_COUNT 个课程"

echo "=== 测试完成 ==="
```

### 完整端到端测试脚本 V2（含数据库清理和动态ID处理）

```bash
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
```

**使用说明**：
1. 保存为脚本文件，例如：`test_full_api_v2.sh`
2. 添加执行权限：`chmod +x test_full_api_v2.sh`
3. 运行脚本：`./test_full_api_v2.sh`
4. 脚本会依次执行所有测试步骤，并在失败时停止

**注意事项**：
- 脚本使用 `set -e`，任何步骤失败都会停止执行
- 需要确保Python3已安装（用于JSON解析）
- 脚本包含辅助函数，可以从JSON响应中动态提取ID
- 脚本会清理并重新创建测试数据，适合用于CI/CD环境或定期功能验证
- 相比V1版本，V2版本动态提取课程ID和预订ID，不依赖硬编码的ID值

## 故障排除

### 常见问题

1. **JWT token无效**
   - 检查token是否过期（默认24小时）
   - 重新登录获取新token
   - 验证token格式：`Bearer <token>`

2. **权限不足**
   - 用户需要特定角色（ADMIN/INSTRUCTOR）才能执行某些操作
   - 通过数据库更新用户角色：
     ```sql
     UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'user@example.com';
     ```

3. **验证错误**
   - 确保请求体包含所有必需字段
   - 检查字段格式（邮箱、日期时间等）
   - 查看API响应中的错误信息

4. **服务未响应**
   - 检查服务是否运行：`docker-compose ps`
   - 查看服务日志：`docker-compose logs class-booking-system`
   - 验证端口8080是否开放

### 调试技巧

```bash
# 查看完整HTTP响应（包括状态码和头部）
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users/me

# 显示详细请求信息
curl -v -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users/me

# 仅显示响应状态码
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/classes

# 使用jq格式化JSON响应（如果已安装jq）
curl -s http://localhost:8080/api/v1/classes | jq .
```

## API端点总结

| 端点 | 方法 | 认证 | 角色 | 描述 |
|------|------|------|------|------|
| `/api/v1/auth/register` | POST | 否 | - | 用户注册 |
| `/api/v1/auth/login` | POST | 否 | - | 用户登录 |
| `/api/v1/users/me` | GET | 是 | 所有 | 获取当前用户信息 |
| `/api/v1/users` | GET | 是 | ADMIN | 获取所有用户 |
| `/api/v1/classes` | GET | 否 | - | 获取课程列表 |
| `/api/v1/classes` | POST | 是 | ADMIN/INSTRUCTOR | 创建课程 |
| `/api/v1/classes/{id}` | PUT | 是 | ADMIN/INSTRUCTOR | 更新课程 |
| `/api/v1/classes/{id}` | DELETE | 是 | ADMIN/INSTRUCTOR | 取消课程 |
| `/api/v1/bookings` | POST | 是 | 所有 | 预订课程 |
| `/api/v1/bookings` | GET | 是 | ADMIN | 获取所有预订 |
| `/api/v1/bookings/my-bookings` | GET | 是 | 所有 | 获取我的预订 |
| `/api/v1/bookings/{id}` | DELETE | 是 | 所有 | 取消预订 |

---

**更新日期**: 2026-02-23
**适用版本**: Class Booking System v1.0.0