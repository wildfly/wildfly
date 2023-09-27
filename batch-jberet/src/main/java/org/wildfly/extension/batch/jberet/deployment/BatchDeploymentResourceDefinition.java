/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import java.util.Properties;
import jakarta.batch.operations.JobExecutionAlreadyCompleteException;
import jakarta.batch.operations.JobExecutionNotMostRecentException;
import jakarta.batch.operations.JobExecutionNotRunningException;
import jakarta.batch.operations.JobRestartException;
import jakarta.batch.operations.JobSecurityException;
import jakarta.batch.operations.JobStartException;
import jakarta.batch.operations.NoSuchJobExecutionException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.batch.jberet.BatchResourceDescriptionResolver;
import org.wildfly.extension.batch.jberet.BatchSubsystemDefinition;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchDeploymentResourceDefinition extends SimpleResourceDefinition {

    private static final ResourceDescriptionResolver DEFAULT_RESOLVER = BatchResourceDescriptionResolver.getResourceDescriptionResolver("deployment");

    private static final SimpleAttributeDefinition EXECUTION_ID = SimpleAttributeDefinitionBuilder.create("execution-id", ModelType.LONG, false)
            .setValidator(new LongRangeValidator(1L, false))
            .build();

    private static final SimpleAttributeDefinition JOB_XML_NAME = SimpleAttributeDefinitionBuilder.create("job-xml-name", ModelType.STRING, false)
            .build();

    private static final SimpleMapAttributeDefinition PROPERTIES = new SimpleMapAttributeDefinition.Builder("properties", ModelType.STRING, true)
            .build();

    private static final SimpleListAttributeDefinition JOB_XML_NAMES = SimpleListAttributeDefinition.Builder.of("job-xml-names", JOB_XML_NAME)
            .setStorageRuntime()
            .build();

    private static final SimpleOperationDefinition START_JOB = new SimpleOperationDefinitionBuilder("start-job", DEFAULT_RESOLVER)
            .setParameters(JOB_XML_NAME, PROPERTIES)
            .setReplyType(ModelType.LONG)
            .setRuntimeOnly()
            .build();

    private static final SimpleOperationDefinition RESTART_JOB = new SimpleOperationDefinitionBuilder("restart-job", DEFAULT_RESOLVER)
            .setParameters(EXECUTION_ID, PROPERTIES)
            .setReplyType(ModelType.LONG)
            .setRuntimeOnly()
            .build();

    private static final SimpleOperationDefinition STOP_JOB = new SimpleOperationDefinitionBuilder("stop-job", DEFAULT_RESOLVER)
            .setParameters(EXECUTION_ID)
            .setRuntimeOnly()
            .build();

    public BatchDeploymentResourceDefinition() {
        super(new Parameters(BatchSubsystemDefinition.SUBSYSTEM_PATH, DEFAULT_RESOLVER)
                .setRuntime()
                .setFeature(false));
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(START_JOB, new JobOperationStepHandler() {
            @Override
            protected void execute(final OperationContext context, final ModelNode operation, final WildFlyJobOperator jobOperator) throws OperationFailedException {
                // Resolve the job XML name
                final String jobName = resolveValue(context, operation, JOB_XML_NAME).asString();
                final Properties properties = resolvePropertyValue(context, operation, PROPERTIES);
                try {
                    final long executionId = jobOperator.start(jobName, properties);
                    context.getResult().set(executionId);
                } catch (JobStartException | JobSecurityException e) {
                    throw createOperationFailure(e);
                }
            }
        });

        resourceRegistration.registerOperationHandler(STOP_JOB, new JobOperationStepHandler() {
            @Override
            protected void execute(final OperationContext context, final ModelNode operation, final WildFlyJobOperator jobOperator) throws OperationFailedException {
                // Resolve the execution id
                final long executionId = resolveValue(context, operation, EXECUTION_ID).asLong();
                try {
                    jobOperator.stop(executionId);
                } catch (NoSuchJobExecutionException | JobExecutionNotRunningException | JobSecurityException e) {
                    throw createOperationFailure(e);
                }
            }
        });

        resourceRegistration.registerOperationHandler(RESTART_JOB, new JobOperationStepHandler() {
            @Override
            protected void execute(final OperationContext context, final ModelNode operation, final WildFlyJobOperator jobOperator) throws OperationFailedException {
                // Resolve the execution id
                final long executionId = resolveValue(context, operation, EXECUTION_ID).asLong();
                final Properties properties = resolvePropertyValue(context, operation, PROPERTIES);
                try {
                    final long newExecutionId = jobOperator.restart(executionId, properties);
                    context.getResult().set(newExecutionId);
                } catch (JobExecutionAlreadyCompleteException | NoSuchJobExecutionException | JobExecutionNotMostRecentException | JobRestartException | JobSecurityException e) {
                    throw createOperationFailure(e);
                }
            }
        });
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(JOB_XML_NAMES, new JobOperationStepHandler(false) {
            @Override
            protected void execute(final OperationContext context, final ModelNode operation, final WildFlyJobOperator jobOperator) throws OperationFailedException {
                final ModelNode list = context.getResult().setEmptyList();
                for (String jobXmlName : jobOperator.getJobXmlNames()) {
                    list.add(jobXmlName);
                }
            }
        });
    }
}
