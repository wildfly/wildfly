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
import org.jboss.as.jpa.container.TransactionScopedEntityManager;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.Type;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceProperty;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the PersistenceContext injected into a component.
 *
 * @author Scott Marlow
 */
public class PersistenceContextInjectorService implements Service<ManagedReferenceFactory>{

    private final String name;
    private final String unitName;
    private final String scopedPuName;
    private final PersistenceContextType type;
    private final Map properties;
    private final ServiceName puServiceName;
    private final PersistenceContextJndiInjectable injectable;

    public PersistenceContextInjectorService(final AnnotationInstance annotation,
                                          final ServiceName puServiceName,
                                          final DeploymentUnit deploymentUnit,
                                          final String scopedPuName) {
        AnnotationValue value = annotation.value("name");
        this.name = value != null ? value.asString(): null;
        value = annotation.value("unitName");
        this.unitName = value != null ? value.asString(): null;
        this.scopedPuName = scopedPuName;
        value = annotation.value("type");
        String type = value != null ? value.asEnum(): null;
        this.type = (value == null || PersistenceContextType.TRANSACTION.name().equals(value.asString()))
            ? PersistenceContextType.TRANSACTION: PersistenceContextType.EXTENDED;
        value = annotation.value("properties");
        AnnotationInstance[] props = value != null ? value.asNestedArray() : null;

        if (props != null) {
            this.properties = new HashMap();
            for(int source=0; source < props.length; source ++) {
                this.properties.put(props[source].value("name"), props[source].value("value"));
            }
        }
        else {
            this.properties = null;
        }
        this.puServiceName = puServiceName;
        injectable = new PersistenceContextJndiInjectable(puServiceName, deploymentUnit, this.type, this.properties, this.scopedPuName);
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

        public PersistenceContextJndiInjectable(final ServiceName puServiceName,
                                                final DeploymentUnit deploymentUnit,
                                                final PersistenceContextType type,
                                                final Map properties,
                                                final String unitName) {
            this.puServiceName = puServiceName;
            this.deploymentUnit = deploymentUnit;
            this.type = type;
            this.properties = properties;
            this.unitName = unitName;
        }

        @Override
        public ManagedReference getReference() {
            PersistenceUnitService service = (PersistenceUnitService)deploymentUnit.getServiceRegistry().getRequiredService(puServiceName).getValue();
            EntityManagerFactory emf = service.getEntityManagerFactory();
            if (type.equals(PersistenceContextType.TRANSACTION)) {
                return new ValueManagedReference(new ImmediateValue<Object>(new TransactionScopedEntityManager(unitName, properties, emf)));
            }
            else {
                // TODO: handle XPC search/inherit/create
                return new ValueManagedReference(new ImmediateValue<Object>(new ExtendedEntityManager(null)));
            }
        }
    }
}
