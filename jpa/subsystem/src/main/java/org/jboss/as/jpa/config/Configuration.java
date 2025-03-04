/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.config;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;

import org.jipijapa.plugin.spi.PersistenceUnitMetadata;


/**
 * configuration properties that may appear in persistence.xml
 *
 * @author Scott Marlow
 */
public class Configuration {
    /**
     * name of the AS module that contains the persistence provider
     */
    public static final String PROVIDER_MODULE = "jboss.as.jpa.providerModule";

    /**
     * Hibernate main module (default) persistence provider
     */
    public static final String PROVIDER_MODULE_HIBERNATE = "org.hibernate";

    /**
     * Hibernate 4.1.x persistence provider, note that Hibernate 4.1.x is expected to be in the 4.1 slot
     */
    public static final String PROVIDER_MODULE_HIBERNATE4_1 = "org.hibernate:4.1";

    /**
     * Hibernate OGM persistence provider
     */
    public static final String PROVIDER_MODULE_HIBERNATE_OGM = "org.hibernate.ogm";

    public static final String PROVIDER_MODULE_ECLIPSELINK = "org.eclipse.persistence";

    public static final String PROVIDER_MODULE_TOPLINK = "oracle.toplink";

    public static final String PROVIDER_MODULE_DATANUCLEUS = "org.datanucleus";

    public static final String PROVIDER_MODULE_DATANUCLEUS_GAE = "org.datanucleus:appengine";

    public static final String PROVIDER_MODULE_OPENJPA = "org.apache.openjpa";

    /**
     * default if no PROVIDER_MODULE is specified.
     */
    public static final String PROVIDER_MODULE_DEFAULT = PROVIDER_MODULE_HIBERNATE;

    /**
     * Hibernate 4.1.x persistence provider class
     */
    public static final String PROVIDER_CLASS_HIBERNATE4_1 = "org.hibernate.ejb.HibernatePersistence";

    /**
     * Hibernate 4.3.x persistence provider class
     */
    public static final String PROVIDER_CLASS_HIBERNATE = "org.hibernate.jpa.HibernatePersistenceProvider";

    /**
     * Hibernate OGM persistence provider class
     */
    public static final String PROVIDER_CLASS_HIBERNATE_OGM = "org.hibernate.ogm.jpa.HibernateOgmPersistence";

    /**
     * TopLink provider class names
     */
    public static final String PROVIDER_CLASS_TOPLINK_ESSENTIALS = "oracle.toplink.essentials.PersistenceProvider";

    public static final String PROVIDER_CLASS_TOPLINK = "oracle.toplink.essentials.ejb.cmp3.EntityManagerFactoryProvider";

    /**
     * EclipseLink provider class name
     */
    public static final String PROVIDER_CLASS_ECLIPSELINK = "org.eclipse.persistence.jpa.PersistenceProvider";

    /**
     * DataNucleus provider
     */
    public static final String PROVIDER_CLASS_DATANUCLEUS = "org.datanucleus.api.jpa.PersistenceProviderImpl";

    /**
     * DataNucleus provider GAE
     */
    public static final String PROVIDER_CLASS_DATANUCLEUS_GAE = "org.datanucleus.store.appengine.jpa.DatastorePersistenceProvider";

    public static final String PROVIDER_CLASS_OPENJPA = "org.apache.openjpa.persistence.PersistenceProviderImpl";
    /**
     * default provider class
     */
    public static final String PROVIDER_CLASS_DEFAULT = PROVIDER_CLASS_HIBERNATE;

    /**
     * if the PROVIDER_MODULE is this value, it is expected that the application has its own provider
     * in the deployment.
     */
    public static final String PROVIDER_MODULE_APPLICATION_SUPPLIED = "application";

    public static final String ADAPTER_MODULE_OPENJPA = "org.jboss.as.jpa.openjpa";

    /**
     * name of the AS module that contains the persistence provider adapter
     */
    public static final String ADAPTER_MODULE = "jboss.as.jpa.adapterModule";

    /**
     * defaults to true, if changed to false (in the persistence.xml),
     * the Jakarta Persistence container will not start the persistence unit service.
     */
    public static final String JPA_CONTAINER_MANAGED = "jboss.as.jpa.managed";

    public static final String JPA_DEFAULT_PERSISTENCE_UNIT = "wildfly.jpa.default-unit";

    /**
     * defaults to true, if false, persistence unit will not support jakarta.persistence.spi.ClassTransformer Interface
     * which means no application class rewriting
     */
    public static final String JPA_CONTAINER_CLASS_TRANSFORMER = "jboss.as.jpa.classtransformer";

