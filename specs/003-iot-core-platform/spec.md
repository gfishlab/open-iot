# Feature Specification: IoT Platform Core Functionality

**Feature Branch**: `003-iot-core-platform`
**Created**: 2026-03-06
**Status**: Draft
**Input**: User description: "Implement complete IoT platform core functionality referencing Alibaba Cloud IoT Platform architecture"

## Clarifications

### Session 2026-03-06

- Q: 产品密钥（Product Key）的格式和作用域是什么？ → A: 租户内唯一、短小易读格式（如 PROD_A1B2C3，10-20字符），在租户范围内唯一
- Q: 设备如何向平台认证身份？ → A: 设备使用 DeviceKey + DeviceSecret 密钥对进行认证（一机一密）
- Q: InfluxDB 中的设备历史数据保留策略是什么？ → A: 保留 90 天原始数据，超过 90 天自动删除

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Product-Device Hierarchy Management (Priority: P1)

As an IoT platform administrator, I need to organize devices into products (device templates) so that I can manage devices with similar characteristics collectively and apply consistent rules across all devices of the same type.

**Why this priority**: This is the foundation for all other IoT platform features. Without product-device hierarchy, we cannot implement thing models, parsing rules, or any other core functionality. Products serve as templates that define how devices behave, what data they produce, and how that data should be processed.

**Independent Test**: Can be fully tested by creating a product with specific characteristics (protocol type, data format, node type), then creating multiple devices associated with that product, and verifying that all devices inherit the product's configuration. Delivers immediate value by enabling device categorization and bulk management.

**Acceptance Scenarios**:

1. **Given** I am logged in as a tenant administrator, **When** I create a new product with name "Smart Temperature Sensor", protocol "MQTT", data format "JSON", and node type "Direct Device", **Then** the product is created successfully with a unique product key and appears in the product list.

2. **Given** a product "Smart Temperature Sensor" exists, **When** I create a new device and associate it with this product, **Then** the device inherits the product's protocol, data format, and node type configuration.

3. **Given** multiple devices are associated with a product, **When** I view the product details, **Then** I can see all devices associated with that product and their current status.

4. **Given** a product has no associated devices, **When** I attempt to delete the product, **Then** the product is deleted successfully.

5. **Given** a product has associated devices, **When** I attempt to delete the product, **Then** the system prevents deletion and displays an appropriate error message.

---

### User Story 2 - Thing Model (TSL) Definition and Validation (Priority: P1)

As a product designer, I need to define a Thing Specification Language (TSL) model for each product that specifies the properties, events, and services that devices of this product type support, so that the platform can validate device data and enable proper device management.

**Why this priority**: Thing models are essential for data validation and enable the platform to understand what data devices will send. This is critical before implementing parsing rules or data forwarding, as the thing model defines the expected structure of device data.

**Independent Test**: Can be fully tested by creating a thing model for a product with specific properties (temperature, humidity), events (high temperature alert), and services (reboot), then validating that device data conforms to this model. Delivers value by enabling data validation and providing clear documentation of device capabilities.

**Acceptance Scenarios**:

1. **Given** a product exists, **When** I define a thing model with properties (temperature: float, humidity: float), events (high_temperature: alert type), and services (reboot: no parameters), **Then** the thing model is saved as JSON and associated with the product.

2. **Given** a device is associated with a product that has a thing model, **When** the device reports data that conforms to the thing model (temperature=25.5, humidity=60.2), **Then** the data is accepted and stored.

3. **Given** a device is associated with a product that has a thing model, **When** the device reports data that violates the thing model (temperature="invalid" instead of float), **Then** the data is rejected with a clear validation error message.

4. **Given** a thing model exists for a product, **When** I view the product details, **Then** I can see the thing model definition with all properties, events, and services clearly documented.

5. **Given** a thing model is already in use by active devices, **When** I modify the thing model, **Then** the system warns about potential impact on existing devices and requires confirmation.

---

### User Story 3 - Configurable Parse Rules (Priority: P1)

