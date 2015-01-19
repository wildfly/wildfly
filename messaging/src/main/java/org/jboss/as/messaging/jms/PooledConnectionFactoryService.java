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

package org.jboss.as.messaging.jms;

import static java.util.Collections.EMPTY_LIST;
import static org.jboss.as.messaging.BinderServiceUtil.installAliasBinderService;
import static org.jboss.as.messaging.MessagingServices.getHornetQServiceName;
import static org.jboss.as.naming.deployment.ContextNames.BindInfo;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hornetq.api.core.BroadcastEndpointFactoryConfiguration;
import org.hornetq.api.core.DiscoveryGroupConfiguration;
import org.hornetq.api.core.JGroupsBroadcastGroupConfiguration;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.UDPBroadcastGroupConfiguration;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
import org.jboss.as.connector.services.mdr.AS7MetadataRepository;
import org.jboss.as.connector.services.resourceadapters.ResourceAdapterActivatorService;
import org.jboss.as.connector.services.resourceadapters.deployment.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.subsystems.jca.JcaSubsystemConfiguration;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.messaging.HornetQActivationService;
import org.jboss.as.messaging.JGroupsChannelLocator;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.as.server.Services;
import org.jboss.as.txn.service.TxnServices;
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
import org.jboss.jca.common.metadata.common.CredentialImpl;
import org.jboss.jca.common.metadata.common.PoolImpl;
import org.jboss.jca.common.metadata.common.SecurityImpl;
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
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.MapInjector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SubjectFactory;

/**
 * A service which translates a pooled connection factory into a resource adapter driven connection pool
 *
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author <a href="mailto:jbertram@redhat.com">Justin Bertram</a>
 * @author Jason T. Greene
 *         Date: 5/13/11
 *         Time: 2:21 PM
 */
public class PooledConnectionFactoryService implements Service<Void> {

    private static final List<LocalizedXsdString> EMPTY_LOCL = Collections.emptyList();
    public static final String CONNECTOR_CLASSNAME = "connectorClassName";
    public static final String CONNECTION_PARAMETERS = "connectionParameters";
    private static final String HQ_ACTIVATION = "org.hornetq.ra.inflow.HornetQActivationSpec";
    private static final String HQ_CONN_DEF = "HornetQConnectionDefinition";
    private static final String HQ_ADAPTER = "org.hornetq.ra.HornetQResourceAdapter";
    private static final String RAMANAGED_CONN_FACTORY = "org.hornetq.ra.HornetQRAManagedConnectionFactory";
    private static final String RA_CONN_FACTORY = "org.hornetq.ra.HornetQRAConnectionFactory";
    private static final String RA_CONN_FACTORY_IMPL = "org.hornetq.ra.HornetQRAConnectionFactoryImpl";
    private static final String JMS_SESSION = "javax.jms.Session";
    private static final String HQ_SESSION = "org.hornetq.ra.HornetQRASession";
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
    public static final String TRANSACTION_MANAGER_LOCATOR_METHOD = "transactionManagerLocatorMethod";
    public static final String TRANSACTION_MANAGER_LOCATOR_CLASS = "transactionManagerLocatorClass";
    public static final String JGROUPS_CHANNEL_LOCATOR_CLASS = "jgroupsChannelLocatorClass";
    public static final String JGROUPS_CHANNEL_NAME = "jgroupsChannelName";
    public static final String JGROUPS_CHANNEL_REF_NAME = "jgroupsChannelRefName";

    private Injector<Object> transactionManager = new InjectedValue<Object>();
    private List<String> connectors;
    private String discoveryGroupName;
    private List<PooledConnectionFactoryConfigProperties> adapterParams;
    private String name;
    private Map<String, SocketBinding> socketBindings = new HashMap<String, SocketBinding>();
    private InjectedValue<HornetQServer> hornetQService = new InjectedValue<HornetQServer>();
    private BindInfo bindInfo;
    private final boolean pickAnyConnectors;
    private List<String> jndiAliases;
    private String txSupport;
    private int minPoolSize;
    private int maxPoolSize;
    private String hqServerName;
    private final String jgroupsChannelName;
    private final boolean createBinderService;

