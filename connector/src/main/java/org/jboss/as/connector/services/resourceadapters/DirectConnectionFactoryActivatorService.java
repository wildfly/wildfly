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

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;
import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;
import static org.jboss.as.connector.logging.ConnectorMessages.MESSAGES;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.resource.spi.TransactionSupport;

import org.jboss.as.connector.services.mdr.AS7MetadataRepository;
import org.jboss.as.connector.services.resourceadapters.deployment.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.subsystems.jca.JcaSubsystemConfiguration;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.common.v11.CommonConnDef;
import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.common.api.metadata.ra.ResourceAdapter1516;
import org.jboss.jca.common.api.metadata.ra.ra10.Connector10;
import org.jboss.jca.common.api.metadata.ra.ra10.ResourceAdapter10;
import org.jboss.jca.common.metadata.common.CommonPoolImpl;
import org.jboss.jca.common.metadata.common.CommonSecurityImpl;
import org.jboss.jca.common.metadata.common.CommonXaPoolImpl;
import org.jboss.jca.common.metadata.common.v11.CommonConnDefImpl;
import org.jboss.jca.common.metadata.ironjacamar.v11.IronJacamarImpl;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SubjectFactory;

public class DirectConnectionFactoryActivatorService implements Service<ContextNames.BindInfo> {
    public static final ServiceName SERVICE_NAME_BASE =
            ServiceName.JBOSS.append("connector").append("direct-connection-factory-activator");

    protected final InjectedValue<AS7MetadataRepository> mdr = new InjectedValue<AS7MetadataRepository>();


    private final String jndiName;
    private final String interfaceName;
    private final String resourceAdapter;
    private final String raId;

    private final int maxPoolSize;
    private final int minPoolSize;

    private final String[] properties;

    private final TransactionSupport.TransactionSupportLevel transactionSupport;

    private final Module module;

    private final ContextNames.BindInfo bindInfo;