As a device integration engineer, I need to configure parsing rules for each product so that raw device data (in various formats like JSON, binary, or custom protocols) can be automatically transformed into a standardized format that the platform can process.

**Why this priority**: Parsing is critical for processing device data. Without flexible parsing rules, the platform cannot handle diverse device protocols and data formats. This must be in place before mapping rules or data forwarding can work.

**Independent Test**: Can be fully tested by creating different parse rules (JSON field mapping, JavaScript transformation, regex extraction, binary parsing) for different products, then sending raw device data and verifying that it's correctly parsed into the expected format. Delivers value by enabling support for diverse device types without code changes.

**Acceptance Scenarios**:

1. **Given** a product with JSON data format, **When** I create a JSON parse rule that maps field "t" to "temperature" and "h" to "humidity", **Then** raw data `{"t": 25.5, "h": 60.2}` is parsed to `{"temperature": 25.5, "humidity": 60.2}`.

2. **Given** a product with custom protocol, **When** I create a JavaScript parse rule with custom transformation logic, **Then** raw data is processed according to the script and produces valid output.

3. **Given** a product with binary protocol, **When** I create a binary parse rule specifying byte positions and data types, **Then** binary data is correctly decoded into structured data.

4. **Given** a parse rule is active for a product, **When** I modify the parse rule, **Then** the new rule takes effect within 30 seconds without requiring service restart (hot-reload).

5. **Given** a parse rule has a JavaScript script, **When** the script execution exceeds 3 seconds, **Then** the script is terminated and an error is logged.

6. **Given** a parse rule has a JavaScript script, **When** the script attempts to access file system or network, **Then** the script is blocked and an error is logged (sandbox security).

---

### User Story 4 - Configurable Mapping Rules (Priority: P2)

As a device integration engineer, I need to configure mapping rules that map parsed data fields to thing model properties, so that device data is automatically aligned with the product's thing model definition.

**Why this priority**: Mapping rules bridge the gap between parsed data and thing models. While important, it depends on both parse rules (P1) and thing models (P1) being in place first.

**Independent Test**: Can be fully tested by creating a mapping rule that maps parsed fields to thing model properties, then sending device data through the parse-and-map pipeline and verifying that data is correctly stored in the thing model structure. Delivers value by enabling flexible data transformation without code changes.

**Acceptance Scenarios**:

1. **Given** a product has a thing model and parse rule, **When** I create a mapping rule that maps "temp_celsius" to thing model property "temperature" with unit conversion (Fahrenheit to Celsius), **Then** device data in Fahrenheit is automatically converted and stored in Celsius.

2. **Given** a mapping rule exists, **When** a required thing model property has no corresponding mapping, **Then** the system logs a warning but continues processing.

3. **Given** a mapping rule specifies a field that doesn't exist in parsed data, **When** data is processed, **Then** the thing model property is set to null and processing continues.

4. **Given** a mapping rule is active, **When** I test the mapping with sample data, **Then** I can see the mapped output before activating the rule.

---

### User Story 5 - Device Property and Event Storage (Priority: P2)

As a data analyst, I need to query historical device property values and events over time, so that I can analyze trends, identify patterns, and generate reports.

**Why this priority**: Historical data storage is important for analytics but can be implemented after real-time processing is working. InfluxDB integration provides high-performance time-series storage.

**Independent Test**: Can be fully tested by sending device data over time, then querying historical values for specific time ranges and verifying data accuracy. Delivers value by enabling trend analysis and historical reporting.

**Acceptance Scenarios**:

1. **Given** a device has been reporting temperature data for 24 hours, **When** I query the device's temperature history for the last 12 hours, **Then** I receive all temperature values with timestamps in chronological order.

2. **Given** a device reports events (e.g., "high_temperature_alert"), **When** I query the device's event history, **Then** I receive all events with timestamps and event details.

3. **Given** historical data exists, **When** I request data aggregation (e.g., average temperature per hour for the last day), **Then** the system returns aggregated results within 5 seconds.

