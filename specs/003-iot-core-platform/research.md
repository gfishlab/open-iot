# Research: IoT Platform Core Functionality

**Date**: 2026-03-06
**Feature**: 003-iot-core-platform

## Overview

本文档记录 IoT 平台核心功能实施前的技术研究结果，解决技术上下文中的未知问题，并为实施提供最佳实践指导。

---

## 1. GraalJS 23.1.2 沙箱配置最佳实践

### Decision

使用 **GraalJS Polyglot API** 在 JDK 21 上集成 GraalJS，无需 GraalVM，通过 Maven 依赖引入。

### Rationale

- GraalJS 23.1.2 支持 JDK 21（LTS 版本）
- 无需安装 GraalVM，降低部署复杂度
- Polyglot API 提供完整的沙箱控制能力
- 性能优秀（编译执行模式）

### Implementation

#### Maven 依赖

```xml
<dependency>
    <groupId>org.graalvm.polyglot</groupId>
    <artifactId>polyglot</artifactId>
    <version>23.1.2</version>
</dependency>
<dependency>
    <groupId>org.graalvm.js</groupId>
    <artifactId>js</artifactId>
    <version>23.1.2</version>
    <type>pom</type>
</dependency>
```

#### 沙箱配置

```java
import org.graalvm.polyglot.*;
import java.util.concurrent.*;

public class JavaScriptParser {
    private static final int SCRIPT_TIMEOUT_SECONDS = 3;
    private static final int MAX_SCRIPT_SIZE_KB = 10;

    public Object execute(String script, Map<String, Object> context) {
        // 1. 校验脚本大小
        if (script.length() > MAX_SCRIPT_SIZE_KB * 1024) {
            throw new IllegalArgumentException("Script size exceeds 10KB limit");
        }

        // 2. 配置沙箱环境
        Context.Builder contextBuilder = Context.newBuilder("js")
            .allowHostAccess(HostAccess.NONE)           // 禁止访问 Java 类
            .allowHostClassLookup(className -> false)   // 禁止类查找
            .allowIO(false)                             // 禁止 IO 操作
            .allowNativeAccess(false)                   // 禁止本地方法调用
            .allowCreateThread(false)                   // 禁止创建线程
            .option("js.strict", "true");               // 严格模式

        try (Context ctx = contextBuilder.build()) {
            // 3. 注入上下文变量（只读）
            context.forEach((key, value) -> {
                ctx.getBindings("js").putMember(key, value);
            });

            // 4. 执行脚本（带超时）
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Object> future = executor.submit(() -> {
                return ctx.eval("js", script);
            });

            try {
                return future.get(SCRIPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new RuntimeException("Script execution timeout (3 seconds)");
            } finally {
                executor.shutdownNow();
            }
        }
    }
}
```

#### 性能优化

- **编译缓存**：首次执行时编译为字节码，后续执行直接运行字节码
- **Context 池化**：使用对象池复用 Context（注意：Context 不是线程安全的）
- **预热脚本**：启动时预编译常用脚本

### Alternatives Considered

1. **Nashorn** (JDK 内置，已废弃)
   - ❌ JDK 15+ 移除，不适用于 JDK 21
   - ❌ 性能较差，功能有限

2. **Rhino** (Mozilla)
   - ❌ 性能差，不支持 ES6+
   - ❌ 维护不活跃

3. **GraalVM 完整安装**
   - ❌ 需要替换 JDK，部署复杂
   - ❌ 对学习型项目过重

---

## 2. InfluxDB 2.x 与 Spring Boot 集成最佳实践

### Decision

使用 **InfluxDB Java Client 6.x**（支持 InfluxDB 2.x），配合 Spring Boot 自动配置。

### Rationale

- 官方 Java 客户端，稳定可靠
- 支持 Flux 查询语言（InfluxDB 2.x 专用）
- 支持批量写入和异步写入
- 与 Spring Boot 集成简单

### Implementation

#### Maven 依赖

