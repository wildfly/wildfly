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

package org.jboss.as.ejb3.component.messagedriven;

import java.beans.IntrospectionException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Properties;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponentCreateService;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.common.beans.property.BeanUtils;
import org.jboss.jca.core.spi.rar.Activation;
import org.jboss.jca.core.spi.rar.Endpoint;
import org.jboss.jca.core.spi.rar.MessageListener;
import org.jboss.jca.core.spi.rar.NotFoundException;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class MessageDrivenComponentCreateService extends EJBComponentCreateService {

    private final Class<?> messageListenerInterface;
    private final Properties activationProps;
    private final String resourceAdapterName;
    private final boolean deliveryActive;
    private final ServiceName deliveryControllerName;
    private final InjectedValue<ResourceAdapterRepository> resourceAdapterRepositoryInjectedValue = new InjectedValue<ResourceAdapterRepository>();
    private final InjectedValue<ResourceAdapter> resourceAdapterInjectedValue = new InjectedValue<ResourceAdapter>();
    private final InjectedValue<PoolConfig> poolConfig = new InjectedValue<PoolConfig>();
    private final InjectedValue<SuspendController> suspendControllerInjectedValue = new InjectedValue<>();
    private final ClassLoader moduleClassLoader;

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public MessageDrivenComponentCreateService(final ComponentConfiguration componentConfiguration, final ApplicationExceptions ejbJarConfiguration, final Class<?> messageListenerInterface) {
        super(componentConfiguration, ejbJarConfiguration);

        final MessageDrivenComponentDescription componentDescription = (MessageDrivenComponentDescription) componentConfiguration.getComponentDescription();
        this.resourceAdapterName = componentDescription.getResourceAdapterName();
        this.deliveryControllerName = componentDescription.isDeliveryControlled()? componentDescription.getDeliveryControllerName(): null;
        this.deliveryActive = !componentDescription.isDeliveryControlled() && componentDescription.isDeliveryActive();
        // see MessageDrivenComponentDescription.<init>
        this.messageListenerInterface = messageListenerInterface;

        this.activationProps = componentDescription.getActivationProps();
        this.moduleClassLoader = componentConfiguration.getModuleClassLoader();
    }

    @Override
    public void start(StartContext context) throws StartException {
        super.start(context);
        // AS7-3073 just log a message about the MDB being started
        EjbLogger.ROOT_LOGGER.logMDBStart(this.getComponentName(), this.resourceAdapterName);
    }

    @Override
    protected BasicComponent createComponent() {
        String configuredResourceAdapterName = resourceAdapterName;

        // Match configured value to the actual RA names
        final String activeResourceAdapterName = searchActiveResourceAdapterName(configuredResourceAdapterName);

        final ActivationSpec activationSpec = createActivationSpecs(activeResourceAdapterName, messageListenerInterface, activationProps, getDeploymentClassLoader());
        final MessageDrivenComponent component = new MessageDrivenComponent(this, messageListenerInterface, activationSpec, deliveryActive, deliveryControllerName, activeResourceAdapterName);
        // set the endpoint
        final Endpoint endpoint = getEndpoint(activeResourceAdapterName);

        component.setEndpoint(endpoint);

        return component;
    }

    private ActivationSpec createActivationSpecs(final String resourceAdapterName, final Class<?> messageListenerInterface,
                                                final Properties activationConfigProperties, final ClassLoader classLoader) {
        try {
            // first get the ra "identifier" (with which it is registered in the resource adapter repository) for the
            // ra name
            final String raIdentifier = ConnectorServices.getRegisteredResourceAdapterIdentifier(resourceAdapterName);
            if (raIdentifier == null) {
                throw EjbLogger.ROOT_LOGGER.unknownResourceAdapter(resourceAdapterName);
            }
            final ResourceAdapterRepository resourceAdapterRepository = resourceAdapterRepositoryInjectedValue.getValue();
            if (resourceAdapterRepository == null) {
                throw EjbLogger.ROOT_LOGGER.resourceAdapterRepositoryUnAvailable();
            }
            // now get the message listeners for this specific ra identifier
            final List<MessageListener> messageListeners = resourceAdapterRepository.getMessageListeners(raIdentifier);
            if (messageListeners == null || messageListeners.isEmpty()) {
                throw EjbLogger.ROOT_LOGGER.unknownMessageListenerType(messageListenerInterface.getName(), resourceAdapterName);
            }
            MessageListener requiredMessageListener = null;
            // now find the expected message listener from the list of message listeners for this resource adapter
            for (final MessageListener messageListener : messageListeners) {
                if (messageListenerInterface.equals(messageListener.getType())) {
                    requiredMessageListener = messageListener;
                    break;
                }
            }
            if (requiredMessageListener == null) {
                throw EjbLogger.ROOT_LOGGER.unknownMessageListenerType(messageListenerInterface.getName(), resourceAdapterName);
            }
            // found the message listener, now finally create the activation spec
            final Activation activation = requiredMessageListener.getActivation();
            // filter out the activation config properties, specified on the MDB, which aren't accepted by the resource
            // adaptor
            final Properties validActivationConfigProps = this.filterUnknownActivationConfigProperties(resourceAdapterName, activation, activationConfigProperties);
            // now set the activation config properties on the ActivationSpec
            final ActivationSpec activationSpec = activation.createInstance();
            BeanUtils.mapJavaBeanProperties(activationSpec, validActivationConfigProps);

            return activationSpec;

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes activation config properties which aren't recognized by the resource adapter <code>activation</code>, from the
     * passed <code>activationConfigProps</code> and returns only those Properties which are valid.
     *
     * @param resourceAdapterName   The resource adapter name
     * @param activation            {@link Activation}
     * @param activationConfigProps Activation config properties which will be checked for validity
     * @return
     */
    private Properties filterUnknownActivationConfigProperties(final String resourceAdapterName, final Activation activation, final Properties activationConfigProps) {
        if (activationConfigProps == null) {
            return null;
        }
        final Map<String, Class<?>> raActivationConfigProps = activation.getConfigProperties();
        final Set<String> raRequiredConfigProps = activation.getRequiredConfigProperties();
        final Enumeration<?> propNames = activationConfigProps.propertyNames();
        final Properties validActivationConfigProps = new Properties();
        // initialize to all the activation config properties that have been set on the MDB
        validActivationConfigProps.putAll(activationConfigProps);
        while (propNames.hasMoreElements()) {
            final Object propName = propNames.nextElement();
            if (!raActivationConfigProps.containsKey(propName) && !raRequiredConfigProps.contains(propName)) {
                // not a valid activation config property, so log a WARN and filter it out from the valid activation config properties
                validActivationConfigProps.remove(propName);
                EjbLogger.ROOT_LOGGER.activationConfigPropertyIgnored(propName, resourceAdapterName);
            }
        }
        return validActivationConfigProps;
    }


    /**
     * Returns the {@link org.jboss.jca.core.spi.rar.Endpoint} corresponding to the passed <code>resourceAdapterName</code>
     *
     * @param resourceAdapterName The resource adapter name
     * @return
     */
    private Endpoint getEndpoint(final String resourceAdapterName) {
        // first get the ra "identifier" (with which it is registered in the resource adapter repository) for the
        // ra name
        final String raIdentifier = ConnectorServices.getRegisteredResourceAdapterIdentifier(resourceAdapterName);
        if (raIdentifier == null) {
            throw EjbLogger.ROOT_LOGGER.unknownResourceAdapter(resourceAdapterName);
        }
        final ResourceAdapterRepository resourceAdapterRepository = resourceAdapterRepositoryInjectedValue.getValue();
        if (resourceAdapterRepository == null) {
            throw EjbLogger.ROOT_LOGGER.resourceAdapterRepositoryUnAvailable();
        }
        try {
            return resourceAdapterRepository.getEndpoint(raIdentifier);
        } catch (NotFoundException nfe) {
            throw EjbLogger.ROOT_LOGGER.noSuchEndpointException(resourceAdapterName, nfe);
        }
    }


    private String searchActiveResourceAdapterName(String configuredResourceAdapterName) {
        // Use the configured value unless it doesn't match and some variant of it does
        String result = configuredResourceAdapterName;
        if (ConnectorServices.getRegisteredResourceAdapterIdentifier(configuredResourceAdapterName) == null) {
            // No direct match. See if we have a match with .rar removed or appended
            String amended = stripDotRarSuffix(configuredResourceAdapterName);
            if (configuredResourceAdapterName.equals(amended)) {
                // There was no .rar to strip; try appending
                amended = configuredResourceAdapterName + ".rar";
            }
            if (ConnectorServices.getRegisteredResourceAdapterIdentifier(amended) != null) {
                result = amended;
            }
        }

        return result;
    }

    PoolConfig getPoolConfig() {
        return this.poolConfig.getOptionalValue();
    }

    public InjectedValue<PoolConfig> getPoolConfigInjector() {
        return this.poolConfig;
    }

    private ClassLoader getDeploymentClassLoader() {
        return getComponentClass().getClassLoader();
    }

    public InjectedValue<ResourceAdapterRepository> getResourceAdapterRepositoryInjector() {
        return this.resourceAdapterRepositoryInjectedValue;
    }

    public InjectedValue<ResourceAdapter> getResourceAdapterInjector() {
        return this.resourceAdapterInjectedValue;
    }

    public ClassLoader getModuleClassLoader() {
        return moduleClassLoader;
    }

    public InjectedValue<SuspendController> getSuspendControllerInjectedValue() {
        return suspendControllerInjectedValue;
    }

    private String stripDotRarSuffix(final String raName) {
        if (raName == null) {
            return null;
        }
        // See RaDeploymentParsingProcessor
        if (raName.endsWith(".rar")) {
            return raName.substring(0, raName.indexOf(".rar"));
        }
        return raName;
    }
}
