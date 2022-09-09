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

package org.wildfly.extension.batch.jberet.job.repository;

import org.jberet.repository.JobRepository;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.batch.jberet.BatchResourceDescriptionResolver;
import org.wildfly.extension.batch.jberet._private.Capabilities;

import java.util.function.Consumer;

/**
 * Represents an in-memory job repository.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class InMemoryJobRepositoryDefinition extends SimpleResourceDefinition {

    public static final String NAME = "in-memory-job-repository";
    public static final PathElement PATH = PathElement.pathElement(NAME);

    public InMemoryJobRepositoryDefinition() {
        super(
                new Parameters(PATH, BatchResourceDescriptionResolver.getResourceDescriptionResolver(NAME))
                        .setAddHandler(new InMemoryAddHandler())
                        .setRemoveHandler(new ReloadRequiredRemoveStepHandler(Capabilities.JOB_REPOSITORY_CAPABILITY))
                        .setCapabilities(Capabilities.JOB_REPOSITORY_CAPABILITY)
        );
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadWriteAttribute(CommonAttributes.EXECUTION_RECORDS_LIMIT, null,
                new ReloadRequiredWriteAttributeHandler(CommonAttributes.EXECUTION_RECORDS_LIMIT));
    }

    private static class InMemoryAddHandler extends AbstractAddStepHandler {
        InMemoryAddHandler() {
            super(Capabilities.JOB_REPOSITORY_CAPABILITY, CommonAttributes.EXECUTION_RECORDS_LIMIT);
        }

        @Override
        protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
            super.performRuntime(context, operation, model);
            final String name = context.getCurrentAddressValue();
            final Integer executionRecordsLimit = CommonAttributes.EXECUTION_RECORDS_LIMIT.resolveModelAttribute(context, model).asIntOrNull();
            final ServiceName inMemorySN = context.getCapabilityServiceName(Capabilities.JOB_REPOSITORY_CAPABILITY.getName(), name, JobRepository.class);
            final ServiceBuilder<?> sb = context.getServiceTarget().addService(inMemorySN);
            final Consumer<JobRepository> jobRepositoryConsumer = sb.provides(inMemorySN);
            sb.setInstance(new InMemoryJobRepositoryService(jobRepositoryConsumer, executionRecordsLimit));
            sb.install();
        }
    }
}
