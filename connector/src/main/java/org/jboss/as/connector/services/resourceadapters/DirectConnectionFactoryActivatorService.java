/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.services.resourceadapters;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.metadata.api.common.Security;
import org.jboss.as.connector.metadata.api.resourceadapter.ActivationSecurityUtil;
import org.jboss.as.connector.metadata.common.SecurityImpl;
import org.jboss.as.connector.services.mdr.AS7MetadataRepository;
import org.jboss.as.connector.services.resourceadapters.deployment.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.subsystems.jca.JcaSubsystemConfiguration;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.security.service.SimpleSecurityManagerService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.common.Pool;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.resourceadapter.AdminObject;
import org.jboss.jca.common.api.metadata.spec.ConnectionDefinition;
import org.jboss.jca.common.api.metadata.spec.Connector;
import org.jboss.jca.common.api.metadata.spec.ResourceAdapter;
import org.jboss.jca.common.metadata.common.PoolImpl;
import org.jboss.jca.common.metadata.common.XaPoolImpl;
import org.jboss.jca.common.metadata.resourceadapter.ActivationImpl;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SubjectFactory;

import javax.resource.spi.TransactionSupport;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;
import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;

public class DirectConnectionFactoryActivatorService implements org.jboss.msc.service.Service<org.jboss.as.naming.deployment.ContextNames.BindInfo> {
    public static final org.jboss.msc.service.ServiceName SERVICE_NAME_BASE =
            org.jboss.msc.service.ServiceName.JBOSS.append("connector").append("direct-connection-factory-activator");

    protected final InjectedValue<AS7MetadataRepository> mdr = new InjectedValue<AS7MetadataRepository>();


    private final String jndiName;
    private final String interfaceName;
    private final String resourceAdapter;
    private final String raId;

    private final int maxPoolSize;
    private final int minPoolSize;

    private final Map<String, String> properties;


    private final TransactionSupport.TransactionSupportLevel transactionSupport;

    private final Module module;

    private final ContextNames.BindInfo bindInfo;

    /**
     * create an instance *
     */
    public DirectConnectionFactoryActivatorService(String jndiName, String interfaceName, String resourceAdapter,
                                                   String raId, int maxPoolSize, int minPoolSize,
                                                   Map<String, String> properties, TransactionSupport.TransactionSupportLevel transactionSupport,
                                                   Module module, ContextNames.BindInfo bindInfo) {
        this.jndiName = jndiName;
        this.interfaceName = interfaceName;
        this.resourceAdapter = resourceAdapter;
        this.raId = raId;
        this.maxPoolSize = maxPoolSize;
        this.minPoolSize = minPoolSize;
        this.properties = properties;
        if (transactionSupport == null)
            transactionSupport = TransactionSupport.TransactionSupportLevel.NoTransaction;
        this.transactionSupport = transactionSupport;
        this.module = module;
        this.bindInfo = bindInfo;
    }

    @Override
    public ContextNames.BindInfo getValue() throws IllegalStateException, IllegalArgumentException {
        return bindInfo;
    }

