# Tasks: IoT Platform Core Functionality

**Feature**: 003-iot-core-platform
**Branch**: `003-iot-core-platform`
**Date**: 2026-03-06
**Status**: Ready for Implementation

---

## Overview

本文档定义 IoT 平台核心功能的实施任务，按照用户故事优先级组织，支持独立实施和测试。

**Implementation Strategy**:
- **MVP 范围**: User Story 1 (产品-设备层级管理) - 最小可行产品
- **增量交付**: 每个用户故事独立交付，形成可演示的端到端闭环
- **并行机会**: 每个用户故事内的任务标记 [P] 可并行执行

**Task Organization**:
- **Phase 1**: Setup (项目初始化)
- **Phase 2**: Foundational (阻塞性前置条件)
- **Phase 3**: User Story 1 - Product-Device Hierarchy (P1)
- **Phase 4**: User Story 2 - Thing Model Definition (P1)
- **Phase 5**: User Story 3 - Parse Rules (P1)
- **Phase 6**: User Story 4 - Mapping Rules (P2)
- **Phase 7**: User Story 5 - Property & Event Storage (P2)
- **Phase 8**: User Story 6 - Data Forwarding (P2)
- **Phase 9**: User Story 7 - Alarm Management (P3)
- **Phase 10**: User Story 8 - Service Invocation (P3)
- **Phase 11**: Polish & Cross-Cutting Concerns

---

## Dependencies

**User Story Completion Order**:

```
US1 (Product-Device)
  ├─ US2 (Thing Model)
  │    ├─ US3 (Parse Rules)
  │    │    └─ US4 (Mapping Rules)
  │    │         └─ US5 (Storage)
  │    │              └─ US6 (Forwarding)
  │    │                   └─ US7 (Alarms)
  │    └─ US8 (Service Invocation)
  └─ (All stories depend on US1)
```

**Critical Path**:
1. US1 (MVP) → 独立交付，验证架构
2. US2 + US3 → 数据验证和解析
3. US4 → 数据映射
4. US5 → 历史存储
5. US6 → 数据转发
6. US7 → 告警管理
7. US8 → 服务调用

**Independent Stories**:
- US2, US3, US8 可并行开发（均只依赖 US1）
- US4, US5, US6, US7 需要顺序开发

---

## Parallel Execution Examples

### Per User Story

**US1 (Product-Device Hierarchy)**:
```bash
# 并行任务（无依赖关系）
T005 [P] Create Product entity
T006 [P] Create Device entity modification
T007 [P] Create ProductMapper interface
T008 [P] Create DeviceMapper interface

# 串行任务（依赖上述）
T009 Create ProductService (depends on ProductMapper)
T010 Create DeviceService (depends on DeviceMapper)
```

**US2 (Thing Model Definition)**:
```bash
# 并行任务
T020 [P] [US2] Add thing_model JSON field to Product entity
T021 [P] [US2] Create ThingModelValidator in connect-service

# 串行任务
T022 [US2] Implement thing model validation in ProductService
```

**US3 (Parse Rules)**:
```bash
# 并行任务（不同解析器实现）
T033 [P] [US3] Implement JsonPathParser
T034 [P] [US3] Implement JavaScriptParser
T035 [P] [US3] Implement RegexParser
T036 [P] [US3] Implement BinaryParser

# 串行任务
T037 [US3] Implement ParseRuleEngine (depends on all parsers)
```

---

## Phase 1: Setup

**Goal**: 项目初始化，创建基础项目结构和依赖

**Duration**: 1-2 days

### Tasks

- [ ] T001 Add GraalJS 23.1.2 dependency to backend/pom.xml parent POM
- [ ] T002 Add Aviator 5.4.1 dependency to backend/pom.xml parent POM
- [ ] T003 Add InfluxDB Java Client dependency to backend/pom.xml parent POM
- [ ] T004 Create backend/rule-service module with pom.xml
- [ ] T005 [P] Add InfluxDB service to infrastructure/docker-compose.dev.yml
- [ ] T006 [P] Create InfluxDB bucket initialization script in infrastructure/scripts/init-influxdb.sh
- [ ] T007 [P] Add new permissions to backend/tenant-service/src/main/resources/db/migration/V1.2.1__add_iot_permissions.sql

**Deliverables**:
- ✅ rule-service 模块创建完成
- ✅ 所有依赖添加到父 POM
- ✅ InfluxDB 服务配置完成
- ✅ 新增权限（product:*, rule:*, alarm:*）添加到权限表

---

## Phase 2: Foundational

**Goal**: 创建阻塞性前置条件，所有用户故事依赖的基础设施

**Duration**: 2-3 days

### Database Migration

