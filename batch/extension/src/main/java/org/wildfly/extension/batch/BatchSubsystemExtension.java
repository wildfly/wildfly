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

package org.wildfly.extension.batch;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.batch.deployment.BatchJobExecutionResourceDefinition;
import org.wildfly.extension.batch.deployment.BatchJobResourceDefinition;

public class BatchSubsystemExtension implements Extension {

    /**
     * Version numbers for batch subsystem management interface.
     */
    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(1, 0, 0);
    public static final String SUBSYSTEM_NAME = BatchSubsystemDefinition.NAME;

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (Namespace namespace : Namespace.values()) {
            final String uri = namespace.getUriString();
            if (uri != null) {
                context.setSubsystemXmlMapping(BatchSubsystemDefinition.NAME, uri, BatchSubsystemParser.INSTANCE);
            }
        }
    }

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(BatchSubsystemDefinition.NAME, CURRENT_MODEL_VERSION);
        subsystem.registerSubsystemModel(BatchSubsystemDefinition.INSTANCE);
        subsystem.registerXMLElementWriter(BatchSubsystemParser.INSTANCE);
        // Register the deployment resources
        if (context.isRuntimeOnlyRegistrationValid()) {
            final SimpleResourceDefinition deploymentResource = new SimpleResourceDefinition(
                    BatchSubsystemDefinition.SUBSYSTEM_PATH,
                    BatchResourceDescriptionResolver.getResourceDescriptionResolver("deployment"));
            final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(deploymentResource);
            final ManagementResourceRegistration jobRegistration = deployments.registerSubModel(BatchJobResourceDefinition.INSTANCE);
            jobRegistration.registerSubModel(new BatchJobExecutionResourceDefinition()).setRuntimeOnly(true);
        }
    }
}
