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
    public static final String ADD_OPERATION = "add-operation";
    public static final String ADDRESS = "address";
    public static final String ANY = "any";
    public static final String ANY_ADDRESS = "any-address";
    public static final String ANY_IPV4_ADDRESS = "any-ipv4-address";
    public static final String ANY_IPV6_ADDRESS = "any-ipv6-address";
    public static final String ATTRIBUTES = "attributes";
    public static final String BYTES = "bytes";
    public static final String CANCELLED = "cancelled";
    public static final String CHILD_TYPE = "child-type";
    public static final String CHILDREN = "children";
    public static final String COMPOSITE = "composite";
    public static final String CPU_AFFINITY = "cpu-affinity";
    public static final String CRITERIA = "criteria";
    public static final String COMPENSATING_OPERATION = "compensating-operation";
    public static final String DEFAULT = "default";
    public static final String DESCRIBE = "describe";
    public static final String DEFAULT_INTERFACE = "default-interface";
    public static final String DEPLOYMENT = "deployment";
    public static final String DESCRIPTION = "description";
    public static final String DOMAIN_CONTROLLER = "domain-controller";
    public static final String EXTENSION = "extension";
    public static final String FAILED = "failed";
    public static final String FAILURE_DESCRIPTION = "failure-description";
    public static final String FIXED_PORT = "fixed-port";
    public static final String GROUP = "group";
    public static final String HASH = "hash";
    public static final String HEAD_COMMENT_ALLOWED = "head-comment-allowed";
    public static final String HTTP_API = "http-api";
    public static final String HOST = "host";
    public static final String INCLUDE = "include";
    public static final String INCLUDES = "includes";
    public static final String INCLUDE_RUNTIME = "include-runtime";
    public static final String INET_ADDRESS = "inet-address";
    public static final String INPUT_STREAM_INDEX = "input-stream-index";
    public static final String INTERFACE = "interface";
    public static final String JVM = "jvm";
    public static final String JVM_TYPE = "type";
    public static final String LOCAL = "local";
    public static final String LOCALE = "locale";
    public static final String MANAGEMENT = "management";
    public static final String MASK = "mask";
    public static final String MAX = "max";
    public static final String MAX_LENGTH = "max-length";
    public static final String MAX_OCCURS = "max-occurs";
    public static final String MAX_THREADS = "max-threads";
    public static final String MIN = "min";
    public static final String MIN_LENGTH = "min-length";
    public static final String MIN_OCCURS = "min-occurs";
    public static final String MIN_VALUE = "min-value";
    public static final String MODEL_DESCRIPTION = "model-description";
    public static final String MULTICAST_ADDRESS = "multicast-address";
    public static final String MULTICAST_PORT = "multicast-port";
    public static final String NAME = "name";
    public static final String NAMESPACE = "namespace";
    public static final String NAMESPACES = "namespaces";
    public static final String NATIVE_API = "native-api";
    public static final String NETWORK = "network";
    public static final String NILLABLE = "nillable";
    public static final String NOT = "not";
    /** Use this as the standard operation name field in the operation *request* ModelNode */
    public static final String OP = "operation";
    /** Use this standard operation address field in the operation *request* ModelNode */
    public static final String OP_ADDR = "address";
    public static final String OPERATIONS = "operations";
    /** Use this standard operation address field in the operation *description* ModelNode */
    public static final String OPERATION_NAME = "operation-name";
    public static final String OUTCOME = "outcome";
    public static final String PATH = "path";
    public static final String PORT = "port";
    public static final String PORT_OFFSET = "port-offset";
    public static final String PRIORITY = "priority";
    public static final String PROFILE = "profile";
    public static final String PROFILE_NAME = "profile-name";
    public static final String READ_ATTRIBUTE_OPERATION = "read-attribute";
    public static final String READ_CHILDREN_NAMES_OPERATION = "read-children-names";
    public static final String READ_OPERATION_DESCRIPTION_OPERATION = "read-operation-description";
    public static final String READ_OPERATION_NAMES_OPERATION = "read-operation-names";
    public static final String READ_RESOURCE_DESCRIPTION_OPERATION = "read-resource-description";
    public static final String READ_RESOURCE_METRICS = "read-resource-metrics";
    public static final String READ_RESOURCE_OPERATION = "read-resource";
    public static final String RELATIVE_TO = "relative-to";
    public static final String REMOVE = "remove";
    public static final String REMOTE = "remote";
    public static final String REMOVE_OPERATION = "remove-operation";
    public static final String REPLY_PROPERTIES = "reply-properties";
    public static final String REQUEST_PROPERTIES = "request-properties";
    public static final String RECURSIVE = "recursive";
    public static final String REQUIRED = "required";
    public static final String RESULT = "result";
    public static final String ROLLBACK_FAILURE_DESCRIPTION = "rollback-failure-description";
    public static final String ROLLBACK_ON_RUNTIME_FAILURE = "rollback-on-runtime-failure";
    public static final String ROLLED_BACK = "rolled-back";
    public static final String RUNTIME_NAME = "runtime-name";
    public static final String RUNNING_SERVER = "server";
    public static final String SCHEMA_LOCATION = "schema-location";
    public static final String SCHEMA_LOCATIONS = "schema-locations";
    public static final String SERVER = "server";
    public static final String SERVER_CONFIG = "server-config";
    public static final String SERVER_GROUP = "server-group";
    public static final String SOCKET_BINDING = "socket-binding";
    public static final String SOCKET_BINDING_GROUP = "socket-binding-group";
    public static final String SOCKET_BINDING_GROUP_NAME = "socket-binding-group-name";
    public static final String SOCKET_BINDING_PORT_OFFSET = "socket-binding-port-offset";
    public static final String START = "start";
    public static final String STEPS = "steps";
    /** The key for {@link AttributeAccess.Storage} fields. */
    public static final String STORAGE = "storage";
    public static final String SUBSYSTEM = "subsystem";
    public static final String SUCCESS = "success";
    public static final String SYSTEM_PROPERTY = "system-property";
    public static final String SYSTEM_PROPERTIES = "system-properties";
    public static final String TAIL_COMMENT_ALLOWED = "tail-comment-allowed";
    public static final String TO_REPLACE = "to-replace";
    public static final String TYPE = "type";
    public static final String URL = "url";
    public static final String VALUE = "value";
    public static final String VALUE_TYPE = "value-type";
    public static final String WRITE_ATTRIBUTE_OPERATION = "write-attribute";

}
