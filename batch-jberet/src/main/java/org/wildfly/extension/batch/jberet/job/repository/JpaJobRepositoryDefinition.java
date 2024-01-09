/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.job.repository;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.jberet.repository.JobRepository;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.batch.jberet.BatchResourceDescriptionResolver;
import org.wildfly.extension.batch.jberet._private.Capabilities;

/**
 * Represents a JPA job repository.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class JpaJobRepositoryDefinition extends SimpleResourceDefinition {

    public static final String NAME = "jpa-job-repository";
    static final PathElement PATH = PathElement.pathElement(NAME);

    /**
     * A data-source attribute which requires the
     * {@link Capabilities#DATA_SOURCE_CAPABILITY}.
     */
    public static final SimpleAttributeDefinition DATA_SOURCE = SimpleAttributeDefinitionBuilder.create("data-source", ModelType.STRING, false)
            .setCapabilityReference(Capabilities.DATA_SOURCE_CAPABILITY, Capabilities.JOB_REPOSITORY_CAPABILITY)
            .setRestartAllServices()
            .build();

    public JpaJobRepositoryDefinition() {
        super(
                new Parameters(PATH, BatchResourceDescriptionResolver.getResourceDescriptionResolver(NAME))
                        .setAddHandler(new JpaRepositoryAddHandler())
                        .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                        .setCapabilities(Capabilities.JOB_REPOSITORY_CAPABILITY)
        );
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadWriteAttribute(DATA_SOURCE, null, new ReloadRequiredWriteAttributeHandler(DATA_SOURCE));
    }

    private static class JpaRepositoryAddHandler extends AbstractAddStepHandler {

        JpaRepositoryAddHandler() {
            super(DATA_SOURCE);
        }

        @Override
        protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
            super.performRuntime(context, operation, model);
            final String name = context.getCurrentAddressValue();
            final String dsName = DATA_SOURCE.resolveModelAttribute(context, model).asString();
            final Integer executionRecordsLimit = CommonAttributes.EXECUTION_RECORDS_LIMIT.resolveModelAttribute(context, model).asIntOrNull();
            final ServiceTarget target = context.getServiceTarget();
            final ServiceName sn = context.getCapabilityServiceName(Capabilities.JOB_REPOSITORY_CAPABILITY.getName(), name, JobRepository.class);
            final ServiceBuilder<?> sb = target.addService(sn);
            final Consumer<JobRepository> jobRepositoryConsumer = sb.provides(sn);
            final Supplier<ExecutorService> executorSupplier = Services.requireServerExecutor(sb);
            final Supplier<DataSource> dataSourceSupplier = sb.requires(context.getCapabilityServiceName(Capabilities.DATA_SOURCE_CAPABILITY, dsName, DataSource.class));
            final JpaJobRepositoryService service = new JpaJobRepositoryService(jobRepositoryConsumer, dataSourceSupplier, executorSupplier, executionRecordsLimit);
            sb.setInstance(service);
            sb.install();
        }
    }
}
