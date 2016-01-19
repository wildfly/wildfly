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

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.lang.reflect.Proxy;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.persistence.SynchronizationType;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.jpa.config.ExtendedPersistenceInheritance;
import org.jboss.as.jpa.config.JPADeploymentSettings;
import org.jboss.as.jpa.container.CreatedEntityManagers;
import org.jboss.as.jpa.container.EntityManagerUnwrappedTargetInvocationHandler;
import org.jboss.as.jpa.container.ExtendedEntityManager;
import org.jboss.as.jpa.container.ExtendedPersistenceDeepInheritance;
import org.jboss.as.jpa.container.ExtendedPersistenceShallowInheritance;
import org.jboss.as.jpa.container.TransactionScopedEntityManager;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.jpa.service.JPAService;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.txn.service.TransactionManagerService;
import org.jboss.as.txn.service.TransactionSynchronizationRegistryService;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.value.ImmediateValue;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

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
     * @param serviceRegistry    The MSC service registry which will be used to find the PersistenceContext service
     * @param scopedPuName      the fully scoped reference to the persistence.xml
     * @param injectionTypeName is normally "javax.persistence.EntityManager" but could be a different target class
     *                          for example "org.hibernate.Session" in which case, EntityManager.unwrap(org.hibernate.Session.class is called)
     * @param pu
     * @param jpaDeploymentSettings Optional {@link JPADeploymentSettings} applicable for the persistence context
     */
    public PersistenceContextInjectionSource(final PersistenceContextType type, final SynchronizationType synchronizationType , final Map properties, final ServiceName puServiceName,
                                             final ServiceRegistry serviceRegistry, final String scopedPuName, final String injectionTypeName, final PersistenceUnitMetadata pu, final JPADeploymentSettings jpaDeploymentSettings) {

        this.type = type;
        injectable = new PersistenceContextJndiInjectable(puServiceName, serviceRegistry, this.type, synchronizationType , properties, scopedPuName, injectionTypeName, pu, jpaDeploymentSettings);
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
        private final ServiceRegistry serviceRegistry;
        private final PersistenceContextType type;
        private final SynchronizationType synchronizationType;
        private final Map properties;
        private final String unitName;
        private final String injectionTypeName;
        private final PersistenceUnitMetadata pu;
        private final JPADeploymentSettings jpaDeploymentSettings;

        private static final String ENTITY_MANAGER_CLASS = "javax.persistence.EntityManager";

        public PersistenceContextJndiInjectable(
            final ServiceName puServiceName,
            final ServiceRegistry serviceRegistry,
            final PersistenceContextType type,
            SynchronizationType synchronizationType,
            final Map properties,
            final String unitName,
            final String injectionTypeName,
            final PersistenceUnitMetadata pu,
            final JPADeploymentSettings jpaDeploymentSettings) {

            this.puServiceName = puServiceName;
            this.serviceRegistry = serviceRegistry;
            this.type = type;
            this.properties = properties;
            this.unitName = unitName;
            this.injectionTypeName = injectionTypeName;
            this.pu = pu;
            this.synchronizationType = synchronizationType;
            this.jpaDeploymentSettings = jpaDeploymentSettings;
        }

        @Override
        public ManagedReference getReference() {
            PersistenceUnitServiceImpl service = (PersistenceUnitServiceImpl) serviceRegistry.getRequiredService(puServiceName).getValue();
            EntityManagerFactory emf = service.getEntityManagerFactory();
            EntityManager entityManager;
            boolean standardEntityManager = ENTITY_MANAGER_CLASS.equals(injectionTypeName);
            //TODO: change all this to use injections
            //this is currntly safe, as there is a DUP dependency on the TSR
            TransactionSynchronizationRegistry tsr = (TransactionSynchronizationRegistry) serviceRegistry.getRequiredService(TransactionSynchronizationRegistryService.SERVICE_NAME).getValue();
            TransactionManager transactionManager = (TransactionManager) serviceRegistry.getRequiredService(TransactionManagerService.SERVICE_NAME).getValue();
            if (type.equals(PersistenceContextType.TRANSACTION)) {
                entityManager = new TransactionScopedEntityManager(unitName, properties, emf, synchronizationType, tsr, transactionManager);
                if (ROOT_LOGGER.isDebugEnabled())
                    ROOT_LOGGER.debugf("created new TransactionScopedEntityManager for unit name=%s", unitName);
            } else {
                boolean useDeepInheritance = !ExtendedPersistenceInheritance.SHALLOW.equals(JPAService.getDefaultExtendedPersistenceInheritance());
                if (jpaDeploymentSettings != null) {
                    useDeepInheritance = ExtendedPersistenceInheritance.DEEP.equals(jpaDeploymentSettings.getExtendedPersistenceInheritanceType());
                }

                boolean createdNewExtendedPersistence = false;
                ExtendedEntityManager entityManager1;
                // handle PersistenceContextType.EXTENDED
                if (useDeepInheritance) {
                    entityManager1 = ExtendedPersistenceDeepInheritance.INSTANCE.findExtendedPersistenceContext(unitName);
                }
                else {
                    entityManager1 = ExtendedPersistenceShallowInheritance.INSTANCE.findExtendedPersistenceContext(unitName);
                }

                if (entityManager1 == null) {
                    if (SynchronizationType.UNSYNCHRONIZED.equals(synchronizationType)) {
                        entityManager1 = new ExtendedEntityManager(unitName, emf.createEntityManager(synchronizationType, properties), synchronizationType, tsr, transactionManager);
                    }
                    else {
                        entityManager1 = new ExtendedEntityManager(unitName, emf.createEntityManager(properties), synchronizationType, tsr, transactionManager);
                    }
                    createdNewExtendedPersistence = true;
                    if (ROOT_LOGGER.isDebugEnabled())
                        ROOT_LOGGER.debugf("created new ExtendedEntityManager for unit name=%s, useDeepInheritance = %b", unitName, useDeepInheritance);

                } else {
                    entityManager1.increaseReferenceCount();
                    if (ROOT_LOGGER.isDebugEnabled())
                        ROOT_LOGGER.debugf("inherited existing ExtendedEntityManager from SFSB invocation stack, unit name=%s, " +
                                "%d beans sharing ExtendedEntityManager, useDeepInheritance = %b", unitName, entityManager1.getReferenceCount(), useDeepInheritance);
                }

                entityManager = entityManager1;

                // register the EntityManager on TL so that SFSBCreateInterceptor will see it.
                // this is important for creating a new XPC or inheriting existing XPC from SFSBCallStack
                CreatedEntityManagers.registerPersistenceContext(entityManager1);

                if (createdNewExtendedPersistence) {
                    //register the pc so it is accessible to other SFSB's during the creation process
                    if (useDeepInheritance) {
                        ExtendedPersistenceDeepInheritance.INSTANCE.registerExtendedPersistenceContext(unitName, entityManager1);
                    }
                    else {
                        ExtendedPersistenceShallowInheritance.INSTANCE.registerExtendedPersistenceContext(unitName, entityManager1);
                    }
                }

            }

            if (!standardEntityManager) {
                /**
                 * inject non-standard wrapped class (e.g. org.hibernate.Session).
                 * To accomplish this, we will create an instance of the (underlying provider's) entity manager and
                 * invoke the EntityManager.unwrap(TargetInjectionClass).
                 */
                Class<?> extensionClass;
                try {
                    // provider classes should be on application classpath
                    extensionClass = pu.getClassLoader().loadClass(injectionTypeName);
                } catch (ClassNotFoundException e) {
                    throw JpaLogger.ROOT_LOGGER.cannotLoadFromJpa(e, injectionTypeName);
                }
                // get example of target object
                Object targetValueToInject = entityManager.unwrap(extensionClass);

                // build array of classes that proxy will represent.
                Class<?>[] targetInterfaces = targetValueToInject.getClass().getInterfaces();
                Class<?>[] proxyInterfaces = new Class[targetInterfaces.length + 1];  // include extra element for extensionClass
                boolean alreadyHasInterfaceClass = false;
                for (int interfaceIndex = 0; interfaceIndex < targetInterfaces.length; interfaceIndex++) {
                    Class<?> interfaceClass =  targetInterfaces[interfaceIndex];
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

                if (ROOT_LOGGER.isDebugEnabled())
                    ROOT_LOGGER.debugf("injecting entity manager into a '%s' (unit name=%s)", extensionClass.getName(), unitName);

                return new ValueManagedReference(new ImmediateValue<Object>(proxyForUnwrappedObject));
            }

            return new ValueManagedReference(new ImmediateValue<Object>(entityManager));
        }

    }

}
