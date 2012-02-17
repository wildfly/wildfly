// $Id$
/*
 * Copyright (c) 2011, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.jboss.as.jpa.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.jpa.spi.TempClassLoaderFactory;
import org.jboss.jandex.Index;

/**
 * Represents the persistence unit definition
 *
 * @author Scott Marlow (based on work by Bill Burke)
 */
public class PersistenceUnitMetadataImpl implements PersistenceUnitMetadata {

    // required: name of the persistent unit
    private String name;

    // required: name of the persistent unit scoped to deployment file
    private String scopedName;

    // optional: jndi name of non-jta datasource
    private String nonJtaDataSourceName;

    // optional: jndi name of jta datasource
    private String jtaDataSourceName;


    private DataSource jtaDatasource;

    private DataSource nonJtaDataSource;

    // optional: provider classname (must implement javax.persistence.spi.PersistenceProvider)
    private String provider;

    // optional: specifies if EntityManagers will be JTA (default) or RESOURCE_LOCAL
    private PersistenceUnitTransactionType transactionType;

    // optional: collection of individually named managed entity classes
    private List<String> classes = new ArrayList<String>(1);

    // optional:
    private List<String> packages = new ArrayList<String>(1);

    // optional:  collection of jar file names that contain entity classes
    private List<String> jarFiles = new ArrayList<String>(1);

    private List<URL> jarFilesUrls = new ArrayList<URL>();

    private URL persistenceUnitRootUrl;

    // optional: collection of orm.xml style entity mapping files
    private List<String> mappingFiles = new ArrayList<String>(1);

    // collection of properties for the persistence provider
    private Properties props = new Properties();

    // optional: specifies whether to include entity classes in the root folder containing the persistence unit.
    private boolean excludeUnlistedClasses;

    // optional:  validation mode can be "auto", "callback", "none".
    private ValidationMode validationMode;

    // optional: version of the JPA specification
    private String version;

    private List<ClassTransformer> transformers = new ArrayList<ClassTransformer>(1);

    private SharedCacheMode sharedCacheMode;

    private ClassLoader classloader;

    private TempClassLoaderFactory tempClassLoaderFactory;

    private Map<URL, Index> annotationIndex;

    @Override
    public void setPersistenceUnitName(String name) {
        this.name = name;
    }

    @Override
    public String getPersistenceUnitName() {
        return name;
    }

    @Override
    public void setScopedPersistenceUnitName(String scopedName) {
        this.scopedName = scopedName;
    }

    @Override
    public String getScopedPersistenceUnitName() {
        return scopedName;
    }

    @Override
    public void setPersistenceProviderClassName(String provider) {
        if (provider != null && provider.endsWith(".class")) {
            this.provider = provider.substring(0, provider.length() - 6);
        }
        this.provider = provider;
    }

    @Override
    public String getPersistenceProviderClassName() {
        return provider;
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return transactionType;
    }

    @Override
    public DataSource getJtaDataSource() {
        return jtaDatasource;
    }

    @Override
    public void setJtaDataSource(DataSource jtaDataSource) {
        this.jtaDatasource = jtaDataSource;
    }

    @Override
    public void setNonJtaDataSource(DataSource nonJtaDataSource) {
        this.nonJtaDataSource = nonJtaDataSource;
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return nonJtaDataSource;
    }

    @Override
    public void setJtaDataSourceName(String jtaDatasource) {
        this.jtaDataSourceName = jtaDatasource;
    }

    @Override
    public String getJtaDataSourceName() {
        return jtaDataSourceName;
    }

    @Override
    public void setNonJtaDataSourceName(String nonJtaDatasource) {
        this.nonJtaDataSourceName = nonJtaDatasource;
    }

    @Override
    public String getNonJtaDataSourceName() {
        return this.nonJtaDataSourceName;
    }

    @Override
    public void setPersistenceUnitRootUrl(URL persistenceUnitRootUrl) {
        this.persistenceUnitRootUrl = persistenceUnitRootUrl;
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return persistenceUnitRootUrl;
    }

    @Override
    public void setAnnotationIndex(Map<URL, Index> indexes) {
        annotationIndex = indexes;
    }

    @Override
    public Map<URL, Index> getAnnotationIndex() {
        return annotationIndex;
    }

    @Override
    public List<String> getManagedClassNames() {
        return classes;
    }

    @Override
    public void setManagedClassNames(List<String> classes) {
        this.classes = classes;
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return excludeUnlistedClasses;
    }

