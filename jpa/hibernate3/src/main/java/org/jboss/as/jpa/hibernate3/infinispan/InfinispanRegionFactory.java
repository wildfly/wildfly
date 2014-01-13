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
package org.jboss.as.jpa.hibernate3.infinispan;

import java.net.URL;
import java.security.AccessController;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Environment;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.jpa.hibernate3.HibernateSecondLevelCache;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.jpa.spi.TempClassLoaderFactory;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.jandex.Index;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;

/**
 * Infinispan-backed region factory for use with standalone (i.e. non-JPA) Hibernate applications.
 * @author Paul Ferraro
 */
public class InfinispanRegionFactory extends org.hibernate.cache.infinispan.InfinispanRegionFactory {

    public static final String CACHE_CONTAINER = "hibernate.cache.infinispan.container";
    public static final String DEFAULT_CACHE_CONTAINER = "hibernate";

    private volatile ServiceName serviceName;

    public InfinispanRegionFactory() {
        super();
    }

    public InfinispanRegionFactory(Properties props) {
        super(props);
    }

    @Override
    protected EmbeddedCacheManager createCacheManager(Properties properties) throws CacheException {
        // Find a suitable service name to represent this session factory instance
        String name = properties.getProperty(Environment.SESSION_FACTORY_NAME);
        this.serviceName = ServiceName.JBOSS.append(DEFAULT_CACHE_CONTAINER, (name != null) ? name : UUID.randomUUID().toString());
        String container = properties.getProperty(CACHE_CONTAINER, DEFAULT_CACHE_CONTAINER);
        ServiceContainer target = currentServiceContainer();
        InjectedValue<EmbeddedCacheManager> manager = new InjectedValue<EmbeddedCacheManager>();
        ServiceBuilder<EmbeddedCacheManager> builder = target.addService(this.serviceName, new ValueService<EmbeddedCacheManager>(manager))
                .addDependency(EmbeddedCacheManagerService.getServiceName(container), EmbeddedCacheManager.class, manager)
                .setInitialMode(ServiceController.Mode.ACTIVE)
        ;
        HibernateSecondLevelCache.addSecondLevelCacheDependencies(target, target, builder, new HibernateMetaData(properties));
        try {
            return ServiceContainerHelper.getValue(builder.install());
        } catch (StartException e) {
            throw new CacheException(e);
        }
    }

    @Override
    public void stop() {
        // Remove the service created in createCacheManager(...)
        ServiceContainerHelper.remove(currentServiceContainer().getRequiredService(this.serviceName));
    }

    private static class HibernateMetaData implements PersistenceUnitMetadata {
        private final Properties properties;

        HibernateMetaData(Properties properties) {
            this.properties = properties;
        }

        @Override
        public void addTransformer(ClassTransformer arg0) {
        }

        @Override
        public boolean excludeUnlistedClasses() {
            return false;
        }

        @Override
        public ClassLoader getClassLoader() {
            return null;
        }

        @Override
        public List<URL> getJarFileUrls() {
            return null;
        }

        @Override
        public DataSource getJtaDataSource() {
            return null;
        }

        @Override
        public List<String> getManagedClassNames() {
            return null;
        }

        @Override
        public List<String> getMappingFileNames() {
            return null;
        }

        @Override
        public ClassLoader getNewTempClassLoader() {
            return null;
        }

        @Override
        public DataSource getNonJtaDataSource() {
            return null;
        }

        @Override
        public String getPersistenceProviderClassName() {
            return null;
        }

        @Override
        public String getPersistenceUnitName() {
            return null;
        }

        @Override
        public URL getPersistenceUnitRootUrl() {
            return null;
        }

        @Override
        public String getPersistenceXMLSchemaVersion() {
            return null;
        }

        @Override
        public Properties getProperties() {
            return this.properties;
        }

        @Override
        public SharedCacheMode getSharedCacheMode() {
            return null;
        }

        @Override
        public PersistenceUnitTransactionType getTransactionType() {
            return null;
        }

        @Override
        public ValidationMode getValidationMode() {
            return null;
        }

        @Override
        public void setPersistenceUnitName(String name) {
        }

        @Override
        public void setScopedPersistenceUnitName(String scopedName) {
        }

        @Override
        public String getScopedPersistenceUnitName() {
            return null;
        }

        @Override
        public void setPersistenceProviderClassName(String provider) {
        }

        @Override
        public void setJtaDataSource(DataSource jtaDataSource) {
        }

        @Override
        public void setNonJtaDataSource(DataSource nonJtaDataSource) {
        }

        @Override
        public void setJtaDataSourceName(String jtaDatasource) {
        }

        @Override
        public String getJtaDataSourceName() {
            return null;
        }

        @Override
        public void setNonJtaDataSourceName(String nonJtaDatasource) {
        }

        @Override
        public String getNonJtaDataSourceName() {
            return null;
        }

        @Override
        public void setPersistenceUnitRootUrl(URL persistenceUnitRootUrl) {
        }

        @Override
        public void setAnnotationIndex(Map<URL, Index> indexes) {
        }

        @Override
        public Map<URL, Index> getAnnotationIndex() {
            return null;
        }

        @Override
        public void setManagedClassNames(List<String> classes) {
        }

        @Override
        public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
        }

        @Override
        public void setTransactionType(PersistenceUnitTransactionType transactionType) {
        }

        @Override
        public void setMappingFiles(List<String> mappingFiles) {
        }

        @Override
        public void setJarFileUrls(List<URL> jarFilesUrls) {
        }

        @Override
        public List<String> getJarFiles() {
            return null;
        }

        @Override
        public void setJarFiles(List<String> jarFiles) {
        }

        @Override
        public void setValidationMode(ValidationMode validationMode) {
        }

        @Override
        public void setProperties(Properties props) {
        }

        @Override
        public void setPersistenceXMLSchemaVersion(String version) {
        }

        @Override
        public void setClassLoader(ClassLoader cl) {
        }

        @Override
        public void setTempClassLoaderFactory(TempClassLoaderFactory tempClassLoaderFactory) {
        }

        @Override
        public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
        }

        @Override
        public List<ClassTransformer> getTransformers() {
            return null;
        }
    }


    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