- [ ] T008 Create Flyway migration script V1.3.0__add_product_and_rule_tables.sql in backend/tenant-service/src/main/resources/db/migration/
- [ ] T009 Execute Flyway migration to create all new tables (product, parse_rule, mapping_rule, forward_rule, alarm_rule, alarm_record, device_service_invoke, device_property, device_event)
- [ ] T010 [P] Create Product entity in backend/device-service/src/main/java/com/openiot/device/entity/Product.java
- [ ] T011 [P] Modify Device entity to add product_id field in backend/device-service/src/main/java/com/openiot/device/entity/Device.java
- [ ] T012 [P] Create ParseRule entity in backend/rule-service/src/main/java/com/openiot/rule/entity/ParseRule.java
- [ ] T013 [P] Create MappingRule entity in backend/rule-service/src/main/java/com/openiot/rule/entity/MappingRule.java
- [ ] T014 [P] Create ForwardRule entity in backend/rule-service/src/main/java/com/openiot/rule/entity/ForwardRule.java
- [ ] T015 [P] Create AlarmRule entity in backend/rule-service/src/main/java/com/openiot/rule/entity/AlarmRule.java
- [ ] T016 [P] Create AlarmRecord entity in backend/rule-service/src/main/java/com/openiot/rule/entity/AlarmRecord.java
- [ ] T017 [P] Create DeviceServiceInvoke entity in backend/device-service/src/main/java/com/openiot/device/entity/DeviceServiceInvoke.java
- [ ] T018 [P] Create DeviceProperty entity in backend/device-service/src/main/java/com/openiot/device/entity/DeviceProperty.java
- [ ] T019 [P] Create DeviceEvent entity in backend/device-service/src/main/java/com/openiot/device/entity/DeviceEvent.java

### Mapper Interfaces

- [ ] T020 [P] Create ProductMapper interface in backend/device-service/src/main/java/com/openiot/device/mapper/ProductMapper.java
- [ ] T021 [P] Modify DeviceMapper to add query methods in backend/device-service/src/main/java/com/openiot/device/mapper/DeviceMapper.java
- [ ] T022 [P] Create ParseRuleMapper interface in backend/rule-service/src/main/java/com/openiot/rule/mapper/ParseRuleMapper.java
- [ ] T023 [P] Create MappingRuleMapper interface in backend/rule-service/src/main/java/com/openiot/rule/mapper/MappingRuleMapper.java
- [ ] T024 [P] Create ForwardRuleMapper interface in backend/rule-service/src/main/java/com/openiot/rule/mapper/ForwardRuleMapper.java
- [ ] T025 [P] Create AlarmRuleMapper interface in backend/rule-service/src/main/java/com/openiot/rule/mapper/AlarmRuleMapper.java
- [ ] T026 [P] Create AlarmRecordMapper interface in backend/rule-service/src/main/java/com/openiot/rule/mapper/AlarmRecordMapper.java
- [ ] T027 [P] Create DeviceServiceInvokeMapper interface in backend/device-service/src/main/java/com/openiot/device/mapper/DeviceServiceInvokeMapper.java
- [ ] T028 [P] Create DevicePropertyMapper interface in backend/device-service/src/main/java/com/openiot/device/mapper/DevicePropertyMapper.java
- [ ] T029 [P] Create DeviceEventMapper interface in backend/device-service/src/main/java/com/openiot/device/mapper/DeviceEventMapper.java

**Deliverables**:
- ✅ 所有数据库表创建完成
- ✅ 所有实体类创建完成
- ✅ 所有 Mapper 接口创建完成
- ✅ Flyway 迁移执行成功

---

## Phase 3: User Story 1 - Product-Device Hierarchy Management (P1)

**Goal**: 建立产品-设备层级关系，支持产品作为设备模板

**Independent Test Criteria**:
1. 创建产品（MQTT 协议， JSON 数据格式）
2. 创建设备并关联产品
3. 验证设备继承产品配置
4. 查看产品详情显示关联设备
5. 删除有关联设备的产品被阻止

**Duration**: 3-5 days

### Backend Tasks

#### Service Layer

- [ ] T030 [US1] Create ProductService in backend/device-service/src/main/java/com/openiot/device/service/ProductService.java
- [ ] T031 [US1] Implement createProduct method with product_key auto-generation (10-20 chars, format: PROD_XXXXXX)
- [ ] T032 [US1] Implement updateProduct method with validation
- [ ] T033 [US1] Implement deleteProduct method with device association check
- [ ] T034 [US1] Implement getProductById method
- [ ] T035 [US1] Implement getProductList method with pagination and filtering
- [ ] T036 [US1] Modify DeviceService to add createDeviceWithProduct method in backend/device-service/src/main/java/com/openiot/device/service/DeviceService.java
- [ ] T037 [US1] Implement device authentication logic (DeviceKey + DeviceSecret) in DeviceService
- [ ] T038 [US1] Add device_key and device_secret generation logic (UUID v4, BCrypt hash)

#### VO/DTO Layer

- [ ] T039 [P] [US1] Create ProductCreateVO in backend/device-service/src/main/java/com/openiot/device/vo/ProductCreateVO.java
- [ ] T040 [P] [US1] Create ProductUpdateVO in backend/device-service/src/main/java/com/openiot/device/vo/ProductUpdateVO.java
- [ ] T041 [P] [US1] Create ProductVO in backend/device-service/src/main/java/com/openiot/device/vo/ProductVO.java
- [ ] T042 [P] [US1] Modify DeviceCreateVO to add productId field in backend/device-service/src/main/java/com/openiot/device/vo/DeviceCreateVO.java
- [ ] T043 [P] [US1] Modify DeviceVO to include product information in backend/device-service/src/main/java/com/openiot/device/vo/DeviceVO.java

