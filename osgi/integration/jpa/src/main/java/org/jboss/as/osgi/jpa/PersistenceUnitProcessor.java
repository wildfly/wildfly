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
package org.jboss.as.osgi.jpa;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.persistence.EntityManagerFactory;

import org.jboss.as.jpa.processor.JpaAttachments;
import org.jboss.as.jpa.spi.PersistenceUnitService;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.framework.spi.BundleManager;
import org.jboss.osgi.resolver.XBundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

/**
 * Install the services for a persistence bundle.
 *
 * @author thomas.diesler@jboss.com
 * @since 01-Sep-2012
 */
public class PersistenceUnitProcessor implements DeploymentUnitProcessor {

    private static AttachmentKey<BundleListener> BUNDLE_LISTENER_KEY = AttachmentKey.create(BundleListener.class);

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        final ServiceName puServiceName = depUnit.getAttachment(JpaAttachments.PERSISTENCE_UNIT_SERVICE_KEY);
        final XBundle bundle = depUnit.getAttachment(OSGiConstants.BUNDLE_KEY);
        if (bundle == null || puServiceName == null)
            return;

        BundleContext syscontext = depUnit.getAttachment(OSGiConstants.SYSTEM_CONTEXT_KEY);
        BundleListener listener = new SynchronousBundleListener() {
            @Override
            public void bundleChanged(BundleEvent event) {
                if (event.getBundle() == bundle && event.getType() == BundleEvent.STARTED) {
                    BundleManager bundleManager = depUnit.getAttachment(OSGiConstants.BUNDLE_MANAGER_KEY);
                    EntityManagerFactoryRegistration.addService(phaseContext.getServiceTarget(), bundleManager, puServiceName, bundle);
                }
            }
        };
        syscontext.addBundleListener(listener);
        depUnit.putAttachment(BUNDLE_LISTENER_KEY, listener);
    }

    @Override
    public void undeploy(DeploymentUnit depUnit) {
        BundleListener listener = depUnit.getAttachment(BUNDLE_LISTENER_KEY);
        if (listener != null) {
            BundleContext syscontext = depUnit.getAttachment(OSGiConstants.SYSTEM_CONTEXT_KEY);
            syscontext.removeBundleListener(listener);
        }
    }

    static class EntityManagerFactoryRegistration implements Service<ServiceRegistration<EntityManagerFactory>> {

        private final InjectedValue<PersistenceUnitService> injectedPersistenceUnitService = new InjectedValue<PersistenceUnitService>();
        private final XBundle bundle;
        private ServiceRegistration<EntityManagerFactory> registration;

        static void addService(ServiceTarget serviceTarget, BundleManager bundleManager, ServiceName puServiceName, XBundle bundle) {
            ServiceName bundleName = bundleManager.getServiceName(bundle);
            ServiceName serviceName = bundleName.append(EntityManagerFactory.class.getSimpleName());
            EntityManagerFactoryRegistration service = new EntityManagerFactoryRegistration(bundle);
            ServiceBuilder<ServiceRegistration<EntityManagerFactory>> builder = serviceTarget.addService(serviceName, service);
            builder.addDependency(puServiceName, PersistenceUnitService.class, service.injectedPersistenceUnitService);
            builder.setInitialMode(Mode.PASSIVE);
            builder.install();
        }

        private EntityManagerFactoryRegistration(XBundle bundle) {
            this.bundle = bundle;
        }

        @Override
        public void start(StartContext context) throws StartException {
            BundleContext bundleContext = bundle.getBundleContext();
            PersistenceUnitService puService = injectedPersistenceUnitService.getValue();
            EntityManagerFactory emf = puService.getEntityManagerFactory();
            Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put(EntityManagerFactoryBuilder.JPA_UNIT_NAME, puService.getScopedPersistenceUnitName());
            // [TODO] unit version/provider
            properties.put(EntityManagerFactoryBuilder.JPA_UNIT_VERSION, "unknown");
            properties.put(EntityManagerFactoryBuilder.JPA_UNIT_PROVIDER, "unknown");
            registration = bundleContext.registerService(EntityManagerFactory.class, emf, properties);
        }

        @Override
        public void stop(StopContext context) {
            if (registration != null) {
                registration.unregister();
                registration = null;
            }
        }

        @Override
        public ServiceRegistration<EntityManagerFactory> getValue() throws IllegalStateException, IllegalArgumentException {
            return registration;
        }
    }
}