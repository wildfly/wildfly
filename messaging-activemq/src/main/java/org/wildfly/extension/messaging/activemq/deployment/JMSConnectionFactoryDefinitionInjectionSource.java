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

package org.wildfly.extension.messaging.activemq.deployment;

import static org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONNECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONNECTORS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.DEFAULT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NO_TX;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SERVER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.XA_TX;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common.DISCOVERY_GROUP;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Pooled.ENLISTMENT_TRACE;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Pooled.MANAGED_CONNECTION_POOL;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Pooled.MAX_POOL_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Pooled.MIN_POOL_SIZE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.resource.spi.TransactionSupport;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.ra.ActiveMQRAConnectionFactoryImpl;

import org.jboss.as.connector.deployers.ra.ConnectionFactoryDefinitionInjectionSource;
import org.jboss.as.connector.services.resourceadapters.ConnectionFactoryReferenceFactoryService;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.ExternalBrokerConfigurationService;
import org.wildfly.extension.messaging.activemq.MessagingExtension;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.MessagingSubsystemRootResourceDefinition;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttribute;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes;
import org.wildfly.extension.messaging.activemq.jms.ExternalPooledConnectionFactoryService;
import org.wildfly.extension.messaging.activemq.jms.JMSServices;
import org.wildfly.extension.messaging.activemq.jms.PooledConnectionFactoryConfigProperties;
import org.wildfly.extension.messaging.activemq.jms.PooledConnectionFactoryConfigurationRuntimeHandler;
import org.wildfly.extension.messaging.activemq.jms.PooledConnectionFactoryDefinition;
import org.wildfly.extension.messaging.activemq.jms.PooledConnectionFactoryService;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 * @author Eduardo Martins
 */
public class JMSConnectionFactoryDefinitionInjectionSource extends ResourceDefinitionInjectionSource {

    /*
    String description() default "";
    String name();
    String interfaceName() default "javax.jms.ConnectionFactory";
    String className() default "";
    String resourceAdapter() default "";
    String user() default "";
    String password() default "";
    String clientId() default "";
    String[] properties() default {};
    boolean transactional() default true;
    int maxPoolSize() default -1;
    int minPoolSize() default -1;
    */

    // not used: ActiveMQ CF implements all JMS CF interfaces
    private String interfaceName;
    // not used
    private String className;
    private String resourceAdapter;
    private String user;
    private String password;
    private String clientId;
    private boolean transactional;
    private int maxPoolSize;
    private int minPoolSize;

    public JMSConnectionFactoryDefinitionInjectionSource(String jndiName) {
        super(jndiName);
    }

    void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    void setClassName(String className) {
        this.className = className;
    }

    void setResourceAdapter(String resourceAdapter) {
        this.resourceAdapter = resourceAdapter;
    }

    void setUser(String user) {
        this.user = user;
    }

    void setPassword(String password) {
        this.password = password;
    }

    void setClientId(String clientId) {
        this.clientId = clientId;
    }