4. **Given** a device has historical data, **When** I export the data for a specific time range, **Then** I receive a downloadable file in CSV or JSON format.

---

### User Story 6 - Multi-Target Data Forwarding (Priority: P2)

As a system integrator, I need to configure data forwarding rules that automatically send processed device data to external systems (Kafka, HTTP endpoints, MQTT brokers, or time-series databases), so that downstream systems can consume IoT data in real-time.

**Why this priority**: Data forwarding enables integration with external systems but depends on parsing and mapping being complete. This is essential for production deployments but can be phased after core data processing works.

**Independent Test**: Can be fully tested by creating forwarding rules for different targets (Kafka topic, HTTP webhook, MQTT broker, InfluxDB), then sending device data and verifying that data appears in all configured targets. Delivers value by enabling real-time data integration with external systems.

**Acceptance Scenarios**:

1. **Given** a forwarding rule targets a Kafka topic, **When** device data is processed, **Then** the data appears in the specified Kafka topic within 2 seconds.

2. **Given** a forwarding rule targets an HTTP webhook, **When** device data is processed, **Then** an HTTP POST request is sent to the configured URL with the device data payload.

3. **Given** a forwarding rule targets an MQTT broker, **When** device data is processed, **Then** the data is published to the configured MQTT topic.

4. **Given** a forwarding rule targets InfluxDB, **When** device data is processed, **Then** the data is written to InfluxDB and can be queried.

5. **Given** a forwarding target is unavailable, **When** forwarding fails, **Then** the system retries with exponential backoff (1s, 2s, 4s, 8s, 16s) and logs failures.

6. **Given** a forwarding target fails after 5 retries, **When** all retries are exhausted, **Then** the message is moved to a dead letter queue for manual inspection.

---

### User Story 7 - Intelligent Alarm Management (Priority: P3)

As an operations manager, I need to configure alarm rules that trigger when device data meets specific conditions, so that I can receive notifications about critical issues and take timely action.

**Why this priority**: Alarm management is critical for production operations but depends on real-time data processing being fully functional. This can be implemented after core data flow is working reliably.

**Independent Test**: Can be fully tested by creating alarm rules with specific conditions (e.g., temperature > 30°C), then sending device data that triggers the condition and verifying that alarms are created and notifications are sent. Delivers value by enabling proactive issue detection and response.

**Acceptance Scenarios**:

1. **Given** an alarm rule specifies "temperature > 30°C for 5 minutes", **When** a device reports temperature > 30°C continuously for 5 minutes, **Then** an alarm is triggered and recorded with timestamp and device ID.

2. **Given** an alarm is triggered, **When** notification is configured for email, **Then** an email notification is sent to the configured address within 30 seconds.

3. **Given** an alarm is triggered, **When** notification is configured for webhook, **Then** an HTTP POST request is sent to the configured URL with alarm details.

4. **Given** the same alarm condition triggers multiple times within 5 minutes, **When** alarm aggregation is enabled, **Then** only one notification is sent with a count of occurrences (alarm storm suppression).

5. **Given** an alarm is active, **When** the condition returns to normal (temperature <= 30°C), **Then** the alarm status changes to "recovered" and a recovery notification is sent.

6. **Given** an alarm is active, **When** an operator acknowledges the alarm, **Then** the alarm status changes to "acknowledged" and no further notifications are sent.

7. **Given** a maintenance window is configured, **When** an alarm triggers during the maintenance window, **Then** the alarm is recorded but no notifications are sent (alarm silence).

---

### User Story 8 - Device Service Invocation (RPC) (Priority: P3)

As a device operator, I need to invoke services on devices (e.g., reboot, configure, firmware update) from the platform, so that I can remotely manage and control devices without physical access.

**Why this priority**: Device service invocation (RPC) is an advanced feature that enables remote device management but is not required for basic data collection and processing. This can be implemented after core platform features are stable.

**Independent Test**: Can be fully tested by defining a service in the thing model (e.g., "reboot"), then invoking the service from the platform UI and verifying that the device receives the command and responds appropriately. Delivers value by enabling remote device management and reducing operational costs.

