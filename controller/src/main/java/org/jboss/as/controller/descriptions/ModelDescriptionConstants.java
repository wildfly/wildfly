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
package org.jboss.as.controller.descriptions;

import org.jboss.as.controller.registry.AttributeAccess;

/**
 * String constants frequently used in model descriptions.
 *
 * @author Brian Stansberry
 */
public class ModelDescriptionConstants {

    // KEEP THESE IN ALPHABETICAL ORDER!

    /** The key for {@link AttributeAccess.AccessType} fields. */
    public static final String ACCESS_TYPE = "access-type";
    public static final String ADD = "add";
    public static final String ADDRESS = "address";
    public static final String ADMIN_ONLY = "admin-only";
    public static final String ADVANCED_FILTER = "advanced-filter";
    public static final String ALLOWED = "allowed";
    public static final String ALLOW_RESOURCE_SERVICE_RESTART = "allow-resource-service-restart";
    public static final String ALTERNATIVES = "alternatives";
    public static final String ANY = "any";
    public static final String ANY_ADDRESS = "any-address";
    public static final String ANY_IPV4_ADDRESS = "any-ipv4-address";
    public static final String ANY_IPV6_ADDRESS = "any-ipv6-address";
    public static final String APPCLIENT = "appclient";
    public static final String ARCHIVE = "archive";
    public static final String ATTRIBUTES = "attributes";
    public static final String AUTHENTICATION = "authentication";
    public static final String AUTHORIZATION = "authorization";
    public static final String AUTO_START = "auto-start";
    public static final String BASE_DN = "base-dn";
    public static final String BOOT_TIME = "boot-time";
    public static final String BYTES = "bytes";
    public static final String CALLER_TYPE = "caller-type";
    public static final String CANCELLED = "cancelled";
    public static final String CHILD_TYPE = "child-type";
    public static final String CHILDREN = "children";
    public static final String CLIENT_MAPPINGS = "client-mappings";
    public static final String CODE = "code";
    public static final String COMPOSITE = "composite";
    public static final String CONCURRENT_GROUPS = "concurrent-groups";
    public static final String CONNECTION = "connection";
    public static final String CONNECTIONS = "connections";
    public static final String CONSOLE_ENABLED = "console-enabled";
    public static final String CONTENT = "content";
    public static final String CORE_SERVICE = "core-service";
    public static final String CPU_AFFINITY = "cpu-affinity";
    public static final String CRITERIA = "criteria";
    public static final String DEFAULT = "default";
    public static final String DEFAULT_INTERFACE = "default-interface";
    public static final String DEPLOY = "deploy";
    public static final String DEPLOYMENT = "deployment";
    public static final String DESCRIBE = "describe";
    public static final String DESCRIPTION = "description";
    public static final String DESTINATION_ADDRESS = "destination-address";
    public static final String DESTINATION_PORT = "destination-port";
    public static final String DIRECTORY = "directory";
    public static final String DIRECTORY_GROUPING = "directory-grouping";
    public static final String DISABLE = "disable";
    public static final String DOMAIN_FAILURE_DESCRIPTION = "domain-failure-description";
    public static final String DOMAIN_CONTROLLER = "domain-controller";
    public static final String DOMAIN_MODEL = "domain-model";
    public static final String DOMAIN_RESULTS = "domain-results";
    public static final String DUMP_SERVICES = "dump-services";
    public static final String ENABLE = "enable";
    public static final String ENABLED = "enabled";
    public static final String EXPRESSIONS_ALLOWED = "expressions-allowed";
    public static final String EXTENSION = "extension";
    public static final String FAILED = "failed";
    public static final String FAILURE_DESCRIPTION = "failure-description";
    public static final String FILE = "file";
    public static final String FIXED_PORT = "fixed-port";
    public static final String FIXED_SOURCE_PORT = "fixed-source-port";
    public static final String FULL_REPLACE_DEPLOYMENT = "full-replace-deployment";
    public static final String GRACEFUL_SHUTDOWN_TIMEOUT = "graceful-shutdown-timeout";
    public static final String GROUP = "group";
    public static final String HASH = "hash";
    public static final String HEAD_COMMENT_ALLOWED = "head-comment-allowed";
    public static final String HOST = "host";
    public static final String HOST_CONTROLLER = "host-controller";
    public static final String HOST_ENVIRONMENT = "host-environment";
    public static final String HOST_FAILURE_DESCRIPTION = "host-failure-description";
    public static final String HOST_FAILURE_DESCRIPTIONS = "host-failure-descriptions";
    public static final String HOST_STATE = "host-state";
    public static final String HTTP_INTERFACE = "http-interface";
    public static final String IGNORED = "ignored-by-unaffected-host-controller";
    public static final String IGNORED_RESOURCES = "ignored-resources";
    public static final String IGNORED_RESOURCE_TYPE = "ignored-resource-type";
    public static final String IN_SERIES = "in-series";
    public static final String INCLUDE = "include";
    public static final String INCLUDES = "includes";
    public static final String INCLUDE_RUNTIME = "include-runtime";
    public static final String INCLUDE_DEFAULTS = "include-defaults";
    public static final String INET_ADDRESS = "inet-address";
    public static final String INHERITED = "inherited";
    public static final String INITIAL_CONTEXT_FACTORY = "initial-context-factory";
    public static final String INPUT_STREAM_INDEX = "input-stream-index";
    public static final String INTERFACE = "interface";
    public static final String JAAS = "jaas";
    public static final String JVM = "jvm";
    public static final String JVM_TYPE = "type";
    public static final String LDAP = "ldap";
    public static final String LDAP_CONNECTION = "ldap-connection";
    public static final String LOCAL = "local";
    public static final String LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING = "local-destination-outbound-socket-binding";
    public static final String LOCAL_HOST_NAME = "local-host-name";
    public static final String LOCALE = "locale";
    public static final String MANAGEMENT_SUBSYSTEM_ENDPOINT = "management-subsystem-endpoint";
    public static final String MANAGEMENT = "management";
    public static final String MANAGEMENT_CLIENT_CONTENT = "management-client-content";
    public static final String MANAGEMENT_INTERFACE = "management-interface";
    public static final String MANAGEMENT_MAJOR_VERSION = "management-major-version";
    public static final String MANAGEMENT_MINOR_VERSION = "management-minor-version";
    public static final String MASK = "mask";
    public static final String MASTER = "master";
    public static final String MAX = "max";
    public static final String MAX_FAILED_SERVERS = "max-failed-servers";
    public static final String MAX_FAILURE_PERCENTAGE = "max-failure-percentage";
    public static final String MAX_LENGTH = "max-length";
    public static final String MAX_OCCURS = "max-occurs";
    public static final String MAX_THREADS = "max-threads";
    public static final String MIN = "min";
    public static final String MIN_LENGTH = "min-length";
    public static final String MIN_OCCURS = "min-occurs";
    public static final String MODEL_DESCRIPTION = "model-description";
    public static final String MODULE = "module";
    public static final String MULTICAST_ADDRESS = "multicast-address";
    public static final String MULTICAST_PORT = "multicast-port";
    public static final String NAME = "name";
    public static final String NAMES = "names";
    public static final String NAMESPACE = "namespace";
    public static final String NAMESPACES = "namespaces";
    public static final String NATIVE = "native";
    public static final String NATIVE_INTERFACE = "native-interface";
    public static final String NATIVE_REMOTING_INTERFACE = "native-remoting-interface";
    public static final String NETWORK = "network";
    public static final String NILLABLE = "nillable";
    public static final String NOT = "not";
    /** Use this as the standard operation name field in the operation *request* ModelNode */
    public static final String OP = "operation";
    /** Use this standard operation address field in the operation *request* ModelNode */
    public static final String OP_ADDR = "address";
    public static final String OPERATION_HEADERS = "operation-headers";
    public static final String OPERATION_NAME = "operation-name";
    public static final String OPERATIONS = "operations";
    public static final String OUTBOUND_CONNECTION = "outbound-connection";
    /** Use this standard operation address field in the operation *description* ModelNode */
    public static final String OUTCOME = "outcome";
    public static final String PASSWORD = "password";
    public static final String PATH = "path";
    public static final String PERSISTENT = "persistent";
    public static final String PLAIN_TEXT = "plain-text";
    public static final String PLATFORM_MBEAN = "platform-mbean";
    public static final String PORT = "port";
    public static final String PORT_OFFSET = "port-offset";
    public static final String PRIORITY = "priority";
    public static final String PROBLEM = "problem";
    public static final String PROCESS_TYPE = "process-type";
    public static final String PROCESS_STATE = "process-state";
    public static final String PRODUCT_NAME = "product-name";
    public static final String PRODUCT_VERSION = "product-version";
    public static final String PROFILE = "profile";
    public static final String PROFILE_NAME = "profile-name";
    public static final String PROPERTIES = "properties";
    public static final String PROTOCOL = "protocol";
    public static final String PROXIES = "proxies";
    public static final String READ_ATTRIBUTE_OPERATION = "read-attribute";
    public static final String READ_CHILDREN_NAMES_OPERATION = "read-children-names";
    public static final String READ_CHILDREN_TYPES_OPERATION = "read-children-types";
    public static final String READ_CHILDREN_RESOURCES_OPERATION = "read-children-resources";
    public static final String READ_CONFIG_AS_XML_OPERATION = "read-config-as-xml";
    public static final String READ_ONLY = "read-only";
    public static final String READ_OPERATION_DESCRIPTION_OPERATION = "read-operation-description";
    public static final String READ_OPERATION_NAMES_OPERATION = "read-operation-names";
    public static final String READ_RESOURCE_DESCRIPTION_OPERATION = "read-resource-description";
    public static final String READ_RESOURCE_METRICS = "read-resource-metrics";
    public static final String READ_RESOURCE_OPERATION = "read-resource";
    public static final String RECURSIVE = "recursive";
    public static final String RECURSIVE_DEPTH = "recursive-depth";
    public static final String REDEPLOY = "redeploy";
    public static final String RELATIVE_TO = "relative-to";
    public static final String RELEASE_CODENAME = "release-codename";
    public static final String RELEASE_VERSION = "release-version";
    public static final String REMOVE = "remove";
    public static final String REMOTE = "remote";
    public static final String REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING = "remote-destination-outbound-socket-binding";
    public static final String REPLACE_DEPLOYMENT = "replace-deployment";
    public static final String REPLY_PROPERTIES = "reply-properties";
    public static final String REQUEST_PROPERTIES = "request-properties";
    public static final String REQUIRED = "required";
    public static final String REQUIRES = "requires";
    public static final String RESPONSE = "response";
    public static final String RESPONSE_HEADERS = "response-headers";
    public static final String RESTART = "restart";
    public static final String RESTART_REQUIRED = "restart-required";
    public static final String RESULT = "result";
    public static final String ROLLBACK_ACROSS_GROUPS = "rollback-across-groups";
    public static final String ROLLBACK_FAILURE_DESCRIPTION = "rollback-failure-description";
    public static final String ROLLBACK_ON_RUNTIME_FAILURE = "rollback-on-runtime-failure";
    public static final String ROLLED_BACK = "rolled-back";
    public static final String ROLLING_TO_SERVERS = "rolling-to-servers";
    public static final String ROLLOUT_PLAN = "rollout-plan";
    public static final String ROLLOUT_PLANS = "rollout-plans";
    public static final String RUNNING_SERVER = "server";
    public static final String RUNTIME_NAME = "runtime-name";
    public static final String RUNTIME_UPDATE_SKIPPED = "runtime-update-skipped";
    public static final String SCHEMA_LOCATION = "schema-location";
    public static final String SCHEMA_LOCATIONS = "schema-locations";
    public static final String SEARCH_CREDENTIAL = "search-credential";
    public static final String SEARCH_DN = "search-dn";
    public static final String SECRET = "secret";
    public static final String SECURE_PORT = "secure-port";
    public static final String SECURE_SOCKET_BINDING = "secure-socket-binding";
    public static final String SECURITY_REALM = "security-realm";
    public static final String SECURITY_REALMS = "security-realms";
    public static final String SERVER = "server";
    public static final String SERVERS = "servers";
    public static final String SERVER_CONFIG = "server-config";
    public static final String SERVER_GROUP = "server-group";
    public static final String SERVER_GROUPS = "server-groups";
    public static final String SERVER_IDENTITIES = "server-identities";
    public static final String SERVER_IDENTITY = "server-identity";
    public static final String SERVER_OPERATIONS = "server-operations";
    public static final String SERVICE_CONTAINER = "service-container";
    public static final String SOURCE_NETWORK = "source-network";
    public static final String OPERATION_REQUIRES_RELOAD = "operation-requires-reload";
    public static final String OPERATION_REQUIRES_RESTART = "operation-requires-restart";
    public static final String RESTART_SERVERS = "restart-servers";
    public static final String SHUTDOWN = "shutdown";
    public static final String SOCKET_BINDING = "socket-binding";
    public static final String SOCKET_BINDING_REF = "socket-binding-ref";
    public static final String SOCKET_BINDING_GROUP = "socket-binding-group";
    public static final String SOCKET_BINDING_GROUP_NAME = "socket-binding-group-name";
    public static final String SOCKET_BINDING_PORT_OFFSET = "socket-binding-port-offset";
    public static final String SOURCE_INTERFACE = "source-interface";
    public static final String SOURCE_PORT = "source-port";
    public static final String SSL = "ssl";
    public static final String START = "start";
    public static final String START_SERVERS = "start-servers";
    public static final String STATUS = "status";
    public static final String STEPS = "steps";
    public static final String STOP = "stop";
    public static final String STOP_SERVERS = "stop-servers";
    /** The key for {@link AttributeAccess.Storage} fields. */
    public static final String STORAGE = "storage";
    public static final String SUBDEPLOYMENT = "subdeployment";
    public static final String SUBSYSTEM = "subsystem";
    public static final String SUCCESS = "success";
    public static final String SYSTEM_PROPERTY = "system-property";
    public static final String SYSTEM_PROPERTIES = "system-properties";
    public static final String TAIL_COMMENT_ALLOWED = "tail-comment-allowed";
    public static final String TO_REPLACE = "to-replace";
    public static final String TRUSTSTORE = "truststore";
    public static final String TYPE = "type";
    public static final String UNDEFINE_ATTRIBUTE_OPERATION = "undefine-attribute";
    public static final String UNDEPLOY = "undeploy";
    public static final String UPLOAD_DEPLOYMENT_BYTES = "upload-deployment-bytes";
    public static final String UPLOAD_DEPLOYMENT_URL = "upload-deployment-url";
    public static final String UPLOAD_DEPLOYMENT_STREAM = "upload-deployment-stream";
    public static final String UNIT = "unit";
    public static final String URI = "uri";
    public static final String URL = "url";
    public static final String USER = "user";
    public static final String USER_DN = "user-dn";
    public static final String USERNAME_ATTRIBUTE = "username-attribute";
    public static final String USERS = "users";
    public static final String VALIDATE_OPERATION = "validate-operation";
    public static final String VALID = "valid";
    public static final String VALUE = "value";
    public static final String VALUE_TYPE = "value-type";
    public static final String VAULT = "vault";
    public static final String VAULT_OPTION = "vault-option";
    public static final String VAULT_OPTIONS = "vault-options";
    public static final String WILDCARD = "wildcard";
    public static final String WRITE_ATTRIBUTE_OPERATION = "write-attribute";

    private ModelDescriptionConstants() {
    }
}
