/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import org.jboss.as.test.shared.ManagementServerSetupTask;

/**
 * A ServerSetupTask to set up logger categories to file handlers for log messages check in the tests.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class LoggingServerSetupTask extends ManagementServerSetupTask {

    public LoggingServerSetupTask() {
        this(SMALLRYE_OPENTELEMETRY_LOG_FILE, VERTX_FEATURE_PACK_LOG_FILE);
    }

    static final String SMALLRYE_OPENTELEMETRY_LOG_FILE = "smallrye-opentelemetry.log";
    static final String VERTX_FEATURE_PACK_LOG_FILE = "vertx-feature-pack.log";

    public LoggingServerSetupTask(String smallryeOpentelemetryLogFile, String vertxSubsystemLogFile) {
        super(createContainerConfigurationBuilder()
                .setupScript(createScriptBuilder()
                        .startBatch()
                        .add("/subsystem=logging/file-handler=otel-exporter:add(file={relative-to=jboss.server.log.dir, path=%s}, append=false, level=DEBUG)", smallryeOpentelemetryLogFile)
                        .add("/subsystem=logging/logger=io.smallrye.opentelemetry.implementation.exporters:add(level=DEBUG, handlers=[otel-exporter])")
                        .add("/subsystem=logging/file-handler=vertx-extension-logger-handler:add(file={relative-to=jboss.server.log.dir, path=%s}, append=false, level=DEBUG)", vertxSubsystemLogFile)
                        .add("/subsystem=logging/logger=org.wildfly.extension.vertx:add(level=DEBUG, handlers=[vertx-extension-logger-handler])")
                        .endBatch()
                        .build())
                .tearDownScript(createScriptBuilder()
                        .startBatch()
                        .add("/subsystem=logging/logger=org.wildfly.extension.vertx:remove()")
                        .add("/subsystem=logging/file-handler=vertx-extension-logger-handler:remove()")
                        .add("/subsystem=logging/logger=io.smallrye.opentelemetry.implementation.exporters:remove()")
                        .add("/subsystem=logging/file-handler=otel-exporter:remove()")
                        .endBatch()
                        .build())
                .build());
    }

}
