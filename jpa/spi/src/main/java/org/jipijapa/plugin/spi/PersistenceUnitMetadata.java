/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.jboss.jandex.Index;

/**
 * Represents the persistence unit definition
 *
 * @author Scott Marlow
 */
public interface PersistenceUnitMetadata extends PersistenceUnitInfo {
    void setPersistenceUnitName(String name);

    void setScopedPersistenceUnitName(String scopedName);

    String getScopedPersistenceUnitName();

    void setContainingModuleName(ArrayList<String> getContainingModuleName);
    ArrayList<String> getContainingModuleName();

    void setPersistenceProviderClassName(String provider);

    void setJtaDataSource(DataSource jtaDataSource);

    void setNonJtaDataSource(DataSource nonJtaDataSource);

    void setJtaDataSourceName(String jtaDatasource);

    String getJtaDataSourceName();

    void setNonJtaDataSourceName(String nonJtaDatasource);

    String getNonJtaDataSourceName();

    void setPersistenceUnitRootUrl(URL persistenceUnitRootUrl);

    void setAnnotationIndex(Map<URL, Index> indexes);

    Map<URL, Index> getAnnotationIndex();

    void setManagedClassNames(List<String> classes);

    void setExcludeUnlistedClasses(boolean excludeUnlistedClasses);

    void setTransactionType(PersistenceUnitTransactionType transactionType);

    void setMappingFiles(List<String> mappingFiles);

    void setJarFileUrls(List<URL> jarFilesUrls);

    List<String> getJarFiles();

    void setJarFiles(List<String> jarFiles);

    void setValidationMode(ValidationMode validationMode);

    void setProperties(Properties props);

    void setPersistenceXMLSchemaVersion(String version);

    void setClassLoader(ClassLoader cl);

    void setTempClassLoaderFactory(TempClassLoaderFactory tempClassLoaderFactory);

    /**
     * Cache a (new, on first use) temp classloader and return it for all subsequent calls.
     * The cached temp classloader is only to be reused by the caller, at the per persistence unit level.
     * @return the cached temp classloader
     */
    ClassLoader cacheTempClassLoader();

    void setSharedCacheMode(SharedCacheMode sharedCacheMode);

    List<ClassTransformer> getTransformers();

    boolean needsJPADelegatingClassFileTransformer();

}