    @Override
    public void start(org.jboss.msc.service.StartContext context) throws org.jboss.msc.service.StartException {
        ROOT_LOGGER.debugf("started DirectConnectionFactoryActivatorService %s", context.getController().getName());
        String cfInterface = null;


        try {

            Connector cmd = mdr.getValue().getResourceAdapter(raId);

            ResourceAdapter ra = cmd.getResourceadapter();
            if (ra.getOutboundResourceadapter() != null) {
                for (ConnectionDefinition cd : ra.getOutboundResourceadapter().getConnectionDefinitions()) {
                    if (cd.getConnectionFactoryInterface().getValue().equals(interfaceName))
                        cfInterface = cd.getConnectionFactoryInterface().getValue();
                }
            }


            if (cfInterface == null || !cfInterface.equals(interfaceName)) {
                throw ConnectorLogger.ROOT_LOGGER.invalidConnectionFactory(cfInterface, resourceAdapter, jndiName);
            }

            Map<String, String> raConfigProperties = new HashMap<String, String>();
            Map<String, String> mcfConfigProperties = new HashMap<String, String>();
            String securitySetting = null;
            String securitySettingDomain = null;
            boolean elytronEnabled = false;

            if (properties != null) {
                for (Map.Entry<String,String> prop : properties.entrySet()) {
                    String key = prop.getKey();
                    String value = prop.getValue();
                    if (key.equals("ironjacamar.security")) {
                        securitySetting = value;
                    } else if (key.equals("ironjacamar.security.elytron") && value.equals("true")) {
                        elytronEnabled = true;
                    } else if (key.equals("ironjacamar.security.elytron-authentication-context")) {
                        securitySettingDomain = value;
                        elytronEnabled = true;
                    } else if (key.equals("ironjacamar.security.domain")) {
                        securitySettingDomain = value;
                    } else {
                        if (key.startsWith("ra.")) {
                            raConfigProperties.put(key.substring(3), value);
                        } else if (key.startsWith("mcf.")) {
                            mcfConfigProperties.put(key.substring(4), value);
                        } else {
                            mcfConfigProperties.put(key, value);
                        }
                    }
                }
            }

            String mcfClass = null;

            if (ra.getOutboundResourceadapter() != null) {
                for (ConnectionDefinition cd :
                        ra.getOutboundResourceadapter().getConnectionDefinitions()) {
                    if (cd.getConnectionFactoryInterface().getValue().equals(cfInterface))
                        mcfClass = cd.getManagedConnectionFactoryClass().getValue();
                }
            }


            Security security = null;
            if (securitySetting != null) {
                if ("".equals(securitySetting)) {
                    security = new SecurityImpl(null, null, false, false);
                } else if ("application".equals(securitySetting)) {
                    security = new SecurityImpl(null, null, true, false);
                } else if ("domain".equals(securitySetting) && securitySettingDomain != null) {
                    security = new SecurityImpl(securitySettingDomain, null, false, elytronEnabled);
                } else if ("domain-and-application".equals(securitySetting) && securitySettingDomain != null) {
                    security = new SecurityImpl(null, securitySettingDomain, false, elytronEnabled);
                }
            }

            if (security == null) {
                SUBSYSTEM_RA_LOGGER.noSecurityDefined(jndiName);
            }


            Pool pool = null;
            Boolean isXA = Boolean.FALSE;
            if (transactionSupport == TransactionSupport.TransactionSupportLevel.XATransaction) {
                pool = new XaPoolImpl(minPoolSize < 0 ? Defaults.MIN_POOL_SIZE : minPoolSize, Defaults.INITIAL_POOL_SIZE, maxPoolSize < 0 ? Defaults.MAX_POOL_SIZE : maxPoolSize,
                        Defaults.PREFILL, Defaults.USE_STRICT_MIN, Defaults.FLUSH_STRATEGY,
                        null, Defaults.FAIR, Defaults.IS_SAME_RM_OVERRIDE, Defaults.INTERLEAVING, Defaults.PAD_XID, Defaults.WRAP_XA_RESOURCE, Defaults.NO_TX_SEPARATE_POOL);
                isXA = Boolean.TRUE;
            } else {
                pool = new PoolImpl(minPoolSize < 0 ? Defaults.MIN_POOL_SIZE : minPoolSize, Defaults.INITIAL_POOL_SIZE, maxPoolSize < 0 ? Defaults.MAX_POOL_SIZE : maxPoolSize,
                        Defaults.PREFILL, Defaults.USE_STRICT_MIN, Defaults.FLUSH_STRATEGY, null, Defaults.FAIR);
            }

            TransactionSupportEnum transactionSupportValue = TransactionSupportEnum.NoTransaction;
            if (transactionSupport == TransactionSupport.TransactionSupportLevel.XATransaction) {
                transactionSupportValue = TransactionSupportEnum.XATransaction;
            } else if (transactionSupport == TransactionSupport.TransactionSupportLevel.LocalTransaction) {
                transactionSupportValue = TransactionSupportEnum.LocalTransaction;
            }

            org.jboss.jca.common.api.metadata.resourceadapter.ConnectionDefinition cd = new org.jboss.jca.common.metadata.resourceadapter.ConnectionDefinitionImpl(mcfConfigProperties, mcfClass, jndiName, poolName(cfInterface),
                    Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Defaults.CONNECTABLE, Defaults.TRACKING,
                    Defaults.MCP, Defaults.ENLISTMENT_TRACE, pool, null, null, security, null, isXA);

            Activation activation = new ActivationImpl(null, null, transactionSupportValue, Collections.singletonList(cd), Collections.<AdminObject>emptyList(), raConfigProperties, Collections.<String>emptyList(), null, null);

            String serviceName = jndiName;
            serviceName = serviceName.replace(':', '_');
            serviceName = serviceName.replace('/', '_');

            ResourceAdapterActivatorService activator = new ResourceAdapterActivatorService(cmd, activation, module.getClassLoader(), serviceName);
            activator.setCreateBinderService(false);
            activator.setBindInfo(bindInfo);
            org.jboss.msc.service.ServiceTarget serviceTarget = context.getChildTarget();
            org.jboss.msc.service.ServiceName activatorServiceName = ConnectorServices.RESOURCE_ADAPTER_ACTIVATOR_SERVICE.append(serviceName);
            org.jboss.msc.service.ServiceBuilder connectionFactoryServiceBuilder = serviceTarget
                    .addService(activatorServiceName, activator)
                    .addDependency(ConnectorServices.IRONJACAMAR_MDR, AS7MetadataRepository.class,
                            activator.getMdrInjector())
                    .addDependency(ConnectorServices.RA_REPOSITORY_SERVICE, ResourceAdapterRepository.class,
                            activator.getRaRepositoryInjector())
                    .addDependency(ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE, ManagementRepository.class,
                            activator.getManagementRepositoryInjector())
                    .addDependency(ConnectorServices.RESOURCE_ADAPTER_REGISTRY_SERVICE,
                            ResourceAdapterDeploymentRegistry.class, activator.getRegistryInjector())
                    .addDependency(ConnectorServices.CONNECTOR_CONFIG_SERVICE,
                            JcaSubsystemConfiguration.class, activator.getConfigInjector())
                    .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class,
                            activator.getCcmInjector())
                    .addDependency(NamingService.SERVICE_NAME)
                    .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class,
                            activator.getTxIntegrationInjector())
                    .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER)
                    .addDependency(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append("default"));

            if (ActivationSecurityUtil.isLegacySecurityRequired(security)) {
                connectionFactoryServiceBuilder
                        .addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class,
                                activator.getSubjectFactoryInjector())
                        .addDependency(SimpleSecurityManagerService.SERVICE_NAME,
                                ServerSecurityManager.class, activator.getServerSecurityManager());
            }

            connectionFactoryServiceBuilder.setInitialMode(org.jboss.msc.service.ServiceController.Mode.ACTIVE).install();


        } catch (Exception e) {
            throw new org.jboss.msc.service.StartException(e);
        }
    }

    public Injector<AS7MetadataRepository> getMdrInjector() {
        return mdr;
    }

    @Override
    public void stop(org.jboss.msc.service.StopContext context) {
        ROOT_LOGGER.debugf("stopped DirectConnectionFactoryActivatorService %s", context.getController().getName());

    }

    private String poolName(final String cfInterface) {
        if (cfInterface.indexOf(".") != -1) {
            return cfInterface.substring(cfInterface.lastIndexOf(".") + 1);
        } else {
            return cfInterface;
        }
    }

}