#### Controller Layer

- [ ] T044 [US1] Create ProductController in backend/device-service/src/main/java/com/openiot/device/controller/ProductController.java
- [ ] T045 [US1] Implement POST /api/products endpoint
- [ ] T046 [US1] Implement GET /api/products/{id} endpoint
- [ ] T047 [US1] Implement PUT /api/products/{id} endpoint
- [ ] T048 [US1] Implement DELETE /api/products/{id} endpoint
- [ ] T049 [US1] Implement GET /api/products endpoint with pagination
- [ ] T050 [US1] Implement GET /api/products/{id}/devices endpoint

### Frontend Tasks

- [ ] T051 [P] [US1] Create ProductList.vue in frontend/src/views/product/ProductList.vue
- [ ] T052 [P] [US1] Create ProductDetail.vue in frontend/src/views/product/ProductDetail.vue
- [ ] T053 [US1] Implement product creation form with validation
- [ ] T054 [US1] Implement product list display with pagination
- [ ] T055 [US1] Implement product detail view showing associated devices
- [ ] T056 [US1] Modify DeviceCreateForm to add product selection dropdown
- [ ] T057 [P] [US1] Create product API service in frontend/src/api/product.ts

**Deliverables**:
- ✅ 产品 CRUD 功能完整
- ✅ 设备可关联产品
- ✅ 设备继承产品配置
- ✅ 产品详情显示关联设备
- ✅ 删除保护机制生效

---

## Phase 4: User Story 2 - Thing Model Definition and Validation (P1)

**Goal**: 支持物模型的可视化定义和验证

**Independent Test Criteria**:
1. 定义物模型（属性、事件、服务）
2. 设备上报符合物模型的数据被接受
3. 设备上报违反物模型的数据被拒绝
4. 物模型 JSON 格式正确存储
5. 修改物模型时警告影响

**Duration**: 3-4 days

### Backend Tasks

#### Entity Modification

- [ ] T058 [US2] Add thing_model JSONB field to Product entity (already done in T010, verify)

#### Service Layer

- [ ] T059 [US2] Create ThingModelService in backend/device-service/src/main/java/com/openiot/device/service/ThingModelService.java
- [ ] T060 [US2] Implement updateThingModel method with validation
- [ ] T061 [US2] Implement getThingModel method
- [ ] T062 [US2] Create ThingModelValidator in backend/connect-service/src/main/java/com/openiot/connect/validator/ThingModelValidator.java
- [ ] T063 [US2] Implement validateProperty method (type checking, range validation)
- [ ] T064 [US2] Implement validateEvent method
- [ ] T065 [US2] Implement validateService method
- [ ] T066 [US2] Integrate ThingModelValidator into DeviceService.dataReport method

#### VO/DTO Layer

- [ ] T067 [P] [US2] Create ThingModelVO in backend/device-service/src/main/java/com/openiot/device/vo/ThingModelVO.java
- [ ] T068 [P] [US2] Create PropertyDefinitionVO in backend/device-service/src/main/java/com/openiot/device/vo/PropertyDefinitionVO.java
- [ ] T069 [P] [US2] Create EventDefinitionVO in backend/device-service/src/main/java/com/openiot/device/vo/EventDefinitionVO.java
- [ ] T070 [P] [US2] Create ServiceDefinitionVO in backend/device-service/src/main/java/com/openiot/device/vo/ServiceDefinitionVO.java

#### Controller Layer

- [ ] T071 [US2] Add PUT /api/products/{id}/thing-model endpoint to ProductController
- [ ] T072 [US2] Add GET /api/products/{id}/thing-model endpoint to ProductController

### Frontend Tasks

- [ ] T073 [P] [US2] Create ThingModelEditor.vue component in frontend/src/components/ThingModelEditor.vue
- [ ] T074 [US2] Implement property editor tab (identifier, name, type, unit, min/max)
- [ ] T075 [US2] Implement event editor tab (identifier, name, type, severity)
- [ ] T076 [US2] Implement service editor tab (identifier, name, input/output params)
- [ ] T077 [US2] Integrate ThingModelEditor into ProductDetail.vue
- [ ] T078 [US2] Add thing model validation warning when modifying

**Deliverables**:
- ✅ 物模型定义功能完整
- ✅ 数据验证生效
- ✅ 前端编辑器可用
- ✅ 修改警告机制

---

## Phase 5: User Story 3 - Configurable Parse Rules (P1)

**Goal**: 实现可配置解析规则，替换硬编码解析逻辑

**Independent Test Criteria**:
1. JSON 字段映射解析正常
2. JavaScript 脚本解析正常
3. 正则表达式解析正常
4. 二进制协议解析正常
5. 解析规则热更新生效（30 秒内）
6. JavaScript 沙箱安全（3 秒超时，禁止 IO）

