/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.subsystem.adapter.logging;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * This interface to be fleshed out later when error messages are fully externalized.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
@MessageLogger(projectCode = "KEYCLOAK")
public interface KeycloakLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    KeycloakLogger ROOT_LOGGER = Logger.getMessageLogger(KeycloakLogger.class, "org.jboss.keycloak");

    //@LogMessage(level = INFO)
    //@Message(value = "Keycloak subsystem override for deployment %s")
    //void deploymentSecured(String deployment);

    @Message(id = 1, value = "The migrate operation can not be performed: the server must be in admin-only mode")
    OperationFailedException migrateOperationAllowedOnlyInAdminOnly();

    @Message(id = 2, value = "Migration failed, see results for more details.")
    String migrationFailed();

    @Message(id = 3, value = "Cannot migrate 'secure-server' resource. This resource is not supported by the new elytron-oidc-subsystem yet.")
    String couldNotMigrateUnsupportedSecureServerResource();
}
