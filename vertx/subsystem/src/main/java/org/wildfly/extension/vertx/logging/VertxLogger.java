package org.wildfly.extension.vertx.logging;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "WFLYRS", length = 4)
public interface VertxLogger extends BasicLogger {

    VertxLogger VERTX_LOGGER = Logger.getMessageLogger(VertxLogger.class, "org.wildfly.extension.vertx");


}
