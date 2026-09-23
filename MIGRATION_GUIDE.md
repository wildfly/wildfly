# Migration Guide: OpenTelemetry Composite Architecture

## Overview

WildFly's OpenTelemetry subsystem has been updated to use a shared server-level instance with per-deployment metric isolation. This guide helps you migrate from the previous per-deployment architecture.

## What Changed

### Architecture
- **Before**: Each deployment created its own OpenTelemetry SDK instance
- **After**: Single shared server OpenTelemetry with deployment-isolated metrics

### Signal Handling
| Signal | Before | After | Impact |
|--------|--------|-------|--------|
| **Traces** | Per-deployment | Shared server | ✅ No change in behavior |
| **Logs** | Per-deployment | Shared server | ✅ No change in behavior |
| **Metrics** | Per-deployment | Isolated per deployment | ✅ Still isolated |
| **Exporter** | Per-deployment | Shared server | ⚠️ Configuration change required |

## Breaking Changes

### 1. Exporter Configuration

**Before** - deployment `microprofile-config.properties`:
```properties
otel.exporter.otlp.endpoint=http://localhost:4317
otel.exporter.otlp.protocol=grpc
otel.exporter.otlp.timeout=10s
```

**After** - server `standalone.xml`:
```xml
<subsystem xmlns="urn:wildfly:opentelemetry:1.0">
    <exporter type="otlp">
        <endpoint>http://localhost:4317</endpoint>
    </exporter>
    <span-processor type="batch">
        <export-timeout>10000</export-timeout>
    </span-processor>
</subsystem>
```

**Impact**: Deployment-level exporter settings are **ignored**. All deployments share the server exporter configuration.

**Action Required**:
1. Review all deployment `microprofile-config.properties` files
2. Extract exporter configuration
3. Move to server `standalone.xml`
4. Remove from deployment configs

### 2. Sampler Configuration

**Before** - deployment `microprofile-config.properties`:
```properties
otel.traces.sampler=traceidratio
otel.traces.sampler.arg=0.1
```

**After** - server `standalone.xml`:
```xml
<subsystem xmlns="urn:wildfly:opentelemetry:1.0">
    <sampler type="ratio">
        <ratio>0.1</ratio>
    </sampler>
</subsystem>
```

**Impact**: Deployment-level sampler settings are **ignored**. All deployments use the server sampler.

**Action Required**:
1. If deployments used different samplers, choose one for server-level
2. Move sampler config to server `standalone.xml`
3. Document any deployment-specific sampling requirements for future enhancement

### 3. Span Processor Configuration

**Before** - deployment `microprofile-config.properties`:
```properties
otel.bsp.schedule.delay=5000
otel.bsp.max.queue.size=2048
otel.bsp.max.export.batch.size=512
```

**After** - server `standalone.xml`:
```xml
<subsystem xmlns="urn:wildfly:opentelemetry:1.0">
    <span-processor type="batch">
        <batch-delay>5000</batch-delay>
        <max-queue-size>2048</max-queue-size>
        <max-export-batch-size>512</max-export-batch-size>
    </span-processor>
</subsystem>
```

**Impact**: Deployment-level processor settings are **ignored**.

**Action Required**: Move to server configuration if needed.

## Non-Breaking Changes

### 1. Service Name (Still Works)

**Deployment** `microprofile-config.properties`:
```properties
otel.service.name=my-custom-service
```

**Impact**: ✅ No change required. Each deployment can still override its service name.

### 2. Resource Attributes (Still Works)

**Deployment** `microprofile-config.properties`:
```properties
otel.resource.attributes=environment=production,version=1.2.3
```

**Impact**: ✅ No change required. Deployment-specific attributes still work.

### 3. Metric Isolation (Improved)

**Before**: Metrics isolated by separate SDK instances
**After**: Metrics isolated by separate `MeterProvider` instances

**Impact**: ✅ Metrics remain isolated. The implementation changed but behavior is the same.

## Migration Steps

### Step 1: Audit Current Configuration

List all deployment configs:
```bash
find . -name "microprofile-config.properties" -exec grep -l "otel\." {} \;
```

Extract OpenTelemetry properties:
```bash
find . -name "microprofile-config.properties" -exec grep "otel\." {} + | sort -u
```

### Step 2: Categorize Settings

**Move to server config**:
- `otel.exporter.otlp.*`
- `otel.traces.sampler*`
- `otel.bsp.*`
- `otel.logs.exporter`
- `otel.metrics.exporter`

**Keep in deployment config**:
- `otel.service.name`
- `otel.resource.attributes`

**Remove (no longer needed)**:
- `otel.sdk.disabled` (controlled by server)
- `otel.propagators` (controlled by server)

### Step 3: Update Server Configuration

Edit `standalone.xml` or use CLI:

```bash
/subsystem=opentelemetry:write-attribute(name=service-name, value=wildfly-server)
/subsystem=opentelemetry/exporter=otlp:write-attribute(name=endpoint, value=http://collector:4317)
/subsystem=opentelemetry/sampler=ratio:write-attribute(name=ratio, value=0.1)
```

### Step 4: Clean Up Deployment Configs

Remove server-controlled properties from each `microprofile-config.properties`:
```bash
# Before
otel.exporter.otlp.endpoint=http://localhost:4317
otel.service.name=my-app
otel.resource.attributes=version=1.0

# After
otel.service.name=my-app
otel.resource.attributes=version=1.0
```

### Step 5: Test Migration

1. **Deploy single application**:
   ```bash
   mvn clean package
   cp target/myapp.war $JBOSS_HOME/standalone/deployments/
   ```

2. **Verify metrics exported**:
   - Check OTLP collector for metrics with `job=myapp.war`
   - Verify service name appears in traces

