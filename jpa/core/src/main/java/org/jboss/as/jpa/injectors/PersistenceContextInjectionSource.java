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

import static org.jboss.as.jpa.JpaLogger.JPA_LOGGER;
import static org.jboss.as.jpa.JpaMessages.MESSAGES;

import java.lang.reflect.Proxy;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.jpa.container.CreatedEntityManagers;
import org.jboss.as.jpa.container.EntityManagerUnwrappedTargetInvocationHandler;
import org.jboss.as.jpa.container.ExtendedEntityManager;
import org.jboss.as.jpa.container.ReferenceCountedEntityManager;
import org.jboss.as.jpa.container.SFSBCallStack;
import org.jboss.as.jpa.container.TransactionScopedEntityManager;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
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

/**
 * Represents the PersistenceContext injected into a component.
 *
 * @author Scott Marlow
 */
public class PersistenceContextInjectionSource extends InjectionSource {

    private final PersistenceContextType type;

    private final PersistenceContextJndiInjectable injectable;

    private final ServiceName puServiceName;

    /**
     * Constructor for the PersistenceContextInjectorService
     *
     * @param type              The persistence context type
     * @param properties        The persistence context properties
     * @param puServiceName     represents the deployed persistence.xml that we are going to use.
     * @param deploymentUnit    represents the deployment that we are injecting into
     * @param scopedPuName      the fully scoped reference to the persistence.xml
     * @param injectionTypeName is normally "javax.persistence.EntityManager" but could be a different target class
     *                          for example "org.hibernate.Session" in which case, EntityManager.unwrap(org.hibernate.Session.class is called)
     * @param pu
     */
    public PersistenceContextInjectionSource(final PersistenceContextType type, final Map properties, final ServiceName puServiceName, final DeploymentUnit deploymentUnit, final String scopedPuName, final String injectionTypeName, final PersistenceUnitMetadata pu) {

        this.type = type;

        injectable = new PersistenceContextJndiInjectable(puServiceName, deploymentUnit, this.type, properties, scopedPuName, injectionTypeName, pu);
        this.puServiceName = puServiceName;
    }

