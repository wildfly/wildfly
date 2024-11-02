/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.microprofile.faulttolerance;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.shared.ManagementServerSetupTask;

/**
 * Reusable {@link ServerSetupTask} that removes 'microprofile-telemetry' for the duration of the test to avoid
 * automatic registration of metrics with telemetry metrics, e.g., to test micrometer metrics integration in isolation.
 *
 * @author Radoslav Husar
 */
public class DisableTelemetryServerSetupTask extends ManagementServerSetupTask {
    public DisableTelemetryServerSetupTask() {
        super(createContainerConfigurationBuilder()
                .setupScript(createScriptBuilder()
                        .startBatch()
                        .add("/subsystem=microprofile-telemetry:remove")
                        .endBatch()
                        .build())
                .tearDownScript(createScriptBuilder()
                        .startBatch()
                        .add("/subsystem=microprofile-telemetry:add")
                        .endBatch()
                        .build())
                .build());
    }
}
