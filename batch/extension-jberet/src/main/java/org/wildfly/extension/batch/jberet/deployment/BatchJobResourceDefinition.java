/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch.jberet.deployment;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.batch.jberet.BatchResourceDescriptionResolver;

/**
 * A definition representing a job resource.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchJobResourceDefinition extends SimpleResourceDefinition {
    static final String JOB = "job";

    private static final SimpleAttributeDefinition RUNNING_EXECUTIONS = SimpleAttributeDefinitionBuilder.create("running-executions", ModelType.INT)
            .setStorageRuntime()
            .build();

    private static final SimpleAttributeDefinition INSTANCE_COUNT = SimpleAttributeDefinitionBuilder.create("instance-count", ModelType.INT)
            .setStorageRuntime()
            .build();

    private static final SimpleAttributeDefinition JOB_XML_NAME = SimpleAttributeDefinitionBuilder.create("job-xml-name", ModelType.STRING)
            .setStorageRuntime()
            .build();

    private static final SimpleListAttributeDefinition JOB_XML_NAMES = SimpleListAttributeDefinition.Builder.of("job-xml-names", JOB_XML_NAME)
            .setStorageRuntime()
            .build();

    public static final BatchJobResourceDefinition INSTANCE = new BatchJobResourceDefinition();

    private BatchJobResourceDefinition() {
        super(new Parameters(PathElement.pathElement(JOB), BatchResourceDescriptionResolver.getResourceDescriptionResolver("deployment", "job")).setRuntime());
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(RUNNING_EXECUTIONS, new JobOperationReadOnlyStepHandler() {
            @Override
            protected void updateModel(final OperationContext context, final ModelNode model, final WildFlyJobOperator jobOperator, final String jobName) throws OperationFailedException {
                model.set(jobOperator.allowMissingJob(() -> jobOperator.getRunningExecutions(jobName).size(), 0));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(INSTANCE_COUNT, new JobOperationReadOnlyStepHandler() {
            @Override
            protected void updateModel(final OperationContext context, final ModelNode model, final WildFlyJobOperator jobOperator, final String jobName) throws OperationFailedException {
                model.set(jobOperator.allowMissingJob(() -> jobOperator.getJobInstanceCount(jobName), 0));
            }
        });
        resourceRegistration.registerReadOnlyAttribute(JOB_XML_NAMES, new JobOperationReadOnlyStepHandler() {
            @Override
            protected void updateModel(final OperationContext context, final ModelNode model, final WildFlyJobOperator jobOperator, final String jobName) throws OperationFailedException {
                final ModelNode list = model.setEmptyList();
                for (String jobXmlName : jobOperator.getJobXmlNames(jobName)) {
                    list.add(jobXmlName);
                }
            }
        });
    }

}
