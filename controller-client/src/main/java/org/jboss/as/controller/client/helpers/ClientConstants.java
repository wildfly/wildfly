/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.client.helpers;

/**
 * Constants for strings frequently used in management operations.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ClientConstants {
    public static final String ADD = "add";
    public static final String AUTO_START = "auto-start";
    public static final String CHILD_TYPE = "child-type";
    public static final String COMPOSITE = "composite";
    public static final String CONTENT = "content";
    public static final String DEPLOYMENT = "deployment";
    public static final String DEPLOYMENT_DEPLOY_OPERATION = "deploy";
    public static final String DEPLOYMENT_FULL_REPLACE_OPERATION = "full-replace-deployment";
    public static final String DEPLOYMENT_REDEPLOY_OPERATION = "redeploy";
    public static final String DEPLOYMENT_REMOVE_OPERATION = "remove";
    public static final String DEPLOYMENT_REPLACE_OPERATION = "replace-deployment";
    public static final String DEPLOYMENT_UNDEPLOY_OPERATION = "undeploy";
    public static final String EXTENSION = "extension";
    public static final String FAILURE_DESCRIPTION = "failure-description";
    public static final String GROUP = "group";
    public static final String HOST = "host";
    public static final String INCLUDE_RUNTIME = "include-runtime";
    public static final String INPUT_STREAM_INDEX = "input-stream-index";
    public static final String NAME = "name";
    public static final String OP = "operation";
    public static final String OPERATION_HEADERS = "operation-headers";
    public static final String OP_ADDR = "address";
    public static final String OUTCOME = "outcome";
    public static final String READ_ATTRIBUTE_OPERATION = "read-attribute";
    public static final String READ_CHILDREN_NAMES_OPERATION = "read-children-names";
    public static final String READ_RESOURCE_OPERATION = "read-resource";
    public static final String RECURSIVE = "recursive";
    public static final String RESULT = "result";
    public static final String ROLLBACK_ON_RUNTIME_FAILURE = "rollback-on-runtime-failure";
    public static final String ROLLOUT_PLAN = "rollout-plan";
    public static final String RUNTIME_NAME = "runtime-name";
    public static final String SERVER = "server";
    public static final String SERVER_CONFIG = "server-config";
    public static final String SERVER_GROUP = "server-group";
    public static final String SOCKET_BINDING = "socket-binding";
    public static final String SOCKET_BINDING_GROUP = "socket-binding-group";
    public static final String STATUS = "status";
    public static final String STEPS = "steps";
    public static final String SUBSYSTEM = "subsystem";
    public static final String SUCCESS = "success";
    public static final String TO_REPLACE = "to-replace";

    public static final String CONTROLLER_PROCESS_STATE_STARTING = "starting";
    public static final String CONTROLLER_PROCESS_STATE_RUNNING = "running";
    public static final String CONTROLLER_PROCESS_STATE_RELOAD_REQUIRED = "reload-required";
    public static final String CONTROLLER_PROCESS_STATE_RESTART_REQUIRED = "restart-required";
    public static final String CONTROLLER_PROCESS_STATE_STOPPING = "stopping";
}
