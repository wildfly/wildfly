/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.messaging.activemq.jms;

import static org.jboss.as.naming.deployment.ContextNames.BindInfo;
import static org.wildfly.extension.messaging.activemq.BinderServiceUtil.installAliasBinderService;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Pooled.REBALANCE_CONNECTIONS_PROP_NAME;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.activemq.artemis.api.core.BroadcastEndpointFactory;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.UDPBroadcastEndpointFactory;
import org.jboss.as.connector.metadata.common.CredentialImpl;
import org.jboss.as.connector.metadata.common.SecurityImpl;
import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
import org.jboss.as.connector.services.mdr.AS7MetadataRepository;
import org.jboss.as.connector.services.resourceadapters.ResourceAdapterActivatorService;
import org.jboss.as.connector.services.resourceadapters.deployment.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.subsystems.jca.JcaSubsystemConfiguration;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.Services;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.common.FlushStrategy;
import org.jboss.jca.common.api.metadata.common.Pool;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.common.Security;
import org.jboss.jca.common.api.metadata.common.TimeOut;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.common.Validation;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.resourceadapter.AdminObject;
import org.jboss.jca.common.api.metadata.resourceadapter.ConnectionDefinition;
import org.jboss.jca.common.api.metadata.spec.Activationspec;
import org.jboss.jca.common.api.metadata.spec.AuthenticationMechanism;
import org.jboss.jca.common.api.metadata.spec.ConfigProperty;
import org.jboss.jca.common.api.metadata.spec.Connector;
import org.jboss.jca.common.api.metadata.spec.CredentialInterfaceEnum;
import org.jboss.jca.common.api.metadata.spec.Icon;
import org.jboss.jca.common.api.metadata.spec.InboundResourceAdapter;
import org.jboss.jca.common.api.metadata.spec.LocalizedXsdString;
import org.jboss.jca.common.api.metadata.spec.MessageListener;
import org.jboss.jca.common.api.metadata.spec.Messageadapter;
import org.jboss.jca.common.api.metadata.spec.OutboundResourceAdapter;
import org.jboss.jca.common.api.metadata.spec.RequiredConfigProperty;
import org.jboss.jca.common.api.metadata.spec.ResourceAdapter;
import org.jboss.jca.common.api.metadata.spec.SecurityPermission;
import org.jboss.jca.common.api.metadata.spec.XsdString;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.common.PoolImpl;
import org.jboss.jca.common.metadata.common.TimeOutImpl;
import org.jboss.jca.common.metadata.common.ValidationImpl;
import org.jboss.jca.common.metadata.common.XaPoolImpl;
import org.jboss.jca.common.metadata.resourceadapter.ActivationImpl;
import org.jboss.jca.common.metadata.resourceadapter.ConnectionDefinitionImpl;
import org.jboss.jca.common.metadata.spec.ActivationSpecImpl;
import org.jboss.jca.common.metadata.spec.AuthenticationMechanismImpl;
import org.jboss.jca.common.metadata.spec.ConfigPropertyImpl;
import org.jboss.jca.common.metadata.spec.ConnectorImpl;
import org.jboss.jca.common.metadata.spec.InboundResourceAdapterImpl;
import org.jboss.jca.common.metadata.spec.MessageAdapterImpl;
import org.jboss.jca.common.metadata.spec.MessageListenerImpl;
import org.jboss.jca.common.metadata.spec.OutboundResourceAdapterImpl;
import org.jboss.jca.common.metadata.spec.RequiredConfigPropertyImpl;
import org.jboss.jca.common.metadata.spec.ResourceAdapterImpl;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.spi.ClusteringDefaultRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.extension.messaging.activemq.ActiveMQResourceAdapter;
import org.wildfly.extension.messaging.activemq.DiscoveryGroupAdd;
import org.wildfly.extension.messaging.activemq.GroupBindingService;
import org.wildfly.extension.messaging.activemq.MessagingExtension;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.TransportConfigOperationHandlers;
import org.wildfly.extension.messaging.activemq.broadcast.CommandDispatcherBroadcastEndpointFactory;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;

