# WildFly OpenTelemetry Architecture: Shared Server with Isolated Deployment Metrics

## Overview

WildFly's OpenTelemetry subsystem uses a composite architecture where traces and logs are shared at the server level while metrics are isolated per deployment. This design ensures:

- **Trace continuity**: Request traces span across deployments seamlessly
- **Log aggregation**: All server and deployment logs flow through a unified pipeline
- **Metric isolation**: Each deployment's metrics are collected independently
- **Efficient export**: All signals are aggregated and exported together via OTLP

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ WildFly Server                                              │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ OpenTelemetryService                                    │ │
│ │                                                         │ │
│ │  OpenTelemetrySdk (autoconfigured)                     │ │
│ │  ├─ SdkTracerProvider (shared)                         │ │
│ │  ├─ SdkLoggerProvider (shared)                         │ │
│ │  ├─ SdkMeterProvider (server metrics only)             │ │
│ │  └─ PeriodicMetricReader                               │ │
│ │       └─ AggregatingMetricExporter                     │ │
│ │            └─ OtlpGrpcMetricExporter                   │ │
│ │                                                         │ │
│ │  ConcurrentHashMap<String, DeploymentMetricReader>     │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────┐  ┌─────────────────────┐           │
│ │ Deployment A        │  │ Deployment B        │           │
│ │                     │  │                     │           │
│ │ DeploymentOtel      │  │ DeploymentOtel      │           │
│ │ ├─ TracerProvider ──┼──┼─> (shared)          │           │
│ │ ├─ LoggerProvider ──┼──┼─> (shared)          │           │
│ │ └─ MeterProvider    │  │ └─ MeterProvider    │           │
│ │     (isolated)      │  │     (isolated)      │           │
│ │                     │  │                     │           │
│ │ SdkMeterProvider    │  │ SdkMeterProvider    │           │
│ │ └─ DeploymentReader │  │ └─ DeploymentReader │           │
│ │    (DELTA)          │  │    (DELTA)          │           │
│ └─────────────────────┘  └─────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

## Key Components

### OpenTelemetryService

Server-level service that:
- Builds shared OpenTelemetry SDK via autoconfigure
- Tracks all deployment metric readers
- Provides the aggregating metric exporter
- Manages lifecycle (shutdown cleanup)

**Location**: `observability/opentelemetry/src/main/java/org/wildfly/extension/opentelemetry/OpenTelemetryService.java`

### DeploymentOpenTelemetry

Wrapper that implements `io.opentelemetry.api.OpenTelemetry` and delegates:
- `getTracerProvider()` → server's shared tracer
- `getLogsBridge()` → server's shared logger
- `getMeterProvider()` → deployment's isolated meter
- `getPropagators()` → server's shared propagators

**Location**: `observability/opentelemetry-api/src/main/java/org/wildfly/extension/opentelemetry/api/DeploymentOpenTelemetry.java`

### AggregatingMetricExporter

Wraps the downstream OTLP exporter and:
- Implements `MetricExporter` interface
- On `export()`, collects from all deployment readers
- Combines server metrics + all deployment metrics
- Delegates to downstream OTLP exporter

**Location**: `observability/opentelemetry/src/main/java/org/wildfly/extension/opentelemetry/AggregatingMetricExporter.java`

### DeploymentMetricReader

Per-deployment metric reader that:
- Implements `MetricReader` interface
- Uses DELTA aggregation temporality
- Called on-demand by aggregating exporter
- Auto-clears after collection (DELTA behavior)

**Location**: `observability/opentelemetry/src/main/java/org/wildfly/extension/opentelemetry/DeploymentMetricReader.java`

## Data Flow

### Traces
1. Request enters Deployment A
2. Deployment A uses server's shared `TracerProvider`
3. Span created with deployment-specific attributes
4. Request propagates to Deployment B
5. Deployment B uses same shared `TracerProvider`
6. Context propagates seamlessly
7. Complete trace exported via OTLP

### Logs
1. Log emitted in Deployment A
2. Deployment A uses server's shared `LogsBridgeProvider`
3. Log record includes deployment resource attributes
4. Exported via server's OTLP exporter

### Metrics
1. Deployment A creates counter via `getMeterProvider()`
2. Counter registered in deployment-specific `SdkMeterProvider`
3. Server's `PeriodicMetricReader` triggers export
4. `AggregatingMetricExporter.export()` called
5. Aggregator collects from Deployment A's reader
6. Aggregator collects from Deployment B's reader
7. Aggregator combines server + all deployment metrics
8. All metrics exported together via OTLP

## Resource Attributes

Each deployment's metrics include:

