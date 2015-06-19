package org.jboss.as.telemetry.logger;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.*;

/**
 * Telemetry logger
 *
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYTELEMTRY", length = 4)
public interface TelemetryLogger extends BasicLogger {
    /**
     * A logger with the category of the default telemetry package.
     */
    TelemetryLogger ROOT_LOGGER = Logger.getMessageLogger(
            TelemetryLogger.class, "org.jboss.as.telemetry.extension");

    /**
     * Telemetry could not create JDR.
     */
    @LogMessage(level = ERROR)
    @Message(id = 1, value = "Could not generate JDR.")
    void couldNotGenerateJdr(@Cause Throwable cause);

    /**
     * Telemetry thread incurred an InterruptedException.
     */
    @LogMessage(level = ERROR)
    @Message(id = 2, value = "Telemetry thread has been interrupted.")
    void threadInterrupted(@Cause Throwable cause);

    /**
     * Frequency was updated to given value
     *
     * @param var
     *            new frequency value
     */
    @LogMessage(level = INFO)
    @Message(id = 3, value = "Frequency of Telemetry Subsystem updated to %s")
    void frequencyUpdated(String var);

    /**
     * System was enabled
     */
    @LogMessage(level = INFO)
    @Message(id = 4, value = "Telemetry Subsystem Enabled")
    void telemetryEnabled();

    /**
     * System was enabled
     */
    @LogMessage(level = INFO)
    @Message(id = 5, value = "Telemetry Subsystem Disabled")
    void telemetryDisabled();

    /**
     * Could not update/create telemetry.properties
     */
    @LogMessage(level = ERROR)
    @Message(id = 6, value = "Could not create/update telemetry.properties file with given username and password.")
    void couldNotCreateEditPropertiesFile(@Cause Throwable cause);

    /**
     * Could not close telemetry.properties
     */
    @LogMessage(level = ERROR)
    @Message(id = 7, value = "Could not close telemetry.properties file.")
    void couldNotClosePropertiesFile(@Cause Throwable cause);

    /**
     * Could not load telemetry.properties
     */
    @LogMessage(level = ERROR)
    @Message(id = 8, value = "Could not load telemetry.properties file.")
    void couldNotLoadPropertiesFile(@Cause Throwable cause);

    /**
     * Could not load telemetry.properties
     */
    @LogMessage(level = ERROR)
    @Message(id = 9, value = "Could not register system with Insights")
    void couldNotRegisterSystem(@Cause Throwable cause);

    /**
     * Could not upload JDR
     */
    @LogMessage(level = ERROR)
    @Message(id = 10, value = "Could not upload JDR")
    void couldNotUploadJdr(@Cause Throwable cause);

    /**
     * Could not find JDR file
     */
    @LogMessage(level = ERROR)
    @Message(id = 11, value = "Could not find JDR file")
    void couldNotFindJdr(@Cause Throwable cause);

    /**
     * Could not start thread
     */
    @LogMessage(level = ERROR)
    @Message(id = 12, value = "Could not start Telemetry thread")
    void couldNotStartThread(@Cause Throwable cause);

    /**
     * Could not find system
     */
    @LogMessage(level = ERROR)
    @Message(id = 13, value = "Could not find system in Insights")
    void couldNotFindSystem(@Cause Throwable cause);
}