**Acceptance Scenarios**:

1. **Given** a device's thing model defines a service "set_threshold" with parameter "threshold_value", **When** I invoke the service with threshold_value=50 from the platform, **Then** the device receives the command and responds with success/failure status.

2. **Given** a synchronous service invocation, **When** I invoke the service, **Then** the platform waits for the device response (up to 30 seconds timeout) and displays the result.

3. **Given** an asynchronous service invocation, **When** I invoke the service, **Then** the platform immediately returns an invocation ID and the device responds later, with the result available for querying.

4. **Given** a service invocation times out (no response within configured timeout), **When** timeout occurs, **Then** the service invocation status is marked as "timeout" and logged.

5. **Given** a service invocation is in progress, **When** I query the invocation status by invocation ID, **Then** I receive the current status (pending, success, failed, timeout) and any output data.

6. **Given** a device is offline, **When** I attempt to invoke a service on the device, **Then** the system immediately rejects the invocation with "device offline" error.

---

### Edge Cases

- **Parse rule failure**: What happens when a parse rule throws an exception? System logs the error, preserves the original raw data in a dead letter queue for manual inspection, and continues processing other device data.

- **Thing model change with active devices**: What happens when a thing model is modified while devices are actively reporting data? System warns the user about potential impact, requires explicit confirmation, and maintains backward compatibility by allowing optional fields.

- **Circular mapping rules**: What happens when mapping rules create circular dependencies (field A maps to B, B maps to A)? System detects circular dependencies during rule validation and prevents saving invalid rules.

- **Forwarding target performance degradation**: What happens when a forwarding target's response time degrades but doesn't completely fail? System monitors response times, logs warnings when latency exceeds 5 seconds, and continues processing with retry logic.

- **Alarm rule with invalid expression**: What happens when an alarm rule's condition expression is syntactically invalid? System validates the expression when the rule is created/updated and prevents saving invalid expressions with clear error messages.

- **Concurrent service invocations**: What happens when multiple operators try to invoke the same service on the same device simultaneously? System queues service invocations per device and executes them sequentially, returning appropriate status to each operator.

- **Time-series database storage limits**: What happens when InfluxDB storage reaches capacity? System applies data retention policies automatically (automatically delete data older than 90 days).

- **JavaScript script size limit**: What happens when a JavaScript parse rule exceeds 10KB in size? System rejects the script during validation and displays a clear error message about size limits.

## Requirements *(mandatory)*

### Functional Requirements

#### Product Management
- **FR-001**: System MUST allow tenant administrators to create products with attributes: product name, product key (auto-generated 10-20 character unique identifier within tenant scope, format: prefix + alphanumeric, e.g., `PROD_A1B2C3`), product type (Device/Gateway), protocol type (MQTT/HTTP/CoAP/LwM2M/Custom), node type (Direct/Gateway), and data format (JSON/XML/Binary/Custom).
- **FR-002**: System MUST ensure product keys are unique within a tenant scope (enforced by database constraint on `tenant_id` + `product_key` composite unique index).
- **FR-003**: System MUST allow devices to be associated with exactly one product.
- **FR-004**: System MUST prevent deletion of products that have associated devices.
- **FR-005**: System MUST display all devices associated with a product when viewing product details.

#### Device Authentication
- **FR-006**: System MUST automatically generate a unique DeviceKey (device identifier) and DeviceSecret (authentication credential) when creating a new device.
- **FR-007**: System MUST ensure DeviceKey is unique across the entire platform (globally unique).
- **FR-008**: System MUST authenticate devices using DeviceKey + DeviceSecret pair before accepting any data or service invocations.
- **FR-009**: System MUST allow regeneration of DeviceSecret by tenant administrators (invalidates old secret immediately).
- **FR-010**: System MUST log all device authentication attempts with timestamp, DeviceKey, success/failure status, and failure reason.

