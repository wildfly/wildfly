/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.mod_cluster;

import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.LISTENER;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.STATUS_INTERVAL;

import java.time.Duration;
import java.util.HashSet;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.common.beans.property.BeanUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modcluster.ModClusterServiceMBean;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProvider;
import org.jboss.modcluster.load.impl.SimpleLoadBalanceFactorProvider;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.ServiceValueRegistry;

/**
 * Resource service handler implementation that handles installation of mod_cluster services. Since mod_cluster requires certain
 * boot time handling, all operations on the subsystem leave the server in the 'reload required' state. This makes the service
 * installation a one time endeavour which is handled here.
 *
 * @author Radoslav Husar
 */
class ModClusterSubsystemServiceHandler implements ResourceServiceHandler {

    private final ServiceValueRegistry<ModClusterServiceMBean> registry;

    ModClusterSubsystemServiceHandler(ServiceValueRegistry<ModClusterServiceMBean> registry) {
        this.registry = registry;
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        if (!context.isBooting()) return;

        Resource subsystemResource = context.readResource(PathAddress.EMPTY_ADDRESS);

        if (subsystemResource.hasChildren(ProxyConfigurationResourceDefinition.WILDCARD_PATH.getKey())) {
            Set<String> adapterNames = new HashSet<>();
            Set<LoadMetric> enabledMetrics = new HashSet<>();

            for (Resource.ResourceEntry proxyResource : subsystemResource.getChildren(ProxyConfigurationResourceDefinition.WILDCARD_PATH.getKey())) {
                String proxyName = proxyResource.getName();
                PathAddress proxyAddress = context.getCurrentAddress().append(ProxyConfigurationResourceDefinition.pathElement(proxyName));
                adapterNames.add(proxyName);
                ModelNode proxyModel = Resource.Tools.readModel(proxyResource);

                ServiceTarget target = context.getServiceTarget();
                ProxyConfigurationServiceConfigurator configurationBuilder = new ProxyConfigurationServiceConfigurator(proxyAddress);
                configurationBuilder.configure(context, proxyModel).build(target).install();

                // Construct LoadBalanceFactorProvider and call pluggable boot time metric
                Set<LoadMetric> metrics = new HashSet<>();
                LoadBalanceFactorProvider loadProvider = this.getLoadProvider(proxyName, metrics, context, proxyModel);
                enabledMetrics.addAll(metrics);

                String listenerName = LISTENER.resolveModelAttribute(context, proxyModel).asString();
                int statusInterval = STATUS_INTERVAL.resolveModelAttribute(context, proxyModel).asInt();

                this.registry.capture(ServiceDependency.on(ProxyConfigurationResourceDefinition.Capability.SERVICE.getDefinition().getCapabilityServiceName(proxyAddress))).install(context);
                new ContainerEventHandlerServiceConfigurator(proxyAddress, loadProvider).build(target).install();

                // Install services for web container integration
                for (ContainerEventHandlerAdapterServiceConfiguratorProvider provider : ServiceLoader.load(ContainerEventHandlerAdapterServiceConfiguratorProvider.class, ContainerEventHandlerAdapterServiceConfiguratorProvider.class.getClassLoader())) {
                    provider.getServiceConfigurator(proxyName, listenerName, Duration.ofSeconds(statusInterval)).configure(context).build(target).setInitialMode(Mode.PASSIVE).install();
                }
            }

            for (BoottimeHandlerProvider handler : ServiceLoader.load(BoottimeHandlerProvider.class, BoottimeHandlerProvider.class.getClassLoader())) {
                handler.performBoottime(context, adapterNames, enabledMetrics);
            }
        }
    }

