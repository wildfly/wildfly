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

import org.hornetq.api.core.DiscoveryGroupConfiguration;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.services.ResourceAdapterActivatorService;
import org.jboss.as.connector.subsystems.jca.JcaSubsystemConfiguration;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.jca.common.api.metadata.common.CommonAdminObject;
import org.jboss.jca.common.api.metadata.common.CommonConnDef;
import org.jboss.jca.common.api.metadata.common.FlushStrategy;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.AdminObject;
import org.jboss.jca.common.api.metadata.ra.AuthenticationMechanism;
import org.jboss.jca.common.api.metadata.ra.ConfigProperty;
import org.jboss.jca.common.api.metadata.ra.ConnectionDefinition;
import org.jboss.jca.common.api.metadata.ra.CredentialInterfaceEnum;
import org.jboss.jca.common.api.metadata.ra.Icon;
import org.jboss.jca.common.api.metadata.ra.InboundResourceAdapter;
import org.jboss.jca.common.api.metadata.ra.LocalizedXsdString;
import org.jboss.jca.common.api.metadata.ra.MessageListener;
import org.jboss.jca.common.api.metadata.ra.Messageadapter;
import org.jboss.jca.common.api.metadata.ra.OutboundResourceAdapter;
import org.jboss.jca.common.api.metadata.ra.RequiredConfigProperty;
import org.jboss.jca.common.api.metadata.ra.ResourceAdapter1516;
import org.jboss.jca.common.api.metadata.ra.SecurityPermission;
import org.jboss.jca.common.api.metadata.ra.XsdString;
import org.jboss.jca.common.api.metadata.ra.ra16.ConfigProperty16;
import org.jboss.jca.common.api.metadata.ra.ra16.Connector16;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.common.CommonConnDefImpl;
import org.jboss.jca.common.metadata.common.CommonPoolImpl;
import org.jboss.jca.common.metadata.common.CommonSecurityImpl;
import org.jboss.jca.common.metadata.common.CommonTimeOutImpl;
import org.jboss.jca.common.metadata.common.CommonValidationImpl;
import org.jboss.jca.common.metadata.common.CredentialImpl;
import org.jboss.jca.common.metadata.ironjacamar.IronJacamarImpl;
import org.jboss.jca.common.metadata.ra.common.AuthenticationMechanismImpl;
import org.jboss.jca.common.metadata.ra.common.ConfigPropertyImpl;
import org.jboss.jca.common.metadata.ra.common.ConnectionDefinitionImpl;
import org.jboss.jca.common.metadata.ra.common.InboundResourceAdapterImpl;
import org.jboss.jca.common.metadata.ra.common.MessageAdapterImpl;
import org.jboss.jca.common.metadata.ra.common.MessageListenerImpl;
import org.jboss.jca.common.metadata.ra.common.OutboundResourceAdapterImpl;
import org.jboss.jca.common.metadata.ra.common.ResourceAdapter1516Impl;
import org.jboss.jca.common.metadata.ra.ra16.Activationspec16Impl;
import org.jboss.jca.common.metadata.ra.ra16.ConfigProperty16Impl;
import org.jboss.jca.common.metadata.ra.ra16.Connector16Impl;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.MapInjector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SubjectFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.jboss.as.messaging.CommonAttributes.*;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

/**
 * A service which translates a pooled connection factory into a resource adapter driven connection pool
 *
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Jason T. Greene
 *         Date: 5/13/11
 *         Time: 2:21 PM
 */
public class PooledConnectionFactoryService implements Service<Void> {

    private static final List<LocalizedXsdString> EMPTY_LOCL = Collections.emptyList();
    private static final String CONNECTOR_CLASSNAME = "ConnectorClassName";
    private static final String CONNECTION_PARAMETERS = "ConnectionParameters";
    private static final String HQ_ACTIVIATION = "org.hornetq.ra.inflow.HornetQActivationSpec";
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
    private static final String GROUP_ADDRESS = "discoveryAddress";
    private static final String DISCOVERY_INITIAL_WAIT_TIMEOUT = "discoveryInitialWaitTimeout";
    private static final String GROUP_PORT = "discoveryPort";
    private static final String REFRESH_TIMEOUT = "discoveryRefreshTimeout";

    private static final Collection<String> JMS_ACTIVATION_CONFIG_PROPERTIES = new HashSet<String>();

   {
        // All the activation-config-properties that are mandated to be supported by the RA, as per EJB3.1 spec,
        // section 5.4.15 through 5.4.17

        JMS_ACTIVATION_CONFIG_PROPERTIES.add("acknowledgeMode");
        JMS_ACTIVATION_CONFIG_PROPERTIES.add("destinationType");
        JMS_ACTIVATION_CONFIG_PROPERTIES.add("messageSelector");
        JMS_ACTIVATION_CONFIG_PROPERTIES.add("subscriptionDurability");
    }

