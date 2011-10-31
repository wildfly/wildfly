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

import org.jboss.as.ejb3.inflow.EndpointDeployer;
import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.jca.common.api.metadata.ra.ResourceAdapter;
import org.jboss.jca.common.api.metadata.ra.ResourceAdapter1516;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
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

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import java.beans.IntrospectionException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.jboss.as.ejb3.EjbLogger.EJB3_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;
/**
 * The gas, water & energy for the EJB subsystem.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Jaikiran Pai
 */
public class EJBUtilities implements EndpointDeployer, Service<EJBUtilities> {


    private static final Map<String, String> knownRar = new HashMap<String, String>(1);

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "utilities");

    private final InjectedValue<MetadataRepository> mdrValue = new InjectedValue<MetadataRepository>();
    private final InjectedValue<ResourceAdapterRepository> resourceAdapterRepositoryValue = new InjectedValue<ResourceAdapterRepository>();
    private final InjectedValue<SimpleSecurityManager> securityManagerValue = new InjectedValue<SimpleSecurityManager>();
    private final InjectedValue<TransactionManager> transactionManagerValue = new InjectedValue<TransactionManager>();
    private final InjectedValue<TransactionSynchronizationRegistry> transactionSynchronizationRegistryValue = new InjectedValue<TransactionSynchronizationRegistry>();
    private final InjectedValue<UserTransaction> userTransactionValue = new InjectedValue<UserTransaction>();

    static {
        knownRar.put("hornetq-ra", "org.hornetq.ra");
        knownRar.put("hornetq-ra.rar", "org.hornetq.ra");
    }

    public ActivationSpec createActivationSpecs(final String resourceAdapterName, final Class<?> messageListenerInterface,
                                                final Properties activationConfigProperties, final ClassLoader classLoader) {
        try {
            ActivationSpec activationSpec = null;
            boolean raFound = false;
            String packageName = knownRar.get(resourceAdapterName);
            if (packageName != null) {
                raFound = true;
            }

            if (!raFound) {
                for (String id : getMdr().getResourceAdapters()) {
                    if (id.equals(resourceAdapterName)) {
                        if (!raFound) {
                            ResourceAdapter ra = getMdr().getResourceAdapter(id).getResourceadapter();
                            if (ra instanceof ResourceAdapter1516
                                    && ((ResourceAdapter1516) ra).getInboundResourceadapter() != null) {
                                String className = ((ResourceAdapter1516) ra).getResourceadapterClass();
                                if (className.lastIndexOf(".") != -1) {
                                    packageName = className.substring(0, className.lastIndexOf("."));
                                } else {
                                    packageName = "";
                                }
                                raFound = true;
                                EJB3_LOGGER.debug("Found resource adapter class: " + className + " for resource-adapter-name: " + resourceAdapterName);
                            }
                        } else {
                            throw MESSAGES.multipleResourceAdapterRegistered(resourceAdapterName);
                        }
                    }
                }
            }
            if (!raFound) {
                throw MESSAGES.failToRegisteredResourceAdapter(resourceAdapterName);
            }
            Set<String> ids = getResourceAdapterRepository().getResourceAdapters(messageListenerInterface);
            List<MessageListener> listeners = null;
            boolean found = false;
            for (String id : ids) {
                if (id.startsWith(packageName)) {
                    if (!found) {
                        listeners = getResourceAdapterRepository().getMessageListeners(id);
                        found = true;
                    } else {
                        throw MESSAGES.multipleResourceAdapterRegistered(resourceAdapterName);
                    }
                }
            }

            if (found) {

                final MessageListener messageListener = listeners.get(0);
                final Activation activation = messageListener.getActivation();
                // filter out the activation config properties, specified on the MDB, which aren't accepted by the resource
                // adaptor
                final Properties validActivationConfigProps = this.filterUnknownActivationConfigProperties(resourceAdapterName, activation, activationConfigProperties);
                // now set the activation config properties on the ActivationSpec
                activationSpec = activation.createInstance();
                PropertyEditors.mapJavaBeanProperties(activationSpec, validActivationConfigProps);

            }
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
        } catch (org.jboss.jca.core.spi.mdr.NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public MetadataRepository getMdr() {
        return mdrValue.getOptionalValue();
    }

    public Injector<MetadataRepository> getMdrInjector() {
        return mdrValue;
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
                EJB3_LOGGER.activationConfigPropertyIgnored(propName,resourceAdapterName);
            }
        }
        return validActivationConfigProps;
    }
}
