package com.redhat.gss.redhat_support_lib.logger;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.*;

/**
 * Red Hat Support Library logger
 *
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 */
@MessageLogger(projectCode = "WFLYRHTSUPLIB", length = 4)
public interface RedHatSupportLibLogger extends BasicLogger {

    /**
     * A logger with the category of the default telemetry package.
     */
    RedHatSupportLibLogger ROOT_LOGGER = Logger.getMessageLogger(
            RedHatSupportLibLogger.class, "com.redhat.gss.redhat_support_lib");

    /**
     * Could not initialize CustomHttpEngine
     */
    @LogMessage(level = ERROR)
    @Message(id = 1, value = "Could not initialize CustomHttpEngine.")
    void couldNotInitializeCustomHttpEngine(@Cause Throwable cause);
}