    private Injector<Object> transactionManager = new InjectedValue<Object>();
    private List<String> connectors;
    private String discoveryGroupName;
    private List<PooledConnectionFactoryConfigProperties> adapterParams;
    private String name;
    private Map<String, SocketBinding> socketBindings = new HashMap<String, SocketBinding>();
    private InjectedValue<HornetQServer> hornetQService = new InjectedValue<HornetQServer>();
    private String jndiName;
    private String txSupport;

   public PooledConnectionFactoryService(String name, List<String> connectors, String discoveryGroupName, List<PooledConnectionFactoryConfigProperties> adapterParams, String jndiName, String txSupport) {
        this.name = name;
        this.connectors = connectors;
        this.discoveryGroupName = discoveryGroupName;
        this.adapterParams = adapterParams;
        this.jndiName = jndiName;
        this.txSupport = txSupport;
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
            throw new StartException(MESSAGES.failedToCreate("resource adapter"), e);
        }

    }

    private void createService(ServiceTarget serviceTarget, ServiceContainer container) throws Exception {
        InputStream is = null;
        InputStream isIj = null;
        List<ConfigProperty16> properties = new ArrayList<ConfigProperty16>();
        try {
            StringBuilder connectorClassname = new StringBuilder();
            StringBuilder connectorParams = new StringBuilder();
            for (String connector : connectors) {
                TransportConfiguration tc = hornetQService.getValue().getConfiguration().getConnectorConfigurations().get(connector);
                if(tc == null) {
                    throw MESSAGES.connectorNotDefined(connector);
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
                properties.add(simpleProperty(CONNECTOR_CLASSNAME, STRING_TYPE, connectorClassname.toString()));
            }
            if (connectorParams.length() > 0) {
                properties.add(simpleProperty(CONNECTION_PARAMETERS, STRING_TYPE, connectorParams.toString()));
            }

            if(discoveryGroupName != null) {
                DiscoveryGroupConfiguration discoveryGroupConfiguration = hornetQService.getValue().getConfiguration().getDiscoveryGroupConfigurations().get(discoveryGroupName);
                properties.add(simpleProperty(GROUP_ADDRESS, STRING_TYPE, discoveryGroupConfiguration.getGroupAddress()));
                properties.add(simpleProperty(DISCOVERY_INITIAL_WAIT_TIMEOUT, LONG_TYPE, "" + discoveryGroupConfiguration.getDiscoveryInitialWaitTimeout()));
                properties.add(simpleProperty(GROUP_PORT, INTEGER_TYPE, "" + discoveryGroupConfiguration.getGroupPort()));
                properties.add(simpleProperty(REFRESH_TIMEOUT, LONG_TYPE, "" + discoveryGroupConfiguration.getRefreshTimeout()));
            }

            boolean hasReconnect = false;
            final String reconnectName = JMSServices.RECONNECT_ATTEMPTS_METHOD;
            for (PooledConnectionFactoryConfigProperties adapterParam : adapterParams) {
                hasReconnect |= reconnectName.equals(adapterParam.getName());

                properties.add(simpleProperty(adapterParam.getName(), adapterParam.getType(), adapterParam.getValue()));
            }

            // The default -1, which will hang forever until a server appears
            if (!hasReconnect) {
                properties.add(simpleProperty(reconnectName, Integer.class.getName(), DEFAULT_MAX_RECONNECTS));
            }

            TransactionManagerLocator.container = container;
            AS7RecoveryRegistry.container = container;
            properties.add(simpleProperty("TransactionManagerLocatorClass", STRING_TYPE, TransactionManagerLocator.class.getName()));
            properties.add(simpleProperty("TransactionManagerLocatorMethod", STRING_TYPE, "getTransactionManager"));

            OutboundResourceAdapter outbound = createOutbound();
            InboundResourceAdapter inbound = createInbound();
            ResourceAdapter1516 ra = createResourceAdapter(properties, outbound, inbound);
            Connector16 cmd = createConnector(ra);

            CommonConnDef common = createConnDef(jndiName);
            IronJacamar ijmd = createIron(common, txSupport);

            ResourceAdapterActivatorService activator = new ResourceAdapterActivatorService(cmd, ijmd,
                    PooledConnectionFactoryService.class.getClassLoader(), name);

            serviceTarget
                    .addService(ConnectorServices.RESOURCE_ADAPTER_ACTIVATOR_SERVICE.append(name), activator)
                    .addDependency(ConnectorServices.IRONJACAMAR_MDR, MetadataRepository.class,
                            activator.getMdrInjector())
                    .addDependency(ConnectorServices.RA_REPOSISTORY_SERVICE, ResourceAdapterRepository.class,
                            activator.getRaRepositoryInjector())
                    .addDependency(ConnectorServices.MANAGEMENT_REPOSISTORY_SERVICE, ManagementRepository.class,
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
                    .setInitialMode(ServiceController.Mode.ACTIVE).install();

            // Mock the deployment service to allow it to start
            serviceTarget.addService(ConnectorServices.RESOURCE_ADAPTER_DEPLOYER_SERVICE_PREFIX.append(name), Service.NULL).install();
        } finally {
            if (is != null)
                is.close();
            if (isIj != null)
                isIj.close();
        }
    }

    private static IronJacamarImpl createIron(CommonConnDef common, String txSupport) {
        TransactionSupportEnum transactionSupport;

        try {
            transactionSupport = TransactionSupportEnum.valueOf(txSupport);
        } catch (RuntimeException e) {
            transactionSupport = TransactionSupportEnum.LocalTransaction;
        }

        List<CommonConnDef> definitions = Collections.singletonList(common);
        return new IronJacamarImpl(transactionSupport, Collections.<String, String>emptyMap(), Collections.<CommonAdminObject>emptyList(), definitions, Collections.<String>emptyList(), null);
    }

    private static CommonConnDef createConnDef(String jndiName) throws ValidateException {
        CommonPoolImpl pool = new CommonPoolImpl(null, null, false, false, FlushStrategy.FAILING_CONNECTION_ONLY);
        CommonTimeOutImpl timeOut = new CommonTimeOutImpl(null, null, null, null, null);
        CommonSecurityImpl security = null;
        Recovery recovery = new Recovery(new CredentialImpl(null, null, null), null, Boolean.FALSE);
        CommonValidationImpl validation = new CommonValidationImpl(null, null, false);
        return new CommonConnDefImpl(Collections.<String, String>emptyMap(), RAMANAGED_CONN_FACTORY, jndiName, HQ_CONN_DEF, true, true, true, pool, timeOut, validation, security, recovery);
    }

    private static Connector16Impl createConnector(ResourceAdapter1516 ra) {
        return new Connector16Impl(null, str("Red Hat"), str("JMS 1.1 Server"), str("1.0"), null, ra, Collections.<String>emptyList(), false, EMPTY_LOCL, EMPTY_LOCL, Collections.<Icon>emptyList(), null);
    }

    private ResourceAdapter1516Impl createResourceAdapter(List<ConfigProperty16> properties, OutboundResourceAdapter outbound, InboundResourceAdapter inbound) {
        return new ResourceAdapter1516Impl(HQ_ADAPTER, properties, outbound, inbound, Collections.<AdminObject>emptyList(), Collections.<SecurityPermission>emptyList(), null);
    }

    private InboundResourceAdapter createInbound() {
        InboundResourceAdapter inbound;
        List<RequiredConfigProperty> destination = Collections.singletonList(new RequiredConfigProperty(EMPTY_LOCL, str("destination"), null));
        // setup the JMS activation config properties
        final List<ConfigProperty> jmsActivationConfigProps = new ArrayList<ConfigProperty>(JMS_ACTIVATION_CONFIG_PROPERTIES.size());
        for (final String activationConfigProp : JMS_ACTIVATION_CONFIG_PROPERTIES) {
            final ConfigProperty configProp = new ConfigPropertyImpl(EMPTY_LOCL, str(activationConfigProp), str(STRING_TYPE), null, null);
            jmsActivationConfigProps.add(configProp);
        }
        Activationspec16Impl activation = new Activationspec16Impl(str(HQ_ACTIVIATION), destination, jmsActivationConfigProps, null);
        List<MessageListener> messageListeners = Collections.<MessageListener>singletonList(new MessageListenerImpl(str(JMS_MESSAGE_LISTENER), activation, null));
        Messageadapter message = new MessageAdapterImpl(messageListeners, null);

        return new InboundResourceAdapterImpl(message, null);
    }

    private static OutboundResourceAdapter createOutbound() {
        List<ConnectionDefinition> definitions = new ArrayList<ConnectionDefinition>();
        List<ConfigProperty16> props = new ArrayList<ConfigProperty16>();
        props.add(simpleProperty(SESSION_DEFAULT_TYPE, STRING_TYPE, JMS_QUEUE));
        props.add(simpleProperty(TRY_LOCK, INTEGER_TYPE, "0"));
        definitions.add(new ConnectionDefinitionImpl(str(RAMANAGED_CONN_FACTORY), props, str(RA_CONN_FACTORY), str(RA_CONN_FACTORY_IMPL), str(JMS_SESSION), str(HQ_SESSION), null));

        AuthenticationMechanism basicPassword = new AuthenticationMechanismImpl(Collections.<LocalizedXsdString>emptyList(), str(BASIC_PASS), CredentialInterfaceEnum.PasswordCredential, null);
        return new OutboundResourceAdapterImpl(definitions, TransactionSupportEnum.XATransaction, Collections.singletonList(basicPassword), false, null);
    }

    private static XsdString str(String str) {
        return new XsdString(str, null);
    }

    private static ConfigProperty16 simpleProperty(String name, String type, String value) {
        return new ConfigProperty16Impl(EMPTY_LOCL, str(name), str(type), str(value), Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null, null);
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
