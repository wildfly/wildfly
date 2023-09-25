/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.batch.jberet.deployment.BatchDeploymentResourceDefinition;
import org.wildfly.extension.batch.jberet.deployment.BatchJobExecutionResourceDefinition;
import org.wildfly.extension.batch.jberet.deployment.BatchJobResourceDefinition;

public class BatchSubsystemExtension implements Extension {

    /**
     * Version numbers for batch subsystem management interface.
     */
    static final ModelVersion VERSION_3_0_0 = ModelVersion.create(3, 0, 0);
    static final ModelVersion VERSION_2_0_0 = ModelVersion.create(2, 0, 0);
    static final ModelVersion VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    static final ModelVersion CURRENT_MODEL_VERSION = VERSION_3_0_0;

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(BatchSubsystemDefinition.NAME, Namespace.BATCH_1_0.getUriString(), BatchSubsystemParser_1_0::new);
        context.setSubsystemXmlMapping(BatchSubsystemDefinition.NAME, Namespace.BATCH_2_0.getUriString(), BatchSubsystemParser_2_0::new);
        context.setSubsystemXmlMapping(BatchSubsystemDefinition.NAME, Namespace.BATCH_3_0.getUriString(), BatchSubsystemParser_3_0::new);
    }

    @Override
    public void initialize(final ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(BatchSubsystemDefinition.NAME, CURRENT_MODEL_VERSION);
        subsystem.registerSubsystemModel(new BatchSubsystemDefinition(context.isRuntimeOnlyRegistrationValid()));
        subsystem.registerXMLElementWriter(new BatchSubsystemWriter());
        // Register the deployment resources
        if (context.isRuntimeOnlyRegistrationValid()) {
            final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(new BatchDeploymentResourceDefinition());
            final ManagementResourceRegistration jobRegistration = deployments.registerSubModel(new BatchJobResourceDefinition());
            jobRegistration.registerSubModel(new BatchJobExecutionResourceDefinition());
        }

    }
}