    @Override
    public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
        this.excludeUnlistedClasses = excludeUnlistedClasses;
    }

    @Override
    public void setTransactionType(PersistenceUnitTransactionType transactionType) {
        this.transactionType = transactionType;
    }

    @Override
    public void setMappingFiles(List<String> mappingFiles) {
        this.mappingFiles = mappingFiles;
    }

    @Override
    public List<String> getMappingFileNames() {
        return mappingFiles;
    }

    @Override
    public List<URL> getJarFileUrls() {
        return jarFilesUrls;
    }

    @Override
    public void setJarFileUrls(List<URL> jarFilesUrls) {
        this.jarFilesUrls = jarFilesUrls;
    }


    @Override
    public List<String> getJarFiles() {
        return jarFiles;
    }

    @Override
    public void setJarFiles(List<String> jarFiles) {
        this.jarFiles = jarFiles;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PersistenceUnitMetadataImpl(version=")
            .append(version)
            .append(") [\n")
            .append("\tname: ").append(name).append("\n")
            .append("\tjtaDataSource: ").append(jtaDataSourceName).append("\n")
            .append("\tnonJtaDataSource: ").append(nonJtaDataSourceName).append("\n")
            .append("\ttransactionType: ").append(transactionType).append("\n")
            .append("\tprovider: ").append(provider).append("\n")
            .append("\tclasses[\n");
        if (classes != null) {
            for (String elt : classes) {
                sb.append("\t\t").append(elt);
            }
        }
        sb.append("\t]\n")
            .append("\tpackages[\n");
        if (packages != null) {
            for (String elt : packages) {
                sb.append("\t\t").append(elt).append("\n");
            }
        }
        sb.append("\t]\n")
            .append("\tmappingFiles[\n");
        if (mappingFiles != null) {
            for (String elt : mappingFiles) {
                sb.append("\t\t").append(elt).append("\n");
            }
        }
        sb.append("\t]\n")
            .append("\tjarFiles[\n");
        if (jarFiles != null) {
            for (String elt : jarFiles) {
                sb.append("\t\t").append(elt).append("\n");
            }
        }
        sb.append("\t]\n");
        if (validationMode != null) {
            sb.append("\tvalidation-mode: ").append(validationMode).append("\n");
        }
        if (sharedCacheMode != null) {
            sb.append("\tshared-cache-mode: ").append(sharedCacheMode).append("\n");
        }

        sb.append("\tproperties[\n");

        if (props != null) {
            for (Map.Entry elt : props.entrySet()) {
                sb.append("\t\t").append(elt.getKey()).append(": ").append(elt.getValue()).append("\n");
            }
        }
        sb.append("\t]").append("]");

        return sb.toString();
    }

    @Override
    public void setValidationMode(ValidationMode validationMode) {
        this.validationMode = validationMode;
    }

    @Override
    public ValidationMode getValidationMode() {
        return validationMode;
    }


    @Override
    public void setProperties(Properties props) {
        this.props = props;
    }

    @Override
    public Properties getProperties() {
        return props;
    }

    @Override
    public void setPersistenceXMLSchemaVersion(String version) {
        this.version = version;
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return version;
    }

    @Override
    public void setClassLoader(ClassLoader cl) {
        classloader = cl;
    }

    /**
     * Return a classloader that the provider can use to load the entity classes.
     * <p/>
     * Note from JPA 8.2:
     * All persistence classes defined at the level of the Java EE EAR must be accessible to other Java EE
     * components in the application—i.e. loaded by the application classloader—such that if the same entity
     * class is referenced by two different Java EE components (which may be using different persistence
     * units), the referenced class is the same identical class.
     *
     * @return
     */
    @Override
    public ClassLoader getClassLoader() {
        return classloader;
    }

    @Override
    public List<ClassTransformer> getTransformers() {
        return transformers;
    }

    @Override
    public void addTransformer(ClassTransformer classTransformer) {
        transformers.add(classTransformer);
    }

    @Override
    public void setTempClassLoaderFactory(TempClassLoaderFactory tempClassloaderFactory) {
        this.tempClassLoaderFactory = tempClassloaderFactory;
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return tempClassLoaderFactory != null ?
            tempClassLoaderFactory.createNewTempClassLoader() : null;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return sharedCacheMode;
    }

    @Override
    public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
        this.sharedCacheMode = sharedCacheMode;
    }
}