    /**
     * set to false to force a single phase persistence unit bootstrap to be used (default is true
     * which uses two phases to start the persistence unit).
     */
    public static final String JPA_ALLOW_TWO_PHASE_BOOTSTRAP = "wildfly.jpa.twophasebootstrap";

    private static final String JPA_ALLOW_APPLICATION_DEFINED_DATASOURCE = "wildfly.jpa.applicationdatasource";

    /**
     * set to false to ignore default data source (defaults to true)
     */
    private static final String JPA_ALLOW_DEFAULT_DATA_SOURCE_USE = "wildfly.jpa.allowdefaultdatasourceuse";

    /**
     * set to true to defer detaching entities until persistence context is closed (WFLY-3674)
     */
    private static final String JPA_DEFER_DETACH = "jboss.as.jpa.deferdetach";

    /**
     * set to true to defer detaching query results until persistence context is closed (WFLY-12674)
     */
    private static final String JPA_SKIP_QUERY_DETACH = "wildfly.jpa.skipquerydetach";

    /**
     * unique name for the persistence unit that is unique across all deployments (
     * defaults to include the application name prepended to the persistence unit name)
     */
    private static final String JPA_SCOPED_PERSISTENCE_UNIT_NAME = "jboss.as.jpa.scopedname";

    /**
     * name of the persistence provider adapter class
     */
    public static final String ADAPTER_CLASS = "jboss.as.jpa.adapterClass";

    public static final String ALLOWJOINEDUNSYNCPC = "wildfly.jpa.allowjoinedunsync";

    public static final String SKIPMIXEDSYNCTYPECHECKING = "wildfly.jpa.skipmixedsynctypechecking";

    /**
     * Document properties that allow Jakarta Persistence apps to disable WildFly Jakarta Transactions platform/2lc integration for Hibernate ORM 5.3+ (WFLY-10433)
     * public static final String CONTROLJTAINTEGRATION = "wildfly.jpa.jtaplatform";
     * public static final String CONTROL2LCINTEGRATION = "wildfly.jpa.regionfactory";
     */

    /**
     * name of the Hibernate Search module name configuration setting in persistence unit definition
     */
    public static final String HIBERNATE_SEARCH_MODULE = "wildfly.jpa.hibernate.search.module";

    /**
     * name of the Hibernate Search integrator adaptor module
     */
    public static final String HIBERNATE_SEARCH_INTEGRATOR_ADAPTOR_MODULE_NAME = "org.hibernate.search.jipijapa-hibernatesearch";

    /**
     * name of the Hibernate Search module providing the ORM mapper
     */
    public static final String HIBERNATE_SEARCH_MODULE_MAPPER_ORM = "org.hibernate.search.mapper.orm";

    /**
     * name of the Hibernate Search module providing the outbox-polling coordination strategy
     */
    public static final String HIBERNATE_SEARCH_MODULE_MAPPER_ORM_OUTBOXPOLLING = "org.hibernate.search.mapper.orm.outboxpolling";

    /**
     * name of the Hibernate Search module providing the Lucene backend
     */
    public static final String HIBERNATE_SEARCH_MODULE_BACKEND_LUCENE = "org.hibernate.search.backend.lucene";

    /**
     * name of the Hibernate Search module providing the Elasticsearch backend
     */
    public static final String HIBERNATE_SEARCH_MODULE_BACKEND_ELASTICSEARCH = "org.hibernate.search.backend.elasticsearch";

    /**
     * name of the Hibernate Search configuration property allowing to set the backend type
     */
    public static final String HIBERNATE_SEARCH_BACKEND_TYPE = "hibernate.search.backend.type";

    /**
     * The value of the {@link #HIBERNATE_SEARCH_BACKEND_TYPE} property that identifies the use of a Lucene backend.
     */
    public static final String HIBERNATE_SEARCH_BACKEND_TYPE_VALUE_LUCENE = "lucene";

    /**
     * The value of the {@link #HIBERNATE_SEARCH_BACKEND_TYPE} property that identifies the use of an Elasticsearch backend.
     */
    public static final String HIBERNATE_SEARCH_BACKEND_TYPE_VALUE_ELASTICSEARCH = "elasticsearch";

    /**
     * Name of the Hibernate Search configuration property allowing to set the coordination strategy
     */
    public static final String HIBERNATE_SEARCH_COORDINATION_STRATEGY = "hibernate.search.coordination.strategy";

