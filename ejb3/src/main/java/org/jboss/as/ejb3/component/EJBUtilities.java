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

import static org.jboss.as.ejb3.EjbLogger.EJB3_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import java.beans.IntrospectionException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.ejb3.inflow.EndpointDeployer;
import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.jca.core.spi.rar.Activation;
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
import org.jboss.util.propertyeditor.PropertyEditors;

/**
 * The gas, water & energy for the EJB subsystem.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Jaikiran Pai
 */
public class EJBUtilities implements EndpointDeployer, Service<EJBUtilities> {


    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "utilities");

    private final InjectedValue<ResourceAdapterRepository> resourceAdapterRepositoryValue = new InjectedValue<ResourceAdapterRepository>();
    private final InjectedValue<SimpleSecurityManager> securityManagerValue = new InjectedValue<SimpleSecurityManager>();
    private final InjectedValue<TransactionManager> transactionManagerValue = new InjectedValue<TransactionManager>();
    private final InjectedValue<TransactionSynchronizationRegistry> transactionSynchronizationRegistryValue = new InjectedValue<TransactionSynchronizationRegistry>();
    private final InjectedValue<UserTransaction> userTransactionValue = new InjectedValue<UserTransaction>();

    public ActivationSpec createActivationSpecs(final String resourceAdapterName, final Class<?> messageListenerInterface,
                                                final Properties activationConfigProperties, final ClassLoader classLoader) {
        try {
            // first get the ra "identifier" (with which it is registered in the resource adapter repository) for the
            // ra name
            final String raIdentifier = ConnectorServices.getRegisteredResourceAdapterIdentifier(resourceAdapterName);
            if (raIdentifier == null) {
                throw MESSAGES.unknownResourceAdapter(resourceAdapterName);
            }
            final ResourceAdapterRepository resourceAdapterRepository = getResourceAdapterRepository();
            // now get the message listeners for this specific ra identifier
            final List<MessageListener> messageListeners = resourceAdapterRepository.getMessageListeners(raIdentifier);
            if (messageListeners == null || messageListeners.isEmpty()) {
                throw MESSAGES.unknownMessageListenerType(messageListenerInterface.getName(), resourceAdapterName);
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
                throw MESSAGES.unknownMessageListenerType(messageListenerInterface.getName(), resourceAdapterName);
            }
            // found the message listener, now finally create the activation spec
            final Activation activation = requiredMessageListener.getActivation();
            // filter out the activation config properties, specified on the MDB, which aren't accepted by the resource
            // adaptor
            final Properties validActivationConfigProps = this.filterUnknownActivationConfigProperties(resourceAdapterName, activation, activationConfigProperties);
            // now set the activation config properties on the ActivationSpec
            final ActivationSpec activationSpec = activation.createInstance();
            PropertyEditors.mapJavaBeanProperties(activationSpec, validActivationConfigProps);

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

    public ResourceAdapterRepository getResourceAdapterRepository() {
        return resourceAdapterRepositoryValue.getOptionalValue();
    }

    public Injector<ResourceAdapterRepository> getResourceAdapterRepositoryInjector() {
        return resourceAdapterRepositoryValue;
    }

    public SimpleSecurityManager getSecurityManager() {
        final SimpleSecurityManager securityManager = securityManagerValue.getOptionalValue();
        if (securityManager == null)
            throw MESSAGES.securityNotEnabled();
        return securityManager;
    }

    public Injector<SimpleSecurityManager> getSecurityManagerInjector() {
        return securityManagerValue;
    }

    public TransactionManager getTransactionManager() {
        return transactionManagerValue.getOptionalValue();
    }

    public Injector<TransactionManager> getTransactionManagerInjector() {
        return transactionManagerValue;
    }

    public InjectedValue<TransactionSynchronizationRegistry> getTransactionSynchronizationRegistryInjector() {
        return transactionSynchronizationRegistryValue;
    }

    public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return transactionSynchronizationRegistryValue.getOptionalValue();
    }

    public UserTransaction getUserTransaction() {
        return userTransactionValue.getOptionalValue();
    }

    public Injector<UserTransaction> getUserTransactionInjector() {
        return userTransactionValue;
    }

    @Override
    public EJBUtilities getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public boolean hasSecurityManager() {
        return securityManagerValue.getOptionalValue() != null;
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
                EJB3_LOGGER.activationConfigPropertyIgnored(propName, resourceAdapterName);
            }
        }
        return validActivationConfigProps;
    }
}
