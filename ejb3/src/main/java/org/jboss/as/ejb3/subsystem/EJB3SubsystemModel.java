/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.msc.service.ServiceName;

/**
 * @author jpai
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public interface EJB3SubsystemModel {
    @Deprecated String LITE = "lite";
    @Deprecated String ABSTACT_TYPE = "abstract-type";
    @Deprecated String ABSTACT_TYPE_AUTHORITY = "abstract-type-authority";
    String ALIASES = "aliases";
    String ATTRIBUTES = "attributes";

    String ASYNC = "async";
    String ALLOW_EJB_NAME_REGEX = "allow-ejb-name-regex";

    String IIOP = "iiop";

    String CONNECTOR_REF = "connector-ref";
    String CONNECTORS = "connectors";
    String IN_VM_REMOTE_INTERFACE_INVOCATION_PASS_BY_VALUE = "in-vm-remote-interface-invocation-pass-by-value";

    String DATASOURCE_JNDI_NAME = "datasource-jndi-name";
    String DEFAULT_DISTINCT_NAME = "default-distinct-name";
    String DEFAULT_SECURITY_DOMAIN = "default-security-domain";
    String DEFAULT_MDB_INSTANCE_POOL = "default-mdb-instance-pool";
    String DEFAULT_MISSING_METHOD_PERMISSIONS_DENY_ACCESS = "default-missing-method-permissions-deny-access";
    String DEFAULT_RESOURCE_ADAPTER_NAME = "default-resource-adapter-name";
    String DEFAULT_SFSB_CACHE = "default-sfsb-cache";
    String DEFAULT_CLUSTERED_SFSB_CACHE = "default-clustered-sfsb-cache";
    String DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE = "default-sfsb-passivation-disabled-cache";
    String DEFAULT_SLSB_INSTANCE_POOL = "default-slsb-instance-pool";
    String INSTANCE_ACQUISITION_TIMEOUT = "timeout";
    String INSTANCE_ACQUISITION_TIMEOUT_UNIT = "timeout-unit";
    String DEFAULT_ENTITY_BEAN_INSTANCE_POOL = "default-entity-bean-instance-pool";
    String DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING = "default-entity-bean-optimistic-locking";
    String DISABLE_DEFAULT_EJB_PERMISSIONS = "disable-default-ejb-permissions";
    String ENABLE_GRACEFUL_TXN_SHUTDOWN = "enable-graceful-txn-shutdown";
    @Deprecated String DISCOVERY = "discovery";
    String STATIC = "static";
    String LOG_SYSTEM_EXCEPTIONS = "log-system-exceptions";

    String ENABLE_STATISTICS = "enable-statistics";
    String STATISTICS_ENABLED = "statistics-enabled";

    String FILE_DATA_STORE = "file-data-store";

    String MAX_POOL_SIZE = "max-pool-size";
    String DERIVE_SIZE = "derive-size";
    String DERIVED_SIZE = "derived-size";

    String STRICT_MAX_BEAN_INSTANCE_POOL = "strict-max-bean-instance-pool";

    @Deprecated String MAX_THREADS = "max-threads";
    @Deprecated String KEEPALIVE_TIME = "keepalive-time";

    String RELATIVE_TO = "relative-to";
    String PATH = "path";

    String DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT = "default-singleton-bean-access-timeout";
    String DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT = "default-stateful-bean-access-timeout";
    String DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT = "default-stateful-bean-session-timeout";
    String DEFAULT_DATA_STORE = "default-data-store";
    String DEFAULT_PERSISTENT_TIMER_MANAGEMENT = "default-persistent-timer-management";
    String DEFAULT_TRANSIENT_TIMER_MANAGEMENT = "default-transient-timer-management";

    String REMOTE = "remote";
    String SERVICE = "service";
    String PROFILE = "profile";
    String REMOTING_PROFILE = "remoting-profile";
    String EXCLUDE_LOCAL_RECEIVER= "exclude-local-receiver";
    String LOCAL_RECEIVER_PASS_BY_VALUE = "local-receiver-pass-by-value";
    String REMOTING_EJB_RECEIVER = "remoting-ejb-receiver";
    String OUTBOUND_CONNECTION_REF= "outbound-connection-ref";
    String CONNECT_TIMEOUT= "connect-timeout";
    String CLIENT_MAPPINGS_CLUSTER_NAME = "cluster";
    String REMOTE_HTTP_CONNECTION = "remote-http-connection";

    String TIMER = "timer";
    String TIMER_SERVICE = "timer-service";
    String THREAD_POOL = "thread-pool";
    String THREAD_POOL_NAME = "thread-pool-name";
    @Deprecated String DEFAULT = "default";

    String USE_QUALIFIED_NAME = "use-qualified-name";
    String ENABLE_BY_DEFAULT = "enable-by-default";

    @Deprecated String CACHE = "cache";
    String SIMPLE_CACHE = "simple-cache";
    String DISTRIBUTABLE_CACHE = "distributable-cache";
    String BEAN_MANAGEMENT = "bean-management";
    @Deprecated String PASSIVATION_STORE = "passivation-store";

    String MDB_DELIVERY_GROUP="mdb-delivery-group";
    String MDB_DELIVERY_GROUP_ACTIVE = "active";

    @Deprecated String FILE_PASSIVATION_STORE = "file-passivation-store";
    String IDLE_TIMEOUT = "idle-timeout";
    @Deprecated String IDLE_TIMEOUT_UNIT = "idle-timeout-unit";
    String MAX_SIZE = "max-size";
    @Deprecated String GROUPS_PATH = "groups-path";
    @Deprecated String SESSIONS_PATH = "sessions-path";
    @Deprecated String SUBDIRECTORY_COUNT = "subdirectory-count";

    @Deprecated String CLUSTER_PASSIVATION_STORE = "cluster-passivation-store";
    String BEAN_CACHE = "bean-cache";
    String CACHE_CONTAINER = "cache-container";
    @Deprecated String CLIENT_MAPPINGS_CACHE = "client-mappings-cache";
    @Deprecated String PASSIVATE_EVENTS_ON_REPLICATE = "passivate-events-on-replicate";

    String CHANNEL_CREATION_OPTIONS = "channel-creation-options";
    String VALUE = "value";
    String TYPE = "type";

    String DATABASE = "database";
    String DATABASE_DATA_STORE = "database-data-store";
    String PARTITION  = "partition";
    String REFRESH_INTERVAL = "refresh-interval";
    String ALLOW_EXECUTION = "allow-execution";

    @Deprecated String STATIC_URLS = "static-urls";

    PathElement REMOTE_SERVICE_PATH = PathElement.pathElement(SERVICE, REMOTE);
    PathElement ASYNC_SERVICE_PATH = PathElement.pathElement(SERVICE, ASYNC);
    PathElement TIMER_PATH = PathElement.pathElement(TIMER);
    PathElement TIMER_SERVICE_PATH = PathElement.pathElement(SERVICE, TIMER_SERVICE);
    @Deprecated PathElement THREAD_POOL_PATH = PathElement.pathElement(THREAD_POOL);
    PathElement IIOP_PATH = PathElement.pathElement(SERVICE, IIOP);
    PathElement FILE_DATA_STORE_PATH = PathElement.pathElement(FILE_DATA_STORE);
    PathElement DATABASE_DATA_STORE_PATH = PathElement.pathElement(DATABASE_DATA_STORE);
    PathElement MDB_DELIVERY_GROUP_PATH = PathElement.pathElement(MDB_DELIVERY_GROUP);
    PathElement STRICT_MAX_BEAN_INSTANCE_POOL_PATH = PathElement.pathElement(STRICT_MAX_BEAN_INSTANCE_POOL);
    PathElement REMOTING_PROFILE_PATH = PathElement.pathElement(REMOTING_PROFILE);
    PathElement SIMPLE_CACHE_PATH = PathElement.pathElement(SIMPLE_CACHE);
    PathElement DISTRIBUTABLE_CACHE_PATH = PathElement.pathElement(DISTRIBUTABLE_CACHE);

    String BASE_EJB_THREAD_POOL_NAME = "ejb3";
    ServiceName BASE_THREAD_POOL_SERVICE_NAME = ThreadsServices.EXECUTOR.append(BASE_EJB_THREAD_POOL_NAME);
    String EXECUTE_IN_WORKER = "execute-in-worker";

    // Elytron integration
    String APPLICATION_SECURITY_DOMAIN = "application-security-domain";
    String IDENTITY = "identity";
    String OUTFLOW_SECURITY_DOMAINS = "outflow-security-domains";
    String REFERENCING_DEPLOYMENTS = "referencing-deployments";
    String SECURITY_DOMAIN = "security-domain";
    String ENABLE_JACC = "enable-jacc";
    String LEGACY_COMPLIANT_PRINCIPAL_PROPAGATION = "legacy-compliant-principal-propagation";

    PathElement IDENTITY_PATH = PathElement.pathElement(SERVICE, IDENTITY);

    //Server interceptors
    String SERVER_INTERCEPTOR = "server-interceptor";
    String SERVER_INTERCEPTORS = "server-interceptors";
    //Client interceptors
    String CLIENT_INTERCEPTOR = "client-interceptor";
    String CLIENT_INTERCEPTORS = "client-interceptors";
    String MODULE = "module";
    String CLASS = "class";
    @Deprecated String BINDING = "binding";

    String URI = "uri";

}
