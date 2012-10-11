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

import java.util.Locale;

/**
 * Constants for strings frequently used in management operations.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ClientConstants {
    public static final String ADD = "add";
    public static final String COMPOSITE = "composite";
    public static final String CONTENT = "content";
    public static final String DEPLOYMENT = "deployment";
    public static final String METADATA = "metadata";
    public static final String EXTENSION = "extension";
    public static final String INPUT_STREAM_INDEX = "input-stream-index";
    public static final String FAILURE_DESCRIPTION = "failure-description";
    public static final String NAME = "name";
    public static final String OP = "operation";
    public static final String OPERATION_HEADERS = "operation-headers";
    public static final String OP_ADDR = "address";
    public static final String OUTCOME = "outcome";
    public static final String RESULT = "result";
    public static final String ROLLBACK_ON_RUNTIME_FAILURE = "rollback-on-runtime-failure";
    public static final String ROLLOUT_PLAN = "rollout-plan";
    public static final String RUNTIME_NAME = "runtime-name";
    public static final String STEPS = "steps";
    public static final String SUCCESS = "success";
    public static final String TO_REPLACE = "to-replace";

    public static final String DEPLOYMENT_DEPLOY_OPERATION = "deploy";
    public static final String DEPLOYMENT_FULL_REPLACE_OPERATION = "full-replace-deployment";
    public static final String DEPLOYMENT_REDEPLOY_OPERATION = "redeploy";
    public static final String DEPLOYMENT_REMOVE_OPERATION = "remove";
    public static final String DEPLOYMENT_REPLACE_OPERATION = "replace-deployment";
    public static final String DEPLOYMENT_UNDEPLOY_OPERATION = "undeploy";

    public static final String DEPLOYMENT_METADATA_START_POLICY = "start.policy";
    public static final String DEPLOYMENT_METADATA_BUNDLE_STARTLEVEL = "bundle.startlevel";

    public enum StartPolicy {
        /**
         * Deployment is automatically started (default).
         */
        AUTO,
        /**
         * Deployment activation is deferred until explicitly started.
         */
        DEFERRED;

        /**
         * Get the default policy
         * @return The default policy: {@link AUTO}
         */
        public static StartPolicy defaultPolicy() {
            return AUTO;
        }

        /**
         * Parse the policy string
         * @param value A policy string value or null
         * @return The policy or the default if value is null
         */
        public static StartPolicy parse(String value) {
            return value != null ? valueOf(value.toUpperCase(Locale.ENGLISH)) : defaultPolicy();
        }
    }
}
