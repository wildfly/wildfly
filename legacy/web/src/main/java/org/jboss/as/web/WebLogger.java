package org.jboss.as.web;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

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

    @Message(id = 2, value = "Could not migrate resource %s")
    String couldNotMigrateResource(ModelNode node);

    @Message(id = 3, value = "Could not migrate attribute %s from resource %s")
    String couldNotMigrateResource(String attribute, PathAddress node);

    @Message(id = 4, value = "Could not migrate SSL connector as no SSL config is defined")
    OperationFailedException noSslConfig();

    @Message(id = 5, value = "Migration failed, see results for more details.")
    String migrationFailed();

    @Message(id = 6, value = "Could not migrate verify-client attribute %s to the Undertow equivalent")
    String couldNotTranslateVerifyClient(String s);

    @Message(id = 7, value = "Could not migrate verify-client expression %s")
    String couldNotTranslateVerifyClientExpression(String s);

    @Message(id = 8, value = "Could not migrate valve %s")
    String couldNotMigrateValve(String valveName);

    @Message(id = 9, value = "Could not migrate attribute %s from valve %s")
    String couldNotMigrateValveAttribute(String attribute, String valveName);
}