#### Thing Model (TSL)
- **FR-011**: System MUST allow definition of thing models with three components: properties (attributes with name, type, unit, min/max), events (occurrences with name, type, severity), and services (actions with name, input parameters, output parameters).
- **FR-012**: System MUST support property data types: integer, float, string, boolean, enum, date, struct (nested object), and array.
- **FR-013**: System MUST validate device-reported data against the product's thing model before accepting data.
- **FR-014**: System MUST reject data that violates thing model constraints (type mismatch, value out of range, missing required fields).
- **FR-015**: System MUST allow thing models to be modified but require explicit confirmation when active devices exist.

#### Parse Rules
- **FR-016**: System MUST support four parse rule types: JSON field mapping, JavaScript transformation, regex extraction, and binary protocol parsing.
- **FR-017**: System MUST allow multiple parse rules per product with priority ordering (highest priority rule executes first).
- **FR-018**: System MUST hot-reload parse rule changes within 30 seconds using Redis pub/sub mechanism.
- **FR-019**: System MUST cache parsed rules in Redis with 30-minute TTL for performance.
- **FR-020**: System MUST execute JavaScript parse rules in a sandboxed environment with: no file system access, no network access, maximum execution time of 3 seconds, and maximum script size of 10KB.
- **FR-021**: System MUST terminate JavaScript scripts that exceed the execution time limit.
- **FR-022**: System MUST log parse failures with original raw data for debugging.

#### Mapping Rules
- **FR-023**: System MUST allow mapping parsed data fields to thing model properties with optional transformation functions (unit conversion, formula application, value mapping).
- **FR-024**: System MUST support one-to-one and one-to-many field mappings.
- **FR-025**: System MUST set unmapped thing model properties to null without failing the entire mapping operation.
- **FR-026**: System MUST validate mapping rules for circular dependencies before saving.
- **FR-027**: System MUST provide a mapping rule test interface that shows output with sample data before activation.

#### Device Property and Event Storage
- **FR-028**: System MUST store device property values in PostgreSQL for current state and InfluxDB for historical time-series data.
- **FR-029**: System MUST configure InfluxDB retention policy to automatically delete device property data older than 90 days.
- **FR-030**: System MUST store device events in PostgreSQL with timestamp, event type, severity, and event details.
- **FR-031**: System MUST support querying historical property values by device, time range, and aggregation interval (raw, per minute, per hour, per day).
- **FR-032**: System MUST return query results within 5 seconds for time ranges up to 90 days.
- **FR-033**: System MUST support data export in CSV and JSON formats.

#### Data Forwarding
- **FR-033**: System MUST support forwarding device data to four target types: Kafka topics, HTTP webhooks (POST requests), MQTT brokers (publish to topics), and InfluxDB (time-series storage).
- **FR-034**: System MUST allow filtering device data before forwarding using rule SQL expressions (e.g., `WHERE temperature > 20`).
- **FR-035**: System MUST retry failed forwarding operations with exponential backoff: 1s, 2s, 4s, 8s, 16s (5 attempts total).
- **FR-036**: System MUST move messages to a dead letter queue (Kafka topic: `forward_dlq`) after all retry attempts are exhausted.
- **FR-037**: System MUST provide a manual retry mechanism for dead letter queue messages.
- **FR-038**: System MUST log all forwarding attempts with timestamp, target, status, and error details.

#### Alarm Management
- **FR-039**: System MUST allow creation of alarm rules with: condition expression (using Aviator syntax), alarm level (Critical/Warning/Info), notification configuration (email/SMS/webhook), and target scope (specific device, product-wide, or tenant-wide).
- **FR-040**: System MUST evaluate alarm conditions in real-time as device data arrives.
- **FR-041**: System MUST trigger alarms when conditions are met and create alarm records with: timestamp, device ID, alarm level, alarm title, alarm content, and status (Active/Acknowledged/Recovered).
- **FR-042**: System MUST aggregate identical alarms within a configurable time window (default 5 minutes) to prevent alarm storms.
- **FR-043**: System MUST send notifications within 30 seconds of alarm trigger for Critical and Warning levels.
- **FR-044**: System MUST support alarm acknowledgment by operators, changing status to "Acknowledged" and stopping further notifications.
- **FR-045**: System MUST automatically detect alarm recovery when conditions return to normal and update alarm status to "Recovered".
- **FR-046**: System MUST support maintenance windows (silence rules) during which alarms are recorded but notifications are suppressed.