```xml
<dependency>
    <groupId>com.influxdb</groupId>
    <artifactId>influxdb-client-java</artifactId>
    <version>6.10.0</version>
</dependency>
```

#### 配置

```yaml
# application.yml
influxdb:
  url: http://localhost:8086
  token: ${INFLUXDB_TOKEN}
  org: openiot
  bucket: device-data
  retention-days: 90
```

#### InfluxDBService

```java
import com.influxdb.client.*;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service
public class InfluxDBService {
    private final InfluxDBClient client;
    private final WriteApi writeApi;
    private final QueryApi queryApi;
    private final String bucket;
    private final String org;

    public InfluxDBService(InfluxDBProperties props) {
        this.client = InfluxDBClientFactory.create(
            props.getUrl(),
            props.getToken().toCharArray(),
            props.getOrg(),
            props.getBucket()
        );
        this.writeApi = client.getWriteApi();
        this.queryApi = client.getQueryApi();
        this.bucket = props.getBucket();
        this.org = props.getOrg();

        // 创建 90 天保留策略的 Bucket
        createBucketWithRetentionPolicy(props);
    }

    // 写入设备属性
    public void writeDeviceProperty(Long deviceId, String property, Object value, Instant time) {
        Point point = Point.measurement("device_property")
            .addTag("device_id", deviceId.toString())
            .addTag("property", property)
            .addField("value", value)
            .time(time, WritePrecision.NS);

        writeApi.writePoint(point);
    }

    // 批量写入（异步）
    public void writeDevicePropertiesBatch(List<DevicePropertyData> dataList) {
        List<Point> points = dataList.stream()
            .map(data -> Point.measurement("device_property")
                .addTag("device_id", data.getDeviceId().toString())
                .addTag("property", data.getProperty())
                .addField("value", data.getValue())
                .time(data.getTime(), WritePrecision.NS))
            .collect(Collectors.toList());

        writeApi.writePoints(points);
    }

    // 查询历史数据
    public List<DevicePropertyData> queryHistory(Long deviceId, String property,
                                                   Instant start, Instant stop) {
        String flux = String.format(
            "from(bucket: \"%s\")\n" +
            "  |> range(start: %s, stop: %s)\n" +
            "  |> filter(fn: (r) => r._measurement == \"device_property\")\n" +
            "  |> filter(fn: (r) => r.device_id == \"%d\")\n" +
            "  |> filter(fn: (r) => r.property == \"%s\")",
            bucket, start.toString(), stop.toString(), deviceId, property
        );

        List<FluxRecord> records = queryApi.query(flux);
        return records.stream()
            .map(record -> new DevicePropertyData(
                Long.parseLong((String) record.getValueByKey("device_id")),
                (String) record.getValueByKey("property"),
                record.getValue(),
                record.getTime()
            ))
            .collect(Collectors.toList());
    }

    private void createBucketWithRetentionPolicy(InfluxDBProperties props) {
        // 使用 InfluxDB API 创建 Bucket，设置 90 天保留策略
        // 详见官方文档：https://docs.influxdata.com/influxdb/v2/api/
    }
}
```

#### 90 天保留策略配置

```bash
# 使用 InfluxDB CLI 创建 Bucket
influx bucket create \
  --name device-data \
  --org openiot \
  --retention 90d \
  --token $INFLUXDB_TOKEN
```

### Alternatives Considered

1. **TimescaleDB** (PostgreSQL 扩展)
   - ❌ 需要额外安装扩展，部署复杂
   - ❌ 与 PostgreSQL 共享资源，影响主库性能

2. **Prometheus** (时序数据库)
   - ❌ 主要用于监控指标，不适合设备属性存储
   - ❌ 数据模型不适合设备属性查询

---

## 3. Aviator 5.4.1 表达式引擎使用指南

### Decision

使用 **Aviator 5.4.1** 作为告警规则表达式引擎。

### Rationale

- 轻量级，无依赖，易于集成
- 语法简洁，支持常见运算符和函数
- 性能优秀，支持表达式编译和缓存
- 支持自定义函数