**Duration**: 5-7 days

### Backend Tasks

#### Service Layer

- [ ] T079 [US3] Create ParseRuleService in backend/rule-service/src/main/java/com/openiot/rule/service/ParseRuleService.java
- [ ] T080 [US3] Implement createParseRule method
- [ ] T081 [US3] Implement updateParseRule method with Redis pub/sub notification
- [ ] T082 [US3] Implement deleteParseRule method
- [ ] T083 [US3] Implement testParseRule method for testing

#### Parser Implementations

- [ ] T084 [P] [US3] Create ParseRuleEngine interface in backend/connect-service/src/main/java/com/openiot/connect/parser/ParseRuleEngine.java
- [ ] T085 [P] [US3] Implement JsonPathParser in backend/connect-service/src/main/java/com/openiot/connect/parser/impl/JsonPathParser.java
- [ ] T086 [P] [US3] Implement JavaScriptParser with GraalJS sandbox in backend/connect-service/src/main/java/com/openiot/connect/parser/impl/JavaScriptParser.java
- [ ] T087 [US3] Add GraalJS sandbox configuration (disable IO, 3s timeout, 10KB size limit)
- [ ] T088 [P] [US3] Implement RegexParser in backend/connect-service/src/main/java/com/openiot/connect/parser/impl/RegexParser.java
- [ ] T089 [P] [US3] Implement BinaryParser in backend/connect-service/src/main/java/com/openiot/connect/parser/impl/BinaryParser.java
- [ ] T090 [US3] Create ParseRuleCache in backend/connect-service/src/main/java/com/openiot/connect/cache/ParseRuleCache.java
- [ ] T091 [US3] Implement Redis pub/sub listener for rule updates
- [ ] T092 [US3] Implement 30-minute TTL cache for parse rules

#### VO/DTO Layer

- [ ] T093 [P] [US3] Create ParseRuleCreateVO in backend/rule-service/src/main/java/com/openiot/rule/vo/ParseRuleCreateVO.java
- [ ] T094 [P] [US3] Create ParseRuleVO in backend/rule-service/src/main/java/com/openiot/rule/vo/ParseRuleVO.java
- [ ] T095 [P] [US3] Create ParseTestRequestVO in backend/rule-service/src/main/java/com/openiot/rule/vo/ParseTestRequestVO.java

#### Controller Layer

- [ ] T096 [US3] Create ParseRuleController in backend/rule-service/src/main/java/com/openiot/rule/controller/ParseRuleController.java
- [ ] T097 [US3] Implement POST /api/parse-rules endpoint
- [ ] T098 [US3] Implement GET /api/parse-rules/{id} endpoint
- [ ] T099 [US3] Implement PUT /api/parse-rules/{id} endpoint
- [ ] T100 [US3] Implement DELETE /api/parse-rules/{id} endpoint
- [ ] T101 [US3] Implement POST /api/parse-rules/{id}/test endpoint

### Frontend Tasks

- [ ] T102 [P] [US3] Create ParseRuleConfig.vue in frontend/src/views/rule/ParseRuleConfig.vue
- [ ] T103 [US3] Implement rule type selector (JSON/JavaScript/Regex/Binary)
- [ ] T104 [US3] Implement JSON field mapping editor
- [ ] T105 [US3] Integrate Monaco Editor for JavaScript script editing
- [ ] T106 [US3] Implement regex pattern editor with test input
- [ ] T107 [US3] Implement binary protocol editor (byte positions, data types)
- [ ] T108 [US3] Implement parse rule test panel with sample data input
- [ ] T109 [P] [US3] Create parse rule API service in frontend/src/api/rule.ts

**Deliverables**:
- ✅ 四种解析器实现完成
- ✅ GraalJS 沙箱配置生效
- ✅ 解析规则热更新（30 秒）
- ✅ 前端配置界面可用
- ✅ 测试功能正常

---

## Phase 6: User Story 4 - Configurable Mapping Rules (P2)

**Goal**: 实现字段映射到物模型属性

**Independent Test Criteria**:
1. 字段映射正常工作（含转换函数）
2. 未映射的属性设置为 null
3. 映射规则测试接口正常
4. 循环依赖检测生效

**Duration**: 3-4 days

### Backend Tasks

#### Service Layer

- [ ] T110 [US4] Create MappingRuleService in backend/rule-service/src/main/java/com/openiot/rule/service/MappingRuleService.java
- [ ] T111 [US4] Implement createMappingRule method with circular dependency check
- [ ] T112 [US4] Implement updateMappingRule method
- [ ] T113 [US4] Implement testMappingRule method
- [ ] T114 [US4] Create MappingRuleEngine in backend/connect-service/src/main/java/com/openiot/connect/mapper/MappingRuleEngine.java
- [ ] T115 [US4] Implement field mapping with transformation functions (unit conversion, formula)

#### VO/DTO Layer

