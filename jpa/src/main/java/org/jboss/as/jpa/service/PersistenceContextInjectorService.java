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

package org.jboss.as.jpa.service;

import org.jboss.as.jpa.container.ExtendedEntityManager;
import org.jboss.as.jpa.container.SFSBCallStack;
import org.jboss.as.jpa.container.SFSBXPCMap;
import org.jboss.as.jpa.container.TransactionScopedEntityManager;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the PersistenceContext injected into a component.
 *
 * @author Scott Marlow
 */
public class PersistenceContextInjectorService implements Service<ManagedReferenceFactory>{

    private final PersistenceContextType type;

    private final PersistenceContextJndiInjectable injectable;

    /**
     * Constructor for the PersistenceContextInjectorService
     * @param annotation represents the annotation that we are satisfying.
     * @param puServiceName represents the deployed persistence.xml that we are going to use.
     * @param deploymentUnit represents the deployment that we are injecting into
     * @param scopedPuName the fully scoped reference to the persistence.xml
     * @param injectionTypeName is normally "javax.persistence.EntityManager" but could be a different target class
     * for example "org.hibernate.Session" in which case, EntityManager.unwrap(org.hibernate.Session.class is called)
     * the unwrap return value is injected (instead of the EntityManager instance)
     */
    public PersistenceContextInjectorService(
        final AnnotationInstance annotation,
        final ServiceName puServiceName,
        final DeploymentUnit deploymentUnit,
        final String scopedPuName,
        final String injectionTypeName) {

        AnnotationValue value;
        value = annotation.value("type");
        this.type = (value == null || PersistenceContextType.TRANSACTION.name().equals(value.asString()))
            ? PersistenceContextType.TRANSACTION: PersistenceContextType.EXTENDED;
        value = annotation.value("properties");
        AnnotationInstance[] props = value != null ? value.asNestedArray() : null;
        Map properties;

        if (props != null) {
            properties = new HashMap();
            for(int source=0; source < props.length; source ++) {
                properties.put(props[source].value("name"), props[source].value("value"));
            }
        }
        else {
            properties = null;
        }
        injectable = new PersistenceContextJndiInjectable(puServiceName, deploymentUnit, this.type, properties, scopedPuName, injectionTypeName);
    }

    @Override
    public void start(StartContext context) throws StartException {

    }

    @Override
    public void stop(StopContext context) {

    }

    @Override
    public ManagedReferenceFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return injectable;
    }

    private static final class PersistenceContextJndiInjectable implements ManagedReferenceFactory {

        private final ServiceName puServiceName;
        private final DeploymentUnit deploymentUnit;
        private final PersistenceContextType type;
        private final Map properties;
        private final String unitName;
        private final String injectionTypeName;

        private static final String ENTITY_MANAGER_CLASS = "javax.persistence.EntityManager";

        public PersistenceContextJndiInjectable(
            final ServiceName puServiceName,
            final DeploymentUnit deploymentUnit,
            final PersistenceContextType type,
            final Map properties,
            final String unitName,
            final String injectionTypeName) {

            this.puServiceName = puServiceName;
            this.deploymentUnit = deploymentUnit;
            this.type = type;
            this.properties = properties;
            this.unitName = unitName;
            this.injectionTypeName = injectionTypeName;
        }

        @Override
        public ManagedReference getReference() {
            PersistenceUnitService service = (PersistenceUnitService)deploymentUnit.getServiceRegistry().getRequiredService(puServiceName).getValue();
            EntityManagerFactory emf = service.getEntityManagerFactory();
            EntityManager entityManager;

            if (type.equals(PersistenceContextType.TRANSACTION)) {
                entityManager = new TransactionScopedEntityManager(unitName, properties, emf);
            }
            else {
                EntityManager entityManager1 = SFSBCallStack.findPersistenceContext(unitName);
                if (entityManager1 == null) {
                    entityManager1 = emf.createEntityManager(properties);
                    entityManager = new ExtendedEntityManager(entityManager1);
                    // register the XPC for inheritance by others
                    SFSBXPCMap.RegisterPersistenceContext(entityManager);
                }
                else {
                    entityManager = entityManager1;
                }

            }

            if (! ENTITY_MANAGER_CLASS.equals(injectionTypeName)) { // inject non-standard wrapped class (e.g. org.hibernate.Session)
                Class extensionClass;
                try {
                    extensionClass = this.getClass().getClassLoader().loadClass(injectionTypeName);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("couldn't load " + injectionTypeName + " from JPA modules classloader", e);
                }
                Object targetValueToInject = entityManager.unwrap(extensionClass);
                new ValueManagedReference(new ImmediateValue<Object>(targetValueToInject));
            }

            return new ValueManagedReference(new ImmediateValue<Object>(entityManager));
        }

    }
}
