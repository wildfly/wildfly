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

import org.apache.ws.scout.registry.ConnectionFactoryImpl;
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
 * @since 17-Nov-2011
 */
public final class JAXRConnectionFactoryService extends AbstractService<Void> {

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jaxr", "connectionfactory");

    // [TODO] AS7-2277 JAXR subsystem i18n
    private final Logger log = Logger.getLogger(JAXRConnectionFactoryService.class);

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
                ImmediateValue value = new ImmediateValue(new ConnectionFactoryImpl());
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
}