### Implementation

#### Maven 依赖

```xml
<dependency>
    <groupId>com.googlecode.aviator</groupId>
    <artifactId>aviator</artifactId>
    <version>5.4.1</version>
</dependency>
```

#### Aviator 集成

```java
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AlarmEngine {

    static {
        // 注册自定义函数：访问设备属性
        AviatorEvaluator.addFunction(new GetDevicePropertyFunction());
    }

    // 评估告警条件
    public boolean evaluate(String conditionExpression, Long deviceId, Map<String, Object> deviceData) {
        // 编译表达式（带缓存）
        Expression compiledExpression = AviatorEvaluator.compile(conditionExpression, true);

        // 准备环境变量
        Map<String, Object> env = new HashMap<>();
        env.put("deviceId", deviceId);
        env.put("data", deviceData);
        env.put("temperature", deviceData.get("temperature"));  // 便捷访问
        env.put("humidity", deviceData.get("humidity"));

        // 执行表达式
        return (Boolean) compiledExpression.execute(env);
    }

    // 示例表达式：
    // - temperature > 30
    // - temperature > 30 && humidity < 50
    // - data.temperature > 30
    // - getDeviceProperty(deviceId, 'temperature') > 30
}

// 自定义函数：获取设备属性
class GetDevicePropertyFunction extends AbstractFunction {
    @Override
    public String getName() {
        return "getDeviceProperty";
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        Long deviceId = (Long) arg1.getValue(env);
        String propertyName = (String) arg2.getValue(env);

        // 从 env 或数据库获取设备属性
        Map<String, Object> data = (Map<String, Object>) env.get("data");
        return new AviatorLong((Number) data.get(propertyName));
    }
}
```

#### 表达式示例

```java
// 简单条件
"temperature > 30"
"humidity < 50"

// 复合条件
"temperature > 30 && humidity < 50"
"temperature > 30 || humidity > 80"

// 字符串匹配
"status == 'offline'"

// 范围判断
"temperature >= 20 && temperature <= 30"

// 使用自定义函数
"getDeviceProperty(deviceId, 'temperature') > 30"
```

### Alternatives Considered

1. **Spring Expression Language (SpEL)**
   - ❌ 过于复杂，依赖 Spring 上下文
   - ❌ 安全性难以控制

2. **MVEL**
   - ❌ 维护不活跃，版本较旧
   - ❌ 性能不如 Aviator

3. **Drools** (规则引擎)
   - ❌ 过于重量级，学习曲线陡峭
   - ❌ 对于简单表达式过重

---

## 4. Redis pub/sub 实现配置热更新

### Decision

使用 **Redis pub/sub + TTL** 实现解析规则热更新。

### Rationale

- Redis 已集成，无需额外组件
- pub/sub 实时性好，延迟低
- TTL 保证数据新鲜度
- 实现简单，可靠性高

### Implementation

#### 架构

```
[修改解析规则] → [ParseRuleService.update()]
    ↓
[发布消息到 Redis Channel: parse_rule:update]
    ↓
[connect-service 订阅该 Channel]
    ↓
[收到消息 → 清除本地缓存 → 重新加载规则]
    ↓
[新规则在 30 秒内生效]
```

#### ParseRuleService (rule-service)

```java
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ParseRuleService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CHANNEL = "parse_rule:update";

    public void updateParseRule(Long ruleId, ParseRule rule) {
        // 1. 更新数据库
        parseRuleMapper.updateById(rule);

        // 2. 发布更新消息
        Map<String, Object> message = new HashMap<>();
        message.put("ruleId", ruleId);
        message.put("productId", rule.getProductId());
        message.put("timestamp", System.currentTimeMillis());

        redisTemplate.convertAndSend(CHANNEL, message);

        // 3. 清除 Redis 缓存
        String cacheKey = "parse_rule:" + rule.getProductId();
        redisTemplate.delete(cacheKey);
    }
}
```

