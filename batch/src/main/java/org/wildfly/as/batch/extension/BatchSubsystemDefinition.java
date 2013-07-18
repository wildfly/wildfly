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

package org.wildfly.as.batch.extension;

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class BatchSubsystemDefinition extends SimpleResourceDefinition {
    /**
     * All element and attribute names in batch schema.
     */
    public static final String JOB_REPOSITORY = "job-repository";
    public static final String JOB_REPOSITORY_TYPE = "type";

    public static final String DEFAULT_JOB_REPOSITORY_TYPE = "in-memory";

    public static final BatchSubsystemDefinition INSTANCE = new BatchSubsystemDefinition();

    protected static final SimpleAttributeDefinition jobRepositoryTypeAttribute =
            new SimpleAttributeDefinitionBuilder(JOB_REPOSITORY_TYPE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName(JOB_REPOSITORY_TYPE)
                    .setValidator(new StringLengthValidator(0, Integer.MAX_VALUE, false, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(DEFAULT_JOB_REPOSITORY_TYPE))
                    .build();

    private BatchSubsystemDefinition() {
        super(BatchSubsystemExtension.SUBSYSTEM_PATH,
                BatchSubsystemExtension.getResourceDescriptionResolver(null),
                BatchSubsystemAdd.INSTANCE,
                //Batch subsystem installs a deployment unit processor, so reload is required
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        //you can register aditional operations here
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        registration.registerReadWriteAttribute(jobRepositoryTypeAttribute,
                null, new ReloadRequiredWriteAttributeHandler(jobRepositoryTypeAttribute));
    }
}
