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

package org.jboss.as.jpa.injectors;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.persistence.EntityManagerFactory;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.value.ImmediateValue;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * Represents the PersistenceUnit injected into a component.
 * TODO:  support injecting into a HibernateSessionFactory.  Initially, hack it by checking injectionTypeName parameter
 * for HibernateSessionFactory.  If/when JPA supports unwrap on the EMF, switch to that.
 *
 * @author Scott Marlow
 */
public class PersistenceUnitInjectionSource extends InjectionSource {

    private final PersistenceUnitJndiInjectable injectable;
    private final ServiceName puServiceName;

    public PersistenceUnitInjectionSource(final ServiceName puServiceName, final ServiceRegistry serviceRegistry, final String injectionTypeName, final PersistenceUnitMetadata pu) {

        injectable = new PersistenceUnitJndiInjectable(puServiceName, serviceRegistry, injectionTypeName, pu);
        this.puServiceName = puServiceName;
    }

    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws
        DeploymentUnitProcessingException {
        serviceBuilder.addDependencies(puServiceName);
        injector.inject(injectable);
    }

    public boolean equals(final Object other) {
        if (other instanceof PersistenceUnitInjectionSource) {
            PersistenceUnitInjectionSource source = (PersistenceUnitInjectionSource) other;
            return (source.puServiceName.equals(puServiceName));
        }
        return false;
    }

    public int hashCode() {
        return puServiceName.hashCode();
    }

    private static final class PersistenceUnitJndiInjectable implements ManagedReferenceFactory {

        final ServiceName puServiceName;
        final ServiceRegistry serviceRegistry;
        final String injectionTypeName;
        final PersistenceUnitMetadata pu;
        private static final String ENTITY_MANAGER_FACTORY_CLASS = "javax.persistence.EntityManagerFactory";

        public PersistenceUnitJndiInjectable(
            final ServiceName puServiceName,
            final ServiceRegistry serviceRegistry,
            final String injectionTypeName,
            final PersistenceUnitMetadata pu) {

            this.puServiceName = puServiceName;
            this.serviceRegistry = serviceRegistry;
            this.injectionTypeName = injectionTypeName;
            this.pu = pu;
        }

        @Override
        public ManagedReference getReference() {
            PersistenceUnitServiceImpl service = (PersistenceUnitServiceImpl) serviceRegistry.getRequiredService(puServiceName).getValue();
            EntityManagerFactory emf = service.getEntityManagerFactory();

            if (!ENTITY_MANAGER_FACTORY_CLASS.equals(injectionTypeName)) { // inject non-standard wrapped class (e.g. org.hibernate.SessionFactory)
                Class<?> extensionClass;
                try {
                    // make sure we can access the target class type
                    extensionClass = pu.getClassLoader().loadClass(injectionTypeName);
                } catch (ClassNotFoundException e) {
                    throw JpaLogger.ROOT_LOGGER.cannotLoadFromJpa(e, injectionTypeName);
                }
                // TODO:  when/if jpa supports unwrap, change to
                //   Object targetValueToInject = emf.unwrap(extensionClass);
                // Until jpa supports unwrap on sessionfactory, only support hibernate

                Method getSessionFactory;
                try {
                    getSessionFactory = emf.getClass().getMethod("getSessionFactory");
                } catch (NoSuchMethodException e) {
                    throw JpaLogger.ROOT_LOGGER.hibernateOnlyEntityManagerFactory();
                }

                Object targetValueToInject = null;
                try {
                    targetValueToInject = getSessionFactory.invoke(emf, new Object[0]);
                } catch (IllegalAccessException e) {
                    throw JpaLogger.ROOT_LOGGER.cannotGetSessionFactory(e);
                } catch (InvocationTargetException e) {
                    throw JpaLogger.ROOT_LOGGER.cannotGetSessionFactory(e);
                }
                return new ValueManagedReference(new ImmediateValue<Object>(targetValueToInject));
            }

            return new ValueManagedReference(new ImmediateValue<Object>(emf));
        }
    }

}