    /**
     * The value of the {@link #HIBERNATE_SEARCH_COORDINATION_STRATEGY} property that identifies the use of an Elasticsearch backend.
     */
    public static final String HIBERNATE_SEARCH_COORDINATION_STRATEGY_VALUE_OUTBOX_POLLING = "outbox-polling";

    private static final String EE_DEFAULT_DATASOURCE = "java:comp/DefaultDataSource";
    // key = provider class name, value = module name
    private static final Map<String, String> providerClassToModuleName = new HashMap<String, String>();

    static {
        // always choose the default hibernate version for the Hibernate provider class mapping
        // if the user wants a different version. they can specify the provider module name
        providerClassToModuleName.put(PROVIDER_CLASS_HIBERNATE, PROVIDER_MODULE_HIBERNATE);
        // WFLY-2136/HHH-8543 to make migration to Hibernate 4.3.x easier, we also map the (now)
        // deprecated PROVIDER_CLASS_HIBERNATE4_1 to the org.hibernate:main module
        // when PROVIDER_CLASS_HIBERNATE4_1 is no longer in a future Hibernate version (5.x?)
        // we can map PROVIDER_CLASS_HIBERNATE4_1 to org.hibernate:4.3 at that time.
        // persistence units can set "jboss.as.jpa.providerModule=org.hibernate:4.1" to use Hibernate 4.1.x/4.2.x
        providerClassToModuleName.put(PROVIDER_CLASS_HIBERNATE4_1, PROVIDER_MODULE_HIBERNATE);
        providerClassToModuleName.put(PROVIDER_CLASS_HIBERNATE_OGM, PROVIDER_MODULE_HIBERNATE_OGM);
        providerClassToModuleName.put(PROVIDER_CLASS_TOPLINK_ESSENTIALS, PROVIDER_MODULE_TOPLINK);
        providerClassToModuleName.put(PROVIDER_CLASS_TOPLINK, PROVIDER_MODULE_TOPLINK);
        providerClassToModuleName.put(PROVIDER_CLASS_ECLIPSELINK, PROVIDER_MODULE_ECLIPSELINK);
        providerClassToModuleName.put(PROVIDER_CLASS_DATANUCLEUS, PROVIDER_MODULE_DATANUCLEUS);
        providerClassToModuleName.put(PROVIDER_CLASS_DATANUCLEUS_GAE, PROVIDER_MODULE_DATANUCLEUS_GAE);
        providerClassToModuleName.put(PROVIDER_CLASS_OPENJPA, PROVIDER_MODULE_OPENJPA);
    }

    /**
     * Get the provider module name for the specified provider class.
     *
     * @param providerClassName the PU class name
     * @return provider module name or null if not known
     */
    public static String getProviderModuleNameFromProviderClassName(final String providerClassName) {
        return providerClassToModuleName.get(providerClassName);
    }

    /**
     * Determine if class file transformer is needed for the specified persistence unit
     *
     * for all persistence providers, the transformer is assumed to be needed unless the application indicates otherwise.
     *
     * @param pu the PU
     * @return true if class file transformer support is needed for pu
     */
    public static boolean needClassFileTransformer(PersistenceUnitMetadata pu) {

        if (pu.getProperties().containsKey(Configuration.JPA_CONTAINER_CLASS_TRANSFORMER)) {
            return Boolean.parseBoolean(pu.getProperties().getProperty(Configuration.JPA_CONTAINER_CLASS_TRANSFORMER));
        }
        return true;
    }

    // key = provider class name, value = adapter module name
    private static final Map<String, String> providerClassToAdapterModuleName = new HashMap<String, String>();

    static {
        providerClassToAdapterModuleName.put(PROVIDER_CLASS_OPENJPA, ADAPTER_MODULE_OPENJPA);
    }

    public static String getProviderAdapterModuleNameFromProviderClassName(final String providerClassName) {
        return providerClassToAdapterModuleName.get(providerClassName);
    }

    public static String getDefaultProviderModuleName() {
        return PROVIDER_MODULE_DEFAULT;
    }

    /**
     * Determine if two phase persistence unit start is allowed
     *
     * @param pu
     * @return
     */
    public static boolean allowTwoPhaseBootstrap(PersistenceUnitMetadata pu) {
        boolean result = true;

        if (EE_DEFAULT_DATASOURCE.equals(pu.getJtaDataSourceName())) {
            result = false;
        }
        if (pu.getProperties().containsKey(Configuration.JPA_ALLOW_TWO_PHASE_BOOTSTRAP)) {
            result = Boolean.parseBoolean(pu.getProperties().getProperty(Configuration.JPA_ALLOW_TWO_PHASE_BOOTSTRAP));
        }
        return result;
    }

