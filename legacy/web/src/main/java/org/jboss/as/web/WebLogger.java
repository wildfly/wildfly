package org.jboss.as.web;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * Logger for new messages in the legacy extension, used only for the migrate operation,
 * this prevents potential conflicts with EAP6 message codes.
 */
@MessageLogger(projectCode = "WFLYWEB", length = 4)
public interface WebLogger extends BasicLogger {

    /**
     * A root logger with the category of the package name.
     */
    WebLogger ROOT_LOGGER = Logger.getMessageLogger(WebLogger.class, "org.jboss.as.web");

    @Message(id = 1, value = "Migrate operation only allowed in admin only mode")
    OperationFailedException migrateOperationAllowedOnlyInAdminOnly();

    @LogMessage(level = WARN)
    @Message(id = 2, value = "Could not migrate resource %s")
    void couldNotMigrateResource(ModelNode node);

    @LogMessage(level = WARN)
    @Message(id = 3, value = "Could not migrate attribute %s from resource %s")
    void couldNotMigrateResource(String attribute, PathAddress node);

    @Message(id = 4, value = "Could not migrate SSL connector as no SSL config is defined")
    OperationFailedException noSslConfig();
}