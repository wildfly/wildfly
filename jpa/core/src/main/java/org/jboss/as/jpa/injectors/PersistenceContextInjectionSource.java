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

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.jpa.container.ExtendedEntityManager;
import org.jboss.as.jpa.container.NonTxEmCloser;
import org.jboss.as.jpa.container.SFSBCallStack;
import org.jboss.as.jpa.container.SFSBXPCMap;
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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import java.util.HashSet;
import java.util.Map;

import static org.jboss.as.jpa.JpaLogger.JPA_LOGGER;
import static org.jboss.as.jpa.JpaMessages.MESSAGES;

/**
 * Represents the PersistenceContext injected into a component.
 *
 * @author Scott Marlow
 */
public class PersistenceContextInjectionSource extends InjectionSource {

    private final PersistenceContextType type;

    private final PersistenceContextJndiInjectable injectable;

    private final ServiceName puServiceName;

    /** the following list of classes determines which unwrap classes are special, in that the underlying entity
     * manager won't be closed, even if no transaction is active on the calling thread.
     * TODO:  move this list to PersistenceProviderAdaptor
     */
    private static final HashSet<String> skipEntityManagerCloseFor = new HashSet<String>();

    static {
        skipEntityManagerCloseFor.add("org.hibernate.Session"); // Hibernate session will take ownership of the session
    }


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
     * @param sfsbxpcMap        TODO: refactor to only pass in for type == PersistenceContextType.EXTENDED
     * @param pu
     */
    public PersistenceContextInjectionSource(final PersistenceContextType type, final Map properties, final ServiceName puServiceName, final DeploymentUnit deploymentUnit, final String scopedPuName, final String injectionTypeName, final SFSBXPCMap sfsbxpcMap, final PersistenceUnitMetadata pu) {

        this.type = type;


        injectable = new PersistenceContextJndiInjectable(puServiceName, deploymentUnit, this.type, properties, scopedPuName, injectionTypeName, sfsbxpcMap, pu);
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
        private final SFSBXPCMap sfsbxpcMap;
        private final PersistenceUnitMetadata pu;

        private static final String ENTITY_MANAGER_CLASS = "javax.persistence.EntityManager";

        public PersistenceContextJndiInjectable(
            final ServiceName puServiceName,
            final DeploymentUnit deploymentUnit,
            final PersistenceContextType type,
            final Map properties,
            final String unitName,
            final String injectionTypeName,
            final SFSBXPCMap sfsbxpcMap,
            final PersistenceUnitMetadata pu) {

            this.puServiceName = puServiceName;
            this.deploymentUnit = deploymentUnit;
            this.type = type;
            this.properties = properties;
            this.unitName = unitName;
            this.injectionTypeName = injectionTypeName;
            this.sfsbxpcMap = sfsbxpcMap;
            this.pu = pu;
        }

        @Override
        public ManagedReference getReference() {
            PersistenceUnitServiceImpl service = (PersistenceUnitServiceImpl) deploymentUnit.getServiceRegistry().getRequiredService(puServiceName).getValue();
            EntityManagerFactory emf = service.getEntityManagerFactory();
            EntityManager entityManager;
            boolean isExtended;
            if (type.equals(PersistenceContextType.TRANSACTION)) {
                isExtended = false;
                entityManager = new TransactionScopedEntityManager(unitName, properties, emf);
                if (JPA_LOGGER.isDebugEnabled())
                    JPA_LOGGER.debugf("created new TransactionScopedEntityManager for unit name=%s", unitName);
            } else {
                // handle PersistenceContextType.EXTENDED
                isExtended = true;
                EntityManager entityManager1 = SFSBCallStack.findPersistenceContext(unitName, sfsbxpcMap);
                if (entityManager1 == null) {
                    entityManager1 = emf.createEntityManager(properties);
                    entityManager = new ExtendedEntityManager(unitName, entityManager1);
                    if (JPA_LOGGER.isDebugEnabled())
                        JPA_LOGGER.debugf("created new ExtendedEntityManager for unit name=%s", unitName);

                } else {
                    entityManager = entityManager1;
                    if (JPA_LOGGER.isDebugEnabled())
                        JPA_LOGGER.debugf("inherited existing ExtendedEntityManager from SFSB invocation stack, unit name=%s", unitName);
                }

                // register the EntityManager on TL so that SFSBCreateInterceptor will see it.
                // this is important for creating a new XPC or inheriting existing XPC from SFSBCallStack
                SFSBXPCMap.registerPersistenceContext(entityManager);

                //register the pc so it is accessible to other SFSB's during the creation process
                SFSBCallStack.extendedPersistenceContextCreated(unitName, entityManager);

            }

            if (!ENTITY_MANAGER_CLASS.equals(injectionTypeName)) { // inject non-standard wrapped class (e.g. org.hibernate.Session)
                Class extensionClass;
                try {
                    // provider classes should be on application classpath
                    extensionClass = pu.getClassLoader().loadClass(injectionTypeName);
                } catch (ClassNotFoundException e) {
                    throw MESSAGES.cannotLoadFromJpa(e, injectionTypeName);
                }
                boolean skipAutoCloseAfterUnwrap = skipEntityManagerCloseFor.contains(injectionTypeName);
                if (!skipAutoCloseAfterUnwrap && !isExtended) {
                    NonTxEmCloser.pushCall();   // create thread local to hold underlying entity manager that unwrap will create
                }
                Object targetValueToInject = entityManager.unwrap(extensionClass);
                if (!skipAutoCloseAfterUnwrap && !isExtended) {
                    NonTxEmCloser.popCall();    // close entity manager that unwrap created
                }

                return new ValueManagedReference(new ImmediateValue<Object>(targetValueToInject));
            }

            return new ValueManagedReference(new ImmediateValue<Object>(entityManager));
        }

    }

}