    public void getResourceValue(final ResolutionContext resolutionContext, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws
        DeploymentUnitProcessingException {
        serviceBuilder.addDependencies(puServiceName);
        injector.inject(injectable);
    }

    public boolean equals(Object other) {
        if (other instanceof PersistenceContextInjectionSource) {
            PersistenceContextInjectionSource source = (PersistenceContextInjectionSource) other;
            return (source.puServiceName.equals(puServiceName));
        }
        return false;
    }

    public int hashCode() {
        return puServiceName.hashCode();
    }

    private static final class PersistenceContextJndiInjectable implements ManagedReferenceFactory {

        private final ServiceName puServiceName;
        private final DeploymentUnit deploymentUnit;
        private final PersistenceContextType type;
        private final Map properties;
        private final String unitName;
        private final String injectionTypeName;
        private final PersistenceUnitMetadata pu;

        private static final String ENTITY_MANAGER_CLASS = "javax.persistence.EntityManager";

        public PersistenceContextJndiInjectable(
            final ServiceName puServiceName,
            final DeploymentUnit deploymentUnit,
            final PersistenceContextType type,
            final Map properties,
            final String unitName,
            final String injectionTypeName,
            final PersistenceUnitMetadata pu) {

            this.puServiceName = puServiceName;
            this.deploymentUnit = deploymentUnit;
            this.type = type;
            this.properties = properties;
            this.unitName = unitName;
            this.injectionTypeName = injectionTypeName;
            this.pu = pu;
        }

        @Override
        public ManagedReference getReference() {
            PersistenceUnitServiceImpl service = (PersistenceUnitServiceImpl) deploymentUnit.getServiceRegistry().getRequiredService(puServiceName).getValue();
            EntityManagerFactory emf = service.getEntityManagerFactory();
            EntityManager entityManager;
            boolean standardEntityManager = ENTITY_MANAGER_CLASS.equals(injectionTypeName);

            if (type.equals(PersistenceContextType.TRANSACTION)) {
                entityManager = new TransactionScopedEntityManager(unitName, properties, emf);
                if (JPA_LOGGER.isDebugEnabled())
                    JPA_LOGGER.debugf("created new TransactionScopedEntityManager for unit name=%s", unitName);
            } else {
                // handle PersistenceContextType.EXTENDED
                ReferenceCountedEntityManager entityManager1 = SFSBCallStack.findPersistenceContext(unitName);
                if (entityManager1 == null) {
                    EntityManager tmpEm = emf.createEntityManager(properties);
                    entityManager = new ExtendedEntityManager(unitName, tmpEm);
                    entityManager1 = new ReferenceCountedEntityManager((ExtendedEntityManager)entityManager);
                    if (JPA_LOGGER.isDebugEnabled())
                        JPA_LOGGER.debugf("created new ExtendedEntityManager for unit name=%s", unitName);

                } else {
                    entityManager1.increaseReferenceCount();
                    entityManager = entityManager1.getEntityManager();
                    if (JPA_LOGGER.isDebugEnabled())
                    JPA_LOGGER.debugf("inherited existing ExtendedEntityManager from SFSB invocation stack, unit name=%s, " +
                        "%d beans sharing ExtendedEntityManager", unitName, entityManager1.getReferenceCount());
                }

                // register the EntityManager on TL so that SFSBCreateInterceptor will see it.
                // this is important for creating a new XPC or inheriting existing XPC from SFSBCallStack
                CreatedEntityManagers.registerPersistenceContext(entityManager1);

                //register the pc so it is accessible to other SFSB's during the creation process
                SFSBCallStack.extendedPersistenceContextCreated(unitName, entityManager1);

            }

            if (!standardEntityManager) {
                /**
                 * inject non-standard wrapped class (e.g. org.hibernate.Session).
                 * To accomplish this, we will create an instance of the (underlying provider's) entity manager and
                 * invoke the EntityManager.unwrap(TargetInjectionClass).
                 */
                Class extensionClass;
                try {
                    // provider classes should be on application classpath
                    extensionClass = pu.getClassLoader().loadClass(injectionTypeName);
                } catch (ClassNotFoundException e) {
                    throw MESSAGES.cannotLoadFromJpa(e, injectionTypeName);
                }
                // get example of target object
                Object targetValueToInject = entityManager.unwrap(extensionClass);

                // build array of classes that proxy will represent.
                Class[] targetInterfaces = targetValueToInject.getClass().getInterfaces();
                Class[] proxyInterfaces = new Class[targetInterfaces.length + 1];  // include extra element for extensionClass
                boolean alreadyHasInterfaceClass = false;
                for (int interfaceIndex = 0; interfaceIndex < targetInterfaces.length; interfaceIndex++) {
                    Class interfaceClass =  targetInterfaces[interfaceIndex];
                    if (interfaceClass.equals(extensionClass)) {
                        proxyInterfaces = targetInterfaces;                     // targetInterfaces already has all interfaces
                        alreadyHasInterfaceClass = true;
                        break;
                    }
                    proxyInterfaces[1 + interfaceIndex] = interfaceClass;
                }
                if (!alreadyHasInterfaceClass) {
                    proxyInterfaces[0] = extensionClass;
                }

                EntityManagerUnwrappedTargetInvocationHandler entityManagerUnwrappedTargetInvocationHandler =
                    new EntityManagerUnwrappedTargetInvocationHandler(entityManager, extensionClass);
                Object proxyForUnwrappedObject = Proxy.newProxyInstance(
                                    extensionClass.getClassLoader(), //use the target classloader so the proxy has the same scope
                                    proxyInterfaces,
                                    entityManagerUnwrappedTargetInvocationHandler
                                );

                if (JPA_LOGGER.isDebugEnabled())
                    JPA_LOGGER.debugf("injecting entity manager into a '%s' (unit name=%s)", extensionClass.getName(), unitName);

                return new ValueManagedReference(new ImmediateValue<Object>(proxyForUnwrappedObject));
            }

            return new ValueManagedReference(new ImmediateValue<Object>(entityManager));
        }

    }

}
