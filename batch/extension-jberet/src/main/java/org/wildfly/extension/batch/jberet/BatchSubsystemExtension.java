/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

    private static final int MANAGEMENT_API_MAJOR_VERSION = 2;
    private static final int MANAGEMENT_API_MINOR_VERSION = 0;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    /**
     * Version numbers for batch subsystem management interface.
     */
    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(
            MANAGEMENT_API_MAJOR_VERSION,
            MANAGEMENT_API_MINOR_VERSION,
            MANAGEMENT_API_MICRO_VERSION);

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(BatchSubsystemDefinition.NAME, Namespace.BATCH_1_0.getUriString(), BatchSubsystemParser_1_0::new);
        context.setSubsystemXmlMapping(BatchSubsystemDefinition.NAME, Namespace.BATCH_2_0.getUriString(), BatchSubsystemParser_2_0::new);
    }

    @Override
    public void initialize(final ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(BatchSubsystemDefinition.NAME, CURRENT_MODEL_VERSION);
        subsystem.registerSubsystemModel(new BatchSubsystemDefinition(context.isRuntimeOnlyRegistrationValid()));
        subsystem.registerXMLElementWriter(new BatchSubsystemWriter());
        // Register the deployment resources
        if (context.isRuntimeOnlyRegistrationValid()) {
            final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(new BatchDeploymentResourceDefinition());
            final ManagementResourceRegistration jobRegistration = deployments.registerSubModel(BatchJobResourceDefinition.INSTANCE);
            jobRegistration.registerSubModel(new BatchJobExecutionResourceDefinition());
        }

    }
}
