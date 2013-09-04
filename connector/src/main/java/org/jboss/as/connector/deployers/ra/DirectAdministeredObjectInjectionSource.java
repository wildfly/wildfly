/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.deployers.ra;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYMENT_CONNECTOR_LOGGER;
import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;
import static org.jboss.as.connector.logging.ConnectorMessages.MESSAGES;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.connector.services.mdr.AS7MetadataRepository;
import org.jboss.as.connector.services.resourceadapters.AdminObjectReferenceFactoryService;
import org.jboss.as.connector.services.resourceadapters.ResourceAdapterActivatorService;
import org.jboss.as.connector.services.resourceadapters.deployment.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.subsystems.jca.JcaSubsystemConfiguration;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.jca.common.api.metadata.common.CommonAdminObject;
import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.common.api.metadata.ra.ResourceAdapter1516;
import org.jboss.jca.common.metadata.common.CommonAdminObjectImpl;
import org.jboss.jca.common.metadata.ironjacamar.v10.IronJacamarImpl;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.SubjectFactory;

/**
 * A binding description for AdministeredObjectDefinition annotations.
 * <p/>
 * The referenced admin object must be directly visible to the
 * component declaring the annotation.
 *
 * @author Jesper Pedersen
 */
public class DirectAdministeredObjectInjectionSource extends InjectionSource {

    public static final String DESCRIPTION = "description";
    public static final String INTERFACE = "interfaceName";
    public static final String PROPERTIES = "properties";

    private final String jndiName;
    private final String className;
    private final String resourceAdapter;
    private final String raId;
    private final MetadataRepository mdr;

    private String description;
    private String interfaceName;
    private String[] properties;

    public DirectAdministeredObjectInjectionSource(final String jndiName, final String className, final String resourceAdapter, final MetadataRepository mdr) {
        this.jndiName = jndiName;
        this.className = className;
        this.resourceAdapter = resourceAdapter;

        String s = resourceAdapter;
        if (s.indexOf("#") != -1)
            s = s.substring(s.indexOf("#") + 1);

        if (s.endsWith(".rar"))
            s = s.substring(0, s.indexOf(".rar"));

        this.raId = s;

        this.mdr = mdr;
    }

    public void getResourceValue(final ResolutionContext context, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);

        String realId = null;
        String aoClass = null;

        SUBSYSTEM_RA_LOGGER.debugf("@AdministeredObjectDefinition: %s for %s binding to %s ", className, resourceAdapter, jndiName);

        try {
            Set<String> rars = mdr.getResourceAdapters();

            if (rars == null || rars.isEmpty()) {
                throw MESSAGES.emptyMdr(jndiName);
            }

            for (String rar : rars) {
                if (rar.indexOf(raId) != -1) {
                    realId = rar;
                }
            }

            if (realId == null) {
                throw MESSAGES.raNotFound(resourceAdapter, jndiName);
            }

            Connector cmd = mdr.getResourceAdapter(realId);
            if (cmd.getVersion() == Connector.Version.V_10) {
                throw MESSAGES.adminObjectForJCA10(resourceAdapter, jndiName);
            } else {
                ResourceAdapter1516 ra1516 = (ResourceAdapter1516)cmd.getResourceadapter();
                if (ra1516.getAdminObjects() != null) {
                    for (org.jboss.jca.common.api.metadata.ra.AdminObject ao : ra1516.getAdminObjects()) {
                        if (ao.getAdminobjectClass().getValue().equals(className))
                            aoClass = ao.getAdminobjectClass().getValue();
                    }
                }
            }

            if (aoClass == null || !aoClass.equals(className)) {
                throw MESSAGES.invalidAdminObject(aoClass, resourceAdapter, jndiName);
            }

            Map<String, String> raConfigProperties = new HashMap<String, String>();
            Map<String, String> aoConfigProperties = new HashMap<String, String>();

            if (properties != null) {
                for (String prop : properties) {
                    String key = prop.substring(0, prop.indexOf("="));
                    String value = prop.substring(prop.indexOf("=") + 1);

                    if (key.startsWith("ra.")) {
                        raConfigProperties.put(key.substring(3), value);
                    } else if (key.startsWith("ao.")) {
                        aoConfigProperties.put(key.substring(3), value);
                    } else {
                        aoConfigProperties.put(key, value);
                    }
                }
            }

            CommonAdminObject ao = new CommonAdminObjectImpl(aoConfigProperties, aoClass, jndiName, poolName(aoClass, interfaceName), Boolean.TRUE, Boolean.TRUE);

            IronJacamar ijmd = new IronJacamarImpl(null, raConfigProperties, Collections.singletonList(ao),
                                                   null, Collections.<String>emptyList(), null);

            String serviceName = jndiName;
            serviceName = serviceName.replace(':', '_');
            serviceName = serviceName.replace('/', '_');

            ResourceAdapterActivatorService activator = new ResourceAdapterActivatorService(cmd, ijmd, module.getClassLoader(), serviceName);
            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(context.getApplicationName(), context.getModuleName(), context.getComponentName(), !context.isCompUsesModule(), jndiName);
            activator.setBindInfo(bindInfo);
            activator.setCreateBinderService(false);
            ServiceTarget serviceTarget = phaseContext.getServiceTarget();

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
                    .addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class,
                            activator.getSubjectFactoryInjector())
                    .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class,
                            activator.getCcmInjector()).addDependency(NamingService.SERVICE_NAME)
                    .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER)
                    .addDependency(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append("default"));


            adminObjectServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();

            serviceBuilder.addDependency(AdminObjectReferenceFactoryService.SERVICE_NAME_BASE.append(bindInfo.getBinderServiceName()), ManagedReferenceFactory.class, injector);
            serviceBuilder.addListener(new AbstractServiceListener<Object>() {
                public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                    switch (transition) {
                        case STARTING_to_UP: {
                            DEPLOYMENT_CONNECTOR_LOGGER.adminObjectAnnotation(jndiName);
                            break;
                        }
                        case STOPPING_to_DOWN: {
                            DEPLOYMENT_CONNECTOR_LOGGER.unboundJca("AdminObject", jndiName);
                            break;
                        }
                        case REMOVING_to_REMOVED: {
                            DEPLOYMENT_CONNECTOR_LOGGER.debugf("Removed JCA AdminObject [%s]", jndiName);
                        }
                    }
                }
            });
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e);
        }
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInterface() {
        return interfaceName;
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String[] getProperties() {
        return properties;
    }

    public void setProperties(String[] properties) {
        this.properties = properties;
    }
}
