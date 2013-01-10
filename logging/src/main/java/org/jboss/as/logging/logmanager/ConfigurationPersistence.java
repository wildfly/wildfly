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

package org.jboss.as.logging.logmanager;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.logging.CommonAttributes;
import org.jboss.as.logging.LoggingLogger;
import org.jboss.as.logging.LoggingMessages;
import org.jboss.as.logging.resolvers.FileResolver;
import org.jboss.logmanager.Configurator;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.PropertyConfigurator;
import org.jboss.logmanager.config.ErrorManagerConfiguration;
import org.jboss.logmanager.config.FilterConfiguration;
import org.jboss.logmanager.config.FormatterConfiguration;
import org.jboss.logmanager.config.HandlerConfiguration;
import org.jboss.logmanager.config.LogContextConfiguration;
import org.jboss.logmanager.config.LoggerConfiguration;
import org.jboss.logmanager.config.PojoConfiguration;

/**
 * Persists the {@literal logging.properties} file.
 * <p/>
 * Commits any changes remaining on the {@link org.jboss.logmanager.config.LogContextConfiguration} and writes out the
 * configuration to the configuration file.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class ConfigurationPersistence implements Configurator, LogContextConfiguration {

    private static final Object LOCK = new Object();
    private static final String PROPERTIES_FILE = "logging.properties";
    private static final byte[] NOTE_MESSAGE = String.format("# Note this file has been generated and will be overwritten if a%n" +
            "# logging subsystem has been defined in the XML configuration.%n%n").getBytes();
    private final PropertyConfigurator config;
    private final LogContextConfiguration delegate;

    public ConfigurationPersistence() {
        this(LogContext.getSystemLogContext());
    }

    public ConfigurationPersistence(final LogContext logContext) {
        config = new PropertyConfigurator(logContext);
        delegate = config.getLogContextConfiguration();
    }

    /**
     * Gets the property configurator. If the {@link ConfigurationPersistence} does not exist a new one is created.
     *
     * @return the property configurator
     */
    public static ConfigurationPersistence getOrCreateConfigurationPersistence() {
        return getOrCreateConfigurationPersistence(LogContext.getLogContext());
    }

    /**
     * Gets the property configurator. If the {@link ConfigurationPersistence} does not exist a new one is created.
     *
     * @param logContext the log context used to find the property configurator or to attach it to.
     *
     * @return the property configurator
     */
    public static ConfigurationPersistence getOrCreateConfigurationPersistence(final LogContext logContext) {
        final Logger root = logContext.getLogger(CommonAttributes.ROOT_LOGGER_NAME);
        ConfigurationPersistence result = (ConfigurationPersistence) root.getAttachment(Configurator.ATTACHMENT_KEY);
        if (result == null) {
            result = new ConfigurationPersistence(logContext);
            ConfigurationPersistence existing = (ConfigurationPersistence) root.attachIfAbsent(Configurator.ATTACHMENT_KEY, result);
            if (existing != null) {
                result = existing;
            }
        }
        return result;
    }

    /**
     * Gets the property configurator. If the {@link ConfigurationPersistence} does not exist a {@code null} is
     * returned.
     *
     * @param logContext the log context used to find the property configurator or to attach it to.
     *
     * @return the property configurator or {@code null}
     */
    public static ConfigurationPersistence getConfigurationPersistence(final LogContext logContext) {
        if (logContext == null) return null;
        return (ConfigurationPersistence) logContext.getAttachment(CommonAttributes.ROOT_LOGGER_NAME, Configurator.ATTACHMENT_KEY);
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable t) {
            LoggingLogger.ROOT_LOGGER.failedToCloseResource(t, closeable);
        }
    }

    @Override
    public void configure(final InputStream inputStream) throws IOException {
        synchronized (LOCK) {
            config.configure(inputStream);
        }
    }

    @Override
    public LogContext getLogContext() {
        synchronized (LOCK) {
            return delegate.getLogContext();
        }
    }

    @Override
    public LoggerConfiguration addLoggerConfiguration(final String loggerName) {
        synchronized (LOCK) {
            return delegate.addLoggerConfiguration(loggerName);
        }
    }

    @Override
    public boolean removeLoggerConfiguration(final String loggerName) {
        synchronized (LOCK) {
            return delegate.removeLoggerConfiguration(loggerName);
        }
    }

    @Override
    public LoggerConfiguration getLoggerConfiguration(final String loggerName) {
        synchronized (LOCK) {
            return delegate.getLoggerConfiguration(loggerName);
        }
    }

    @Override
    public List<String> getLoggerNames() {
        synchronized (LOCK) {
            return delegate.getLoggerNames();
        }
    }

    @Override
    public HandlerConfiguration addHandlerConfiguration(final String moduleName, final String className, final String handlerName, final String... constructorProperties) {
        synchronized (LOCK) {
            return delegate.addHandlerConfiguration(moduleName, className, handlerName, constructorProperties);
        }
    }

    @Override
    public boolean removeHandlerConfiguration(final String handlerName) {
        synchronized (LOCK) {
            return delegate.removeHandlerConfiguration(handlerName);
        }
    }

    @Override
    public HandlerConfiguration getHandlerConfiguration(final String handlerName) {
        synchronized (LOCK) {
            return delegate.getHandlerConfiguration(handlerName);
        }
    }

    @Override
    public List<String> getHandlerNames() {
        synchronized (LOCK) {
            return delegate.getHandlerNames();
        }
    }

    @Override
    public FormatterConfiguration addFormatterConfiguration(final String moduleName, final String className, final String formatterName, final String... constructorProperties) {
        synchronized (LOCK) {
            return delegate.addFormatterConfiguration(moduleName, className, formatterName, constructorProperties);
        }
    }

    @Override
    public boolean removeFormatterConfiguration(final String formatterName) {
        synchronized (LOCK) {
            return delegate.removeFormatterConfiguration(formatterName);
        }
    }

    @Override
    public FormatterConfiguration getFormatterConfiguration(final String formatterName) {
        synchronized (LOCK) {
            return delegate.getFormatterConfiguration(formatterName);
        }
    }

    @Override
    public List<String> getFormatterNames() {
        synchronized (LOCK) {
            return delegate.getFormatterNames();
        }
    }

    @Override
    public FilterConfiguration addFilterConfiguration(final String moduleName, final String className, final String filterName, final String... constructorProperties) {
        synchronized (LOCK) {
            return delegate.addFilterConfiguration(moduleName,  className, filterName, constructorProperties);
        }
    }

    @Override
    public boolean removeFilterConfiguration(final String filterName) {
        synchronized (LOCK) {
            return delegate.removeFilterConfiguration(filterName);
        }
    }

    @Override
    public FilterConfiguration getFilterConfiguration(final String filterName) {
        synchronized (LOCK) {
            return delegate.getFilterConfiguration(filterName);
        }
    }

    @Override
    public List<String> getFilterNames() {
        synchronized (LOCK) {
            return delegate.getFilterNames();
        }
    }

    @Override
    public ErrorManagerConfiguration addErrorManagerConfiguration(final String moduleName, final String className, final String errorManagerName, final String... constructorProperties) {
        synchronized (LOCK) {
            return delegate.addErrorManagerConfiguration(moduleName, className, errorManagerName, constructorProperties);
        }
    }

    @Override
    public boolean removeErrorManagerConfiguration(final String errorManagerName) {
        synchronized (LOCK) {
            return delegate.removeErrorManagerConfiguration(errorManagerName);
        }
    }

    @Override
    public ErrorManagerConfiguration getErrorManagerConfiguration(final String errorManagerName) {
        synchronized (LOCK) {
            return delegate.getErrorManagerConfiguration(errorManagerName);
        }
    }

    @Override
    public List<String> getErrorManagerNames() {
        synchronized (LOCK) {
            return delegate.getErrorManagerNames();
        }
    }

    @Override
    public void prepare() {
        synchronized (LOCK) {
            delegate.prepare();
        }
    }

    @Override
    public PojoConfiguration addPojoConfiguration(final String moduleName, final String className, final String pojoName, final String... constructorProperties) {
        synchronized (LOCK) {
            return delegate.addPojoConfiguration(moduleName, className, pojoName, constructorProperties);
        }
    }

    @Override
    public boolean removePojoConfiguration(final String pojoName) {
        synchronized (LOCK) {
            return delegate.removePojoConfiguration(pojoName);
        }
    }

    @Override
    public PojoConfiguration getPojoConfiguration(final String pojoName) {
        synchronized (LOCK) {
            return delegate.getPojoConfiguration(pojoName);
        }
    }

    @Override
    public List<String> getPojoNames() {
        synchronized (LOCK) {
            return delegate.getPojoNames();
        }
    }

    @Override
    public void commit() {
        synchronized (LOCK) {
            delegate.commit();
        }
    }

    @Override
    public void forget() {
        synchronized (LOCK) {
            delegate.forget();
        }
    }

    /**
     * Rolls back the runtime changes.
     */
    public void rollback() {
        forget();
    }

    /**
     * Get the log context configuration.
     * <p/>
     * <em>WARNING</em>: this instance is not thread safe in any way.  The returned object should never be used from
     * more than one thread at a time; furthermore the {@link #writeConfiguration(OperationContext)} method also
     * accesses this object directly.
     *
     * @return the log context configuration instance
     */
    public LogContextConfiguration getLogContextConfiguration() {
        return this;
    }

    /**
     * Write the logging configuration to the {@code logging.properties} file.
     *
     * @param context the context used to determine the file location.
     */
    public void writeConfiguration(final OperationContext context) {
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
            synchronized (LOCK) {
                try {
                    // Commit the log context configuration
                    commit();
                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(configFile);
                        out.write(NOTE_MESSAGE);
                        config.writeConfiguration(out);
                        out.close();
                        LoggingLogger.ROOT_LOGGER.tracef("Logging configuration file '%s' successfully written.", configFile.getAbsolutePath());
                    } catch (IOException e) {
                        throw LoggingMessages.MESSAGES.failedToWriteConfigurationFile(e, configFile);
                    } finally {
                        safeClose(out);
                    }
                } finally {
                    forget();
                }
            }
        }
    }
}