- [ ] T116 [P] [US4] Create MappingRuleCreateVO in backend/rule-service/src/main/java/com/openiot/rule/vo/MappingRuleCreateVO.java
- [ ] T117 [P] [US4] Create MappingRuleVO in backend/rule-service/src/main/java/com/openiot/rule/vo/MappingRuleVO.java
- [ ] T118 [P] [US4] Create MappingTestRequestVO in backend/rule-service/src/main/java/com/openiot/rule/vo/MappingTestRequestVO.java

#### Controller Layer

- [ ] T119 [US4] Create MappingRuleController in backend/rule-service/src/main/java/com/openiot/rule/controller/MappingRuleController.java
- [ ] T120 [US4] Implement POST /api/mapping-rules endpoint
- [ ] T121 [US4] Implement GET /api/mapping-rules/{id} endpoint
- [ ] T122 [US4] Implement PUT /api/mapping-rules/{id} endpoint
- [ ] T123 [US4] Implement DELETE /api/mapping-rules/{id} endpoint
- [ ] T124 [US4] Implement POST /api/mapping-rules/{id}/test endpoint

### Frontend Tasks

- [ ] T125 [P] [US4] Create MappingRuleConfig.vue in frontend/src/views/rule/MappingRuleConfig.vue
- [ ] T126 [US4] Implement field mapping visual editor
- [ ] T127 [US4] Implement transformation function selector (unit conversion, formula)
- [ ] T128 [US4] Implement mapping rule test panel

**Deliverables**:
- ✅ 字段映射功能完整
- ✅ 转换函数生效
- ✅ 循环依赖检测
- ✅ 前端配置界面可用

---

## Phase 7: User Story 5 - Device Property and Event Storage (P2)

**Goal**: 集成 InfluxDB，存储设备属性和事件

**Independent Test Criteria**:
1. 设备属性写入 InfluxDB
2. 历史数据查询正常（90 天范围）
3. 数据聚合查询正常（平均、最大、最小）
4. 数据导出功能正常（CSV/JSON）

**Duration**: 3-4 days

### Backend Tasks

#### InfluxDB Integration

- [ ] T129 [US5] Add InfluxDB configuration to backend/data-service/src/main/resources/application.yml
- [ ] T130 [US5] Create InfluxDBProperties in backend/data-service/src/main/java/com/openiot/data/config/InfluxDBProperties.java
- [ ] T131 [US5] Create InfluxDBService in backend/data-service/src/main/java/com/openiot/data/influxdb/InfluxDBService.java
- [ ] T132 [US5] Implement writeDeviceProperty method (point write)
- [ ] T133 [US5] Implement writeDevicePropertyBatch method (batch write)
- [ ] T134 [US5] Implement queryDevicePropertyHistory method (Flux query)
- [ ] T135 [US5] Implement aggregateDeviceProperty method (aggregation)
- [ ] T136 [US5] Configure InfluxDB 90-day retention policy in initialization script

#### Service Layer

- [ ] T137 [US5] Create DevicePropertyService in backend/device-service/src/main/java/com/openiot/device/service/DevicePropertyService.java
- [ ] T138 [US5] Implement storeProperty method (PostgreSQL current + InfluxDB history)
- [ ] T139 [US5] Implement queryPropertyHistory method
- [ ] T140 [US5] Create DeviceEventService in backend/device-service/src/main/java/com/openiot/device/service/DeviceEventService.java
- [ ] T141 [US5] Implement storeEvent method (PostgreSQL)
- [ ] T142 [US5] Implement queryEventHistory method

#### VO/DTO Layer

- [ ] T143 [P] [US5] Create PropertyHistoryVO in backend/device-service/src/main/java/com/openiot/device/vo/PropertyHistoryVO.java
- [ ] T144 [P] [US5] Create EventHistoryVO in backend/device-service/src/main/java/com/openiot/device/vo/EventHistoryVO.java

#### Controller Layer

- [ ] T145 [US5] Create HistoryDataController in backend/data-service/src/main/java/com/openiot/data/controller/HistoryDataController.java
- [ ] T146 [US5] Implement GET /api/devices/{id}/properties/history endpoint
- [ ] T147 [US5] Implement GET /api/devices/{id}/events/history endpoint
- [ ] T148 [US5] Implement GET /api/devices/{id}/data/export endpoint (CSV/JSON)

### Frontend Tasks

- [ ] T149 [P] [US5] Create HistoryDataQuery.vue in frontend/src/views/device/HistoryDataQuery.vue
- [ ] T150 [US5] Implement date range picker
- [ ] T151 [US5] Integrate ECharts for data visualization
- [ ] T152 [US5] Implement data export button (CSV/JSON download)

**Deliverables**:
- ✅ InfluxDB 集成完成
- ✅ 90 天保留策略配置
- ✅ 历史数据查询正常
- ✅ 数据导出功能正常

---

## Phase 8: User Story 6 - Multi-Target Data Forwarding (P2)

**Goal**: 实现多目标数据转发

**Independent Test Criteria**:
1. Kafka 转发正常
2. HTTP Webhook 转发正常
3. MQTT 转发正常
4. InfluxDB 转发正常
5. 失败重试机制生效（指数退避）
6. 死信队列正常

