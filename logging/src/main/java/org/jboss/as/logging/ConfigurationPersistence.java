/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import static org.jboss.as.logging.Logging.createOperationFailure;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.logging.resolvers.FileResolver;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.PropertyConfigurator;

/**
 * Persists the {@literal logging.properties} file.
 * <p/>
 * Commits any changes remaining on the {@link org.jboss.logmanager.config.LogContextConfiguration} and writes out the
 * configuration to the configuration file.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class ConfigurationPersistence {

    private static final String PROPERTIES_FILE = "logging.properties";

    private static final byte[] NOTE_MESSAGE = String.format("# Note this file has been generated and will be overwritten if a%n" +
            "# logging subsystem has been defined in the XML configuration.%n%n").getBytes();

    private ConfigurationPersistence() {
    }

    /**
     * Write the logging configuration to the {@code logging.properties} file.
     *
     * @param context the context used to determine the file location.
     *
     * @throws OperationFailedException if the write fails.
     */
    public static void writeConfiguration(final OperationContext context) throws OperationFailedException {
        final String loggingConfig;
        switch (context.getProcessType()) {
            case DOMAIN_SERVER: {
                loggingConfig = FileResolver.resolvePath(context, "jboss.server.data.dir", PROPERTIES_FILE);
                break;
            }
            default: {
                loggingConfig = FileResolver.resolvePath(context, "jboss.server.config.dir", PROPERTIES_FILE);
            }
        }
        if (loggingConfig == null) {
            LoggingLogger.ROOT_LOGGER.pathManagerServiceNotStarted();
        } else {
            final File configFile = new File(loggingConfig);
            final PropertyConfigurator config = Logging.getPropertyConfigurator(LogContext.getLogContext());
            // We need a configuration to write.
            if (config == null) {
                throw new IllegalStateException(LoggingMessages.MESSAGES.failedToWriteConfigurationFile(configFile));
            }
            try {
                // Commit the log context configuration
                config.getLogContextConfiguration().commit();
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(configFile);
                    out.write(NOTE_MESSAGE);
                    config.writeConfiguration(out);
                    out.close();
                    LoggingLogger.ROOT_LOGGER.tracef("Logging configuration file '%s' successfully written.", configFile.getAbsolutePath());
                } catch (IOException e) {
                    throw createOperationFailure(e, LoggingMessages.MESSAGES.failedToWriteConfigurationFile(configFile));
                } finally {
                    safeClose(out);
                }
            } finally {
                config.getLogContextConfiguration().forget();
            }
        }
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable t) {
            LoggingLogger.ROOT_LOGGER.failedToCloseResource(t, closeable);
        }
    }
}
