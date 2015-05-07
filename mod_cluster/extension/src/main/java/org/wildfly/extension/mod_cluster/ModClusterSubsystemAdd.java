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
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.ADVERTISE;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.ADVERTISE_SECURITY_KEY;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.ADVERTISE_SOCKET;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.AUTO_ENABLE_CONTEXTS;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.BALANCER;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.CONNECTOR;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.EXCLUDED_CONTEXTS;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.FLUSH_PACKETS;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.FLUSH_WAIT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.LOAD_BALANCING_GROUP;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.MAX_ATTEMPTS;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.NODE_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.PING;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.PROXY_URL;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.SESSION_DRAINING_STRATEGY;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.SMAX;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.SOCKET_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STATUS_INTERVAL;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STICKY_SESSION;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STICKY_SESSION_FORCE;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STICKY_SESSION_REMOVE;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STOP_CONTEXT_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.TTL;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.WORKER_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.CA_CERTIFICATE_FILE;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.CA_REVOCATION_URL;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.CERTIFICATE_KEY_FILE;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.CIPHER_SUITE;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.KEY_ALIAS;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.PASSWORD;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.PROTOCOL;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.common.beans.property.BeanUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modcluster.config.impl.ModClusterConfig;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProvider;
import org.jboss.modcluster.load.impl.SimpleLoadBalanceFactorProvider;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;

/**
 * The managed subsystem add update.
 *
 * @author Jean-Frederic Clere
 * @author Tomaz Cerar
 * @author Radoslav Husar
 * @version Jan 2014
 */
class ModClusterSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private static final OperationContext.AttachmentKey<Boolean> SUBSYSTEM_ADD_KEY = OperationContext.AttachmentKey.create(Boolean.class);

    static final ModClusterSubsystemAdd INSTANCE = new ModClusterSubsystemAdd();

    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        ServiceTarget target = context.getServiceTarget();
        final ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        final ModelNode modelConfig = fullModel.get(ModClusterConfigResourceDefinition.PATH.getKeyValuePair());
        final ModClusterConfig config = getModClusterConfig(context, modelConfig);

        target.addService(ContainerEventHandlerService.CONFIG_SERVICE_NAME, new ValueService<>(new ImmediateValue<>(config)))
                .setInitialMode(Mode.ACTIVE)
                .install();

        // Construct LoadBalanceFactorProvider and call pluggable boot time handlers.
        Set<LoadMetric> metrics = new HashSet<LoadMetric>();
        final LoadBalanceFactorProvider loadProvider = getModClusterLoadProvider(metrics, context, modelConfig);

        for (BoottimeHandlerProvider handler : ServiceLoader.load(BoottimeHandlerProvider.class, BoottimeHandlerProvider.class.getClassLoader())) {
            handler.performBoottime(metrics, context, operation, model);
        }

        final String connector = CONNECTOR.resolveModelAttribute(context, modelConfig).asString();
        final int statusInterval = STATUS_INTERVAL.resolveModelAttribute(context, modelConfig).asInt();
        InjectedValue<SocketBindingManager> socketBindingManager = new InjectedValue<SocketBindingManager>();
        ContainerEventHandlerService service = new ContainerEventHandlerService(config, loadProvider, socketBindingManager);
        final ServiceBuilder<?> builder = new AsynchronousServiceBuilder<>(ContainerEventHandlerService.SERVICE_NAME, service).build(target)
                .addDependency(SocketBindingManager.SOCKET_BINDING_MANAGER, SocketBindingManager.class, socketBindingManager)
                .setInitialMode(Mode.ACTIVE);

        // Add advertise socket binding dependency
        final ModelNode bindingRefNode = ADVERTISE_SOCKET.resolveModelAttribute(context, modelConfig);
        final String bindingRef = bindingRefNode.isDefined() ? bindingRefNode.asString() : null;
        if (bindingRef != null) {
            builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(bindingRef), SocketBinding.class, service.getSocketBindingInjector());
        }

        // Add proxies socket binding dependencies
        List<ModelNode> modelNodes = modelConfig.get(CommonAttributes.PROXIES).isDefined() ? modelConfig.get(CommonAttributes.PROXIES).asList() : null;

        if (modelNodes != null) {
            for (ModelNode node : modelNodes) {
                String ref = node.asString();
                builder.addDependency(
                        OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(ref),
                        OutboundSocketBinding.class, service.getOutboundSocketBindingInjector(ref)
                );
            }
        }

        // Install the main service
        builder.install();

        // Install services for web container integration
        for (ContainerEventHandlerAdapterBuilder adapterBuilder: ServiceLoader.load(ContainerEventHandlerAdapterBuilder.class, ContainerEventHandlerAdapterBuilder.class.getClassLoader())) {
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

    private ModClusterConfig getModClusterConfig(final OperationContext context, ModelNode model) throws OperationFailedException {
        ModClusterConfig config = new ModClusterConfig();
        config.setAdvertise(ADVERTISE.resolveModelAttribute(context, model).asBoolean());

        if (model.get(ModClusterSSLResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
            // Add SSL configuration.
            config.setSsl(true);
            final ModelNode ssl = model.get(ModClusterSSLResourceDefinition.PATH.getKeyValuePair());
            ModelNode keyAlias = KEY_ALIAS.resolveModelAttribute(context, ssl);
            ModelNode password = PASSWORD.resolveModelAttribute(context, ssl);
            if (keyAlias.isDefined()) {
                config.setSslKeyAlias(keyAlias.asString());
            }
            if (password.isDefined()) {
                config.setSslTrustStorePassword(password.asString());
                config.setSslKeyStorePassword(password.asString());
            }
            if (ssl.hasDefined(CommonAttributes.CERTIFICATE_KEY_FILE)) {
                config.setSslKeyStore(CERTIFICATE_KEY_FILE.resolveModelAttribute(context, ssl).asString());
            }
            if (ssl.hasDefined(CommonAttributes.CIPHER_SUITE)) {
                config.setSslCiphers(CIPHER_SUITE.resolveModelAttribute(context, ssl).asString());
            }
            if (ssl.hasDefined(CommonAttributes.PROTOCOL)) {
                config.setSslProtocol(PROTOCOL.resolveModelAttribute(context, ssl).asString());
            }
            if (ssl.hasDefined(CommonAttributes.CA_CERTIFICATE_FILE)) {
                config.setSslTrustStore(CA_CERTIFICATE_FILE.resolveModelAttribute(context, ssl).asString());
            }
            if (ssl.hasDefined(CommonAttributes.CA_REVOCATION_URL)) {
                config.setSslCrlFile(CA_REVOCATION_URL.resolveModelAttribute(context, ssl).asString());
            }
        }
        if (model.hasDefined(CommonAttributes.PROXY_LIST)) {
            throw new OperationFailedException(ROOT_LOGGER.proxyListNotAllowedInCurrentModel());
        }
        if (model.hasDefined(CommonAttributes.ADVERTISE_SECURITY_KEY)) {
            config.setAdvertiseSecurityKey(ADVERTISE_SECURITY_KEY.resolveModelAttribute(context, model).asString());
        }
        config.setProxyURL(PROXY_URL.resolveModelAttribute(context, model).asString());
        config.setExcludedContexts(EXCLUDED_CONTEXTS.resolveModelAttribute(context, model).asString().trim());
        config.setAutoEnableContexts(AUTO_ENABLE_CONTEXTS.resolveModelAttribute(context, model).asBoolean());

        config.setStopContextTimeout(STOP_CONTEXT_TIMEOUT.resolveModelAttribute(context, model).asInt());
        config.setStopContextTimeoutUnit(TimeUnit.valueOf(STOP_CONTEXT_TIMEOUT.getMeasurementUnit().getName()));
        //config.setStopContextTimeoutUnit(TimeUnit.SECONDS); //todo use AttributeDefinition.getMeasurementUnit
        // the default value is 20000 = 20 seconds.
        config.setSocketTimeout(SOCKET_TIMEOUT.resolveModelAttribute(context, model).asInt() * 1000);
        config.setStickySession(STICKY_SESSION.resolveModelAttribute(context, model).asBoolean());
        config.setStickySessionRemove(STICKY_SESSION_REMOVE.resolveModelAttribute(context, model).asBoolean());
        config.setStickySessionForce(STICKY_SESSION_FORCE.resolveModelAttribute(context, model).asBoolean());
        config.setWorkerTimeout(WORKER_TIMEOUT.resolveModelAttribute(context, model).asInt());
        config.setMaxAttempts(MAX_ATTEMPTS.resolveModelAttribute(context, model).asInt());
        config.setFlushPackets(FLUSH_PACKETS.resolveModelAttribute(context, model).asBoolean());
        config.setFlushWait(FLUSH_WAIT.resolveModelAttribute(context, model).asInt());
        config.setPing(PING.resolveModelAttribute(context, model).asInt());
        config.setSmax(SMAX.resolveModelAttribute(context, model).asInt());
        config.setTtl(TTL.resolveModelAttribute(context, model).asInt());
        config.setNodeTimeout(NODE_TIMEOUT.resolveModelAttribute(context, model).asInt());
        config.setSessionDrainingStrategy(SESSION_DRAINING_STRATEGY.resolveModelAttribute(context, model).asString());

        if (model.hasDefined(CommonAttributes.BALANCER)) {
            config.setBalancer(BALANCER.resolveModelAttribute(context, model).asString());
        }
        if (model.hasDefined(CommonAttributes.LOAD_BALANCING_GROUP)) {
            config.setLoadBalancingGroup(LOAD_BALANCING_GROUP.resolveModelAttribute(context, model).asString());
        }
        return config;
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