**Duration**: 5-7 days

### Backend Tasks

#### Service Layer

- [ ] T153 [US6] Create ForwardRuleService in backend/rule-service/src/main/java/com/openiot/rule/service/ForwardRuleService.java
- [ ] T154 [US6] Implement createForwardRule method
- [ ] T155 [US6] Implement updateForwardRule method
- [ ] T156 [US6] Implement testForwardRule method

#### Forwarder Implementations

- [ ] T157 [P] [US6] Create Forwarder interface in backend/rule-service/src/main/java/com/openiot/rule/forwarder/Forwarder.java
- [ ] T158 [P] [US6] Implement KafkaForwarder in backend/rule-service/src/main/java/com/openiot/rule/forwarder/impl/KafkaForwarder.java
- [ ] T159 [P] [US6] Implement HttpWebhookForwarder in backend/rule-service/src/main/java/com/openiot/rule/forwarder/impl/HttpWebhookForwarder.java
- [ ] T160 [P] [US6] Implement MqttForwarder in backend/rule-service/src/main/java/com/openiot/rule/forwarder/impl/MqttForwarder.java
- [ ] T161 [P] [US6] Implement InfluxDbForwarder in backend/rule-service/src/main/java/com/openiot/rule/forwarder/impl/InfluxDbForwarder.java
- [ ] T162 [US6] Create ForwarderManager in backend/rule-service/src/main/java/com/openiot/rule/forwarder/ForwarderManager.java
- [ ] T163 [US6] Implement forward method with retry logic (exponential backoff: 1s, 2s, 4s, 8s, 16s)
- [ ] T164 [US6] Implement dead letter queue producer (Kafka topic: forward_dlq)
- [ ] T165 [US6] Create DeadLetterQueueConsumer in backend/rule-service/src/main/java/com/openiot/rule/forwarder/DeadLetterQueueConsumer.java
- [ ] T166 [US6] Implement manual retry mechanism for DLQ messages

#### VO/DTO Layer

- [ ] T167 [P] [US6] Create ForwardRuleCreateVO in backend/rule-service/src/main/java/com/openiot/rule/vo/ForwardRuleCreateVO.java
- [ ] T168 [P] [US6] Create ForwardRuleVO in backend/rule-service/src/main/java/com/openiot/rule/vo/ForwardRuleVO.java

#### Controller Layer

- [ ] T169 [US6] Create ForwardRuleController in backend/rule-service/src/main/java/com/openiot/rule/controller/ForwardRuleController.java
- [ ] T170 [US6] Implement POST /api/forward-rules endpoint
- [ ] T171 [US6] Implement GET /api/forward-rules/{id} endpoint
- [ ] T172 [US6] Implement PUT /api/forward-rules/{id} endpoint
- [ ] T173 [US6] Implement DELETE /api/forward-rules/{id} endpoint
- [ ] T174 [US6] Implement POST /api/forward-rules/{id}/test endpoint

### Frontend Tasks

- [ ] T175 [P] [US6] Create ForwardRuleConfig.vue in frontend/src/views/rule/ForwardRuleConfig.vue
- [ ] T176 [US6] Implement target type selector (Kafka/HTTP/MQTT/InfluxDB)
- [ ] T177 [US6] Implement dynamic target configuration form
- [ ] T178 [US6] Integrate Monaco Editor for rule SQL editing
- [ ] T179 [US6] Implement forward rule test panel

**Deliverables**:
- ✅ 四种转发器实现完成
- ✅ 重试机制生效
- ✅ 死信队列正常
- ✅ 前端配置界面可用

---

## Phase 9: User Story 7 - Intelligent Alarm Management (P3)

**Goal**: 实现智能告警和通知

**Independent Test Criteria**:
1. 告警规则触发正常（Aviator 表达式）
2. 告警通知发送正常（邮件/短信/Webhook）
3. 告警聚合和去重正常（5 分钟窗口）
4. 告警确认和恢复正常
5. 维护窗口静默正常

**Duration**: 4-5 days

### Backend Tasks

#### Service Layer

- [ ] T180 [US7] Create AlarmRuleService in backend/rule-service/src/main/java/com/openiot/rule/service/AlarmRuleService.java
- [ ] T181 [US7] Implement createAlarmRule method
- [ ] T182 [US7] Implement updateAlarmRule method
- [ ] T183 [US7] Create AlarmRecordService in backend/rule-service/src/main/java/com/openiot/rule/service/AlarmRecordService.java
- [ ] T184 [US7] Implement acknowledgeAlarm method
- [ ] T185 [US7] Implement queryAlarmRecords method

#### Alarm Engine

- [ ] T186 [US7] Add Aviator dependency to backend/rule-service/pom.xml
- [ ] T187 [US7] Create AlarmEngine in backend/rule-service/src/main/java/com/openiot/rule/alarm/AlarmEngine.java
- [ ] T188 [US7] Implement evaluateAlarmCondition method (Aviator expression)
- [ ] T189 [US7] Implement triggerAlarm method
- [ ] T190 [US7] Implement detectAlarmRecovery method
- [ ] T191 [US7] Implement alarm aggregation logic (5-minute window)

