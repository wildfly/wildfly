/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem.deployment;

import static org.jboss.as.ejb3.subsystem.deployment.TimerResourceDefinition.CALENDAR_TIMER;
import static org.jboss.as.ejb3.subsystem.deployment.TimerResourceDefinition.INFO;
import static org.jboss.as.ejb3.subsystem.deployment.TimerResourceDefinition.NEXT_TIMEOUT;
import static org.jboss.as.ejb3.subsystem.deployment.TimerResourceDefinition.PERSISTENT;
import static org.jboss.as.ejb3.subsystem.deployment.TimerResourceDefinition.SCHEDULE;
import static org.jboss.as.ejb3.subsystem.deployment.TimerResourceDefinition.TIME_REMAINING;

import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectMapAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.invocationmetrics.InvocationMetrics;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCache;
import org.jboss.as.ejb3.subsystem.EJB3Extension;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.ejb.client.SessionID;

/**
 * Base class for {@link org.jboss.as.controller.ResourceDefinition}s describing runtime {@link EJBComponent}s.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractEJBComponentResourceDefinition extends SimpleResourceDefinition {

     static final SimpleAttributeDefinition COMPONENT_CLASS_NAME = new SimpleAttributeDefinitionBuilder("component-class-name", ModelType.STRING)
            .setValidator(new StringLengthValidator(1))
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

     static final StringListAttributeDefinition JNDI_NAMES = StringListAttributeDefinition.Builder.of("jndi-names")
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

     static final StringListAttributeDefinition BUSINESS_LOCAL = StringListAttributeDefinition.Builder.of("business-local")
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

     static final StringListAttributeDefinition BUSINESS_REMOTE = StringListAttributeDefinition.Builder.of("business-remote")
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

     static final SimpleAttributeDefinition TIMEOUT_METHOD = new SimpleAttributeDefinitionBuilder("timeout-method", ModelType.STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

     static final StringListAttributeDefinition ASYNC_METHODS = StringListAttributeDefinition.Builder.of("async-methods")
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

     static final SimpleAttributeDefinition TRANSACTION_TYPE = new SimpleAttributeDefinitionBuilder("transaction-type", ModelType.STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    private static final AttributeDefinition EXECUTION_TIME = new SimpleAttributeDefinitionBuilder("execution-time", ModelType.LONG)
            .setUndefinedMetricValue(ModelNode.ZERO)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME, AttributeAccess.Flag.COUNTER_METRIC)
            .build();

    private static final AttributeDefinition INVOCATIONS = new SimpleAttributeDefinitionBuilder("invocations", ModelType.LONG)
            .setUndefinedMetricValue(ModelNode.ZERO)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME, AttributeAccess.Flag.COUNTER_METRIC)
            .build();

    private static final AttributeDefinition PEAK_CONCURRENT_INVOCATIONS = new SimpleAttributeDefinitionBuilder("peak-concurrent-invocations", ModelType.LONG)
            .setUndefinedMetricValue(ModelNode.ZERO)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    public static final SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder("security-domain", ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, true))
            .build();

    private static final AttributeDefinition WAIT_TIME = new SimpleAttributeDefinitionBuilder("wait-time", ModelType.LONG)
            .setUndefinedMetricValue(ModelNode.ZERO)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME, AttributeAccess.Flag.COUNTER_METRIC)
            .build();

    private static final AttributeDefinition METHODS = ObjectMapAttributeDefinition.Builder.of(
            "methods",
            ObjectTypeAttributeDefinition.Builder.of("complex", EXECUTION_TIME, INVOCATIONS, WAIT_TIME)
            .build())
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    public static final SimpleAttributeDefinition RUN_AS_ROLE = new SimpleAttributeDefinitionBuilder("run-as-role", ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, true))
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    public static final StringListAttributeDefinition DECLARED_ROLES = StringListAttributeDefinition.Builder.of("declared-roles")
            .setRequired(false)
            .setElementValidator(new StringLengthValidator(1))
            .setStorageRuntime()
            .build();

    private static final AttributeDefinition CACHE_SIZE = new SimpleAttributeDefinitionBuilder("cache-size", ModelType.LONG)
            .setUndefinedMetricValue(ModelNode.ZERO)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    private static final AttributeDefinition PASSIVATED_SIZE = new SimpleAttributeDefinitionBuilder("passivated-count",
            ModelType.LONG)
            .setUndefinedMetricValue(ModelNode.ZERO)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    private static final AttributeDefinition TOTAL_SIZE = new SimpleAttributeDefinitionBuilder("total-size", ModelType.LONG)
            .setUndefinedMetricValue(ModelNode.ZERO)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    // Pool attributes

    public static final SimpleAttributeDefinition POOL_AVAILABLE_COUNT = new SimpleAttributeDefinitionBuilder("pool-available-count", ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();
    public static final SimpleAttributeDefinition POOL_CREATE_COUNT = new SimpleAttributeDefinitionBuilder("pool-create-count", ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME, AttributeAccess.Flag.COUNTER_METRIC).build();
    public static final SimpleAttributeDefinition POOL_CURRENT_SIZE = new SimpleAttributeDefinitionBuilder("pool-current-size", ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
    public static final SimpleAttributeDefinition POOL_NAME = new SimpleAttributeDefinitionBuilder("pool-name", ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
    public static final SimpleAttributeDefinition POOL_REMOVE_COUNT = new SimpleAttributeDefinitionBuilder("pool-remove-count", ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME, AttributeAccess.Flag.COUNTER_METRIC).build();
    public static final SimpleAttributeDefinition POOL_MAX_SIZE = new SimpleAttributeDefinitionBuilder("pool-max-size", ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();

    static final ObjectTypeAttributeDefinition TIMER = new ObjectTypeAttributeDefinition.Builder("timer",
            TIME_REMAINING, NEXT_TIMEOUT, CALENDAR_TIMER, PERSISTENT, INFO, new ObjectTypeAttributeDefinition.Builder(SCHEDULE.getName(), SCHEDULE.getValueTypes()).setSuffix("schedule").build())
            .setStorageRuntime()
            .build();

    static final ObjectListAttributeDefinition TIMERS = new ObjectListAttributeDefinition.Builder("timers", TIMER)
            .setStorageRuntime()
            .build();

    final EJBComponentType componentType;

    public AbstractEJBComponentResourceDefinition(final EJBComponentType componentType) {
        super(PathElement.pathElement(componentType.getResourceType()),
                EJB3Extension.getResourceDescriptionResolver(componentType.getResourceType()));
        this.componentType = componentType;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        final AbstractEJBComponentRuntimeHandler<?> handler = componentType.getRuntimeHandler();
        resourceRegistration.registerReadOnlyAttribute(COMPONENT_CLASS_NAME, handler);
        resourceRegistration.registerReadOnlyAttribute(SECURITY_DOMAIN, handler);
        resourceRegistration.registerReadOnlyAttribute(RUN_AS_ROLE, handler);
        resourceRegistration.registerReadOnlyAttribute(DECLARED_ROLES, handler);
        resourceRegistration.registerReadOnlyAttribute(TRANSACTION_TYPE, handler);

        if (!componentType.equals(EJBComponentType.MESSAGE_DRIVEN)) {
            resourceRegistration.registerReadOnlyAttribute(JNDI_NAMES, handler);
            resourceRegistration.registerReadOnlyAttribute(BUSINESS_LOCAL, handler);
            resourceRegistration.registerReadOnlyAttribute(BUSINESS_REMOTE, handler);
            resourceRegistration.registerReadOnlyAttribute(ASYNC_METHODS, handler);
        }

        if (componentType.hasTimer()) {
            resourceRegistration.registerReadOnlyAttribute(TIMERS, handler);
            resourceRegistration.registerReadOnlyAttribute(TIMEOUT_METHOD, handler);
        }

        if (componentType.hasPool()) {
            resourceRegistration.registerReadOnlyAttribute(POOL_AVAILABLE_COUNT, handler);
            resourceRegistration.registerReadOnlyAttribute(POOL_CREATE_COUNT, handler);
            resourceRegistration.registerReadOnlyAttribute(POOL_NAME, handler);
            resourceRegistration.registerReadOnlyAttribute(POOL_REMOVE_COUNT, handler);
            resourceRegistration.registerReadOnlyAttribute(POOL_CURRENT_SIZE, handler);
            resourceRegistration.registerReadWriteAttribute(POOL_MAX_SIZE, handler, handler);
        }

        if (componentType.equals(EJBComponentType.STATEFUL)) {
            resourceRegistration.registerMetric(CACHE_SIZE, new AbstractRuntimeMetricsHandler() {
                @Override
                protected void executeReadMetricStep(final OperationContext context, final ModelNode operation, final EJBComponent component) {
                    StatefulSessionBeanCache<SessionID, StatefulSessionComponentInstance> cache = ((StatefulSessionComponent) component).getCache();
                    context.getResult().set(cache.getActiveCount());
                }
            });
            resourceRegistration.registerMetric(PASSIVATED_SIZE, new AbstractRuntimeMetricsHandler() {
                @Override
                protected void executeReadMetricStep(final OperationContext context, final ModelNode operation, final EJBComponent component) {
                    StatefulSessionBeanCache<SessionID, StatefulSessionComponentInstance> cache = ((StatefulSessionComponent) component).getCache();
                    context.getResult().set(cache.getPassiveCount());
                }
            });
            resourceRegistration.registerMetric(TOTAL_SIZE, new AbstractRuntimeMetricsHandler() {
                @Override
                protected void executeReadMetricStep(final OperationContext context, final ModelNode operation, final EJBComponent component) {
                    StatefulSessionBeanCache<SessionID, StatefulSessionComponentInstance> cache = ((StatefulSessionComponent) component).getCache();
                    context.getResult().set(cache.getActiveCount() + cache.getPassiveCount());
                }
            });
        }

        resourceRegistration.registerMetric(EXECUTION_TIME, new AbstractRuntimeMetricsHandler() {
            @Override
            protected void executeReadMetricStep(final OperationContext context, final ModelNode operation, final EJBComponent component) {
                context.getResult().set(component.getInvocationMetrics().getExecutionTime());
            }
        });
        resourceRegistration.registerMetric(INVOCATIONS, new AbstractRuntimeMetricsHandler() {
            @Override
            protected void executeReadMetricStep(final OperationContext context, final ModelNode operation, final EJBComponent component) {
                context.getResult().set(component.getInvocationMetrics().getInvocations());
            }
        });
        resourceRegistration.registerMetric(PEAK_CONCURRENT_INVOCATIONS, new AbstractRuntimeMetricsHandler() {
            @Override
            protected void executeReadMetricStep(final OperationContext context, final ModelNode operation, final EJBComponent component) {
                context.getResult().set(component.getInvocationMetrics().getPeakConcurrent());
            }
        });
        resourceRegistration.registerMetric(WAIT_TIME, new AbstractRuntimeMetricsHandler() {
            @Override
            protected void executeReadMetricStep(final OperationContext context, final ModelNode operation, final EJBComponent component) {
                context.getResult().set(component.getInvocationMetrics().getWaitTime());
            }
        });
        resourceRegistration.registerMetric(METHODS, new AbstractRuntimeMetricsHandler() {
            @Override
            protected void executeReadMetricStep(final OperationContext context, final ModelNode operation, final EJBComponent component) {
                context.getResult().setEmptyObject();
                for (final Map.Entry<String, InvocationMetrics.Values> entry : component.getInvocationMetrics().getMethods().entrySet()) {
                    final InvocationMetrics.Values values = entry.getValue();
                    final ModelNode result = new ModelNode();
                    result.get("execution-time").set(values.getExecutionTime());
                    result.get("invocations").set(values.getInvocations());
                    result.get("wait-time").set(values.getWaitTime());
                    context.getResult().get(entry.getKey()).set(result);
                }
            }
        });
    }

    /* (non-Javadoc)
     * @see org.jboss.as.controller.SimpleResourceDefinition#registerChildren(org.jboss.as.controller.registry.ManagementResourceRegistration)
     */
    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);

        if (componentType.hasTimer()) {
            // /deployment=DU/**/subsystem=ejb3/*=EJBName/service=timer-service
            final AbstractEJBComponentRuntimeHandler<?> handler = componentType.getRuntimeHandler();
            resourceRegistration.registerSubModel(new TimerServiceResourceDefinition(handler));
        }
    }
}
