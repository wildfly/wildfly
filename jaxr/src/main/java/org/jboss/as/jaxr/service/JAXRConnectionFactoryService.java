/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.service;

import java.util.Enumeration;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.xml.registry.ConnectionFactory;
import javax.xml.registry.JAXRException;

import org.jboss.as.jaxr.JAXRConfiguration;
import org.jboss.as.jaxr.JAXRConstants;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;

/**
 * Binds the JAXR ConnectionFactory to JNDI
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kurt Stam
 * @since 17-Nov-2011
 */
public final class JAXRConnectionFactoryService extends AbstractService<Void> {

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jaxr", "connectionfactory");

    // [TODO] AS7-2277 JAXR subsystem i18n
    private final Logger log = Logger.getLogger("org.jboss.as.jaxr");

    private final InjectedValue<NamingStore> injectedJavaContext = new InjectedValue<NamingStore>();
    private final InjectedValue<JAXRConfiguration> injectedConfig = new InjectedValue<JAXRConfiguration>();

    public static ServiceController<?> addService(final ServiceTarget target, final ServiceListener<Object>... listeners) {
        JAXRConnectionFactoryService service = new JAXRConnectionFactoryService();
        ServiceBuilder<?> builder = target.addService(SERVICE_NAME, service);
        builder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, NamingStore.class, service.injectedJavaContext);
        builder.addDependency(JAXRConfiguration.SERVICE_NAME, JAXRConfiguration.class, service.injectedConfig);
        builder.addListener(listeners);
        return builder.install();
    }

    // Hide ctor
    private JAXRConnectionFactoryService() {
    }

    @Override
    public void start(final StartContext context) throws StartException {
        JAXRConfiguration config = injectedConfig.getValue();
        if (config.getConnectionFactoryBinding() != null) {
            log.infof("Binding JAXR ConnectionFactory: %s", config.getConnectionFactoryBinding());
            try {
                String jndiName = config.getConnectionFactoryBinding();
                ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
                BinderService binderService = new BinderService(bindInfo.getBindName());
                ConnectionFactory jaxrFactory = loadConnectionFactoryImplementation(config);
                //set jaxr properties from the config if there are any
                setJAXRFactoryProperies(jaxrFactory, config.getProperties());
                ImmediateValue<ConnectionFactory> value = new ImmediateValue<ConnectionFactory>(jaxrFactory);
                binderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(value));
                binderService.getNamingStoreInjector().inject((ServiceBasedNamingStore) injectedJavaContext.getValue());
                ServiceBuilder<?> builder = context.getChildTarget().addService(bindInfo.getBinderServiceName(), binderService);
                builder.install();
            } catch (Exception ex) {
                log.errorf(ex, "Cannot bind JAXR ConnectionFactory");
            }
        }
    }

    @Override
    public void stop(final StopContext context) {
        JAXRConfiguration config = injectedConfig.getValue();
        if (config.getConnectionFactoryBinding() != null) {
            log.debugf("Unbind JAXR ConnectionFactory");
            try {
                String jndiName = config.getConnectionFactoryBinding();
                ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
                ServiceContainer serviceContainer = context.getController().getServiceContainer();
                ServiceController<?> service = serviceContainer.getService(bindInfo.getBinderServiceName());
                if (service != null) {
                    service.setMode(ServiceController.Mode.REMOVE);
                }
            } catch (Exception ex) {
                log.errorf(ex, "Cannot unbind JAXR ConnectionFactory");
            }
        }
    }
    /**
     * Loads the JAXR service provider, by going down the following list:
     * <ol>
     * <li>Look for System property {@link JAXRConstants#JAXR_FACTORY_IMPLEMENTATION},
     * and if it contains a value, instantiate the class.</li>
     * <li>Obtain from the JBoss configuration, and if it contains a value, instantiate the class.</li>
     * <li>Use the {@link ServiceLoader} which check the value in the file META-INF/services/javax.xml.registry.ConnectionFactory.</li>
     * <li>Finally when still no service provider has been found, return the default implementation, which
     * is <a href="http://juddi.apache.org/scout">Apache Scout</a></li>.
     * </ol>
     * @param config
     * @return
     * @throws JAXRException
     */
    protected ConnectionFactory loadConnectionFactoryImplementation(final JAXRConfiguration config) throws JAXRException {

        ConnectionFactory jaxrFactory = null;
            //1. try to read from a system property
        String factoryName = SecurityActions.getSystemProperty(JAXRConstants.JAXR_FACTORY_IMPLEMENTATION, null);
        if (factoryName!=null) { if (log.isDebugEnabled()) log.debug("Obtained the JAXR factory name from System Property "
                + JAXRConstants.JAXR_FACTORY_IMPLEMENTATION + " factory implementation class is "
                + factoryName);
        } else {
            //2. try to obtain from jboss config
          factoryName = config.getConnectionFactoryImplementation();
        }
        if (factoryName!=null ) { if (log.isDebugEnabled()) log.debug("Obtained the JAXR factory name from JBoss configuration: "
                + " factory implementation class is " + factoryName);
        } else {
            //3. try to obtain from the ServiceAPI
            //looking for file META-INF/services/javax.xml.registry.ConnectionFactory
            ServiceLoader<ConnectionFactory> factoryLoader = ServiceLoader.load(ConnectionFactory.class);
            if (factoryLoader!=null) {
                for (ConnectionFactory factory : factoryLoader) {
                    if (log.isDebugEnabled()) log.debug("Obtained the JAXR factory name from the Service API: "
                            + " using file META-INF/services/javax.xml.registry.ConnectionFactory="
                            + factory.getClass().getName());
                    return factory;
                }
            }
        }
            //4. default to scout
        if (factoryName==null) factoryName = JAXRConstants.DEFAULT_JAXR_FACTORY_IMPL;
        if (log.isDebugEnabled()) log.debug("Using default JAXR factory name " + JAXRConstants.DEFAULT_JAXR_FACTORY_IMPL);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> factoryClass;
        try {
            try {
                factoryClass = loader.loadClass(factoryName);
            } catch (ClassNotFoundException e) {
                // Fall back to defining CL
                factoryClass = this.getClass().getClassLoader().loadClass(factoryName);
            }
            jaxrFactory = (ConnectionFactory) factoryClass.newInstance();
        } catch(Throwable e) {
            throw new JAXRException("Failed to create instance of: "+factoryName, e);
        }
        return jaxrFactory;
    }

    private void setJAXRFactoryProperies(ConnectionFactory jaxrFactory, Properties properties) throws JAXRException
    {
        String defaultQueryManager     = JAXRConstants.DEFAULT_QUERYMANAGER;
        String defaultLifeCycleManager = JAXRConstants.DEFAULT_LIFECYCLEMANAGER;
        // if we are using scout the some more defaults can can be set
        if (jaxrFactory.getClass().getName().equals(JAXRConstants.DEFAULT_JAXR_FACTORY_IMPL)) {
            String version = properties.getProperty(JAXRConstants.UDDI_VERSION_PROPERTY_NAME, JAXRConstants.UDDI_V2_VERSION);
            if (version.equals(JAXRConstants.UDDI_V2_VERSION)) {
                properties.setProperty(JAXRConstants.UDDI_VERSION_PROPERTY_NAME,JAXRConstants.UDDI_V2_VERSION);
                properties.setProperty(JAXRConstants.UDDI_NAMESPACE_PROPERTY_NAME,JAXRConstants.UDDI_V2_NAMESPACE);
                if (! properties.containsKey(JAXRConstants.SCOUT_TRANSPORT)) {
                    properties.setProperty(JAXRConstants.SCOUT_TRANSPORT, JAXRConstants.SCOUT_SAAJ_TRANSPORT);
                }
            } else {
                properties.setProperty(JAXRConstants.UDDI_VERSION_PROPERTY_NAME,JAXRConstants.UDDI_V3_VERSION);
                properties.setProperty(JAXRConstants.UDDI_NAMESPACE_PROPERTY_NAME,JAXRConstants.UDDI_V3_NAMESPACE);
                defaultQueryManager = JAXRConstants.DEFAULT_V3_QUERYMANAGER;
                defaultLifeCycleManager = JAXRConstants.DEFAULT_V3_LIFECYCLEMANAGER;
                if (! properties.containsKey(JAXRConstants.SECURITYMANAGER)) {
                    properties.setProperty(JAXRConstants.SECURITYMANAGER, JAXRConstants.DEFAULT_V3_SECURITYMANAGER);
                }
                if (! properties.containsKey(JAXRConstants.SCOUT_TRANSPORT)) {
                    properties.setProperty(JAXRConstants.SCOUT_TRANSPORT, JAXRConstants.SCOUT_LOCAL_TRANSPORT);
                }
            }
        }
        // always defaulting the query and lifecycle URLs
        if (! properties.containsKey(JAXRConstants.QUERYMANAGER)) {
            properties.setProperty(JAXRConstants.QUERYMANAGER, defaultQueryManager);
        }
        if (! properties.containsKey(JAXRConstants.LIFECYCLEMANAGER)) {
            properties.setProperty(JAXRConstants.LIFECYCLEMANAGER, defaultLifeCycleManager);
        }
        if (log.isDebugEnabled()) {
            log.debug("JAXR is using the " + jaxrFactory.getClass().getName() + " implementation");
            log.debug("By default the following JAXR Properties are set:");
            Enumeration<?> propertyNames = properties.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String key = (String) propertyNames.nextElement();
                log.debug(key + "=" + properties.getProperty(key));
            }
        }
        jaxrFactory.setProperties(properties);
    }
}