/**
 * A service which translates a pooled connection factory into a resource adapter driven connection pool
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalPooledConnectionFactoryService implements Service<Void> {

    private static final ServiceName JBOSS_MESSAGING_ACTIVEMQ = ServiceName.JBOSS.append(MessagingExtension.SUBSYSTEM_NAME);
    private static final List<LocalizedXsdString> EMPTY_LOCL = Collections.emptyList();
    public static final String CONNECTOR_CLASSNAME = "connectorClassName";
    public static final String CONNECTION_PARAMETERS = "connectionParameters";
    private static final String ACTIVEMQ_ACTIVATION = "org.apache.activemq.artemis.ra.inflow.ActiveMQActivationSpec";
    private static final String ACTIVEMQ_CONN_DEF = "ActiveMQConnectionDefinition";
    private static final String ACTIVEMQ_RESOURCE_ADAPTER = ActiveMQResourceAdapter.class.getName();
    private static final String RAMANAGED_CONN_FACTORY = "org.apache.activemq.artemis.ra.ActiveMQRAManagedConnectionFactory";
    private static final String RA_CONN_FACTORY = "org.apache.activemq.artemis.ra.ActiveMQRAConnectionFactory";
    private static final String RA_CONN_FACTORY_IMPL = "org.apache.activemq.artemis.ra.ActiveMQRAConnectionFactoryImpl";
    private static final String JMS_SESSION = "javax.jms.Session";
    private static final String ACTIVEMQ_RA_SESSION = "org.apache.activemq.artemis.ra.ActiveMQRASession";
    private static final String BASIC_PASS = "BasicPassword";
    private static final String JMS_QUEUE = "javax.jms.Queue";
    private static final String STRING_TYPE = "java.lang.String";
    private static final String INTEGER_TYPE = "java.lang.Integer";
    private static final String LONG_TYPE = "java.lang.Long";
    private static final String SESSION_DEFAULT_TYPE = "SessionDefaultType";
    private static final String TRY_LOCK = "UseTryLock";
    private static final String JMS_MESSAGE_LISTENER = "javax.jms.MessageListener";
    private static final String DEFAULT_MAX_RECONNECTS = "5";
    public static final String GROUP_ADDRESS = "discoveryAddress";
    public static final String DISCOVERY_INITIAL_WAIT_TIMEOUT = "discoveryInitialWaitTimeout";
    public static final String GROUP_PORT = "discoveryPort";
    public static final String REFRESH_TIMEOUT = "discoveryRefreshTimeout";
    public static final String DISCOVERY_LOCAL_BIND_ADDRESS = "discoveryLocalBindAddress";
    public static final String JGROUPS_CHANNEL_LOCATOR_CLASS = "jgroupsChannelLocatorClass";
    public static final String JGROUPS_CHANNEL_NAME = "jgroupsChannelName";
    public static final String JGROUPS_CHANNEL_REF_NAME = "jgroupsChannelRefName";

    private Supplier<Object> transactionManager;
    private final DiscoveryGroupConfiguration discoveryGroupConfiguration;
    private final TransportConfiguration[] connectors;
    private List<PooledConnectionFactoryConfigProperties> adapterParams;
    private String name;
    private final Map<String, Supplier<SocketBinding>> socketBindings = new HashMap<>();
    private final Map<String, Supplier<OutboundSocketBinding>> outboundSocketBindings = new HashMap<>();
    private Map<String, Supplier<SocketBinding>> groupBindings = new HashMap<>();
    // mapping between the {discovery}-groups and the cluster names they use
    private final Map<String, String> clusterNames = new HashMap<>();
    // mapping between the {discovery}-groups and the command dispatcher factory they use
    private final Map<String, Supplier<CommandDispatcherFactory>> commandDispatcherFactories = new HashMap<>();
    private BindInfo bindInfo;
    private List<String> jndiAliases;
    private String txSupport;
    private int minPoolSize;
    private int maxPoolSize;
    private final String jgroupsClusterName;
    private final String jgroupChannelName;
    private final boolean createBinderService;
    private final String managedConnectionPoolClassName;
    // can be null. In that case the behaviour is depending on the IronJacamar container setting.
    private final Boolean enlistmentTrace;
    private ExceptionSupplier<CredentialSource, Exception> credentialSourceSupplier;


    public ExternalPooledConnectionFactoryService(String name, TransportConfiguration[] connectors, DiscoveryGroupConfiguration groupConfiguration, String jgroupsClusterName,
            String jgroupChannelName, List<PooledConnectionFactoryConfigProperties> adapterParams, List<String> jndiNames, String txSupport, int minPoolSize, int maxPoolSize, String managedConnectionPoolClassName, Boolean enlistmentTrace) {
        this.name = name;
        this.connectors = connectors;
        this.discoveryGroupConfiguration = groupConfiguration;
        this.jgroupsClusterName = jgroupsClusterName;
        this.jgroupChannelName = jgroupChannelName;
        this.adapterParams = adapterParams;
        initJNDIBindings(jndiNames);
        createBinderService = true;
        this.txSupport = txSupport;
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.managedConnectionPoolClassName = managedConnectionPoolClassName;
        this.enlistmentTrace = enlistmentTrace;
    }

    private void initJNDIBindings(List<String> jndiNames) {
        // create the definition with the 1st jndi names and create jndi aliases for the rest
        String jndiName = jndiNames.get(0);
        this.bindInfo = ContextNames.bindInfoFor(jndiName);
        this.jndiAliases = new ArrayList<>();
        if (jndiNames.size() > 1) {
            jndiAliases = jndiNames.subList(1, jndiNames.size());
        }
    }

    static ServiceName getResourceAdapterActivatorsServiceName(String name) {
        return ConnectorServices.RESOURCE_ADAPTER_ACTIVATOR_SERVICE.append(name);
    }


    public static ExternalPooledConnectionFactoryService installService(OperationContext context,
            String name,
            TransportConfiguration[] connectors,
            DiscoveryGroupConfiguration groupConfiguration,
            Set<String> connectorsSocketBindings,
            String jgroupClusterName,
            String jgroupChannelName,
            List<PooledConnectionFactoryConfigProperties> adapterParams,
            List<String> jndiNames,
            String txSupport,
            int minPoolSize,
            int maxPoolSize,
            String managedConnectionPoolClassName,
            Boolean enlistmentTrace,
            ModelNode model) throws OperationFailedException {

        ServiceName serviceName = JMSServices.getPooledConnectionFactoryBaseServiceName(JBOSS_MESSAGING_ACTIVEMQ).append(name);
        ExternalPooledConnectionFactoryService service = new ExternalPooledConnectionFactoryService(name,
                connectors, groupConfiguration, jgroupClusterName, jgroupChannelName, adapterParams,
                jndiNames, txSupport, minPoolSize, maxPoolSize, managedConnectionPoolClassName, enlistmentTrace);

        installService0(context, serviceName, service, groupConfiguration, jgroupClusterName, jgroupChannelName, connectorsSocketBindings, model);
        return service;
    }

    private static void installService0(OperationContext context,
            ServiceName serviceName,
            ExternalPooledConnectionFactoryService service,
            DiscoveryGroupConfiguration groupConfiguration,
            String jgroupsClusterName,
            String jgroupsChannelName,
            Set<String> connectorsSocketBindings,
            ModelNode model) throws OperationFailedException {
        ServiceBuilder serviceBuilder = context.getServiceTarget().addService(serviceName);
        service.transactionManager = serviceBuilder.requires(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER);
       // ensures that Artemis client thread pools are not stopped before any deployment depending on a pooled-connection-factory
       serviceBuilder.requires(MessagingServices.ACTIVEMQ_CLIENT_THREAD_POOL);
        ModelNode credentialReference = ConnectionFactoryAttributes.Pooled.CREDENTIAL_REFERENCE.resolveModelAttribute(context, model);
        if (credentialReference.isDefined()) {
            service.credentialSourceSupplier = CredentialReference.getCredentialSourceSupplier(context, ConnectionFactoryAttributes.Pooled.CREDENTIAL_REFERENCE, model, serviceBuilder);
        }
        Map<String, Boolean> outbounds = TransportConfigOperationHandlers.listOutBoundSocketBinding(context, connectorsSocketBindings);
        for (final String connectorSocketBinding : connectorsSocketBindings) {
            // find whether the connectorSocketBinding references a SocketBinding or an OutboundSocketBinding
            if (outbounds.get(connectorSocketBinding)) {
                final ServiceName outboundSocketName = OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(connectorSocketBinding);
                Supplier<OutboundSocketBinding> outboundSocketBindingSupplier = serviceBuilder.requires(outboundSocketName);
                service.outboundSocketBindings.put(connectorSocketBinding, outboundSocketBindingSupplier);
            } else {
                final ServiceName socketName = SocketBinding.JBOSS_BINDING_NAME.append(connectorSocketBinding);
                Supplier<SocketBinding> socketBindingSupplier = serviceBuilder.requires(socketName);
                service.socketBindings.put(connectorSocketBinding, socketBindingSupplier);
            }
        }
        if (groupConfiguration != null) {
            final String key = "discovery" + groupConfiguration.getName();
            if (jgroupsClusterName != null) {
                ServiceName commandDispatcherFactoryServiceName = jgroupsChannelName != null ? ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(context, jgroupsChannelName) : ClusteringDefaultRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(context);
                Supplier<CommandDispatcherFactory> commandDispatcherFactorySupplier = serviceBuilder.requires(commandDispatcherFactoryServiceName);
                service.commandDispatcherFactories.put(key, commandDispatcherFactorySupplier);
                service.clusterNames.put(key, jgroupsClusterName);
            } else {
                final ServiceName groupBinding = GroupBindingService.getDiscoveryBaseServiceName(JBOSS_MESSAGING_ACTIVEMQ).append(groupConfiguration.getName());
                Supplier<SocketBinding> socketBindingSupplier = serviceBuilder.requires(groupBinding);
                service.groupBindings.put(key, socketBindingSupplier);
            }
        }
        serviceBuilder.setInstance(service);
        serviceBuilder.install();
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceTarget serviceTarget = context.getChildTarget();
        try {
            createService(serviceTarget, context.getController().getServiceContainer());
        } catch (Exception e) {
            throw MessagingLogger.ROOT_LOGGER.failedToCreate(e, "resource adapter");
        }

    }

    private void createService(ServiceTarget serviceTarget, ServiceContainer container) throws Exception {
        InputStream is = null;
        InputStream isIj = null;
        // Properties for the resource adapter
        List<ConfigProperty> properties = new ArrayList<ConfigProperty>();
        try {
            StringBuilder connectorClassname = new StringBuilder();
            StringBuilder connectorParams = new StringBuilder();
            TransportConfigOperationHandlers.processConnectorBindings(Arrays.asList(connectors), socketBindings, outboundSocketBindings);
            for (TransportConfiguration tc : connectors) {
                if (tc == null) {
                    throw MessagingLogger.ROOT_LOGGER.connectorNotDefined(tc.getName());
                }
                if (connectorClassname.length() > 0) {
                    connectorClassname.append(",");
                    connectorParams.append(",");
                }
                connectorClassname.append(tc.getFactoryClassName());
                Map<String, Object> params = tc.getParams();
                boolean multiple = false;
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    if (multiple) {
                        connectorParams.append(";");
                    }
                    connectorParams.append(entry.getKey()).append("=").append(entry.getValue());
                    multiple = true;
                }
            }

            if (connectorClassname.length() > 0) {
                properties.add(simpleProperty15(CONNECTOR_CLASSNAME, STRING_TYPE, connectorClassname.toString()));
            }
            if (connectorParams.length() > 0) {
                properties.add(simpleProperty15(CONNECTION_PARAMETERS, STRING_TYPE, connectorParams.toString()));
            }

            if (discoveryGroupConfiguration != null) {

                final String dgName = discoveryGroupConfiguration.getName();
                final String key = "discovery" + dgName;
                final DiscoveryGroupConfiguration config;
                if (commandDispatcherFactories.containsKey(key)) {
                    CommandDispatcherFactory commandDispatcherFactory = commandDispatcherFactories.get(key).get();
                    String clusterName = clusterNames.get(key);
                    config = DiscoveryGroupAdd.createDiscoveryGroupConfiguration(name, discoveryGroupConfiguration, commandDispatcherFactory, clusterName);
                } else {
                    final SocketBinding binding = groupBindings.get(key).get();
                    if (binding == null) {
                        throw MessagingLogger.ROOT_LOGGER.failedToFindDiscoverySocketBinding(dgName);
                    }
                    config = DiscoveryGroupAdd.createDiscoveryGroupConfiguration(name, discoveryGroupConfiguration, binding);
                    binding.getSocketBindings().getNamedRegistry().registerBinding(ManagedBinding.Factory.createSimpleManagedBinding(binding));
                }
                BroadcastEndpointFactory bgCfg = config.getBroadcastEndpointFactory();
                if (bgCfg instanceof UDPBroadcastEndpointFactory) {
                    UDPBroadcastEndpointFactory udpCfg = (UDPBroadcastEndpointFactory) bgCfg;
                    properties.add(simpleProperty15(GROUP_ADDRESS, STRING_TYPE, udpCfg.getGroupAddress()));
                    properties.add(simpleProperty15(GROUP_PORT, INTEGER_TYPE, "" + udpCfg.getGroupPort()));
                    properties.add(simpleProperty15(DISCOVERY_LOCAL_BIND_ADDRESS, STRING_TYPE, "" + udpCfg.getLocalBindAddress()));
                } else if (bgCfg instanceof CommandDispatcherBroadcastEndpointFactory) {
                    String external = "/" + name + ":discovery" + dgName;
                    properties.add(simpleProperty15(JGROUPS_CHANNEL_NAME, STRING_TYPE, jgroupsClusterName));
                    properties.add(simpleProperty15(JGROUPS_CHANNEL_REF_NAME, STRING_TYPE, external));
                }
                properties.add(simpleProperty15(DISCOVERY_INITIAL_WAIT_TIMEOUT, LONG_TYPE, "" + config.getDiscoveryInitialWaitTimeout()));
                properties.add(simpleProperty15(REFRESH_TIMEOUT, LONG_TYPE, "" + config.getRefreshTimeout()));
            }

            boolean hasReconnect = false;
            final List<ConfigProperty> inboundProperties = new ArrayList<>();
            final List<ConfigProperty> outboundProperties = new ArrayList<>();
            final String reconnectName = ConnectionFactoryAttributes.Pooled.RECONNECT_ATTEMPTS_PROP_NAME;
            for (PooledConnectionFactoryConfigProperties adapterParam : adapterParams) {
                hasReconnect |= reconnectName.equals(adapterParam.getName());

                ConfigProperty p = simpleProperty15(adapterParam.getName(), adapterParam.getType(), adapterParam.getValue());
                if (adapterParam.getName().equals(REBALANCE_CONNECTIONS_PROP_NAME)) {
                    boolean rebalanceConnections = Boolean.parseBoolean(adapterParam.getValue());
                    if (rebalanceConnections) {
                        inboundProperties.add(p);
                    }
                } else {
                    if (null == adapterParam.getConfigType()) {
                        properties.add(p);
                    } else {
                        switch (adapterParam.getConfigType()) {
                            case INBOUND:
                                inboundProperties.add(p);
                                break;
                            case OUTBOUND:
                                outboundProperties.add(p);
                                break;
                            default:
                                properties.add(p);
                                break;
                        }
                    }
                }
            }

            // The default -1, which will hang forever until a server appears
            if (!hasReconnect) {
                properties.add(simpleProperty15(reconnectName, Integer.class.getName(), DEFAULT_MAX_RECONNECTS));
            }

            configureCredential(properties);

            WildFlyRecoveryRegistry.container = container;

            OutboundResourceAdapter outbound = createOutbound(outboundProperties);
            InboundResourceAdapter inbound = createInbound(inboundProperties);
            ResourceAdapter ra = createResourceAdapter15(properties, outbound, inbound);
            Connector cmd = createConnector15(ra);

            TransactionSupportEnum transactionSupport = getTransactionSupport(txSupport);
            ConnectionDefinition common = createConnDef(transactionSupport, bindInfo.getBindName(), minPoolSize, maxPoolSize, managedConnectionPoolClassName, enlistmentTrace);
            Activation activation = createActivation(common, transactionSupport);

            ResourceAdapterActivatorService activator = new ResourceAdapterActivatorService(cmd, activation,
                    ExternalPooledConnectionFactoryService.class.getClassLoader(), name);
            activator.setBindInfo(bindInfo);
            activator.setCreateBinderService(createBinderService);

            ServiceController<ResourceAdapterDeployment> controller
                    = Services.addServerExecutorDependency(
                            serviceTarget.addService(getResourceAdapterActivatorsServiceName(name), activator),
                            activator.getExecutorServiceInjector())
                            .addDependency(ConnectorServices.IRONJACAMAR_MDR, AS7MetadataRepository.class,
                                    activator.getMdrInjector())
                            .addDependency(ConnectorServices.RA_REPOSITORY_SERVICE, ResourceAdapterRepository.class,
                                    activator.getRaRepositoryInjector())
                            .addDependency(ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE, ManagementRepository.class,
                                    activator.getManagementRepositoryInjector())
                            .addDependency(ConnectorServices.RESOURCE_ADAPTER_REGISTRY_SERVICE,
                                    ResourceAdapterDeploymentRegistry.class, activator.getRegistryInjector())
                            .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class,
                                    activator.getTxIntegrationInjector())
                            .addDependency(ConnectorServices.CONNECTOR_CONFIG_SERVICE,
                                    JcaSubsystemConfiguration.class, activator.getConfigInjector())
                            // No legacy security services needed as this activation's sole connection definition
                            // does not configure a legacy security domain
                            /*
                    .addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class,
                            activator.getSubjectFactoryInjector())
                             */
                            .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class,
                                    activator.getCcmInjector()).addDependency(NamingService.SERVICE_NAME)
                            .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER)
                            .addDependency(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append("default"))
                            .setInitialMode(ServiceController.Mode.PASSIVE).install();

            createJNDIAliases(bindInfo, jndiAliases, controller, serviceTarget);

            // Mock the deployment service to allow it to start
            serviceTarget.addService(ConnectorServices.RESOURCE_ADAPTER_DEPLOYER_SERVICE_PREFIX.append(name), Service.NULL).install();
        } finally {
            if (is != null) {
                is.close();
            }
            if (isIj != null) {
                isIj.close();
            }
        }
    }

    /**
     * Configure password from a credential-reference (as an alternative to the password attribute)
     * and add it to the RA properties.
     */
    private void configureCredential(List<ConfigProperty> properties) {
        // if a credential-reference has been defined, get the password property from it
        if (credentialSourceSupplier != null) {
            try {
                CredentialSource credentialSource = credentialSourceSupplier.get();
                if (credentialSource != null) {
                    char[] password = credentialSource.getCredential(PasswordCredential.class).getPassword(ClearPassword.class).getPassword();
                    if (password != null) {
                        // add the password property
                        properties.add(simpleProperty15("password", String.class.getName(), new String(password)));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void createJNDIAliases(final BindInfo bindInfo, List<String> aliases, ServiceController<ResourceAdapterDeployment> controller, ServiceTarget serviceTarget) {
        for (final String alias : aliases) {
            // do not install the alias' binder service if it is already registered
            if (controller.getServiceContainer().getService(ContextNames.bindInfoFor(alias).getBinderServiceName()) == null) {
                installAliasBinderService(serviceTarget,
                        bindInfo,
                        alias);
            }
        }
    }

    private static TransactionSupportEnum getTransactionSupport(String txSupport) {
        try {
            return TransactionSupportEnum.valueOf(txSupport);
        } catch (RuntimeException e) {
            return TransactionSupportEnum.LocalTransaction;
        }
    }

    private static Activation createActivation(ConnectionDefinition common, TransactionSupportEnum transactionSupport) {
        List<ConnectionDefinition> definitions = Collections.singletonList(common);
        return new ActivationImpl(null, null, transactionSupport, definitions, Collections.<AdminObject>emptyList(), Collections.<String, String>emptyMap(), Collections.<String>emptyList(), null, null);
    }

    private static ConnectionDefinition createConnDef(TransactionSupportEnum transactionSupport, String jndiName, int minPoolSize, int maxPoolSize, String managedConnectionPoolClassName, Boolean enlistmentTrace) throws ValidateException {
        Integer minSize = (minPoolSize == -1) ? null : minPoolSize;
        Integer maxSize = (maxPoolSize == -1) ? null : maxPoolSize;
        boolean prefill = false;
        boolean useStrictMin = false;
        FlushStrategy flushStrategy = FlushStrategy.FAILING_CONNECTION_ONLY;
        Boolean isXA = Boolean.FALSE;
        final Pool pool;
        if (transactionSupport == TransactionSupportEnum.XATransaction) {
            pool = new XaPoolImpl(minSize, Defaults.INITIAL_POOL_SIZE, maxSize, prefill, useStrictMin, flushStrategy, null, Defaults.FAIR,
                    Defaults.IS_SAME_RM_OVERRIDE, Defaults.INTERLEAVING, Defaults.PAD_XID, Defaults.WRAP_XA_RESOURCE, Defaults.NO_TX_SEPARATE_POOL);
            isXA = Boolean.TRUE;
        } else {
            pool = new PoolImpl(minSize, Defaults.INITIAL_POOL_SIZE, maxSize, prefill, useStrictMin, flushStrategy, null, Defaults.FAIR);
        }
        TimeOut timeOut = new TimeOutImpl(null, null, null, null, null) {
        };
        // <security>
        //   <application />
        // </security>
        // => PoolStrategy.POOL_BY_CRI
        Security security = new SecurityImpl(null, null, true, false);
        // register the XA Connection *without* recovery. ActiveMQ already takes care of the registration with the correct credentials
        // when its ResourceAdapter is started
        Recovery recovery = new Recovery(new CredentialImpl(null, null, null, false, null), null, Boolean.TRUE);
        Validation validation = new ValidationImpl(Defaults.VALIDATE_ON_MATCH, null, null, false);
        // do no track
        return new ConnectionDefinitionImpl(Collections.<String, String>emptyMap(), RAMANAGED_CONN_FACTORY, jndiName, ACTIVEMQ_CONN_DEF, true, true, true, Defaults.SHARABLE, Defaults.ENLISTMENT, Defaults.CONNECTABLE, false, managedConnectionPoolClassName, enlistmentTrace, pool, timeOut, validation, security, recovery, isXA);
    }

    private static Connector createConnector15(ResourceAdapter ra) {
        return new ConnectorImpl(Connector.Version.V_15, null, str("Red Hat"), str("JMS 1.1 Server"), str("1.0"), null, ra, null, false, EMPTY_LOCL, EMPTY_LOCL, Collections.<Icon>emptyList(), null);
    }

    private ResourceAdapter createResourceAdapter15(List<ConfigProperty> properties, OutboundResourceAdapter outbound, InboundResourceAdapter inbound) {
        return new ResourceAdapterImpl(str(ACTIVEMQ_RESOURCE_ADAPTER), properties, outbound, inbound, Collections.<org.jboss.jca.common.api.metadata.spec.AdminObject>emptyList(), Collections.<SecurityPermission>emptyList(), null);
    }

    private InboundResourceAdapter createInbound(List<ConfigProperty> inboundProps) {
        List<RequiredConfigProperty> destination = Collections.<RequiredConfigProperty>singletonList(new RequiredConfigPropertyImpl(EMPTY_LOCL, str("destination"), null));

        Activationspec activation15 = new ActivationSpecImpl(str(ACTIVEMQ_ACTIVATION), destination, inboundProps, null);
        List<MessageListener> messageListeners = Collections.<MessageListener>singletonList(new MessageListenerImpl(str(JMS_MESSAGE_LISTENER), activation15, null));
        Messageadapter message = new MessageAdapterImpl(messageListeners, null);

        return new InboundResourceAdapterImpl(message, null);
    }

    private static OutboundResourceAdapter createOutbound(List<ConfigProperty> outboundProperties) {
        List<org.jboss.jca.common.api.metadata.spec.ConnectionDefinition> definitions = new ArrayList<>();
        List<ConfigProperty> props = new ArrayList<>(outboundProperties);
        props.add(simpleProperty15(SESSION_DEFAULT_TYPE, STRING_TYPE, JMS_QUEUE));
        props.add(simpleProperty15(TRY_LOCK, INTEGER_TYPE, "0"));
        definitions.add(new org.jboss.jca.common.metadata.spec.ConnectionDefinitionImpl(str(RAMANAGED_CONN_FACTORY), props, str(RA_CONN_FACTORY), str(RA_CONN_FACTORY_IMPL), str(JMS_SESSION), str(ACTIVEMQ_RA_SESSION), null));

        AuthenticationMechanism basicPassword = new AuthenticationMechanismImpl(Collections.<LocalizedXsdString>emptyList(), str(BASIC_PASS), CredentialInterfaceEnum.PasswordCredential, null, null);
        return new OutboundResourceAdapterImpl(definitions, TransactionSupportEnum.XATransaction, Collections.singletonList(basicPassword), false, null, null, null);
    }

    private static XsdString str(String str) {
        return new XsdString(str, null);
    }

    private static ConfigProperty simpleProperty15(String name, String type, String value) {
        return new ConfigPropertyImpl(EMPTY_LOCL, str(name), str(type), str(value), null, null, null, null, false, null, null, null, null);
    }

    @Override
    public void stop(StopContext context) {
        // Service context takes care of this
    }
    public CommandDispatcherFactory getCommandDispatcherFactory(String name) {
        return this.commandDispatcherFactories.get(name).get();
    }
}
