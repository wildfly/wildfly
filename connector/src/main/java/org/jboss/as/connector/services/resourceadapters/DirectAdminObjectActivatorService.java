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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.services.mdr.AS7MetadataRepository;
import org.jboss.as.connector.services.resourceadapters.deployment.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.subsystems.jca.JcaSubsystemConfiguration;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.resourceadapter.ConnectionDefinition;
import org.jboss.jca.common.api.metadata.spec.AdminObject;
import org.jboss.jca.common.api.metadata.spec.Connector;
import org.jboss.jca.common.api.metadata.spec.ResourceAdapter;
import org.jboss.jca.common.metadata.resourceadapter.ActivationImpl;
import org.jboss.jca.common.metadata.resourceadapter.AdminObjectImpl;
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

public class DirectAdminObjectActivatorService implements Service<ContextNames.BindInfo> {
    public static final ServiceName SERVICE_NAME_BASE =
            ServiceName.JBOSS.append("connector").append("direct-connection-factory-activator");

    protected final InjectedValue<AS7MetadataRepository> mdr = new InjectedValue<AS7MetadataRepository>();


    private final String jndiName;
    private final String className;
    private final String resourceAdapter;
    private final String raId;

    private final Map<String, String> properties;


    private final Module module;

    private final ContextNames.BindInfo bindInfo;

    /**
     * create an instance *
     */
    public DirectAdminObjectActivatorService(String jndiName, String className, String resourceAdapter,
                                             String raId, Map<String, String> properties, Module module, ContextNames.BindInfo bindInfo) {
        this.jndiName = jndiName;
        this.className = className;
        this.resourceAdapter = resourceAdapter;
        this.raId = raId;
        this.properties = properties;
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
        String aoClass = null;


        try {

            Connector cmd = mdr.getValue().getResourceAdapter(raId);
            if (cmd.getVersion() == Connector.Version.V_10) {
                throw ConnectorLogger.ROOT_LOGGER.adminObjectForJCA10(resourceAdapter, jndiName);
            } else {
                ResourceAdapter ra1516 = (ResourceAdapter) cmd.getResourceadapter();
                if (ra1516.getAdminObjects() != null) {
                    for (AdminObject ao : ra1516.getAdminObjects()) {
                        if (ao.getAdminobjectClass().getValue().equals(className))
                            aoClass = ao.getAdminobjectClass().getValue();
                    }
                }
            }

            if (aoClass == null || !aoClass.equals(className)) {
                throw ConnectorLogger.ROOT_LOGGER.invalidAdminObject(aoClass, resourceAdapter, jndiName);
            }

            Map<String, String> raConfigProperties = new HashMap<String, String>();
            Map<String, String> aoConfigProperties = new HashMap<String, String>();

            if (properties != null) {
                for (Map.Entry<String,String> prop : properties.entrySet()) {
                    String key = prop.getKey();
                    String value = prop.getValue();
                    if (key.startsWith("ra.")) {
                        raConfigProperties.put(key.substring(3), value);
                    } else if (key.startsWith("ao.")) {
                        aoConfigProperties.put(key.substring(3), value);
                    } else {
                        aoConfigProperties.put(key, value);
                    }
                }
            }

            org.jboss.jca.common.api.metadata.resourceadapter.AdminObject ao = new AdminObjectImpl(aoConfigProperties, aoClass, jndiName, poolName(aoClass, className), Boolean.TRUE, Boolean.TRUE);

            Activation activation = new ActivationImpl(null, null, TransactionSupportEnum.LocalTransaction, Collections.<ConnectionDefinition>emptyList(), Collections.singletonList(ao),
                    null, Collections.<String>emptyList(), null, null);

            String serviceName = jndiName;
            serviceName = serviceName.replace(':', '_');
            serviceName = serviceName.replace('/', '_');

            ResourceAdapterActivatorService activator = new ResourceAdapterActivatorService(cmd, activation, module.getClassLoader(), serviceName);
            activator.setCreateBinderService(false);
            activator.setBindInfo(bindInfo);
            ServiceTarget serviceTarget = context.getChildTarget();

            ServiceBuilder adminObjectServiceBuilder = serviceTarget
                    .addService(ConnectorServices.RESOURCE_ADAPTER_ACTIVATOR_SERVICE.append(serviceName), activator)
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
                    // No legacy security services needed as this activation has no connection definitions
                    // or work manager, so nothing configures a legacy security domain
                    /*
                    .addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class,
                            activator.getSubjectFactoryInjector())
                    .addDependency(SimpleSecurityManagerService.SERVICE_NAME,
                            ServerSecurityManager.class, activator.getServerSecurityManager())
                    */
                    .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class,
                            activator.getCcmInjector()).addDependency(NamingService.SERVICE_NAME)
                    .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER)
                    .addDependency(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append("default"));


            adminObjectServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();


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

    private String poolName(final String aoClass, final String aoInterface) {
        if (aoInterface != null) {
            if (aoInterface.indexOf(".") != -1) {
                return aoInterface.substring(aoInterface.lastIndexOf(".") + 1);
            } else {
                return aoInterface;
            }
        }

        if (aoClass.indexOf(".") != -1) {
            return aoClass.substring(aoClass.lastIndexOf(".") + 1);
        } else {
            return aoClass;
        }
    }
}