    void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }

    void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    @Override
    public void getResourceValue(ResolutionContext context, ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(resourceAdapter == null || resourceAdapter.isEmpty()) {
            resourceAdapter = getDefaulResourceAdapter(deploymentUnit);
        }
        boolean external = targetsExternalPooledConnectionFactory(resourceAdapter, phaseContext.getServiceRegistry());
        if (external || targetsPooledConnectionFactory(getActiveMQServerName(properties), resourceAdapter, phaseContext.getServiceRegistry())) {
            try {
                startedPooledConnectionFactory(context, jndiName, serviceBuilder, phaseContext.getServiceTarget(), deploymentUnit, injector, external);
            } catch (OperationFailedException e) {
                throw new DeploymentUnitProcessingException(e);
            }
        } else {
            // delegate to the resource-adapter subsystem to create a generic Jakarta Connectors connection factory.
            ConnectionFactoryDefinitionInjectionSource cfdis = new ConnectionFactoryDefinitionInjectionSource(jndiName, interfaceName, resourceAdapter);
            cfdis.setMaxPoolSize(maxPoolSize);
            cfdis.setMinPoolSize(minPoolSize);
            cfdis.setTransactionSupportLevel(transactional ? TransactionSupport.TransactionSupportLevel.XATransaction : TransactionSupport.TransactionSupportLevel.NoTransaction);
            // transfer all the generic properties + the additional properties specific to the JMSConnectionFactoryDefinition
            for (Map.Entry<String, String> property : properties.entrySet()) {
                cfdis.addProperty(property.getKey(), property.getValue());
            }
            if (!user.isEmpty()) {
                cfdis.addProperty("user", user);
            }
            if (!password.isEmpty()) {
                cfdis.addProperty("password", password);
            }
            if (!clientId.isEmpty()) {
                cfdis.addProperty("clientId", clientId);
            }
            cfdis.getResourceValue(context, serviceBuilder, phaseContext, injector);
        }
    }

    private void startedPooledConnectionFactory(ResolutionContext context, String name, ServiceBuilder<?> serviceBuilder,
            ServiceTarget serviceTarget, DeploymentUnit deploymentUnit, Injector<ManagedReferenceFactory> injector,
            boolean external) throws DeploymentUnitProcessingException, OperationFailedException {
        Map<String, String> props = new HashMap<>(properties);
        List<String> connectors = getConnectors(props);
        clearUnknownProperties(properties);

        ModelNode model = new ModelNode();
        for (String connector : connectors) {
            model.get(CONNECTORS).add(connector);
        }
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            model.get(entry.getKey()).set(entry.getValue());
        }
        model.get(MIN_POOL_SIZE.getName()).set(minPoolSize);
        model.get(MAX_POOL_SIZE.getName()).set(maxPoolSize);
        if (user != null && !user.isEmpty()) {
            model.get(ConnectionFactoryAttributes.Pooled.USER.getName()).set(user);
        }
        if (password != null && !password.isEmpty()) {
            model.get(ConnectionFactoryAttributes.Pooled.PASSWORD.getName()).set(password);
        }

        if (clientId != null && !clientId.isEmpty()) {
            model.get(CommonAttributes.CLIENT_ID.getName()).set(clientId);
        }

        final String discoveryGroupName = properties.containsKey(DISCOVERY_GROUP.getName()) ? properties.get(DISCOVERY_GROUP.getName()) : null;
        if (discoveryGroupName != null) {
            model.get(DISCOVERY_GROUP.getName()).set(discoveryGroupName);
        }
        final String jgroupsChannelName = properties.containsKey(JGROUPS_CLUSTER.getName()) ? properties.get(JGROUPS_CLUSTER.getName()) : null;
        if (jgroupsChannelName != null) {
            model.get(JGROUPS_CLUSTER.getName()).set(jgroupsChannelName);
        }
        final String managedConnectionPoolClassName = properties.containsKey(MANAGED_CONNECTION_POOL.getName()) ? properties.get(MANAGED_CONNECTION_POOL.getName()) : null;
        if (managedConnectionPoolClassName != null) {
            model.get(MANAGED_CONNECTION_POOL.getName()).set(managedConnectionPoolClassName);
        }
        final Boolean enlistmentTrace = properties.containsKey(ENLISTMENT_TRACE.getName()) ? Boolean.valueOf(properties.get(ENLISTMENT_TRACE.getName())) : null;

        List<PooledConnectionFactoryConfigProperties> adapterParams = getAdapterParams(model);
        String txSupport = transactional ? XA_TX : NO_TX;

        final String serverName;
        final String pcfName = uniqueName(context, name);
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(context.getApplicationName(), context.getModuleName(), context.getComponentName(), !context.isCompUsesModule(), name);
        if(external) {
            serverName = null;
            Set<String> connectorsSocketBindings = new HashSet<>();
            ExternalBrokerConfigurationService configuration = (ExternalBrokerConfigurationService)deploymentUnit.getServiceRegistry().getRequiredService(MessagingSubsystemRootResourceDefinition.CONFIGURATION_CAPABILITY.getCapabilityServiceName()).getService().getValue();
            TransportConfiguration[] tcs = new TransportConfiguration[connectors.size()];
            for (int i = 0; i < tcs.length; i++) {
                tcs[i] = configuration.getConnectors().get(connectors.get(i));
                if (tcs[i].getParams().containsKey(ModelDescriptionConstants.SOCKET_BINDING)) {
                    connectorsSocketBindings.add(tcs[i].getParams().get(ModelDescriptionConstants.SOCKET_BINDING).toString());
                }
            }
            DiscoveryGroupConfiguration discoveryGroupConfiguration = null;
            if(discoveryGroupName != null) {
                discoveryGroupConfiguration = configuration.getDiscoveryGroupConfigurations().get(discoveryGroupName);
            }
            if (connectors.isEmpty() && discoveryGroupConfiguration == null) {
                tcs = getExternalPooledConnectionFactory(resourceAdapter, deploymentUnit.getServiceRegistry()).getConnectors();
                for(int i = 0 ; i < tcs.length; i++) {
                 if(tcs[i].getParams().containsKey(ModelDescriptionConstants.SOCKET_BINDING)) {
                    connectorsSocketBindings.add(tcs[i].getParams().get(ModelDescriptionConstants.SOCKET_BINDING).toString());
                 }
             }
            }
            ExternalPooledConnectionFactoryService.installService(serviceTarget, configuration, pcfName, tcs, discoveryGroupConfiguration,
                    connectorsSocketBindings, null, jgroupsChannelName, adapterParams, bindInfo, Collections.emptyList(),
                    txSupport, minPoolSize, maxPoolSize, managedConnectionPoolClassName, enlistmentTrace, deploymentUnit.getAttachment(CAPABILITY_SERVICE_SUPPORT));
        } else {
            serverName = getActiveMQServerName(properties);
            PooledConnectionFactoryService.installService(serviceTarget, pcfName, serverName, connectors,
                    discoveryGroupName, jgroupsChannelName, adapterParams, bindInfo, txSupport, minPoolSize,
                    maxPoolSize, managedConnectionPoolClassName, enlistmentTrace, true);
        }

        final ServiceName referenceFactoryServiceName = ConnectionFactoryReferenceFactoryService.SERVICE_NAME_BASE
                .append(bindInfo.getBinderServiceName());
        serviceBuilder.addDependency(referenceFactoryServiceName, ManagedReferenceFactory.class, injector);

        //create the management registration
        String managementName = managementName(context, name);
        final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
        final PathElement pcfPath = PathElement.pathElement(POOLED_CONNECTION_FACTORY, managementName);
        PathAddress registration;
        if (external) {
            deploymentResourceSupport.getDeploymentSubsystemModel(MessagingExtension.SUBSYSTEM_NAME);
            registration = PathAddress.pathAddress(pcfPath);
        } else {
            final PathElement serverElement = PathElement.pathElement(SERVER, serverName);
            deploymentResourceSupport.getDeploymentSubModel(MessagingExtension.SUBSYSTEM_NAME, serverElement);
            registration = PathAddress.pathAddress(serverElement, pcfPath);
        }
        MessagingXmlInstallDeploymentUnitProcessor.createDeploymentSubModel(registration, deploymentUnit);
        PooledConnectionFactoryConfigurationRuntimeHandler.INSTANCE.registerResource(serverName, managementName, model);
    }

    private List<String> getConnectors(Map<String, String> props) {
        List<String> connectors = new ArrayList<>();
        if (props.containsKey(CONNECTORS)) {
            String connectorsStr = properties.remove(CONNECTORS);
            for (String s : connectorsStr.split(",")) {
                String connector = s.trim();
                if (!connector.isEmpty()) {
                    connectors.add(connector);
                }
            }
        }
        if (props.containsKey(CONNECTOR)) {
            String connector = properties.remove(CONNECTOR).trim();
            if (!connector.isEmpty()) {
                connectors.add(connector);
            }
        }
        return connectors;
    }

    void clearUnknownProperties(final Map<String, String> props) {
        Set<String> attributeNames = PooledConnectionFactoryDefinition.getAttributesMap().keySet();

        final Iterator<Map.Entry<String, String>> it = props.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<String, String> entry = it.next();
            String value = entry.getKey();
            if (value == null || "".equals(value)) {
                it.remove();
            } else if (!attributeNames.contains(entry.getKey())) {
                MessagingLogger.ROOT_LOGGER.unknownPooledConnectionFactoryAttribute(entry.getKey());
                it.remove();
            }
        }
    }

    private static String uniqueName(InjectionSource.ResolutionContext context, final String jndiName) {
        StringBuilder uniqueName = new StringBuilder();
        return uniqueName.append(context.getApplicationName()).append("_")
                .append(managementName(context, jndiName))
                .toString();
    }

    private static String managementName(InjectionSource.ResolutionContext context, final String jndiName) {
        StringBuilder uniqueName = new StringBuilder();
        uniqueName.append(context.getModuleName()).append("_");
        if (context.getComponentName() != null) {
            uniqueName.append(context.getComponentName()).append("_");
        }
        return uniqueName
                .append(jndiName.replace(':', '_'))
                .toString();
    }

    private List<PooledConnectionFactoryConfigProperties> getAdapterParams(ModelNode model) {
        Map<String, ConnectionFactoryAttribute> attributes = PooledConnectionFactoryDefinition.getAttributesMap();
        List<PooledConnectionFactoryConfigProperties> props = new ArrayList<>();

        for (Property property : model.asPropertyList()) {
            ConnectionFactoryAttribute attribute = attributes.get(property.getName());

            if (attribute.getPropertyName() == null) {
                // not a RA property
                continue;
            }

            props.add(new PooledConnectionFactoryConfigProperties(attribute.getPropertyName(), property.getValue().asString(), attribute.getClassType(), attribute.getConfigType()));
        }
        return props;
    }


    /**
     * Return whether the definition targets an existing pooled connection factory or use a Jakarta Connectors-based ConnectionFactory.
     *
     * Checks the service registry for a PooledConnectionFactoryService with the ServiceName
     * created by the {@code server} property (or {@code "default") and the {@code resourceAdapter} property.
     */
    static boolean targetsPooledConnectionFactory(String server, String resourceAdapter, ServiceRegistry serviceRegistry) {
        // if the resourceAdapter is not defined, the default behaviour is to create a pooled-connection-factory.
        if (resourceAdapter == null || resourceAdapter.isEmpty()) {
            return true;
        }
        ServiceName activeMQServiceName = MessagingServices.getActiveMQServiceName(server);
        ServiceName pcfName = JMSServices.getPooledConnectionFactoryBaseServiceName(activeMQServiceName).append(resourceAdapter);
        return serviceRegistry.getServiceNames().contains(pcfName);
    }

    /**
     * Return whether the definition targets an existing external pooled connection factory.
     *
     * Checks the service registry for a PooledConnectionFactoryService with the ServiceName
     * created by the {@code server} property (or {@code "default") and the {@code resourceAdapter} property.
     */
    static boolean targetsExternalPooledConnectionFactory(String resourceAdapter, ServiceRegistry serviceRegistry) {
        // if the resourceAdapter is not defined, the default behaviour is to create a pooled-connection-factory.
        if (resourceAdapter == null || resourceAdapter.isEmpty()) {
            return false;
        }
        //let's look into the external-pooled-connection-factory
        ServiceName pcfName = JMSServices.getPooledConnectionFactoryBaseServiceName(MessagingServices.getActiveMQServiceName("")).append(resourceAdapter);
        return serviceRegistry.getServiceNames().contains(pcfName);
    }

    private static ExternalPooledConnectionFactoryService getExternalPooledConnectionFactory(String resourceAdapter, ServiceRegistry serviceRegistry) {
        //let's look into the external-pooled-connection-factory
        ServiceName pcfName = JMSServices.getPooledConnectionFactoryBaseServiceName(MessagingServices.getActiveMQServiceName("")).append(resourceAdapter);
        return (ExternalPooledConnectionFactoryService) serviceRegistry.getService(pcfName).getValue();
    }

    static String getDefaulResourceAdapter(DeploymentUnit deploymentUnit) {
        EEModuleDescription eeDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        if (eeDescription != null) {
            String defaultJndiName = eeDescription.getDefaultResourceJndiNames().getJmsConnectionFactory();
            ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(defaultJndiName);
            ServiceController binder = deploymentUnit.getServiceRegistry().getService(bindInfo.getBinderServiceName());
            if (binder != null) {
                Object pcf = binder.getService().getValue();
                //In case of multiple JNDI entries only the 1st is properly bound
                if (pcf != null && pcf instanceof ContextListAndJndiViewManagedReferenceFactory) {
                    ManagedReference ref = ((ContextListAndJndiViewManagedReferenceFactory) pcf).getReference();
                    Object ra = ref.getInstance();
                    if (ra instanceof ActiveMQRAConnectionFactoryImpl) {
                        bindInfo = ContextNames.bindInfoFor(((ActiveMQRAConnectionFactoryImpl) ra).getReference().getClassName());
                        binder = deploymentUnit.getServiceRegistry().getService(bindInfo.getBinderServiceName());
                        if (binder != null) {
                            pcf = binder.getService().getValue();
                        }
                    }
                }
                if (pcf != null && pcf instanceof ConnectionFactoryReferenceFactoryService) {
                    return ((ConnectionFactoryReferenceFactoryService) pcf).getName();
                }
            }
        }
        return null;
    }

    /**
     * The JMS connection factory can specify another server to deploy its destinations
     * by passing a property server=&lt;name of the server>. Otherwise, "default" is used by default.
     */
    static String getActiveMQServerName(Map<String, String> properties) {
        return properties.getOrDefault(SERVER, DEFAULT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        JMSConnectionFactoryDefinitionInjectionSource that = (JMSConnectionFactoryDefinitionInjectionSource) o;

        if (maxPoolSize != that.maxPoolSize) return false;
        if (minPoolSize != that.minPoolSize) return false;
        if (transactional != that.transactional) return false;
        if (className != null ? !className.equals(that.className) : that.className != null) return false;
        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) return false;
        if (interfaceName != null ? !interfaceName.equals(that.interfaceName) : that.interfaceName != null)
            return false;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;
        if (resourceAdapter != null ? !resourceAdapter.equals(that.resourceAdapter) : that.resourceAdapter != null)
            return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (interfaceName != null ? interfaceName.hashCode() : 0);
        result = 31 * result + (className != null ? className.hashCode() : 0);
        result = 31 * result + (resourceAdapter != null ? resourceAdapter.hashCode() : 0);
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
        result = 31 * result + (transactional ? 1 : 0);
        result = 31 * result + maxPoolSize;
        result = 31 * result + minPoolSize;
        return result;
    }
}
