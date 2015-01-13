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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.batch.job.repository.JobRepositoryFactory;
import org.wildfly.extension.batch.job.repository.JobRepositoryType;

/**
 * A job repository resource used to configure settings of a {@link BatchSubsystemDefinition#JOB_REPOSITORY_TYPE job
 * repository type}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class JobRepositoryDefinition extends SimpleResourceDefinition {

    /**
     * The name of the resource
     */
    public static final String NAME = "job-repository";

    /**
     * The JNDI name for a {@link #JDBC JDBC} resource.
     */
    public static final SimpleAttributeDefinition JNDI_NAME = SimpleAttributeDefinitionBuilder.create("jndi-name", ModelType.STRING, true)
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, true, true))
            .build();

    /**
     * A JDBC resource definition
     */
    public static final JobRepositoryDefinition JDBC = new JobRepositoryDefinition(JobRepositoryType.JDBC.toString(), JNDI_NAME);

    private JobRepositoryDefinition(final String name, final AttributeDefinition... attributes) {
        super(PathElement.pathElement(NAME, name), BatchSubsystemDefinition.getResourceDescriptionResolver(NAME, name),
                new JobRepositoryAdd(attributes), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(JNDI_NAME, null, new ReloadRequiredWriteAttributeHandler(JNDI_NAME));
    }

    private static class JobRepositoryAdd extends AbstractAddStepHandler {

        public JobRepositoryAdd(final AttributeDefinition... attributes) {
            super(attributes);
        }

        @Override
        protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
            final ModelNode jndiName = JNDI_NAME.resolveModelAttribute(context, model);
            if (jndiName.isDefined()) {
                JobRepositoryFactory.getInstance().setJndiName(jndiName.asString());
            }
        }
    }
}
