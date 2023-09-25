/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.lra;

import org.jboss.as.test.shared.ManagementServerSetupTask;

public class EnableLRAExtensionsSetupTask extends ManagementServerSetupTask {
    private static final String MODULE_LRA_PARTICIPANT = "org.wildfly.extension.microprofile.lra-participant";
    private static final String MODULE_LRA_COORDINATOR = "org.wildfly.extension.microprofile.lra-coordinator";
    private static final String SUBSYSTEM_LRA_PARTICIPANT = "microprofile-lra-participant";
    private static final String SUBSYSTEM_LRA_COORDINATOR = "microprofile-lra-coordinator";

    public EnableLRAExtensionsSetupTask() {
        super(createContainerConfigurationBuilder()
            .setupScript(createScriptBuilder()
                .startBatch()
                .add("/extension=" + MODULE_LRA_COORDINATOR + ":add")
                .add("/subsystem=" + SUBSYSTEM_LRA_COORDINATOR + ":add")
                .add("/extension=" + MODULE_LRA_PARTICIPANT + ":add")
                .add("/subsystem=" + SUBSYSTEM_LRA_PARTICIPANT + ":add")
                .endBatch()
                .build()
            )
            .tearDownScript(createScriptBuilder()
                .startBatch()
                .add("/subsystem=" + SUBSYSTEM_LRA_PARTICIPANT + ":remove")
                .add("/extension=" + MODULE_LRA_PARTICIPANT + ":remove")
                .add("/subsystem=" + SUBSYSTEM_LRA_COORDINATOR + ":remove")
                .add("/extension=" + MODULE_LRA_COORDINATOR + ":remove")
                .endBatch()
                .build())
            .build());
    }
}