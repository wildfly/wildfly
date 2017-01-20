/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster;

import static org.wildfly.extension.mod_cluster.LoadMetricDefinition.CAPACITY;
import static org.wildfly.extension.mod_cluster.LoadMetricDefinition.PROPERTY;
import static org.wildfly.extension.mod_cluster.LoadMetricDefinition.TYPE;
import static org.wildfly.extension.mod_cluster.LoadMetricDefinition.WEIGHT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.CONNECTOR;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STATUS_INTERVAL;
import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.common.beans.property.BeanUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modcluster.config.ModClusterConfiguration;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProvider;
import org.jboss.modcluster.load.impl.SimpleLoadBalanceFactorProvider;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;

/**
 * The managed subsystem add update.
 *
 * @author Jean-Frederic Clere
 * @author Tomaz Cerar
 * @author Radoslav Husar
 */
class ModClusterSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private static final OperationContext.AttachmentKey<Boolean> SUBSYSTEM_ADD_KEY = OperationContext.AttachmentKey.create(Boolean.class);

    static final ModClusterSubsystemAdd INSTANCE = new ModClusterSubsystemAdd();

    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        ServiceTarget target = context.getServiceTarget();
        final ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        final ModelNode modelConfig = fullModel.get(ModClusterConfigResourceDefinition.PATH.getKeyValuePair());

        ModClusterConfigurationServiceBuilder configurationBuilder = new ModClusterConfigurationServiceBuilder();
        configurationBuilder.configure(context, modelConfig).build(target).install();

        // Construct LoadBalanceFactorProvider and call pluggable boot time handlers.
        Set<LoadMetric> metrics = new HashSet<>();
        final LoadBalanceFactorProvider loadProvider = getModClusterLoadProvider(metrics, context, modelConfig);

        for (BoottimeHandlerProvider handler : ServiceLoader.load(BoottimeHandlerProvider.class, BoottimeHandlerProvider.class.getClassLoader())) {
            handler.performBoottime(metrics, context, operation, modelConfig);
        }

        final String connector = CONNECTOR.resolveModelAttribute(context, modelConfig).asString();
        final int statusInterval = STATUS_INTERVAL.resolveModelAttribute(context, modelConfig).asInt();
        InjectedValue<ModClusterConfiguration> modClusterConfiguration = new InjectedValue<>();
        ContainerEventHandlerService service = new ContainerEventHandlerService(modClusterConfiguration, loadProvider);
        // Install the main service
        new AsynchronousServiceBuilder<>(ContainerEventHandlerService.SERVICE_NAME, service).build(target)
                .addDependency(configurationBuilder.getServiceName(), ModClusterConfiguration.class, modClusterConfiguration)
                .setInitialMode(Mode.ACTIVE)
                .install();

        // Install services for web container integration
        for (ContainerEventHandlerAdapterBuilder adapterBuilder : ServiceLoader.load(ContainerEventHandlerAdapterBuilder.class, ContainerEventHandlerAdapterBuilder.class.getClassLoader())) {
            adapterBuilder.build(target, connector, statusInterval).setInitialMode(Mode.PASSIVE).install();
        }
    }

    /**
     * This is here so legacy configuration can be supported.
     */
    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        if (operation.hasDefined(CommonAttributes.MOD_CLUSTER_CONFIG)) {
            PathAddress opAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
            PathAddress parent = opAddress.append(ModClusterConfigResourceDefinition.PATH);
            ModelNode targetOperation = Util.createAddOperation(parent);
            for (AttributeDefinition def : ModClusterConfigResourceDefinition.ATTRIBUTES) {
                def.validateAndSet(operation, targetOperation);
            }
            context.addStep(targetOperation, ModClusterConfigAdd.INSTANCE, OperationContext.Stage.MODEL, true);
        }

        // Inform handlers for child resources that we are part of the set of operations
        // so they know we'll be utilizing any model they write. We do this in Stage.MODEL
        // so in their Stage.MODEL they can decide to skip adding a runtime step
        context.attach(SUBSYSTEM_ADD_KEY, Boolean.TRUE);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

    }

    /**
     * Allows handlers for child resources to check whether this operation is part of the set
     * of operations active in the given {@code context}
     *
     * @param context the context
     * @return {@code true} if this handler has executed in this context
     */
    static boolean isActiveInContext(final OperationContext context) {
        return context.getAttachment(SUBSYSTEM_ADD_KEY) != null;
    }

    private LoadBalanceFactorProvider getModClusterLoadProvider(final Set<LoadMetric> metrics, final OperationContext context, ModelNode model) throws OperationFailedException {
        LoadBalanceFactorProvider load = null;
        if (model.hasDefined(CommonAttributes.SIMPLE_LOAD_PROVIDER_FACTOR)) {
            // TODO it seems we don't support that stuff.
            int value = ModClusterConfigResourceDefinition.SIMPLE_LOAD_PROVIDER.resolveModelAttribute(context, model).asInt(1);
            SimpleLoadBalanceFactorProvider myload = new SimpleLoadBalanceFactorProvider();
            myload.setLoadBalanceFactor(value);
            load = myload;
        }

        if (model.get(DynamicLoadProviderDefinition.PATH.getKeyValuePair()).isDefined()) {
            final ModelNode node = model.get(DynamicLoadProviderDefinition.PATH.getKeyValuePair());
            int decayFactor = DynamicLoadProviderDefinition.DECAY.resolveModelAttribute(context, node).asInt();
            int history = DynamicLoadProviderDefinition.HISTORY.resolveModelAttribute(context, node).asInt();
            if (node.hasDefined(CommonAttributes.LOAD_METRIC)) {
                addLoadMetrics(metrics, node.get(CommonAttributes.LOAD_METRIC), context);
            }
            if (node.hasDefined(CommonAttributes.CUSTOM_LOAD_METRIC)) {
                addLoadMetrics(metrics, node.get(CommonAttributes.CUSTOM_LOAD_METRIC), context);
            }
            if (!metrics.isEmpty()) {
                DynamicLoadBalanceFactorProvider loader = new DynamicLoadBalanceFactorProvider(metrics);
                loader.setDecayFactor(decayFactor);
                loader.setHistory(history);
                load = loader;
            }
        }
        if (load == null) {
            // Use a default one...
            ROOT_LOGGER.useDefaultLoadBalancer();
            SimpleLoadBalanceFactorProvider myload = new SimpleLoadBalanceFactorProvider();
            myload.setLoadBalanceFactor(1);
            load = myload;
        }
        return load;
    }


    private void addLoadMetrics(Set<LoadMetric> metrics, ModelNode nodes, final OperationContext context) throws OperationFailedException {
        for (Property p : nodes.asPropertyList()) {
            ModelNode node = p.getValue();
            double capacity = CAPACITY.resolveModelAttribute(context, node).asDouble();
            int weight = WEIGHT.resolveModelAttribute(context, node).asInt();
            Map<String, String> propertyMap = PROPERTY.unwrap(context, node);

            Class<? extends LoadMetric> loadMetricClass = null;
            if (node.hasDefined(CommonAttributes.TYPE)) {
                String type = TYPE.resolveModelAttribute(context, node).asString();

                // MODCLUSTER-288 Metric "mem" has been dropped, keep it in the model for versions prior to 8.0
                if (type.equals("mem")) {
                    ROOT_LOGGER.unsupportedMetric(type);
                    continue;
                }

                LoadMetricEnum metric = LoadMetricEnum.forType(type);
                loadMetricClass = (metric != null) ? metric.getLoadMetricClass() : null;
            } else {
                String className = CustomLoadMetricDefinition.CLASS.resolveModelAttribute(context, node).asString();
                try {
                    loadMetricClass = this.getClass().getClassLoader().loadClass(className).asSubclass(LoadMetric.class);
                } catch (ClassNotFoundException e) {
                    ROOT_LOGGER.errorAddingMetrics(e);
                }
            }

            if (loadMetricClass != null) {
                try {
                    LoadMetric metric = loadMetricClass.newInstance();
                    metric.setCapacity(capacity);
                    metric.setWeight(weight);

                    // Apply Java Bean properties if any are set
                    if (propertyMap != null && !propertyMap.isEmpty()) {
                        Properties props = new Properties();
                        props.putAll(propertyMap);

                        try {
                            BeanUtils.mapJavaBeanProperties(metric, props, true);
                        } catch (Exception ex) {
                            ROOT_LOGGER.errorApplyingMetricProperties(ex, loadMetricClass.getCanonicalName());

                            // Do not add this incomplete metric.
                            continue;
                        }
                    }

                    metrics.add(metric);
                } catch (InstantiationException | IllegalAccessException e) {
                    ROOT_LOGGER.errorAddingMetrics(e);
                }
            }
        }
    }

}
