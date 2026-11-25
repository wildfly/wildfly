/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import org.jboss.as.test.shared.ManagementServerSetupTask;

/**
 * A ServerSetupTask to add vertx extension and subsystem, add a Vertx instance with default options.
 * It will clean up the configuration on tearDown.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class VertxSubsystemSetupTask extends ManagementServerSetupTask {
    public VertxSubsystemSetupTask() {
        super(createContainerConfigurationBuilder()
                .setupScript(createScriptBuilder()
                        .startBatch()
                        .add("/extension=org.wildfly.extension.vertx:add()")
                        .add("/subsystem=vertx:add()")
                        .endBatch()
                        .startBatch()
                        .add("/subsystem=vertx/vertx=vertx:add()")
                        .endBatch()
                        .build())
                .tearDownScript(createScriptBuilder()
                        .startBatch()
                        .add("/subsystem=vertx/vertx=vertx:remove()")
                        .add("/subsystem=vertx:remove()")
                        .add("/extension=org.wildfly.extension.vertx:remove()")
                        .endBatch()
                        .build())
                .build());
    }
}
