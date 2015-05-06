/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jipijapa.plugin.spi;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
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

    void setSharedCacheMode(SharedCacheMode sharedCacheMode);

    List<ClassTransformer> getTransformers();
}
