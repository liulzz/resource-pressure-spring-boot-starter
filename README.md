# Resource Pressure Spring Boot Starter

一个 Spring Boot starter：业务系统引入依赖后，通过 YAML 配置目标资源利用率，然后调用 HTTP API 即可在当前 JVM 进程内自适应施加 CPU 与堆内存压力。

> ⚠️ 这是压测/演练工具，只应在测试环境、隔离环境或明确授权的环境中启用。不要在生产环境默认开启。

## 安装

```xml
<dependency>
  <groupId>io.github.liulzz</groupId>
  <artifactId>resource-pressure-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## YAML 配置

推荐使用标准 Spring Boot 配置键：

```yaml
resource-pressure:
  enabled: true
  endpoint-enabled: true
  endpoint-base-path: /resource-pressure
  fluctuation-range: 10%
  statistics-window: 2s
  utilization:
    - concurrency: 10
      cpu-usage: 20%
      memory-usage: 40%
    - concurrency: 200
      cpu-usage: 120%
      memory-usage: 66%
```

字段对应关系：

| 中文需求 | 配置键 | 说明 |
| --- | --- | --- |
| 波动范围 | `fluctuation-range` | 允许误差，比如 `10%` 表示目标上下浮动 ±10% |
| 请求统计窗口 | `statistics-window` | CPU 反馈控制窗口，比如 `2s` |
| 并发 | `concurrency` | 当前请求并发数，API 入参会按它选择/插值目标档位 |
| CPU 使用率 | `cpu-usage` | 以单核 100% 计，`120%` 表示约 1.2 个 CPU core |
| 内存使用率 | `memory-usage` | 以 JVM 最大堆内存 `Runtime.maxMemory()` 为基准 |

## API

### 启动压力

```bash
curl -X POST 'http://localhost:8080/resource-pressure/start?concurrency=10&duration=30s'
```

响应示例：

```json
{
  "running": true,
  "concurrency": 10,
  "targetCpuPercent": 20.0,
  "targetMemoryPercent": 40.0,
  "duration": "PT30S"
}
```

### 查看状态

```bash
curl 'http://localhost:8080/resource-pressure/status'
```

### 停止压力

```bash
curl -X POST 'http://localhost:8080/resource-pressure/stop'
```

## 算法概要

- **档位选择**：按 API 传入的 `concurrency` 选择配置档；若没有精确命中，在相邻档位之间线性插值。
- **CPU 自适应**：使用 `ThreadMXBean` 统计本 JVM 进程线程 CPU 时间差，得到类似 Linux `top` 的“单核 100%”CPU 百分比；后台 worker 按统计窗口进行忙等/休眠，并通过反馈控制调整 duty cycle。
- **内存自适应**：以 JVM 最大堆 `Runtime.maxMemory()` 为分母，根据目标比例分块持有 byte 数组；当前堆使用量低于下界就增持，高于上界就释放。
- **安全边界**：默认最多持有 `max-heap-allocation-ratio`（默认 90%）以内的堆内存，避免刻意打满堆导致不可控 OOM。

## 配置元数据

starter 提供 `spring-configuration-metadata.json`，IDE 可以自动提示 `resource-pressure.*` 配置项。