    /**
     * Determine if persistence unit can use application defined DataSource (e.g. DataSourceDefinition or resource ref).
     *
     * @param pu
     * @return true if application defined DataSource can be used, false (default) if not.
     */
    public static boolean allowApplicationDefinedDatasource(PersistenceUnitMetadata pu) {
        boolean result = false;
        if (pu.getProperties().containsKey(Configuration.JPA_ALLOW_APPLICATION_DEFINED_DATASOURCE)) {
            result = Boolean.parseBoolean(pu.getProperties().getProperty(Configuration.JPA_ALLOW_APPLICATION_DEFINED_DATASOURCE));
        }
        return result;
    }

    /**
     * Determine if the default data-source should be used
     *
     * @param pu
     * @return true if the default data-source should be used
     */
    public static boolean allowDefaultDataSourceUse(PersistenceUnitMetadata pu) {
        boolean result = true;
        if (pu.getProperties().containsKey(Configuration.JPA_ALLOW_DEFAULT_DATA_SOURCE_USE)) {
            result = Boolean.parseBoolean(pu.getProperties().getProperty(Configuration.JPA_ALLOW_DEFAULT_DATA_SOURCE_USE));
        }
        return result;
    }

    /**
     * Return true if detaching of managed entities should be deferred until the entity manager is closed.
     * Note:  only applies to transaction scoped entity managers used without an active Jakarta Transactions transaction.
     *
     * @param properties
     * @return
     */
    public static boolean deferEntityDetachUntilClose(final Map<String, Object> properties) {
        boolean result = false;
        if ( properties.containsKey(JPA_DEFER_DETACH))
            result = Boolean.parseBoolean((String)properties.get(JPA_DEFER_DETACH));
        return result;
    }

    /**
     * Return true if detaching of query results (entities) should be deferred until the entity manager is closed.
     * Note:  only applies to transaction scoped entity managers used without an active Jakarta Transactions transaction.
     *
     * @param properties
     * @return
     */
    public static boolean skipQueryDetach(final Map<String, Object> properties) {
        boolean result = false;
        if ( properties.containsKey(JPA_SKIP_QUERY_DETACH))
            result = Boolean.parseBoolean((String)properties.get(JPA_SKIP_QUERY_DETACH));
        return result;
    }

    public static String getScopedPersistenceUnitName(PersistenceUnitMetadata pu) {

        Object name = pu.getProperties().get(JPA_SCOPED_PERSISTENCE_UNIT_NAME);
        if (name instanceof String) {
            return (String)name;
        }
        return null;
    }

    /**
     * Allow the mixed synchronization checking to be skipped for backward compatibility with WildFly 10.1.0
     *
     *
     * @param emf
     * @param targetEntityManagerProperties
     * @return
     */
    public static boolean skipMixedSynchronizationTypeCheck(EntityManagerFactory emf, Map targetEntityManagerProperties) {
        boolean result = false;
        // EntityManager properties will take priority over persistence.xml level (emf) properties
        if(targetEntityManagerProperties != null && targetEntityManagerProperties.containsKey(SKIPMIXEDSYNCTYPECHECKING)) {
            result = Boolean.parseBoolean((String) targetEntityManagerProperties.get(SKIPMIXEDSYNCTYPECHECKING));
        }
        else if(emf.getProperties() != null && emf.getProperties().containsKey(SKIPMIXEDSYNCTYPECHECKING)) {
            result = Boolean.parseBoolean((String) emf.getProperties().get(SKIPMIXEDSYNCTYPECHECKING));
        }
        return result;
    }

    /**
     * Allow an unsynchronized persistence context that is joined to the transaction, be treated the same as a synchronized
     * persistence context, with respect to the checking for mixed unsync/sync types.
     *
     *
     * @param emf
     * @param targetEntityManagerProperties
     * @return
     */
    public static boolean allowJoinedUnsyncPersistenceContext(EntityManagerFactory emf, Map targetEntityManagerProperties) {
        boolean result = false;
        // EntityManager properties will take priority over persistence.xml (emf) properties
        if(targetEntityManagerProperties != null && targetEntityManagerProperties.containsKey(ALLOWJOINEDUNSYNCPC)) {
            result = Boolean.parseBoolean((String) targetEntityManagerProperties.get(ALLOWJOINEDUNSYNCPC));
        }
        else if(emf.getProperties() != null && emf.getProperties().containsKey(ALLOWJOINEDUNSYNCPC)) {
            result = Boolean.parseBoolean((String) emf.getProperties().get(ALLOWJOINEDUNSYNCPC));
        }
        return result;
    }
}