#### ParseRuleCache (connect-service)

```java
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
public class ParseRuleCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ParseRuleMapper parseRuleMapper;
    private static final String CHANNEL = "parse_rule:update";
    private static final int TTL_MINUTES = 30;

    private final Map<Long, ParseRule> localCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 订阅 Redis Channel
        redisTemplate.getConnectionFactory().getConnection().subscribe(
            (message, pattern) -> {
                Map<String, Object> data = (Map<String, Object>) redisTemplate
                    .getValueSerializer().deserialize(message.getBody());

                Long productId = (Long) data.get("productId");
                localCache.remove(productId);  // 清除本地缓存

                log.info("Parse rule updated for product: {}", productId);
            },
            CHANNEL.getBytes()
        );
    }

    public ParseRule getParseRule(Long productId) {
        // 1. 查询本地缓存
        if (localCache.containsKey(productId)) {
            return localCache.get(productId);
        }

        // 2. 查询 Redis 缓存
        String cacheKey = "parse_rule:" + productId;
        ParseRule rule = (ParseRule) redisTemplate.opsForValue().get(cacheKey);

        if (rule == null) {
            // 3. 查询数据库
            rule = parseRuleMapper.selectByProductId(productId);
            if (rule != null) {
                // 写入 Redis 缓存（30 分钟 TTL）
                redisTemplate.opsForValue().set(cacheKey, rule, TTL_MINUTES, TimeUnit.MINUTES);
            }
        }

        // 4. 写入本地缓存
        if (rule != null) {
            localCache.put(productId, rule);
        }

        return rule;
    }
}
```

### 消息丢失处理

- **客户端重连**：Redis 客户端自动重连
- **TTL 兜底**：即使消息丢失，30 分钟后缓存也会自动失效并重新加载
- **数据库为真实来源**：Redis 缓存失效时从数据库重新加载

### Alternatives Considered

1. **ZooKeeper Watch**
   - ❌ 需要额外组件，部署复杂
   - ❌ 性能不如 Redis

2. **Nacos Config**
   - ❌ Nacos 配置中心主要用于应用配置，不适合频繁变更的解析规则
   - ❌ 实时性不如 pub/sub

---

## 5. 设备认证（DeviceKey + DeviceSecret）安全实践

### Decision

使用 **UUID v4** 生成 DeviceKey 和 DeviceSecret，DeviceSecret 使用 **BCrypt** 哈希存储。

### Rationale

- UUID v4 随机性好，碰撞概率极低
- BCrypt 自动加盐，防止彩虹表攻击
- 验证速度快，安全性高

### Implementation

#### 设备创建时生成密钥对

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.UUID;

@Service
public class DeviceService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Device createDevice(DeviceCreateVO vo) {
        Device device = new Device();
        device.setTenantId(getCurrentTenantId());
        device.setProductId(vo.getProductId());
        device.setDeviceName(vo.getDeviceName());

        // 生成 DeviceKey（UUID v4）
        device.setDeviceKey(UUID.randomUUID().toString());

        // 生成 DeviceSecret（UUID v4）并 BCrypt 哈希存储
        String plainSecret = UUID.randomUUID().toString();
        device.setDeviceSecret(passwordEncoder.encode(plainSecret));

        deviceMapper.insert(device);

        // 返回明文密钥（仅此一次）
        return device;
    }

    // 返回给用户的响应包含明文 DeviceSecret（仅创建时）
    public DeviceVO createDeviceWithSecret(DeviceCreateVO vo) {
        Device device = createDevice(vo);
        DeviceVO response = new DeviceVO(device);
        response.setDeviceSecret(plainSecret);  // 仅此一次返回明文
        return response;
    }
}
```

#### 设备认证

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
public class DeviceAuthService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Device authenticate(String deviceKey, String deviceSecret) {
        // 1. 查询设备
        Device device = deviceMapper.selectByDeviceKey(deviceKey);
        if (device == null) {
            throw new AuthenticationException("Invalid DeviceKey");
        }

        // 2. 验证 DeviceSecret
        if (!passwordEncoder.matches(deviceSecret, device.getDeviceSecret())) {
            throw new AuthenticationException("Invalid DeviceSecret");
        }

        // 3. 检查设备状态
        if (!"1".equals(device.getStatus())) {
            throw new AuthenticationException("Device is disabled");
        }

        return device;
    }
}
```

