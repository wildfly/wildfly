/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.job.repository;

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

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents a JDBC job repository.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class JdbcJobRepositoryDefinition extends SimpleResourceDefinition {

    public static final String NAME = "jdbc-job-repository";
    public static final PathElement PATH = PathElement.pathElement(NAME);

    /**
     * A data-source attribute which requires the {@link Capabilities#DATA_SOURCE_CAPABILITY}.
     */
    public static final SimpleAttributeDefinition DATA_SOURCE = SimpleAttributeDefinitionBuilder.create("data-source", ModelType.STRING, false)
            .setCapabilityReference(Capabilities.DATA_SOURCE_CAPABILITY, Capabilities.JOB_REPOSITORY_CAPABILITY)
            .setRestartAllServices()
            .build();

    public JdbcJobRepositoryDefinition() {
        super(
                new Parameters(PATH, BatchResourceDescriptionResolver.getResourceDescriptionResolver(NAME))
                        .setAddHandler(new JdbcRepositoryAddHandler())
                        .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                        .setCapabilities(Capabilities.JOB_REPOSITORY_CAPABILITY)
        );
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadWriteAttribute(DATA_SOURCE, null,
                new ReloadRequiredWriteAttributeHandler(DATA_SOURCE));
        resourceRegistration.registerReadWriteAttribute(CommonAttributes.EXECUTION_RECORDS_LIMIT, null,
                new ReloadRequiredWriteAttributeHandler(CommonAttributes.EXECUTION_RECORDS_LIMIT));
    }

    private static class JdbcRepositoryAddHandler extends AbstractAddStepHandler {

        JdbcRepositoryAddHandler() {
            super(DATA_SOURCE, CommonAttributes.EXECUTION_RECORDS_LIMIT);
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
            final JdbcJobRepositoryService service = new JdbcJobRepositoryService(jobRepositoryConsumer, dataSourceSupplier, executorSupplier, executionRecordsLimit);
            sb.setInstance(service);
            sb.install();
        }
    }
}
