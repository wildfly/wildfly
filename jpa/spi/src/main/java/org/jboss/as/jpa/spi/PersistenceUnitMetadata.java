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

package org.jboss.as.jpa.spi;

import org.jboss.jandex.Index;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

    void setAnnotationIndex(Map<URL, Index> indexs);

    Map<URL, Index> getAnnotationIndex();

    void setManagedClassNames(List<String> classes);

    void setExcludeUnlistedClasses(boolean excludeUnlistedClasses);

    void setTransactionType(PersistenceUnitTransactionType transactionType);

    void setMappingFiles(List<String> mappingFiles);

    void setJarFileUrls(List<URL> jarfilesUrls);

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
