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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.msc.service.ServiceName;

/**
 * User: jpai
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public interface EJB3SubsystemModel {
    String LITE = "lite";
    String ABSTACT_TYPE = "abstract-type";
    String ABSTACT_TYPE_AUTHORITY = "abstract-type-authority";
    String ALIASES = "aliases";
    String ATTRIBUTES = "attributes";

    String ASYNC = "async";
    String ALLOW_EJB_NAME_REGEX = "allow-ejb-name-regex";

    String IIOP = "iiop";

    String CONNECTOR_REF = "connector-ref";
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
    String DISCOVERY = "discovery";
    String STATIC = "static";
    String LOG_SYSTEM_EXCEPTIONS = "log-system-exceptions";

    String ENABLE_STATISTICS = "enable-statistics";
    String STATISTICS_ENABLED = "statistics-enabled";

    String FILE_DATA_STORE = "file-data-store";

    String MAX_POOL_SIZE = "max-pool-size";
    String DERIVE_SIZE = "derive-size";
    String DERIVED_SIZE = "derived-size";

    String STRICT_MAX_BEAN_INSTANCE_POOL = "strict-max-bean-instance-pool";

    String MAX_THREADS = "max-threads";
    String KEEPALIVE_TIME = "keepalive-time";

    String RELATIVE_TO = "relative-to";
    String PATH = "path";

    String DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT = "default-singleton-bean-access-timeout";
    String DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT = "default-stateful-bean-access-timeout";
    String DEFAULT_DATA_STORE = "default-data-store";

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

    String TIMER = "timer";
    String TIMER_SERVICE = "timer-service";
    String THREAD_POOL = "thread-pool";
    String THREAD_POOL_NAME = "thread-pool-name";
    String DEFAULT = "default";

    String USE_QUALIFIED_NAME = "use-qualified-name";
    String ENABLE_BY_DEFAULT = "enable-by-default";

    String CACHE = "cache";
    String PASSIVATION_STORE = "passivation-store";

    String MDB_DELIVERY_GROUP="mdb-delivery-group";
    String MDB_DELVIERY_GROUP_ACTIVE = "active";

    @Deprecated String FILE_PASSIVATION_STORE = "file-passivation-store";
    @Deprecated String IDLE_TIMEOUT = "idle-timeout";
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

    String STATIC_URLS = "static-urls";

    PathElement REMOTE_SERVICE_PATH = PathElement.pathElement(SERVICE, REMOTE);
    PathElement ASYNC_SERVICE_PATH = PathElement.pathElement(SERVICE, ASYNC);
    PathElement TIMER_PATH = PathElement.pathElement(TIMER);
    PathElement TIMER_SERVICE_PATH = PathElement.pathElement(SERVICE, TIMER_SERVICE);
    PathElement THREAD_POOL_PATH = PathElement.pathElement(THREAD_POOL);
    PathElement IIOP_PATH = PathElement.pathElement(SERVICE, IIOP);
    PathElement FILE_DATA_STORE_PATH = PathElement.pathElement(FILE_DATA_STORE);
    PathElement DATABASE_DATA_STORE_PATH = PathElement.pathElement(DATABASE_DATA_STORE);

    ServiceName BASE_THREAD_POOL_SERVICE_NAME = ThreadsServices.EXECUTOR.append("ejb3");
    String EXECUTE_IN_WORKER = "execute-in-worker";

    // Elytron integration
    String APPLICATION_SECURITY_DOMAIN = "application-security-domain";
    String IDENTITY = "identity";
    String OUTFLOW_SECURITY_DOMAINS = "outflow-security-domains";
    String REFERENCING_DEPLOYMENTS = "referencing-deployments";
    String SECURITY_DOMAIN = "security-domain";
    String ENABLE_JACC = "enable-jacc";

    PathElement IDENTITY_PATH = PathElement.pathElement(SERVICE, IDENTITY);
}
