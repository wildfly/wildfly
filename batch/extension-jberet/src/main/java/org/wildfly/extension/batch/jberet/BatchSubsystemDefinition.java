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

package org.wildfly.extension.batch.jberet;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jberet.repository.JobRepository;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXmlParserRegisteringProcessor;
import org.jboss.as.threads.ThreadFactoryResolver;
import org.jboss.as.threads.ThreadFactoryResourceDefinition;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.as.threads.UnboundedQueueThreadPoolResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.batch.jberet._private.Capabilities;
import org.wildfly.extension.batch.jberet.deployment.BatchDependencyProcessor;
import org.wildfly.extension.batch.jberet.deployment.BatchDeploymentDescriptorParser_1_0;
import org.wildfly.extension.batch.jberet.deployment.BatchDeploymentResourceProcessor;
import org.wildfly.extension.batch.jberet.deployment.BatchEnvironmentProcessor;
import org.wildfly.extension.batch.jberet.job.repository.InMemoryJobRepositoryDefinition;
import org.wildfly.extension.batch.jberet.job.repository.JdbcJobRepositoryDefinition;
import org.wildfly.extension.requestcontroller.RequestControllerExtension;

public class BatchSubsystemDefinition extends SimpleResourceDefinition {

    /**
     * The name of our subsystem within the model.
     */
    public static final String NAME = "batch-jberet";
    public static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, NAME);
    static final String THREAD_POOL = "thread-pool";
    static final String THREAD_FACTORY = "thread-factory";
    static final PathElement THREAD_POOL_PATH = PathElement.pathElement(THREAD_POOL);

    static final SimpleAttributeDefinition DEFAULT_JOB_REPOSITORY = SimpleAttributeDefinitionBuilder.create("default-job-repository", ModelType.STRING, true)
            .setAllowExpression(false)
            .setAttributeGroup("environment")
            .setAttributeMarshaller(NameAttributeMarshaller.INSTANCE)
            .setCapabilityReference(Capabilities.JOB_REPOSITORY_CAPABILITY.getName(), Capabilities.DEFAULT_JOB_REPOSITORY_CAPABILITY)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition DEFAULT_THREAD_POOL = SimpleAttributeDefinitionBuilder.create("default-thread-pool", ModelType.STRING, true)
            .setAllowExpression(false)
            .setAttributeGroup("environment")
            .setAttributeMarshaller(NameAttributeMarshaller.INSTANCE)
            .setRestartAllServices()
            .build();

    private final boolean registerRuntimeOnly;

    BatchSubsystemDefinition(final boolean registerRuntimeOnly) {
        super(SUBSYSTEM_PATH, BatchResourceDescriptionResolver.getResourceDescriptionResolver(), BatchSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerChildren(final ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new InMemoryJobRepositoryDefinition());
        resourceRegistration.registerSubModel(new JdbcJobRepositoryDefinition());
        // thread-pool resource
        final UnboundedQueueThreadPoolResourceDefinition threadPoolResource = UnboundedQueueThreadPoolResourceDefinition.create(THREAD_POOL_PATH,
                BatchThreadFactoryResolver.INSTANCE, BatchServiceNames.BASE_BATCH_THREAD_POOL_NAME, registerRuntimeOnly);
        resourceRegistration.registerSubModel(threadPoolResource);

        // thread-factory resource
        final ThreadFactoryResourceDefinition threadFactoryResource = new ThreadFactoryResourceDefinition();
        resourceRegistration.registerSubModel(threadFactoryResource);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(DEFAULT_JOB_REPOSITORY, DEFAULT_THREAD_POOL);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_JOB_REPOSITORY, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(DEFAULT_THREAD_POOL, null, writeHandler);
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerCapability(Capabilities.DEFAULT_JOB_REPOSITORY_CAPABILITY);
        resourceRegistration.registerCapability(Capabilities.DEFAULT_THREAD_POOL_CAPABILITY);
    }

    /**
     * Handler responsible for adding the subsystem resource to the model.
     */
    static class BatchSubsystemAdd extends AbstractAddStepHandler {

        static final BatchSubsystemAdd INSTANCE = new BatchSubsystemAdd();

        private BatchSubsystemAdd() {
            super(Stream.of(Capabilities.DEFAULT_JOB_REPOSITORY_CAPABILITY, Capabilities.DEFAULT_THREAD_POOL_CAPABILITY).collect(Collectors.toSet()), DEFAULT_JOB_REPOSITORY, DEFAULT_THREAD_POOL);
        }

        @Override
        protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model)
                throws OperationFailedException {
            // Check if the request-controller subsystem exists
            final boolean rcPresent = context.getOriginalRootResource().hasChild(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, RequestControllerExtension.SUBSYSTEM_NAME));

            context.addStep(new AbstractDeploymentChainStep() {
                public void execute(DeploymentProcessorTarget processorTarget) {
                    processorTarget.addDeploymentProcessor(BatchSubsystemDefinition.NAME,
                            Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_BATCH,
                            new JBossAllXmlParserRegisteringProcessor<>(BatchDeploymentDescriptorParser_1_0.ROOT_ELEMENT,
                                    BatchDeploymentDescriptorParser_1_0.ATTACHMENT_KEY, new BatchDeploymentDescriptorParser_1_0()));
                    processorTarget.addDeploymentProcessor(NAME,
                            Phase.DEPENDENCIES, Phase.DEPENDENCIES_BATCH, new BatchDependencyProcessor());
                    processorTarget.addDeploymentProcessor(NAME,
                            Phase.POST_MODULE, Phase.POST_MODULE_BATCH_ENVIRONMENT, new BatchEnvironmentProcessor(rcPresent));
                    processorTarget.addDeploymentProcessor(NAME,
                            Phase.INSTALL, Phase.INSTALL_BATCH_RESOURCES, new BatchDeploymentResourceProcessor(NAME));

                }
            }, OperationContext.Stage.RUNTIME);

            final ServiceTarget target = context.getServiceTarget();

            final ModelNode defaultJobRepository = DEFAULT_JOB_REPOSITORY.resolveModelAttribute(context, model);
            if (defaultJobRepository.isDefined()) {
                final String name = defaultJobRepository.asString();
                final DefaultValueService<JobRepository> service = DefaultValueService.create();
                target.addService(context.getCapabilityServiceName(Capabilities.DEFAULT_JOB_REPOSITORY_CAPABILITY.getName(), JobRepository.class), service)
                        .addDependency(
                                context.getCapabilityServiceName(Capabilities.JOB_REPOSITORY_CAPABILITY.getName(), name, JobRepository.class),
                                JobRepository.class,
                                service.getInjector()
                        )
                        .install();
            }

            final ModelNode defaultThreadPool = DEFAULT_THREAD_POOL.resolveModelAttribute(context, model);
            if (defaultThreadPool.isDefined()) {
                final String name = defaultThreadPool.asString();
                final DefaultValueService<ExecutorService> service = DefaultValueService.create();
                target.addService(context.getCapabilityServiceName(Capabilities.DEFAULT_THREAD_POOL_CAPABILITY.getName(), ExecutorService.class), service)
                        .addDependency(
                                BatchServiceNames.BASE_BATCH_THREAD_POOL_NAME.append(name),
                                ExecutorService.class,
                                service.getInjector()
                        )
                        .install();
            }
        }
    }

    private static class BatchThreadFactoryResolver extends ThreadFactoryResolver.SimpleResolver {
        static final BatchThreadFactoryResolver INSTANCE = new BatchThreadFactoryResolver();

        private BatchThreadFactoryResolver() {
            super(ThreadsServices.FACTORY);
        }

        @Override
        protected String getThreadGroupName(String threadPoolName) {
            return "Batch Thread";
        }
    }
}
