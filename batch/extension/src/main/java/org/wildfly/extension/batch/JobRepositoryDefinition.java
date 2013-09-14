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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.batch.services.BatchPropertiesService;
import org.wildfly.extension.batch.services.BatchServiceNames;

public class JobRepositoryDefinition extends SimpleResourceDefinition {

    static final String NAME = "job-repository";

    // TODO (jrp) should this be moved into a JDBC def?
    public static final SimpleAttributeDefinition JNDI_NAME = SimpleAttributeDefinitionBuilder.create("jndi-name", ModelType.STRING, false)
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(0, false, true))
            .build();

    // These might be better as AttributeDefinitions
    public static final JobRepositoryDefinition IN_MEMORY = new JobRepositoryDefinition("in-memory", new JobRepositoryAdd() {
        @Override
        protected void addProperties(final OperationContext context, final ModelNode model, final BatchPropertiesService service) throws OperationFailedException {
            service.addProperty(BatchConstants.JOB_REPOSITORY_TYPE, "in-memory");
        }
    });

    public static final JobRepositoryDefinition JDBC = new JobRepositoryDefinition("jdbc", new JobRepositoryAdd(JNDI_NAME) {
        @Override
        protected void addProperties(final OperationContext context, final ModelNode model, final BatchPropertiesService service) throws OperationFailedException {
            final String jndiName = JNDI_NAME.resolveModelAttribute(context, model).asString();
            service.addProperty(BatchConstants.JOB_REPOSITORY_TYPE, "jdbc");
            service.addProperty("datasource-jndi", jndiName);
        }
    }, JNDI_NAME);

    private final Collection<AttributeDefinition> attributes;

    private JobRepositoryDefinition(final String name, final OperationStepHandler addHandler) {
        this(name, addHandler, Collections.<AttributeDefinition>emptyList());
    }

    private JobRepositoryDefinition(final String name, final OperationStepHandler addHandler, final AttributeDefinition... attributes) {
        this(name, addHandler, Arrays.asList(attributes));
    }

    private JobRepositoryDefinition(final String name, final OperationStepHandler addHandler, final Collection<AttributeDefinition> attributes) {
        super(PathElement.pathElement(NAME, name), BatchSubsystemDefinition.getResourceDescriptionResolver(NAME, name), addHandler,
                ReloadRequiredRemoveStepHandler.INSTANCE);
        this.attributes = Collections.unmodifiableCollection(attributes);
    }

    abstract static class JobRepositoryAdd extends AbstractBoottimeAddStepHandler {

        private static final AttributeDefinition[] EMPTY = {};

        private final AttributeDefinition[] attributes;

        protected JobRepositoryAdd() {
            this(EMPTY);
        }

        protected JobRepositoryAdd(AttributeDefinition... attributes) {
            this.attributes = attributes;
        }

        @Override
        protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
            for (AttributeDefinition attribute : attributes) {
                attribute.validateAndSet(operation, model);
            }
        }

        @Override
        public void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
                                    ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
                throws OperationFailedException {

            // Create the BatchEnvironment
            final BatchPropertiesService service = new BatchPropertiesService();
            addProperties(context, model, service);
            final ServiceTarget serviceTarget = context.getServiceTarget();
            final ServiceBuilder<Properties> builder = serviceTarget.addService(BatchServiceNames.BATCH_SERVICE_NAME, service);
            newControllers.add(builder.install());
        }

        protected abstract void addProperties(OperationContext context, ModelNode model, BatchPropertiesService service) throws OperationFailedException;
    }
}