3. **Deploy multiple applications**:
   ```bash
   cp target/app1.war target/app2.war $JBOSS_HOME/standalone/deployments/
   ```

4. **Verify isolation**:
   - Each app should have distinct `service.name`
   - Metrics should be tagged by deployment
   - Traces should propagate between apps

### Step 6: Validate Behavior

Run the OpenTelemetry test suite:
```bash
cd testsuite/integration/expansion
mvn test -Dtest=OpenTelemetryMultipleWarTestCase
```

## Troubleshooting

### Metrics Not Exported

**Symptom**: Deployment metrics not appearing in OTLP collector

**Check**:
1. Server subsystem configured: `/subsystem=opentelemetry:read-resource`
2. Deployment has CDI enabled (deployment contains `beans.xml`)
3. Application creates metrics via injected `OpenTelemetry`

**Solution**:
```bash
# Verify subsystem
/subsystem=opentelemetry:read-resource(recursive=true)

# Check deployment logs
grep "OpenTelemetry" server.log
```

### Wrong Service Name in Metrics

**Symptom**: Metrics show server service name instead of deployment name

**Check**: Deployment's `microprofile-config.properties` for `otel.service.name`

**Solution**:
```properties
# Add to microprofile-config.properties
otel.service.name=my-app
```

Or rely on default: `{deployment-name}.war`

### Traces Not Propagating

**Symptom**: Traces split across deployments instead of single trace

**Check**: Context propagation headers

**Solution**: Ensure JAX-RS client includes propagation:
```java
@Inject
OpenTelemetry openTelemetry;

Client client = ClientBuilder.newBuilder()
    .register(new ClientTracingFilter(openTelemetry))
    .build();
```

### Deployment Config Ignored

**Symptom**: Deployment's exporter/sampler config has no effect

**Explanation**: This is **expected behavior**. Exporter and sampler are server-controlled.

**Solution**: Move configuration to server `standalone.xml`.

### Multiple OTLP Endpoints

**Symptom**: Need different OTLP endpoints per deployment

**Current Limitation**: Not supported. All deployments share server exporter.

**Workaround**:
1. Use OTLP collector with routing rules
2. Tag metrics with deployment name (automatic)
3. Route in collector based on `deployment.name` attribute

Example collector config:
```yaml
processors:
  routing:
    attribute_source: resource
    from_attribute: deployment.name
    table:
      - value: app1.war
        exporters: [otlp/endpoint1]
      - value: app2.war
        exporters: [otlp/endpoint2]
```

## Rollback Plan

If migration causes issues, rollback to previous version:

1. **Stop WildFly**:
   ```bash
   $JBOSS_HOME/bin/jboss-cli.sh --connect command=:shutdown
   ```

2. **Restore previous version**:
   ```bash
   # Restore backup of modules
   cp -r $JBOSS_HOME.backup/modules/system/layers/base/io/opentelemetry \
         $JBOSS_HOME/modules/system/layers/base/io/
   ```

3. **Restore deployment configs**:
   ```bash
   # Restore microprofile-config.properties from backup
   ```

4. **Start WildFly**:
   ```bash
   $JBOSS_HOME/bin/standalone.sh
   ```

## Getting Help

- **WildFly Issues**: https://issues.redhat.com/browse/WFLY
- **OpenTelemetry Docs**: https://opentelemetry.io/docs/languages/java/
- **Architecture Details**: See `ARCHITECTURE.md`

## Example Configurations

### Simple Setup (Single Deployment)

**Server** `standalone.xml`:
```xml
<subsystem xmlns="urn:wildfly:opentelemetry:1.0">
    <service-name>wildfly-server</service-name>
    <exporter type="otlp">
        <endpoint>http://localhost:4317</endpoint>
    </exporter>
</subsystem>
```

**Deployment** `microprofile-config.properties`:
```properties
otel.service.name=my-app
```

### Multi-Deployment Setup

**Server** `standalone.xml`:
```xml
<subsystem xmlns="urn:wildfly:opentelemetry:1.0">
    <service-name>wildfly-prod-01</service-name>
    <exporter type="otlp">
        <endpoint>http://collector.prod.internal:4317</endpoint>
    </exporter>
    <sampler type="ratio">
        <ratio>0.01</ratio>
    </sampler>
</subsystem>
```

**App1** `microprofile-config.properties`:
```properties
otel.service.name=frontend
otel.resource.attributes=tier=web,team=platform
```

**App2** `microprofile-config.properties`:
```properties
otel.service.name=backend
otel.resource.attributes=tier=api,team=services
```

### Development Setup

**Server** `standalone.xml`:
```xml
<subsystem xmlns="urn:wildfly:opentelemetry:1.0">
    <exporter type="otlp">
        <endpoint>http://localhost:4317</endpoint>
    </exporter>
    <sampler type="on"/> <!-- Sample everything in dev -->
</subsystem>
```

**Deployment**: No config needed, defaults used.

## FAQ

### Q: Can I still use per-deployment exporters?
**A**: No. This was removed to ensure consistent export and reduce resource usage.

### Q: Will my metrics change?
**A**: No. Metrics remain isolated per deployment with the same attributes.

### Q: Do I need to update my application code?
**A**: No. Application code using `@Inject OpenTelemetry` works unchanged.

### Q: What about MicroProfile Telemetry?
**A**: MicroProfile Telemetry continues to work and uses the same shared instance.

### Q: Can I disable OpenTelemetry for specific deployments?
**A**: Not directly. Remove OpenTelemetry instrumentation from the deployment or deploy to a server without the OpenTelemetry subsystem.

### Q: How do I test this locally?
**A**: Start an OTLP collector:
```bash
docker run -p 4317:4317 -p 16686:16686 jaegertracing/all-in-one:latest
```
Deploy your app and check `http://localhost:16686` for traces.