#### Device Service Invocation (RPC)
- **FR-047**: System MUST allow invocation of thing model services on devices from the platform UI or API.
- **FR-048**: System MUST support synchronous service invocation (wait for device response, up to 30 seconds timeout).
- **FR-049**: System MUST support asynchronous service invocation (immediate return with invocation ID, device responds later).
- **FR-050**: System MUST generate unique invocation IDs for all service invocations.
- **FR-051**: System MUST record all service invocations with: invocation ID, device ID, service identifier, input data, output data, status (Pending/Success/Failed/Timeout), timestamps.
- **FR-052**: System MUST reject service invocations for offline devices immediately.
- **FR-053**: System MUST allow querying service invocation status and results by invocation ID.

### Key Entities

- **Product**: Represents a device template/abstraction that defines common characteristics for a category of devices. Key attributes: product key (tenant-scoped unique identifier, 10-20 characters, format: prefix + alphanumeric, e.g., `PROD_A1B2C3`), product name, product type (Device/Gateway), protocol type (MQTT/HTTP/CoAP/LwM2M/Custom), node type (Direct/Gateway), data format (JSON/XML/Binary/Custom), thing model (JSON definition), status (active/inactive), tenant association.

- **Device**: Represents a physical or virtual device instance associated with a product. Inherits protocol, data format, and node type from product. Key attributes: device ID, device key (unique identifier for authentication, auto-generated), device secret (authentication credential, auto-generated), device name, product association, device status (online/offline), current property values, last communication time, tenant association.

- **Thing Model (TSL)**: Defines the capabilities of devices in a product including properties (measurable attributes like temperature), events (occurrences like alerts), and services (actions like reboot). Stored as JSON structure with type definitions, constraints, and metadata.

- **Parse Rule**: Defines how to transform raw device data into a standardized format. Key attributes: rule type (JSON/JavaScript/Regex/Binary), rule configuration (mapping logic or script), priority (execution order), product association, status (active/inactive).

- **Mapping Rule**: Defines how to map parsed data fields to thing model properties. Key attributes: field mappings (source field → target property with optional transformation), product association, status (active/inactive).

- **Forward Rule**: Defines where to send processed device data. Key attributes: rule SQL (filter condition), target type (Kafka/HTTP/MQTT/InfluxDB), target configuration (URL, topic, credentials), product/device scope, status (active/inactive).

- **Alarm Rule**: Defines conditions that trigger alarms. Key attributes: condition expression (Aviator syntax), alarm level (Critical/Warning/Info), notification configuration (email/SMS/webhook), target scope (device/product/tenant), status (active/inactive).

- **Alarm Record**: Represents a triggered alarm instance. Key attributes: alarm rule association, device association, alarm level, alarm title, alarm content, status (Active/Acknowledged/Recovered), trigger time, recovery time, acknowledgment time, acknowledgment by.

- **Device Property**: Stores current and historical property values for a device. Key attributes: device association, property name, property value, timestamp, data type.

- **Device Event**: Stores events reported by devices. Key attributes: device association, event type, event severity, event details (JSON), timestamp.

- **Device Service Invoke**: Records service invocation requests and responses. Key attributes: device association, service identifier, invocation ID (unique), invocation type (Sync/Async), input data (JSON), output data (JSON), status (Pending/Success/Failed/Timeout), error message, invoke time, complete time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Product creation and device association workflow completes in under 2 minutes for a new product with 10 devices.

- **SC-002**: Thing model validation processes device data in under 100 milliseconds per message.

- **SC-003**: Parse rules execute in under 100 milliseconds per message for JSON/Regex types, and under 3 seconds for JavaScript type.

