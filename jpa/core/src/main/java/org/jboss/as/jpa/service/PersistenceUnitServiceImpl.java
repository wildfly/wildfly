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

import static org.jboss.as.jpa.JpaLogger.JPA_LOGGER;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;

import org.jboss.as.jpa.classloader.TempClassLoaderFactoryImpl;
import org.jboss.as.jpa.container.SFSBXPCMap;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.jpa.spi.PersistenceUnitService;
import org.jboss.as.jpa.util.JPAServiceNames;
import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Persistence Unit service that is created for each deployed persistence unit that will be referenced by the
 * persistence context/unit injector.
 * <p/>
 * The persistence unit scoped
 *
 * @author Scott Marlow
 */
public class PersistenceUnitServiceImpl implements Service<PersistenceUnitServiceImpl>, PersistenceUnitService {


    private final InjectedValue<Map> properties = new InjectedValue<Map>();

    private final InjectedValue<DataSource> jtaDataSource = new InjectedValue<DataSource>();
    private final InjectedValue<DataSource> nonJtaDataSource = new InjectedValue<DataSource>();

    private final PersistenceProviderAdaptor persistenceProviderAdaptor;
    private final PersistenceProvider persistenceProvider;
    private final PersistenceUnitMetadata pu;
    private final ClassLoader classLoader;

    private volatile EntityManagerFactory entityManagerFactory;

    public PersistenceUnitServiceImpl(final ClassLoader classLoader, final PersistenceUnitMetadata pu, final PersistenceProviderAdaptor persistenceProviderAdaptor, final PersistenceProvider persistenceProvider) {
        this.pu = pu;
        this.persistenceProviderAdaptor = persistenceProviderAdaptor;
        this.persistenceProvider = persistenceProvider;
        this.classLoader = classLoader;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            JPA_LOGGER.startingService("Persistence Unit", pu.getScopedPersistenceUnitName());
            pu.setTempClassLoaderFactory(new TempClassLoaderFactoryImpl(classLoader));
            pu.setJtaDataSource(jtaDataSource.getOptionalValue());
            pu.setNonJtaDataSource(nonJtaDataSource.getOptionalValue());
            WritableServiceBasedNamingStore.pushOwner(context.getController().getServiceContainer().subTarget());
            this.entityManagerFactory = createContainerEntityManagerFactory();
        } finally {
            pu.setTempClassLoaderFactory(null);    // release the temp classloader factory (only needed when creating the EMF)
            WritableServiceBasedNamingStore.popOwner();
        }
    }

    @Override
    public void stop(StopContext context) {
        JPA_LOGGER.stoppingService("Persistence Unit", pu.getScopedPersistenceUnitName());
        if (entityManagerFactory != null) {
            WritableServiceBasedNamingStore.pushOwner(context.getController().getServiceContainer().subTarget());
            try {
                entityManagerFactory.close();
            } finally {
                entityManagerFactory = null;
                pu.setTempClassLoaderFactory(null);
                WritableServiceBasedNamingStore.popOwner();
            }
        }
    }

    @Override
    public PersistenceUnitServiceImpl getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * Get the entity manager factory
     *
     * @return the entity manager factory
     */
    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    @Override
    public String getScopedPersistenceUnitName() {
        return pu.getScopedPersistenceUnitName();
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
        return JPAServiceNames.getPUServiceName(pu.getScopedPersistenceUnitName());
    }

    public static ServiceName getPUServiceName(String scopedPersistenceUnitName) {
        return JPAServiceNames.getPUServiceName(scopedPersistenceUnitName);
    }

    /**
     * Create EE container entity manager factory
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
                //This is needed if the datasource is restarted
                //pu.setTempClassLoaderFactory(null);    // close reference to temp classloader factory (only needed during call to createEntityManagerFactory)
            }
        }
    }

}