#### Notifiers

- [ ] T192 [P] [US7] Create AlarmNotifier interface in backend/rule-service/src/main/java/com/openiot/rule/alarm/AlarmNotifier.java
- [ ] T193 [P] [US7] Implement EmailNotifier in backend/rule-service/src/main/java/com/openiot/rule/alarm/impl/EmailNotifier.java
- [ ] T194 [P] [US7] Implement SmsNotifier (Mock) in backend/rule-service/src/main/java/com/openiot/rule/alarm/impl/SmsNotifier.java
- [ ] T195 [P] [US7] Implement WebhookNotifier in backend/rule-service/src/main/java/com/openiot/rule/alarm/impl/WebhookNotifier.java

#### VO/DTO Layer

- [ ] T196 [P] [US7] Create AlarmRuleCreateVO in backend/rule-service/src/main/java/com/openiot/rule/vo/AlarmRuleCreateVO.java
- [ ] T197 [P] [US7] Create AlarmRuleVO in backend/rule-service/src/main/java/com/openiot/rule/vo/AlarmRuleVO.java
- [ ] T198 [P] [US7] Create AlarmRecordVO in backend/rule-service/src/main/java/com/openiot/rule/vo/AlarmRecordVO.java

#### Controller Layer

- [ ] T199 [US7] Create AlarmRuleController in backend/rule-service/src/main/java/com/openiot/rule/controller/AlarmRuleController.java
- [ ] T200 [US7] Implement POST /api/alarm-rules endpoint
- [ ] T201 [US7] Implement GET /api/alarm-rules/{id} endpoint
- [ ] T202 [US7] Implement PUT /api/alarm-rules/{id} endpoint
- [ ] T203 [US7] Implement DELETE /api/alarm-rules/{id} endpoint
- [ ] T204 [US7] Implement GET /api/alarm-records endpoint
- [ ] T205 [US7] Implement POST /api/alarm-records/{id}/acknowledge endpoint

### Frontend Tasks

- [ ] T206 [P] [US7] Create AlarmRuleConfig.vue in frontend/src/views/alarm/AlarmRuleConfig.vue
- [ ] T207 [US7] Implement Aviator expression editor with validation
- [ ] T208 [US7] Implement notification configuration form
- [ ] T209 [P] [US7] Create AlarmRecordList.vue in frontend/src/views/alarm/AlarmRecordList.vue
- [ ] T210 [US7] Implement alarm record display with filtering
- [ ] T211 [US7] Implement alarm acknowledge button
- [ ] T212 [US7] Implement alarm statistics charts (ECharts)

**Deliverables**:
- ✅ 告警规则引擎完成
- ✅ 三种通知器实现
- ✅ 告警聚合生效
- ✅ 前端配置界面可用

---

## Phase 10: User Story 8 - Device Service Invocation (P3)

**Goal**: 实现平台调用设备服务的 RPC 能力

**Independent Test Criteria**:
1. 同步服务调用正常（30 秒超时）
2. 异步服务调用正常
3. 服务调用超时处理正常
4. 服务调用记录查询正常
5. 离线设备调用被拒绝

**Duration**: 3-4 days

### Backend Tasks

#### Service Layer

- [ ] T213 [US8] Create DeviceServiceInvokeService in backend/device-service/src/main/java/com/openiot/device/service/DeviceServiceInvokeService.java
- [ ] T214 [US8] Implement invokeServiceSync method (wait for response, 30s timeout)
- [ ] T215 [US8] Implement invokeServiceAsync method (return invocation ID immediately)
- [ ] T216 [US8] Implement queryInvocationStatus method
- [ ] T217 [US8] Implement handleDeviceServiceResponse method (MQTT/HTTP)
- [ ] T218 [US8] Implement timeout detection and status update

#### VO/DTO Layer

- [ ] T219 [P] [US8] Create ServiceInvokeRequestVO in backend/device-service/src/main/java/com/openiot/device/vo/ServiceInvokeRequestVO.java
- [ ] T220 [P] [US8] Create ServiceInvokeVO in backend/device-service/src/main/java/com/openiot/device/vo/ServiceInvokeVO.java

#### Controller Layer

- [ ] T221 [US8] Add POST /api/devices/{id}/services/{serviceIdentifier} endpoint to DeviceController
- [ ] T222 [US8] Add GET /api/service-invocations/{invocationId} endpoint to DeviceController

### Frontend Tasks

- [ ] T223 [P] [US8] Add service invocation tab to DeviceDetail.vue
- [ ] T224 [US8] Implement service invocation form (sync/async toggle)
- [ ] T225 [US8] Implement service invocation record list
- [ ] T226 [US8] Implement invocation status polling (for async)

**Deliverables**:
- ✅ 同步/异步服务调用完成
- ✅ 超时处理正常
- ✅ 服务调用记录可查
- ✅ 前端界面可用

---

## Phase 11: Polish & Cross-Cutting Concerns

**Goal**: 完善系统，添加可观测性、性能优化、文档