- **SC-004**: Parse rule hot-reload completes within 30 seconds after rule modification, with zero service downtime.

- **SC-005**: System processes 10,000 device messages per second with P95 latency under 3 seconds end-to-end (from device upload to data availability in storage).

- **SC-006**: Historical data queries for time ranges up to 90 days return results within 5 seconds.

- **SC-007**: Data forwarding delivers messages to target systems within 2 seconds under normal conditions.

- **SC-008**: Data forwarding retry mechanism successfully recovers from transient failures within 31 seconds (1+2+4+8+16 retry intervals).

- **SC-009**: Alarm notifications are delivered within 30 seconds of alarm trigger for Critical and Warning levels.

- **SC-010**: Alarm aggregation reduces notification volume by at least 80% during alarm storms (multiple identical alarms within 5 minutes).

- **SC-011**: Service invocation timeout detection and status update completes within 30 seconds.

- **SC-012**: System supports 100,000 concurrent devices per tenant without performance degradation.

- **SC-013**: 95% of device data processing operations complete successfully without manual intervention over a 30-day period.

- **SC-014**: Dead letter queue size remains under 1% of total message volume under normal operating conditions.

- **SC-015**: JavaScript sandbox prevents 100% of unauthorized file system and network access attempts in security testing.

- **SC-016**: Zero data loss during parse rule hot-reload operations.

- **SC-017**: System maintains 99.9% uptime for device data ingestion and processing over a 30-day period.

- **SC-018**: Platform administrators can configure a complete product (thing model, parse rule, mapping rule, forward rule, alarm rule) for a new device type in under 30 minutes without code changes or service restarts.

## Assumptions

1. **Database Architecture**: The project uses a shared PostgreSQL database across all microservices, with Flyway migrations managed centrally by tenant-service. This spec assumes this architecture remains unchanged.

2. **Redis Configuration**: Redis is already configured with RedisTemplate for caching and Redisson for distributed locks. This spec assumes these configurations are production-ready.

3. **Kafka Infrastructure**: Kafka is already deployed and operational for event streaming. This spec assumes existing Kafka infrastructure can handle increased message volume.

4. **Device Connectivity**: Devices communicate via MQTT, HTTP, or other protocols through connect-service. This spec assumes connect-service is functional and can route messages to appropriate handlers.

5. **Tenant Isolation**: Multi-tenancy is already implemented with tenant isolation at the data level. This spec assumes all new entities (Product, ParseRule, etc.) will include tenant_id for proper isolation.

6. **Authentication & Authorization**: Sa-Token is already integrated for authentication. This spec assumes existing auth mechanisms will be extended to new APIs without requiring architectural changes.

7. **GraalJS Compatibility**: GraalJS 23.1.2 is compatible with JDK 21 and can run in sandboxed mode without requiring GraalVM installation.

8. **InfluxDB Deployment**: InfluxDB 2.x will be deployed as part of this feature, with appropriate storage allocation and 90-day retention policy for device property data.

9. **Network Connectivity**: All target systems for data forwarding (Kafka, HTTP endpoints, MQTT brokers, InfluxDB) are accessible from the platform's network.

10. **Monitoring & Observability**: Existing observability stack (Prometheus, Grafana, LGTM) will be extended to monitor new services and components.

11. **Data Volume**: Initial deployment assumes moderate data volume (10,000 devices, 10 messages/second/device average). Performance criteria are based on this assumption.

12. **Script Security**: JavaScript parse rules are written by trusted users (tenant administrators). While sandboxing is implemented, additional code review processes may be needed for production environments.

13. **Time Synchronization**: All servers and devices have synchronized clocks (NTP) to ensure accurate timestamp-based operations (alarm triggers, time-series queries).

14. **Alarm Notification Delivery**: Email and SMS gateways are assumed to be reliable with high availability. Notification failures are logged but don't block alarm processing.

15. **Device Service Support**: Devices must support receiving and responding to service invocation commands via their communication protocol (MQTT/HTTP).