#### 防重放攻击

```java
import java.time.Instant;

public class DeviceAuthRequest {
    private String deviceKey;
    private String deviceSecret;
    private long timestamp;   // 客户端时间戳（毫秒）
    private String nonce;     // 随机字符串
    private String signature; // 签名 = MD5(deviceKey + timestamp + nonce + deviceSecret)
}

@Service
public class DeviceAuthService {

    private static final long MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000; // 5 分钟

    public Device authenticate(DeviceAuthRequest request) {
        // 1. 校验时间戳（防重放）
        long now = System.currentTimeMillis();
        if (Math.abs(now - request.getTimestamp()) > MAX_TIMESTAMP_DIFF_MS) {
            throw new AuthenticationException("Request expired");
        }

        // 2. 校验 Nonce（防重放，Redis 记录已使用的 Nonce）
        String nonceKey = "device:nonce:" + request.getDeviceKey() + ":" + request.getNonce();
        if (redisTemplate.hasKey(nonceKey)) {
            throw new AuthenticationException("Nonce already used");
        }

        // 3. 校验签名
        Device device = deviceMapper.selectByDeviceKey(request.getDeviceKey());
        String expectedSignature = DigestUtils.md5Hex(
            request.getDeviceKey() +
            request.getTimestamp() +
            request.getNonce() +
            device.getDeviceSecret()
        );

        if (!expectedSignature.equals(request.getSignature())) {
            throw new AuthenticationException("Invalid signature");
        }

        // 4. 记录 Nonce（5 分钟后过期）
        redisTemplate.opsForValue().set(nonceKey, "1", MAX_TIMESTAMP_DIFF_MS, TimeUnit.MILLISECONDS);

        return device;
    }
}
```

#### DeviceSecret 重新生成

```java
public DeviceVO regenerateSecret(Long deviceId) {
    Device device = deviceMapper.selectById(deviceId);

    // 生成新的 DeviceSecret
    String newPlainSecret = UUID.randomUUID().toString();
    device.setDeviceSecret(passwordEncoder.encode(newPlainSecret));
    device.setUpdateTime(LocalDateTime.now());

    deviceMapper.updateById(device);

    // 返回明文密钥（仅此一次）
    DeviceVO response = new DeviceVO(device);
    response.setDeviceSecret(newPlainSecret);
    return response;
}
```

### Alternatives Considered

1. **X.509 证书认证**
   - ❌ 证书管理复杂，设备配置困难
   - ❌ 对于学习型项目过重

2. **JWT Token**
   - ❌ JWT 无法撤销，不适合长期设备认证
   - ❌ 需要额外的 Token 管理机制

3. **明文存储 DeviceSecret**
   - ❌ 安全性差，数据库泄露会导致所有设备被攻击

---

## Summary

| 技术点 | 决策 | 关键优势 |
|--------|------|----------|
| JavaScript 脚本引擎 | GraalJS 23.1.2 + 沙箱 | 无需 GraalVM，沙箱安全，性能优秀 |
| 时序数据库 | InfluxDB 2.x + 90 天保留 | 高性能查询，自动数据过期，与 Grafana 集成良好 |
| 表达式引擎 | Aviator 5.4.1 | 轻量级，易集成，支持自定义函数，性能优秀 |
| 配置热更新 | Redis pub/sub + TTL | 实时性好（30 秒内生效），可靠性高，实现简单 |
| 设备认证 | DeviceKey + DeviceSecret (BCrypt) | 安全性高，防重放攻击，支持密钥轮换 |

所有技术选型均符合项目宪法原则，可进入 Phase 1 设计阶段。
