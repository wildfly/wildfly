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


import org.hibernate.ejb.EntityManagerFactoryImpl;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.jpa.service.PersistenceUnitService;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.ImmediateValue;

import javax.persistence.EntityManagerFactory;


/**
 * Represents the PersistenceUnit injected into a component.
 * TODO:  support injecting into a HibernateSessionFactory.  Initially, hack it by checknig injectionTypeName parameter
 * for HibernateSessionFactory.  If/when JPA supports unwrap on the EMF, switch to that.
 *
 * @author Scott Marlow
 */
public class PersistenceUnitInjectionSource extends InjectionSource {

    private final PersistenceUnitJndiInjectable injectable;
    private final ServiceName puServiceName;

    public PersistenceUnitInjectionSource(final ServiceName puServiceName, final DeploymentUnit deploymentUnit, final String injectionTypeName) {

        injectable = new PersistenceUnitJndiInjectable(puServiceName, deploymentUnit, injectionTypeName);
        this.puServiceName = puServiceName;
    }

    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws
        DeploymentUnitProcessingException {
        serviceBuilder.addDependencies(puServiceName);
        injector.inject(injectable);
    }

    @Override
    public boolean equalTo(final InjectionSource other, final DeploymentPhaseContext phaseContext) {
        if (other instanceof PersistenceUnitInjectionSource) {
            PersistenceUnitInjectionSource source = (PersistenceUnitInjectionSource) other;
            return (source.puServiceName.equals(puServiceName));
        }
        return false;
    }

    private static final class PersistenceUnitJndiInjectable implements ManagedReferenceFactory {

        final ServiceName puServiceName;
        final DeploymentUnit deploymentUnit;
        final String injectionTypeName;
        private static final String ENTITY_MANAGER_FACTORY_CLASS = "javax.persistence.EntityManagerFactory";

        public PersistenceUnitJndiInjectable(
                final ServiceName puServiceName,
                final DeploymentUnit deploymentUnit,
                final String injectionTypeName) {

            this.puServiceName = puServiceName;
            this.deploymentUnit = deploymentUnit;
            this.injectionTypeName = injectionTypeName;
        }

        @Override
        public ManagedReference getReference() {
            PersistenceUnitService service = (PersistenceUnitService) deploymentUnit.getServiceRegistry().getRequiredService(puServiceName).getValue();
            EntityManagerFactory emf = service.getEntityManagerFactory();

            if (!ENTITY_MANAGER_FACTORY_CLASS.equals(injectionTypeName)) { // inject non-standard wrapped class (e.g. org.hibernate.SessionFactory)
                Class extensionClass;
                try {
                    // make sure we can access the target class type
                    extensionClass = this.getClass().getClassLoader().loadClass(injectionTypeName);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("couldn't load " + injectionTypeName + " from JPA modules classloader", e);
                }
                // TODO:  when/if jpa supports unwrap, change to
                //   Object targetValueToInject = emf.unwrap(extensionClass);
                // Until jpa supports unwrap on sessionfactory, only support hibernate
                if (!(emf instanceof EntityManagerFactoryImpl)) {
                    throw new RuntimeException("Can only inject from a Hibernate EntityManagerFactoryImpl");
                }
                Object targetValueToInject = ((EntityManagerFactoryImpl) emf).getSessionFactory();
                return new ValueManagedReference(new ImmediateValue<Object>(targetValueToInject));
            }

            return new ValueManagedReference(new ImmediateValue<Object>(emf));
        }
    }

}
