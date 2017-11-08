/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component;

import java.beans.IntrospectionException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.ejb3.inflow.EndpointDeployer;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.common.beans.property.BeanUtils;
import org.jboss.jca.core.spi.rar.Activation;
import org.jboss.jca.core.spi.rar.Endpoint;
import org.jboss.jca.core.spi.rar.MessageListener;
import org.jboss.jca.core.spi.rar.NotFoundException;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * The gas, water & energy for the EJB subsystem.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Jaikiran Pai
 * @deprecated Deprecated post 7.2.x version. Services/Components which depended on this service for getting access to various other services are expected to directly depend on the relevant services themselves.
 */
@Deprecated
public class EJBUtilities implements EndpointDeployer, Service<EJBUtilities> {


    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "utilities");

    private final InjectedValue<ResourceAdapterRepository> resourceAdapterRepositoryValue = new InjectedValue<ResourceAdapterRepository>();

    private volatile boolean statisticsEnabled = false;

    public ActivationSpec createActivationSpecs(final String resourceAdapterName, final Class<?> messageListenerInterface,
                                                final Properties activationConfigProperties, final ClassLoader classLoader) {
        try {
            // first get the ra "identifier" (with which it is registered in the resource adapter repository) for the
            // ra name
            final String raIdentifier = ConnectorServices.getRegisteredResourceAdapterIdentifier(resourceAdapterName);
            if (raIdentifier == null) {
                throw EjbLogger.ROOT_LOGGER.unknownResourceAdapter(resourceAdapterName);
            }
            final ResourceAdapterRepository resourceAdapterRepository = getResourceAdapterRepository();
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
     * Returns the {@link org.jboss.jca.core.spi.rar.Endpoint} corresponding to the passed <code>resourceAdapterName</code>
     *
     * @param resourceAdapterName The resource adapter name
     * @return
     */
    public Endpoint getEndpoint(final String resourceAdapterName) {
        // first get the ra "identifier" (with which it is registered in the resource adapter repository) for the
        // ra name
        final String raIdentifier = ConnectorServices.getRegisteredResourceAdapterIdentifier(resourceAdapterName);
        if (raIdentifier == null) {
            throw EjbLogger.ROOT_LOGGER.unknownResourceAdapter(resourceAdapterName);
        }
        final ResourceAdapterRepository resourceAdapterRepository = getResourceAdapterRepository();
        if (resourceAdapterRepository == null) {
            throw EjbLogger.ROOT_LOGGER.resourceAdapterRepositoryUnAvailable();
        }
        try {
            return resourceAdapterRepository.getEndpoint(raIdentifier);
        } catch (NotFoundException nfe) {
            throw EjbLogger.ROOT_LOGGER.noSuchEndpointException(resourceAdapterName, nfe);
        }
    }

    public ResourceAdapterRepository getResourceAdapterRepository() {
        return resourceAdapterRepositoryValue.getOptionalValue();
    }

    public Injector<ResourceAdapterRepository> getResourceAdapterRepositoryInjector() {
        return resourceAdapterRepositoryValue;
    }

    @Override
    public EJBUtilities getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }

    public void setStatisticsEnabled(final boolean b) {
        this.statisticsEnabled = b;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
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
            if (raActivationConfigProps.containsKey(propName) == false && raRequiredConfigProps.contains(propName) == false) {
                // not a valid activation config property, so log a WARN and filter it out from the valid activation config properties
                validActivationConfigProps.remove(propName);
                EjbLogger.ROOT_LOGGER.activationConfigPropertyIgnored(propName, resourceAdapterName);
            }
        }
        return validActivationConfigProps;
    }
}
