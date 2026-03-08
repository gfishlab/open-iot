package com.openiot.device.influxdb;

import com.influxdb.client.DeleteApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import jakarta.persistence.Column;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * InfluxDB 数据仓库
 *
 * @author OpenIoT Team
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "influxdb", name = "enabled", havingValue = "true", matchIfMissing = false)
public class InfluxDBRepository {

    private final InfluxDBClient influxDBClient;
    private final InfluxDBConfig.InfluxDBProperties properties;

    /**
     * 查询设备状态历史轨迹
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 状态记录列表
     */
    public List<DeviceStatusPoint> queryDeviceStatus(Long deviceId, Instant startTime, Instant endTime) {
        String flux = String.format("""
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r["_measurement"] == "device_status")
                  |> filter(fn: (r) => r["device_id"] == "%s")
                  |> yield(name: "status")
                """,
                properties.bucket(),
                startTime.toString(),
                endTime.toString(),
                deviceId
        );

        return query(flux, DeviceStatusPoint.class);
    }

    /**
     * 查询设备属性历史轨迹
     *
     * @param deviceId           设备ID
     * @param propertyIdentifier 属性标识符
     * @param startTime          开始时间
     * @param endTime            结束时间
     * @return 属性记录列表
     */
    public List<DevicePropertyPoint> queryDeviceProperty(Long deviceId, String propertyIdentifier, Instant startTime, Instant endTime) {
        String flux = String.format("""
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r["_measurement"] == "device_property")
                  |> filter(fn: (r) => r["device_id"] == "%s")
                  |> filter(fn: (r) => r["property_identifier"] == "%s")
                  |> yield(name: "property")
                """,
                properties.bucket(),
                startTime.toString(),
                endTime.toString(),
                deviceId,
                propertyIdentifier
        );

        return query(flux, DevicePropertyPoint.class);
    }

    /**
     * 查询设备事件历史轨迹
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 事件记录列表
     */
    public List<DeviceEventPoint> queryDeviceEvents(Long deviceId, Instant startTime, Instant endTime) {
        String flux = String.format("""
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r["_measurement"] == "device_event")
                  |> filter(fn: (r) => r["device_id"] == "%s")
                  |> yield(name: "event")
                """,
                properties.bucket(),
                startTime.toString(),
                endTime.toString(),
                deviceId
        );

        return query(flux, DeviceEventPoint.class);
    }

    /**
     * 聚合查询设备状态统计
     *
     * @param deviceId 设备ID
     * @param startTime 开始时间
     * @param endTime  结束时间
     * @param window   聚合窗口（如 1m, 5m, 1h, 1d）
     * @return 统计数据
     */
    public List<Map<String, Object>> queryDeviceStatusStats(Long deviceId, Instant startTime, Instant endTime, String window) {
        String flux = String.format("""
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r["_measurement"] == "device_status")
                  |> filter(fn: (r) => r["device_id"] == "%s")
                  |> aggregateWindow(every: %s, fn: mean, createEmpty: false)
                  |> yield(name: "mean")
                """,
                properties.bucket(),
                startTime.toString(),
                endTime.toString(),
                deviceId,
                window
        );

        return queryRaw(flux);
    }

    /**
     * 查询最新设备状态
     *
     * @param deviceId 设备ID
     * @return 最新状态记录
     */
    public DeviceStatusPoint queryLatestStatus(Long deviceId) {
        String flux = String.format("""
                from(bucket: "%s")
                  |> range(start: -1h)
                  |> filter(fn: (r) => r["_measurement"] == "device_status")
                  |> filter(fn: (r) => r["device_id"] == "%s")
                  |> last(column: "_time")
                  |> yield(name: "last")
                """,
                properties.bucket(),
                deviceId
        );

        List<DeviceStatusPoint> results = query(flux, DeviceStatusPoint.class);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 删除指定时间范围的数据
     *
     * @param measurement 测量名称
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @param predicates  额外过滤条件
     */
    public void deleteData(String measurement, Instant startTime, Instant endTime, Map<String, String> predicates) {
        DeleteApi deleteApi = influxDBClient.getDeleteApi();

        StringBuilder predicate = new StringBuilder(String.format("_measurement=\"%s\"", measurement));
        if (predicates != null && !predicates.isEmpty()) {
            predicates.forEach((key, value) ->
                predicate.append(String.format(" AND %s=\"%s\"", key, value))
            );
        }

        try {
            // Instant 转 OffsetDateTime
            OffsetDateTime start = OffsetDateTime.ofInstant(startTime, ZoneOffset.UTC);
            OffsetDateTime end = OffsetDateTime.ofInstant(endTime, ZoneOffset.UTC);
            deleteApi.delete(start, end, predicate.toString(), properties.org(), properties.bucket());
            log.info("删除 InfluxDB 数据成功: measurement={}, predicate={}", measurement, predicate);
        } catch (Exception e) {
            log.error("删除 InfluxDB 数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 执行 Flux 查询并映射到指定类型
     *
     * @param flux  Flux 查询语句
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 查询结果列表
     */
    private <T> List<T> query(String flux, Class<T> clazz) {
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux, properties.org());

        List<T> results = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                try {
                    T instance = clazz.getDeclaredConstructor().newInstance();
                    mapRecordToInstance(record, instance, clazz);
                    results.add(instance);
                } catch (Exception e) {
                    log.error("映射记录失败: {}", e.getMessage(), e);
                }
            }
        }

        return results;
    }

    /**
     * 执行原生 Flux 查询
     *
     * @param flux Flux 查询语句
     * @return 查询结果
     */
    private List<Map<String, Object>> queryRaw(String flux) {
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux, properties.org());

        List<Map<String, Object>> results = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                results.add(record.getValues());
            }
        }

        return results;
    }

    /**
     * 将 FluxRecord 映射到实例
     */
    private <T> void mapRecordToInstance(FluxRecord record, T instance, Class<T> clazz) throws Exception {
        Map<String, Object> values = record.getValues();

        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                String columnName = column.name().isEmpty() ? field.getName() : column.name();
                Object value = values.get(columnName);

                if (value != null) {
                    // 类型转换
                    if (field.getType() == Long.class && value instanceof Number) {
                        value = ((Number) value).longValue();
                    } else if (field.getType() == Integer.class && value instanceof Number) {
                        value = ((Number) value).intValue();
                    } else if (field.getType() == Instant.class && value instanceof Instant) {
                        // Instant 直接使用
                    }

                    field.set(instance, value);
                }
            }
        }
    }
}