   public PooledConnectionFactoryService(String name, List<String> connectors, String discoveryGroupName, String hqServerName, String jgroupsChannelName, List<PooledConnectionFactoryConfigProperties> adapterParams, List<String> jndiNames, String txSupport, int minPoolSize, int maxPoolSize) {
        this.name = name;
        this.connectors = connectors;
        this.discoveryGroupName = discoveryGroupName;
        this.hqServerName = hqServerName;
        this.jgroupsChannelName = jgroupsChannelName;
        this.adapterParams = adapterParams;
        initJNDIBindings(jndiNames);
        createBinderService = true;
        this.txSupport = txSupport;
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.pickAnyConnectors = false;
    }

    public PooledConnectionFactoryService(String name, List<String> connectors, String discoveryGroupName, String hqServerName, String jgroupsChannelName, List<PooledConnectionFactoryConfigProperties> adapterParams, BindInfo bindInfo, String txSupport, int minPoolSize, int maxPoolSize, boolean pickAnyConnectors) {
        this.name = name;
        this.connectors = connectors;
        this.discoveryGroupName = discoveryGroupName;
        this.hqServerName = hqServerName;
        this.jgroupsChannelName = jgroupsChannelName;
        this.adapterParams = adapterParams;
        this.bindInfo = bindInfo;
        this.jndiAliases = EMPTY_LIST;
        this.createBinderService = false;
        this.txSupport = txSupport;
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.pickAnyConnectors = pickAnyConnectors;
    }

    private void initJNDIBindings(List<String> jndiNames) {
        // create the definition with the 1st jndi names and create jndi aliases for the rest
        String jndiName = jndiNames.get(0);
        this.bindInfo = ContextNames.bindInfoFor(jndiName);
        this.jndiAliases = new ArrayList<String>();
        if (jndiNames.size() > 1) {
            jndiAliases = jndiNames.subList(1, jndiNames.size());
        }
    }

    /**
     *
     */

    public static void installService(ServiceTarget serviceTarget,
                                      String name,
                                      String hqServerName,
                                      List<String> connectors,
                                      String discoveryGroupName,
                                      String jgroupsChannelName,
                                      List<PooledConnectionFactoryConfigProperties> adapterParams,
                                      BindInfo bindInfo,
                                      String txSupport,
                                      int minPoolSize,
                                      int maxPoolSize,
                                      boolean pickAnyConnectors) {

        ServiceName hqServiceName = MessagingServices.getHornetQServiceName(hqServerName);
        ServiceName serviceName = JMSServices.getPooledConnectionFactoryBaseServiceName(hqServiceName).append(name);

        PooledConnectionFactoryService service = new PooledConnectionFactoryService(name,
                connectors, discoveryGroupName, hqServerName, jgroupsChannelName, adapterParams,
                bindInfo, txSupport, minPoolSize, maxPoolSize, pickAnyConnectors);

        installService0(serviceTarget, hqServiceName, serviceName, service);
    }

    /**
     *
     */
    public static void installService(ServiceTarget serviceTarget,
                                      String name,
                                      String hqServerName,
                                      List<String> connectors,
                                      String discoveryGroupName,
                                      String jgroupsChannelName,
                                      List<PooledConnectionFactoryConfigProperties> adapterParams,
                                      List<String> jndiNames,
                                      String txSupport,
                                      int minPoolSize,
                                      int maxPoolSize) {

        ServiceName hqServiceName = MessagingServices.getHornetQServiceName(hqServerName);
        ServiceName serviceName = JMSServices.getPooledConnectionFactoryBaseServiceName(hqServiceName).append(name);
        PooledConnectionFactoryService service = new PooledConnectionFactoryService(name,
                connectors, discoveryGroupName, hqServerName, jgroupsChannelName, adapterParams,
                jndiNames, txSupport, minPoolSize, maxPoolSize);

        installService0(serviceTarget, hqServiceName, serviceName, service);
    }

