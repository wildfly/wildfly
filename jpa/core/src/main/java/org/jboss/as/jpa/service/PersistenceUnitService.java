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

import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;
import java.util.Map;

/**
 * Persistence Unit service that is created for each deployed persistence unit that will be referenced by the
 * persistence context/unit injector.
 * <p/>
 * The persistence unit scoped
 *
 * @author Scott Marlow
 */
public class PersistenceUnitService implements Service<PersistenceUnitService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("persistenceunit");
    private static final Logger log = Logger.getLogger("org.jboss.jpa");

    private final InjectedValue<Map> properties = new InjectedValue<Map>();

    private final InjectedValue<DataSource> jtaDataSource = new InjectedValue<DataSource>();
    private final InjectedValue<DataSource> nonJtaDataSource = new InjectedValue<DataSource>();

    private final PersistenceProviderAdaptor persistenceProviderAdaptor;
    private final PersistenceProvider persistenceProvider;
    private final PersistenceUnitMetadata pu;

    private EntityManagerFactory entityManagerFactory;

    public PersistenceUnitService(final PersistenceUnitMetadata pu, final PersistenceProviderAdaptor persistenceProviderAdaptor, final PersistenceProvider persistenceProvider) {
        this.pu = pu;
        this.persistenceProviderAdaptor = persistenceProviderAdaptor;
        this.persistenceProvider = persistenceProvider;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            log.infof("starting Persistence Unit Service '%s' ", pu.getScopedPersistenceUnitName() );
            pu.setJtaDataSource(jtaDataSource.getOptionalValue());
            pu.setNonJtaDataSource(nonJtaDataSource.getOptionalValue());
            this.entityManagerFactory = createContainerEntityManagerFactory();

        } finally {
            pu.setTempClassloader(null);    // release the temp classloader (only needed when creating the EMF)
        }
    }

    @Override
    public void stop(StopContext context) {
        log.infof("stopping Persistence Unit Service '%s' ", pu.getScopedPersistenceUnitName() );
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
            entityManagerFactory = null;
        }
    }

    @Override
    public PersistenceUnitService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * Get the entity manager factory
     *
     * @return the entity manager factory
     */
    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public Injector<Map> getPropertiesInjector() {
        return properties;
    }

    public Injector<DataSource> getJtaDataSourceInjector() {
        return jtaDataSource;
    }

    public Injector<DataSource> getNonJtaDataSourceInjector() {
        return nonJtaDataSource;
    }

    /**
     * Returns the Persistence Unit service name used for creation or lookup.
     * The service name contains the unique fully scoped persistence unit name
     *
     * @param pu persistence unit definition
     * @return
     */
    public static ServiceName getPUServiceName(PersistenceUnitMetadata pu) {
        return PersistenceUnitService.SERVICE_NAME.append(pu.getScopedPersistenceUnitName());
    }

    public static ServiceName getPUServiceName(String scopedPersistenceUnitName) {
        return PersistenceUnitService.SERVICE_NAME.append(scopedPersistenceUnitName);
    }

    /**
     * Create EE container entity manager factory
     *
     *
     * @return EntityManagerFactory
     */
    private EntityManagerFactory createContainerEntityManagerFactory() {
        persistenceProviderAdaptor.beforeCreateContainerEntityManagerFactory(pu);
        try {
            return persistenceProvider.createContainerEntityManagerFactory(pu, properties.getValue());
        } finally {
            try {
                persistenceProviderAdaptor.afterCreateContainerEntityManagerFactory(pu);
            } finally {
                pu.setAnnotationIndex(null);    // close reference to Annotation Index (only needed during call to createContainerEntityManagerFactory)
                pu.setTempClassloader(null);    // close reference to temp classloader (only needed during call to createEntityManagerFactory)
            }
        }
    }

}