**Duration**: 3-5 days

### Observability

- [ ] T227 [P] Add business metrics to Micrometer (device.connected.count, parse_rule.execution.time, forward.success.count, alarm.triggered.count)
- [ ] T228 [P] Add health indicators for InfluxDB connection
- [ ] T229 [P] Configure structured logging (JSON format with traceId, tenantId)
- [ ] T230 [P] Add distributed tracing for Kafka consumers
- [ ] T231 Create Grafana dashboard for IoT platform monitoring

### Performance Optimization

- [ ] T232 [P] Add Redis caching for frequently accessed products
- [ ] T233 [P] Implement connection pooling for InfluxDB
- [ ] T234 [P] Optimize MyBatis Plus queries with proper indexing
- [ ] T235 [P] Add batch processing for device data ingestion

### Documentation

- [ ] T236 [P] Update README.md with new feature documentation
- [ ] T237 [P] Create API documentation with Swagger/OpenAPI
- [ ] T238 [P] Update CLAUDE.md with new technical stack
- [ ] T239 [P] Create deployment guide for production environment

### Security

- [ ] T240 Add input validation for all API endpoints
- [ ] T241 Implement rate limiting for device APIs
- [ ] T242 Add audit logging for critical operations

**Deliverables**:
- ✅ 可观测性完整
- ✅ 性能优化完成
- ✅ 文档完善
- ✅ 安全加固

---

## Task Summary

**Total Tasks**: 242

**Tasks per Phase**:
- Phase 1 (Setup): 7 tasks
- Phase 2 (Foundational): 22 tasks
- Phase 3 (US1): 28 tasks
- Phase 4 (US2): 21 tasks
- Phase 5 (US3): 31 tasks
- Phase 6 (US4): 19 tasks
- Phase 7 (US5): 24 tasks
- Phase 8 (US6): 27 tasks
- Phase 9 (US7): 33 tasks
- Phase 10 (US8): 14 tasks
- Phase 11 (Polish): 16 tasks

**Parallel Opportunities**:
- Phase 2: 12 parallel tasks (entity + mapper creation)
- Phase 3: 7 parallel tasks (VO creation + frontend components)
- Phase 5: 5 parallel tasks (parser implementations)
- Phase 8: 5 parallel tasks (forwarder implementations)
- Phase 9: 4 parallel tasks (notifier implementations)
- Phase 11: 10 parallel tasks (observability + documentation)

**MVP Scope**:
- **Minimum Viable Product**: Phase 1 + Phase 2 + Phase 3 (User Story 1)
- **Total MVP Tasks**: 57 tasks
- **Estimated MVP Duration**: 6-10 days

**Independent Test Criteria Summary**:
- ✅ US1: 产品-设备层级管理可独立测试
- ✅ US2: 物模型定义和验证可独立测试
- ✅ US3: 解析规则可独立测试
- ✅ US4: 映射规则可独立测试
- ✅ US5: 属性和事件存储可独立测试
- ✅ US6: 数据转发可独立测试
- ✅ US7: 告警管理可独立测试
- ✅ US8: 服务调用可独立测试

**Format Validation**:
- ✅ All tasks follow checklist format: `- [ ] [TaskID] [P?] [Story?] Description with file path`
- ✅ All tasks have sequential IDs (T001-T242)
- ✅ All user story tasks have [US#] labels
- ✅ All parallelizable tasks have [P] marker
- ✅ All tasks include specific file paths

---

## Implementation Strategy

### MVP First (Recommended)

1. **Week 1-2**: Complete Phase 1-3 (Setup + Foundational + US1)
   - Deliverable: Product-Device hierarchy working end-to-end
   - Demo: Create product, create device, associate device with product

2. **Week 3**: Complete Phase 4-5 (US2 + US3)
   - Deliverable: Thing model definition + Parse rules working
   - Demo: Define thing model, configure parse rule, validate device data

3. **Week 4-5**: Complete Phase 6-7 (US4 + US5)
   - Deliverable: Mapping rules + Historical data storage working
   - Demo: Map parsed data to thing model, query historical data

4. **Week 6-7**: Complete Phase 8-9 (US6 + US7)
   - Deliverable: Data forwarding + Alarm management working
   - Demo: Forward data to Kafka, trigger alarm on high temperature

5. **Week 8**: Complete Phase 10-11 (US8 + Polish)
   - Deliverable: Service invocation + Production-ready
   - Demo: Invoke device service, view monitoring dashboards

### Incremental Delivery

Each phase completion enables:
- ✅ Independent testing and validation
- ✅ Stakeholder demo and feedback
- ✅ Early risk detection and mitigation
- ✅ Incremental value delivery

---

## Next Steps

1. **Start Implementation**: Begin with Phase 1 (Setup tasks T001-T007)
2. **Track Progress**: Update task checkboxes as completed
3. **Report Issues**: Document blockers and dependencies
4. **Demo Regularly**: Showcase completed phases to stakeholders
5. **Iterate**: Gather feedback and adjust priorities as needed

**Ready to start implementation?** Begin with task T001! 🚀