    private LoadBalanceFactorProvider getLoadProvider(String proxyName, final Set<LoadMetric> metrics, final OperationContext context, ModelNode model) throws OperationFailedException {
        LoadBalanceFactorProvider load = null;

        if (model.get(SimpleLoadProviderResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
            ModelNode simpleProviderModel = model.get(SimpleLoadProviderResourceDefinition.PATH.getKeyValuePair());
            int value = SimpleLoadProviderResourceDefinition.Attribute.FACTOR.resolveModelAttribute(context, simpleProviderModel).asInt();
            SimpleLoadBalanceFactorProvider simpleLoadProvider = new SimpleLoadBalanceFactorProvider();
            simpleLoadProvider.setLoadBalanceFactor(value);
            load = simpleLoadProvider;
        }
        if (model.get(DynamicLoadProviderResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
            ModelNode node = model.get(DynamicLoadProviderResourceDefinition.PATH.getKeyValuePair());
            float decayFactor = (float) DynamicLoadProviderResourceDefinition.Attribute.DECAY.resolveModelAttribute(context, node).asDouble();
            int history = DynamicLoadProviderResourceDefinition.Attribute.HISTORY.resolveModelAttribute(context, node).asInt();
            int initialLoad = DynamicLoadProviderResourceDefinition.Attribute.INITIAL_LOAD.resolveModelAttribute(context, node).asInt();

            if (node.hasDefined(LoadMetricResourceDefinition.WILDCARD_PATH.getKey())) {
                this.addLoadMetrics(metrics, node.get(LoadMetricResourceDefinition.WILDCARD_PATH.getKey()), context);
            }
            if (node.hasDefined(CustomLoadMetricResourceDefinition.WILDCARD_PATH.getKey())) {
                this.addLoadMetrics(metrics, node.get(CustomLoadMetricResourceDefinition.WILDCARD_PATH.getKey()), context);
            }
            if (!metrics.isEmpty()) {
                DynamicLoadBalanceFactorProvider loadBalanceFactorProvider = new DynamicLoadBalanceFactorProvider(metrics, initialLoad);
                loadBalanceFactorProvider.setDecayFactor(decayFactor);
                loadBalanceFactorProvider.setHistory(history);
                load = loadBalanceFactorProvider;
            }
        }
        if (load == null) {
            ROOT_LOGGER.usingSimpleLoadProvider(proxyName);
            load = new SimpleLoadBalanceFactorProvider();
        }
        return load;
    }


    private void addLoadMetrics(Set<LoadMetric> metrics, ModelNode nodes, final OperationContext context) throws OperationFailedException {
        for (Property property1 : nodes.asPropertyList()) {
            ModelNode node = property1.getValue();
            double capacity = LoadMetricResourceDefinition.SharedAttribute.CAPACITY.resolveModelAttribute(context, node).asDouble();
            int weight = LoadMetricResourceDefinition.SharedAttribute.WEIGHT.resolveModelAttribute(context, node).asInt();

            Class<? extends LoadMetric> loadMetricClass = null;
            if (node.hasDefined(LoadMetricResourceDefinition.Attribute.TYPE.getName())) {
                String type = LoadMetricResourceDefinition.Attribute.TYPE.resolveModelAttribute(context, node).asString();
                LoadMetricEnum metric = LoadMetricEnum.forType(type);
                loadMetricClass = (metric != null) ? metric.getLoadMetricClass() : null;
            } else {
                String className = CustomLoadMetricResourceDefinition.Attribute.CLASS.resolveModelAttribute(context, node).asString();
                String moduleName = CustomLoadMetricResourceDefinition.Attribute.MODULE.resolveModelAttribute(context, node).asString();

                try {
                    Module module = Module.getContextModuleLoader().loadModule(moduleName);
                    loadMetricClass = module.getClassLoader().loadClass(className).asSubclass(LoadMetric.class);
                } catch (ModuleLoadException e) {
                    ROOT_LOGGER.errorLoadingModuleForCustomMetric(moduleName, e);
                } catch (ClassNotFoundException e) {
                    ROOT_LOGGER.errorAddingMetrics(e);
                }
            }

            if (loadMetricClass != null) {
                try {
                    LoadMetric metric = loadMetricClass.newInstance();
                    metric.setCapacity(capacity);
                    metric.setWeight(weight);

                    Properties props = new Properties();
                    for (Property property : LoadMetricResourceDefinition.SharedAttribute.PROPERTY.resolveModelAttribute(context, node).asPropertyListOrEmpty()) {
                        props.put(property.getName(), property.getValue().asString());
                    }

                    // Apply Java Bean properties if any are set
                    if (!props.isEmpty()) {
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

    @Override
    public void removeServices(OperationContext context, ModelNode model) {
        // Ignore -- the server is now in reload-required state.
    }

}