**Inherited from server**:
- `host.name`
- `service.version`
- `deployment.environment`
- Any custom server-level attributes

**Deployment-specific** (override server):
- `service.name` = `{deployment-name}.war`
- `deployment.name` = canonical deployment unit name

Example for `myapp.war`:
```
service.name = "myapp.war"
deployment.name = "jboss.deployment.unit.\"myapp.war\""
host.name = "server-01" (from server)
```

## Aggregation Temporality

**DELTA temporality** is used for deployment metrics:
- Each `collectAllMetrics()` returns only changes since last collection
- Metrics automatically reset after collection
- No memory accumulation
- Server controls collection timing via `PeriodicMetricReader`

## Edge Cases

### Deployment Name Collisions
- Unique key: `deploymentUnit.getServiceName().getCanonicalName()`
- On redeploy: existing reader unregistered before new registration
- No collision possible with canonical names

### Concurrent Deploy/Undeploy
- `ConcurrentHashMap` for thread-safe reader tracking
- Undeploy removes reader from map
- Export gracefully handles missing readers

### Server Shutdown
- `OpenTelemetryService.shutdown()` called
- All deployment readers cleared
- OpenTelemetry SDK closed

## Configuration

### Server-level (controls all deployments)
- `otel.exporter.otlp.endpoint`
- `otel.exporter.otlp.protocol`
- `otel.traces.sampler`
- `otel.bsp.schedule.delay`
- All exporter/protocol settings

### Deployment-level (optional overrides)
- `otel.service.name` - overrides deployment name
- `otel.resource.attributes` - adds custom attributes

Deployment-level config is read from `META-INF/microprofile-config.properties`.

## Migration from Previous Versions

### What Changed
- **Before**: Each deployment had its own OpenTelemetry instance
- **After**: Shared server instance with isolated deployment metrics

### Breaking Changes
1. Deployment-level exporter configuration ignored
   - **Old**: `otel.exporter.otlp.endpoint` per deployment
   - **New**: Server-level only
   - **Migration**: Move to server config

2. Deployment-level sampler configuration ignored
   - **Old**: `otel.traces.sampler` per deployment
   - **New**: Server-level only
   - **Migration**: Move to server config

### Non-Breaking Changes
1. `otel.service.name` still works (per deployment)
2. `otel.resource.attributes` still works (per deployment)
3. Traces/logs behavior unchanged
4. Metrics still exported with deployment-specific tags

### Migration Steps
1. Review deployment `microprofile-config.properties`
2. Move exporter settings to server `standalone.xml`
3. Move sampler settings to server config
4. Keep resource attributes in deployment config
5. Test metric isolation with multiple deployments

## Performance Characteristics

### Memory
- **Server**: One OpenTelemetry SDK instance
- **Per deployment**: One `SdkMeterProvider` + one `DeploymentMetricReader`
- **DELTA temporality**: No metric accumulation

### CPU
- **Export cycle**: O(n) where n = number of deployments
- **Metric collection**: Parallel-safe via `ConcurrentHashMap`
- **No additional overhead** vs single deployment

### Network
- **Single OTLP connection** per exporter type (metrics/traces/logs)
- **Batched export** of all metrics together
- **Reduced overhead** vs per-deployment connections

## Testing

### Integration Test
`OpenTelemetryMultipleWarTestCase` verifies:
- Multiple deployments can coexist
- Each deployment's metrics are distinct
- All metrics exported successfully
- Trace context propagates between deployments

**Location**: `testsuite/integration/expansion/src/test/java/org/wildfly/test/integration/observability/opentelemetry/OpenTelemetryMultipleWarTestCase.java`

### Unit Tests
- `SubsystemParsingTestCase`: XML parsing
- `SubsystemTransformersTestCase`: Model transformation

## Future Enhancements

### Potential Improvements
1. **Per-deployment sampling**: Allow deployment-specific sampler override
2. **Metric filtering**: Filter metrics by deployment before export
3. **Resource injection**: Inject deployment metadata into traces/logs
4. **Dynamic reconfiguration**: Hot-reload exporter config

### Performance Optimizations
1. **Lazy reader creation**: Only create reader on first metric
2. **Reader pooling**: Reuse readers for redeployed applications
3. **Batch size tuning**: Optimize based on deployment count

## References

- [OpenTelemetry Java SDK](https://github.com/open-telemetry/opentelemetry-java)
- [OpenTelemetry Autoconfigure](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure)
- [WildFly Micrometer Composite Pattern](../micrometer/src/main/java/org/wildfly/extension/micrometer/)
- [Implementation Plan](COMPOSITE_OTEL.md)