    /**
     * create an instance *
     */
    public DirectConnectionFactoryActivatorService(String jndiName, String interfaceName, String resourceAdapter,
                                                   String raId, int maxPoolSize, int minPoolSize,
                                                   String[] properties, TransactionSupport.TransactionSupportLevel transactionSupport,
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
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("started DirectConnectionFactoryActivatorService %s", context.getController().getName());
        String cfInterface = null;


        try {

            Connector cmd = mdr.getValue().getResourceAdapter(raId);
            if (cmd.getVersion() == Connector.Version.V_10) {
                Connector10 c10 = (Connector10) cmd;
                ResourceAdapter10 ra10 = (ResourceAdapter10) c10.getResourceadapter();
                cfInterface = ra10.getConnectionFactoryInterface().getValue();
            } else {
                ResourceAdapter1516 ra1516 = (ResourceAdapter1516) cmd.getResourceadapter();
                if (ra1516.getOutboundResourceadapter() != null) {
                    for (org.jboss.jca.common.api.metadata.ra.ConnectionDefinition cd :
                            ra1516.getOutboundResourceadapter().getConnectionDefinitions()) {
                        if (cd.getConnectionFactoryInterface().getValue().equals(interfaceName))
                            cfInterface = cd.getConnectionFactoryInterface().getValue();
                    }
                }
            }

            if (cfInterface == null || !cfInterface.equals(interfaceName)) {
                throw MESSAGES.invalidConnectionFactory(cfInterface, resourceAdapter, jndiName);
            }

            Map<String, String> raConfigProperties = new HashMap<String, String>();
            Map<String, String> mcfConfigProperties = new HashMap<String, String>();
            String securitySetting = null;
            String securitySettingDomain = null;

            if (properties != null) {
                for (String prop : properties) {
                    if (prop.startsWith("ironjacamar.security")) {
                        securitySetting = prop.substring(prop.indexOf("=") + 1);
                    } else if (prop.startsWith("ironjacamar.security.domain")) {
                        securitySettingDomain = prop.substring(prop.indexOf("=") + 1);
                    } else {
                        String key = prop.substring(0, prop.indexOf("="));
                        String value = prop.substring(prop.indexOf("=") + 1);

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
            if (cmd.getVersion() == Connector.Version.V_10) {
                Connector10 c10 = (Connector10) cmd;
                ResourceAdapter10 ra10 = (ResourceAdapter10) c10.getResourceadapter();
                mcfClass = ra10.getManagedConnectionFactoryClass().getValue();
            } else {
                ResourceAdapter1516 ra1516 = (ResourceAdapter1516) cmd.getResourceadapter();
                if (ra1516.getOutboundResourceadapter() != null) {
                    for (org.jboss.jca.common.api.metadata.ra.ConnectionDefinition cd :
                            ra1516.getOutboundResourceadapter().getConnectionDefinitions()) {
                        if (cd.getConnectionFactoryInterface().getValue().equals(cfInterface))
                            mcfClass = cd.getManagedConnectionFactoryClass().getValue();
                    }
                }
            }

            CommonSecurity security = null;
            if (securitySetting != null) {
                if ("".equals(securitySetting)) {
                    security = new CommonSecurityImpl(null, null, false);
                } else if ("application".equals(securitySetting)) {
                    security = new CommonSecurityImpl(null, null, true);
                } else if ("domain".equals(securitySetting) && securitySettingDomain != null) {
                    security = new CommonSecurityImpl(securitySettingDomain, null, false);
                } else if ("domain-and-application".equals(securitySetting) && securitySettingDomain != null) {
                    security = new CommonSecurityImpl(null, securitySettingDomain, false);
                }
            }

            if (security == null) {
                SUBSYSTEM_RA_LOGGER.noSecurityDefined(jndiName);
            }


            CommonPool pool = null;
            Boolean isXA = Boolean.FALSE;
            if (transactionSupport == TransactionSupport.TransactionSupportLevel.XATransaction) {
                pool = new CommonXaPoolImpl(minPoolSize < 0 ? Defaults.MIN_POOL_SIZE : minPoolSize, maxPoolSize < 0 ? Defaults.MAX_POOL_SIZE : maxPoolSize,
                        Defaults.PREFILL, Defaults.USE_STRICT_MIN, Defaults.FLUSH_STRATEGY,
                        Defaults.IS_SAME_RM_OVERRIDE, Defaults.INTERLEAVING, Defaults.PAD_XID, Defaults.WRAP_XA_RESOURCE, Defaults.NO_TX_SEPARATE_POOL);
                isXA = Boolean.TRUE;
            } else {
                pool = new CommonPoolImpl(minPoolSize < 0 ? Defaults.MIN_POOL_SIZE : minPoolSize, maxPoolSize < 0 ? Defaults.MAX_POOL_SIZE : maxPoolSize,
                        Defaults.PREFILL, Defaults.USE_STRICT_MIN, Defaults.FLUSH_STRATEGY);
            }

            TransactionSupportEnum transactionSupportValue = TransactionSupportEnum.NoTransaction;
            if (transactionSupport == TransactionSupport.TransactionSupportLevel.XATransaction) {
                transactionSupportValue = TransactionSupportEnum.XATransaction;
            } else if (transactionSupport == TransactionSupport.TransactionSupportLevel.LocalTransaction) {
                transactionSupportValue = TransactionSupportEnum.LocalTransaction;
            }

            CommonConnDef cd = new CommonConnDefImpl(mcfConfigProperties, mcfClass, jndiName, poolName(cfInterface),
                    Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE,
                    pool, null, null, security, null, isXA);

            IronJacamar ijmd = new IronJacamarImpl(transactionSupportValue, raConfigProperties, null,
                    Collections.singletonList(cd), Collections.<String>emptyList(), null, null);

            String serviceName = jndiName;
            serviceName = serviceName.replace(':', '_');
            serviceName = serviceName.replace('/', '_');

            ResourceAdapterActivatorService activator = new ResourceAdapterActivatorService(cmd, ijmd, module.getClassLoader(), serviceName);
            activator.setCreateBinderService(false);
            activator.setBindInfo(bindInfo);
            ServiceTarget serviceTarget = context.getChildTarget();
            ServiceName activatorServiceName = ConnectorServices.RESOURCE_ADAPTER_ACTIVATOR_SERVICE.append(serviceName);
            ServiceBuilder connectionFactoryServiceBuilder = serviceTarget
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
                    .addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class,
                            activator.getSubjectFactoryInjector())
                    .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class,
                            activator.getCcmInjector())
                    .addDependency(NamingService.SERVICE_NAME)
                    .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class,
                            activator.getTxIntegrationInjector())
                    .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER)
                    .addDependency(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append("default"));


            connectionFactoryServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();


        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    public Injector<AS7MetadataRepository> getMdrInjector() {
        return mdr;
    }

    @Override
    public void stop(StopContext context) {
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