    private static void installService0(ServiceTarget serviceTarget,
                                        ServiceName hqServiceName,
                                        ServiceName serviceName,
                                        PooledConnectionFactoryService service) {
        serviceTarget
                .addService(serviceName, service)
                .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, service.transactionManager)
                .addDependency(hqServiceName, HornetQServer.class, service.hornetQService)
                .addDependency(HornetQActivationService.getHornetQActivationServiceName(hqServiceName))
                .addDependency(JMSServices.getJmsManagerBaseServiceName(hqServiceName))
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install();
    }

    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }


    public void start(StartContext context) throws StartException {
        ServiceTarget serviceTarget = context.getChildTarget();
        try {
            createService(serviceTarget, context.getController().getServiceContainer());
        }
        catch (Exception e) {
            throw MessagingLogger.ROOT_LOGGER.failedToCreate(e, "resource adapter");
        }

    }

    private void createService(ServiceTarget serviceTarget, ServiceContainer container) throws Exception {
        InputStream is = null;
        InputStream isIj = null;
        List<ConfigProperty> properties = new ArrayList<ConfigProperty>();
        try {
            StringBuilder connectorClassname = new StringBuilder();
            StringBuilder connectorParams = new StringBuilder();
            // if there is no discovery-group and the connector list is empty,
            // pick the first connector available if pickAnyConnectors is true
            if (discoveryGroupName == null && connectors.isEmpty() && pickAnyConnectors) {
                Set<String> connectorNames = hornetQService.getValue().getConfiguration().getConnectorConfigurations().keySet();
                if (connectorNames.size() > 0) {
                    String connectorName = connectorNames.iterator().next();
                    MessagingLogger.ROOT_LOGGER.connectorForPooledConnectionFactory(name, connectorName);
                    connectors.add(connectorName);
                }
            }
            for (String connector : connectors) {
                TransportConfiguration tc = hornetQService.getValue().getConfiguration().getConnectorConfigurations().get(connector);
                if(tc == null) {
                    throw MessagingLogger.ROOT_LOGGER.connectorNotDefined(connector);
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

            if(discoveryGroupName != null) {
                DiscoveryGroupConfiguration discoveryGroupConfiguration = hornetQService.getValue().getConfiguration().getDiscoveryGroupConfigurations().get(discoveryGroupName);
                BroadcastEndpointFactoryConfiguration bgCfg = discoveryGroupConfiguration.getBroadcastEndpointFactoryConfiguration();
                if (bgCfg instanceof UDPBroadcastGroupConfiguration) {
                    UDPBroadcastGroupConfiguration udpCfg = (UDPBroadcastGroupConfiguration) bgCfg;
                    properties.add(simpleProperty15(GROUP_ADDRESS, STRING_TYPE, udpCfg.getGroupAddress()));
                    properties.add(simpleProperty15(GROUP_PORT, INTEGER_TYPE, "" + udpCfg.getGroupPort()));
                    properties.add(simpleProperty15(DISCOVERY_LOCAL_BIND_ADDRESS, STRING_TYPE, "" + udpCfg.getLocalBindAddress()));
                } else if (bgCfg instanceof JGroupsBroadcastGroupConfiguration) {
                    properties.add(simpleProperty15(JGROUPS_CHANNEL_LOCATOR_CLASS, STRING_TYPE, JGroupsChannelLocator.class.getName()));
                    properties.add(simpleProperty15(JGROUPS_CHANNEL_NAME, STRING_TYPE, jgroupsChannelName));
                    properties.add(simpleProperty15(JGROUPS_CHANNEL_REF_NAME, STRING_TYPE, hqServerName + '/' + jgroupsChannelName));

                }
                properties.add(simpleProperty15(DISCOVERY_INITIAL_WAIT_TIMEOUT, LONG_TYPE, "" + discoveryGroupConfiguration.getDiscoveryInitialWaitTimeout()));
                properties.add(simpleProperty15(REFRESH_TIMEOUT, LONG_TYPE, "" + discoveryGroupConfiguration.getRefreshTimeout()));
            }

            boolean hasReconnect = false;
            final String reconnectName = ConnectionFactoryAttributes.Pooled.RECONNECT_ATTEMPTS_PROP_NAME;
            for (PooledConnectionFactoryConfigProperties adapterParam : adapterParams) {
                hasReconnect |= reconnectName.equals(adapterParam.getName());

                properties.add(simpleProperty15(adapterParam.getName(), adapterParam.getType(), adapterParam.getValue()));
            }

            // The default -1, which will hang forever until a server appears
            if (!hasReconnect) {
                properties.add(simpleProperty15(reconnectName, Integer.class.getName(), DEFAULT_MAX_RECONNECTS));
            }

            TransactionManagerLocator.container = container;
            AS7RecoveryRegistry.container = container;
            properties.add(simpleProperty15(TRANSACTION_MANAGER_LOCATOR_CLASS, STRING_TYPE, TransactionManagerLocator.class.getName()));
            properties.add(simpleProperty15(TRANSACTION_MANAGER_LOCATOR_METHOD, STRING_TYPE, "getTransactionManager"));

            OutboundResourceAdapter outbound = createOutbound();
            InboundResourceAdapter inbound = createInbound();
            ResourceAdapter ra = createResourceAdapter15(properties, outbound, inbound);
            Connector cmd = createConnector15(ra);

            TransactionSupportEnum transactionSupport = getTransactionSupport(txSupport);
            ConnectionDefinition common = createConnDef(transactionSupport, bindInfo.getBindName(), minPoolSize, maxPoolSize);
            Activation activation = createActivation(common, transactionSupport);

            ResourceAdapterActivatorService activator = new ResourceAdapterActivatorService(cmd, activation,
                    PooledConnectionFactoryService.class.getClassLoader(), name);
            activator.setBindInfo(bindInfo);
            activator.setCreateBinderService(createBinderService);

            ServiceController<ResourceAdapterDeployment> controller =
                    Services.addServerExecutorDependency(
                        serviceTarget.addService(ConnectorServices.RESOURCE_ADAPTER_ACTIVATOR_SERVICE.append(name), activator),
                            activator.getExecutorServiceInjector(), false)
                    .addDependency(HornetQActivationService.getHornetQActivationServiceName(getHornetQServiceName(hqServerName)))
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
                    .addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class,
                            activator.getSubjectFactoryInjector())
                    .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class,
                            activator.getCcmInjector()).addDependency(NamingService.SERVICE_NAME)
                    .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER)
                    .addDependency(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append("default"))
                    .setInitialMode(ServiceController.Mode.PASSIVE).install();

            createJNDIAliases(bindInfo, jndiAliases, controller);

            // Mock the deployment service to allow it to start
            serviceTarget.addService(ConnectorServices.RESOURCE_ADAPTER_DEPLOYER_SERVICE_PREFIX.append(name), Service.NULL).install();
        } finally {
            if (is != null)
                is.close();
            if (isIj != null)
                isIj.close();
        }
    }

    private void createJNDIAliases(final BindInfo bindInfo, List<String> aliases, ServiceController<ResourceAdapterDeployment> controller) {
        for (final String alias : aliases) {
            // do not install the alias' binder service if it is already registered
            if (controller.getServiceContainer().getService(ContextNames.bindInfoFor(alias).getBinderServiceName()) == null) {
                installAliasBinderService(controller.getServiceContainer(),
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


    private static ConnectionDefinition createConnDef(TransactionSupportEnum transactionSupport, String jndiName, int minPoolSize, int maxPoolSize) throws ValidateException {
        Integer minSize = (minPoolSize == -1) ? null : minPoolSize;
        Integer maxSize = (maxPoolSize == -1) ? null : maxPoolSize;
        boolean prefill = false;
        boolean useStrictMin = false;
        FlushStrategy flushStrategy = FlushStrategy.FAILING_CONNECTION_ONLY;
        Boolean isXA = Boolean.FALSE;
        final Pool pool;
        if (transactionSupport == TransactionSupportEnum.XATransaction) {
            pool = new XaPoolImpl(minSize, Defaults.INITIAL_POOL_SIZE, maxSize, prefill, useStrictMin, flushStrategy, null,
                    Defaults.IS_SAME_RM_OVERRIDE, Defaults.INTERLEAVING, Defaults.PAD_XID, Defaults.WRAP_XA_RESOURCE, Defaults.NO_TX_SEPARATE_POOL);
            isXA = Boolean.TRUE;
        } else {
            pool = new PoolImpl(minSize, Defaults.INITIAL_POOL_SIZE, maxSize, prefill, useStrictMin, flushStrategy, null);
        }
        TimeOut timeOut = new TimeOutImpl(null, null, null, null, null) {
        };
        // <security>
        //   <application />
        // </security>
        // => PoolStrategy.POOL_BY_CRI
        Security security = new SecurityImpl(null, null, true);
        // register the XA Connection *without* recovery. HornetQ already takes care of the registration with the correct credentials
        // when its ResourceAdapter is started
        Recovery recovery = new Recovery(new CredentialImpl(null, null, null), null, Boolean.TRUE);
        Validation validation = new ValidationImpl(Defaults.VALIDATE_ON_MATCH, null, null, false);
        // do no track
        return new ConnectionDefinitionImpl(Collections.<String, String>emptyMap(), RAMANAGED_CONN_FACTORY, jndiName, HQ_CONN_DEF, true, true, true, Defaults.SHARABLE, Defaults.ENLISTMENT, Defaults.CONNECTABLE, false, pool, timeOut, validation, security, recovery, isXA);
    }

    private static Connector createConnector15(ResourceAdapter ra) {
        return new ConnectorImpl(Connector.Version.V_15, null, str("Red Hat"), str("JMS 1.1 Server"), str("1.0"), null, ra, null, false, EMPTY_LOCL, EMPTY_LOCL, Collections.<Icon>emptyList(), null);
    }

    private ResourceAdapter createResourceAdapter15(List<ConfigProperty> properties, OutboundResourceAdapter outbound, InboundResourceAdapter inbound) {
        return new ResourceAdapterImpl(str(HQ_ADAPTER), properties, outbound, inbound, Collections.<org.jboss.jca.common.api.metadata.spec.AdminObject>emptyList(), Collections.<SecurityPermission>emptyList(), null);
    }

    private InboundResourceAdapter createInbound() {
        List<RequiredConfigProperty> destination = Collections.<RequiredConfigProperty>singletonList(new RequiredConfigPropertyImpl(EMPTY_LOCL, str("destination"), null));

        Activationspec activation15 = new ActivationSpecImpl(str(HQ_ACTIVATION), destination, null, null);
        List<MessageListener> messageListeners = Collections.<MessageListener>singletonList(new MessageListenerImpl(str(JMS_MESSAGE_LISTENER), activation15, null));
        Messageadapter message = new MessageAdapterImpl(messageListeners, null);

        return new InboundResourceAdapterImpl(message, null);
    }

    private static OutboundResourceAdapter createOutbound() {
        List<org.jboss.jca.common.api.metadata.spec.ConnectionDefinition> definitions = new ArrayList<org.jboss.jca.common.api.metadata.spec.ConnectionDefinition>();
        List<ConfigProperty> props = new ArrayList<ConfigProperty>();
        props.add(simpleProperty15(SESSION_DEFAULT_TYPE, STRING_TYPE, JMS_QUEUE));
        props.add(simpleProperty15(TRY_LOCK, INTEGER_TYPE, "0"));
        definitions.add(new org.jboss.jca.common.metadata.spec.ConnectionDefinitionImpl(str(RAMANAGED_CONN_FACTORY), props, str(RA_CONN_FACTORY), str(RA_CONN_FACTORY_IMPL), str(JMS_SESSION), str(HQ_SESSION), null));

        AuthenticationMechanism basicPassword = new AuthenticationMechanismImpl(Collections.<LocalizedXsdString>emptyList(), str(BASIC_PASS), CredentialInterfaceEnum.PasswordCredential, null, null);
        return new OutboundResourceAdapterImpl(definitions, TransactionSupportEnum.XATransaction, Collections.singletonList(basicPassword), false, null, null, null);
    }

    private static XsdString str(String str) {
        return new XsdString(str, null);
    }

    private static ConfigProperty simpleProperty15(String name, String type, String value) {
        return new ConfigPropertyImpl(EMPTY_LOCL, str(name), str(type), str(value), null, null, null, null, false, null, null, null, null);
    }


    public void stop(StopContext context) {
        // Service context takes care of this
    }

    public Injector<Object> getTransactionManager() {
        return transactionManager;
    }

    Injector<SocketBinding> getSocketBindingInjector(String name) {
        return new MapInjector<String, SocketBinding>(socketBindings, name);
    }

    public Injector<HornetQServer> getHornetQService() {
        return hornetQService;
    }

}
