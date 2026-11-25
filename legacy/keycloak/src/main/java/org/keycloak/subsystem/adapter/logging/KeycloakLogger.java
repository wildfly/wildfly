/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.keycloak.subsystem.adapter.logging;

import java.lang.invoke.MethodHandles;

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
    KeycloakLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), KeycloakLogger.class, "org.jboss.keycloak");

    //@LogMessage(level = INFO)
    //@Message(value = "Keycloak subsystem override for deployment %s")
    //void deploymentSecured(String deployment);

    @Message(id = 1, value = "The migrate operation can not be performed: the server must be in admin-only mode")
    OperationFailedException migrateOperationAllowedOnlyInAdminOnly();

    @Message(id = 2, value = "Migration failed, see results for more details.")
    String migrationFailed();

}
