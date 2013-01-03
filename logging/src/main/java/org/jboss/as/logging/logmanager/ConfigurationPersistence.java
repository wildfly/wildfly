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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.logging.CommonAttributes;
import org.jboss.as.logging.LoggingLogger;
import org.jboss.as.logging.LoggingMessages;
import org.jboss.as.logging.resolvers.FileResolver;
import org.jboss.logmanager.Configurator;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.config.ErrorManagerConfiguration;
import org.jboss.logmanager.config.FilterConfiguration;
import org.jboss.logmanager.config.FormatterConfiguration;
import org.jboss.logmanager.config.HandlerConfiguration;
import org.jboss.logmanager.config.LogContextConfiguration;
import org.jboss.logmanager.config.LoggerConfiguration;
import org.jboss.logmanager.config.PropertyConfigurable;

/**
 * Persists the {@literal logging.properties} file.
 * <p/>
 * Commits any changes remaining on the {@link org.jboss.logmanager.config.LogContextConfiguration} and writes out the
 * configuration to the configuration file.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class ConfigurationPersistence implements Configurator {

    private static final String[] EMPTY_STRINGS = new String[0];
    private static final String ENCODING = "utf-8";
    private static final Object PROP_CONFIG_LOCK = new Object();

    private static final String PROPERTIES_FILE = "logging.properties";

    private static final byte[] NOTE_MESSAGE = String.format("# Note this file has been generated and will be overwritten if a%n" +
            "# logging subsystem has been defined in the XML configuration.%n%n").getBytes();

    private final LogContextConfigurationImpl config;

    public ConfigurationPersistence() {
        this(LogContext.getSystemLogContext());
    }

    public ConfigurationPersistence(final LogContext logContext) {
        config = new LogContextConfigurationImpl(logContext);
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
        ConfigurationPersistence result = (ConfigurationPersistence) logContext.getAttachment(CommonAttributes.ROOT_LOGGER_NAME, Configurator.ATTACHMENT_KEY);
        if (result == null) {
            final Logger root = logContext.getLogger(CommonAttributes.ROOT_LOGGER_NAME);
            result = (ConfigurationPersistence) root.getAttachment(Configurator.ATTACHMENT_KEY);
            if (result == null) {
                synchronized (PROP_CONFIG_LOCK) {
                    result = (ConfigurationPersistence) logContext.getAttachment(CommonAttributes.ROOT_LOGGER_NAME, Configurator.ATTACHMENT_KEY);
                    if (result == null) {
                        result = (ConfigurationPersistence) root.getAttachment(Configurator.ATTACHMENT_KEY);
                    }
                    if (result == null) {
                        result = new ConfigurationPersistence(logContext);
                        ConfigurationPersistence current = (ConfigurationPersistence) root.attachIfAbsent(Configurator.ATTACHMENT_KEY, result);
                        if (current != null) {
                            result = current;
                        }
                    }
                }
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

    @Override
    public void configure(final InputStream inputStream) throws IOException {
        final Properties properties = new Properties();
        try {
            properties.load(new InputStreamReader(inputStream, ENCODING));
            inputStream.close();
        } finally {
            safeClose(inputStream);
        }
        configure(properties);
    }

    /**
     * Prepare the runtime configuration. Applies the changes to the runtime, but allows the changes to be {@link
     * #rollback() rolled back} until {@link #commit()} is invoked.
     */
    public void prepare() {
        config.prepare();
    }

    /**
     * Commits the runtime changes.
     */
    public void commit() {
        config.commit();
    }

    /**
     * Rolls back the runtime changes.
     */
    public void rollback() {
        config.forget();
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
        synchronized (config) {
            return config;
        }
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
            synchronized (config) {
                try {
                    // Commit the log context configuration
                    config.commit();
                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(configFile);
                        out.write(NOTE_MESSAGE);
                        writeConfiguration(out);
                        out.close();
                        LoggingLogger.ROOT_LOGGER.tracef("Logging configuration file '%s' successfully written.", configFile.getAbsolutePath());
                    } catch (IOException e) {
                        throw LoggingMessages.MESSAGES.failedToWriteConfigurationFile(e, configFile);
                    } finally {
                        safeClose(out);
                    }
                } finally {
                    config.forget();
                }
            }
        }
    }

    /**
     * Writes the current configuration to the output stream.
     * <p/>
     * <b>Note:</b> the output stream will be closed.
     *
     * @param outputStream the output stream to write to.
     *
     * @throws IOException if an error occurs while writing the configuration.
     */
    private void writeConfiguration(final OutputStream outputStream) throws IOException {
        try {
            final PrintStream out = new PrintStream(outputStream, true, ENCODING);
            try {
                final Set<String> implicitHandlers = new HashSet<String>();
                final Set<String> implicitFormatters = new HashSet<String>();
                final Set<String> implicitErrorManagers = new HashSet<String>();
                final List<String> loggerNames = config.getLoggerNames();
                writePropertyComment(out, "Additional loggers to configure (the root logger is always configured)");
                writeProperty(out, "loggers", toCsvString(loggerNames));
                final LoggerConfiguration rootLogger = config.getLoggerConfiguration("");
                writeLoggerConfiguration(out, rootLogger, implicitHandlers);
                // Remove the root loggers
                loggerNames.remove("");
                for (String loggerName : loggerNames) {
                    writeLoggerConfiguration(out, config.getLoggerConfiguration(loggerName), implicitHandlers);
                }
                final List<String> allHandlerNames = config.getHandlerNames();
                final List<String> explicitHandlerNames = new ArrayList<String>(allHandlerNames);
                explicitHandlerNames.removeAll(implicitHandlers);
                if (!explicitHandlerNames.isEmpty()) {
                    writePropertyComment(out, "Additional handlers to configure");
                    writeProperty(out, "handlers", toCsvString(explicitHandlerNames));
                    out.println();
                }
                for (String handlerName : allHandlerNames) {
                    writeHandlerConfiguration(out, config.getHandlerConfiguration(handlerName), implicitHandlers, implicitFormatters, implicitErrorManagers);
                }
                final List<String> allFilterNames = config.getFilterNames();
                if (!allFilterNames.isEmpty()) {
                    writePropertyComment(out, "Additional filters to configure");
                    writeProperty(out, "filters", toCsvString(allFilterNames));
                    out.println();
                }
                for (String filterName : allFilterNames) {
                    writeFilterConfiguration(out, config.getFilterConfiguration(filterName));
                }
                final List<String> allFormatterNames = config.getFormatterNames();
                final ArrayList<String> explicitFormatterNames = new ArrayList<String>(allFormatterNames);
                explicitFormatterNames.removeAll(implicitFormatters);
                if (!explicitFormatterNames.isEmpty()) {
                    writePropertyComment(out, "Additional formatters to configure");
                    writeProperty(out, "formatters", toCsvString(explicitFormatterNames));
                    out.println();
                }
                for (String formatterName : allFormatterNames) {
                    writeFormatterConfiguration(out, config.getFormatterConfiguration(formatterName));
                }
                final List<String> allErrorManagerNames = config.getErrorManagerNames();
                final ArrayList<String> explicitErrorManagerNames = new ArrayList<String>(allErrorManagerNames);
                explicitErrorManagerNames.removeAll(implicitErrorManagers);
                if (!explicitErrorManagerNames.isEmpty()) {
                    writePropertyComment(out, "Additional errorManagers to configure");
                    writeProperty(out, "errorManagers", toCsvString(explicitErrorManagerNames));
                    out.println();
                }
                for (String errorManagerName : allErrorManagerNames) {
                    writeErrorManagerConfiguration(out, config.getErrorManagerConfiguration(errorManagerName));
                }
                out.close();
            } finally {
                safeClose(out);
            }
            outputStream.close();
        } finally {
            safeClose(outputStream);
        }
    }

    private static void writeLoggerConfiguration(final PrintStream out, final LoggerConfiguration logger, final Set<String> implicitHandlers) throws IOException {
        if (logger != null) {
            out.println();
            final String name = logger.getName();
            final String prefix = name.isEmpty() ? "logger." : "logger." + name + ".";
            final String level = logger.getLevel();
            if (level != null) {
                writeProperty(out, prefix, "level", level);
            }
            final String filterName = logger.getFilter();
            if (filterName != null) {
                writeProperty(out, prefix, "filter", filterName);
            }
            final Boolean useParentHandlers = logger.getUseParentHandlers();
            if (useParentHandlers != null) {
                writeProperty(out, prefix, "useParentHandlers", useParentHandlers.toString());
            }
            final List<String> handlerNames = logger.getHandlerNames();
            if (!handlerNames.isEmpty()) {
                writeProperty(out, prefix, "handlers", toCsvString(handlerNames));
                for (String handlerName : handlerNames) {
                    implicitHandlers.add(handlerName);
                }
            }
        }
    }

    private static void writeHandlerConfiguration(final PrintStream out, final HandlerConfiguration handler, final Set<String> implicitHandlers, final Set<String> implicitFormatters, final Set<String> implicitErrorManagers) throws IOException {
        if (handler != null) {
            out.println();
            // Should always be, but let's be safe
            if (handler instanceof HandlerConfigurationImpl) {
                if (((HandlerConfigurationImpl) handler).isLog4jAppender()) {
                    // Write a warning message that this will not work with other log managers
                    out.println("# Note: This is a log4j appender and will not work with normal log managers.");
                    out.println("#       References to this appender should be removed if a different log manager is used.");
                }
            }
            final String name = handler.getName();
            final String prefix = "handler." + name + ".";
            final String className = handler.getClassName();
            writeProperty(out, "handler.", name, className);
            final String moduleName = handler.getModuleName();
            if (moduleName != null) {
                writeProperty(out, prefix, "module", moduleName);
            }
            final String level = handler.getLevel();
            if (level != null) {
                writeProperty(out, prefix, "level", level);
            }
            final String encoding = handler.getEncoding();
            if (encoding != null) {
                writeProperty(out, prefix, "encoding", encoding);
            }
            final String filter = handler.getFilter();
            if (filter != null) {
                writeProperty(out, prefix, "filter", filter);
            }
            final String formatterName = handler.getFormatterName();
            if (formatterName != null) {
                writeProperty(out, prefix, "formatter", formatterName);
                implicitFormatters.add(formatterName);
            }
            final String errorManagerName = handler.getErrorManagerName();
            if (errorManagerName != null) {
                writeProperty(out, prefix, "errorManager", errorManagerName);
                implicitErrorManagers.add(errorManagerName);
            }
            final List<String> handlerNames = handler.getHandlerNames();
            if (!handlerNames.isEmpty()) {
                writeProperty(out, prefix, "handlers", toCsvString(handlerNames));
                for (String handlerName : handlerNames) {
                    implicitHandlers.add(handlerName);
                }
            }
            writeProperties(out, prefix, handler);
        }
    }

    private static void writeFilterConfiguration(final PrintStream out, final FilterConfiguration filter) throws IOException {
        if (filter != null) {
            out.println();
            final String name = filter.getName();
            final String prefix = "filter." + name + ".";
            final String className = filter.getClassName();
            writeProperty(out, "filter.", name, className);
            final String moduleName = filter.getModuleName();
            if (moduleName != null) {
                writeProperty(out, prefix, "module", moduleName);
            }
            writeProperties(out, prefix, filter);
        }
    }

    private static void writeFormatterConfiguration(final PrintStream out, final FormatterConfiguration formatter) throws IOException {
        if (formatter != null) {
            out.println();
            final String name = formatter.getName();
            final String prefix = "formatter." + name + ".";
            final String className = formatter.getClassName();
            writeProperty(out, "formatter.", name, className);
            final String moduleName = formatter.getModuleName();
            if (moduleName != null) {
                writeProperty(out, prefix, "module", moduleName);
            }
            writeProperties(out, prefix, formatter);
        }
    }

    private static void writeErrorManagerConfiguration(final PrintStream out, final ErrorManagerConfiguration errorManager) throws IOException {
        if (errorManager != null) {
            out.println();
            final String name = errorManager.getName();
            final String prefix = "errorManager." + name + ".";
            final String className = errorManager.getClassName();
            writeProperty(out, "errorManager.", name, className);
            final String moduleName = errorManager.getModuleName();
            if (moduleName != null) {
                writeProperty(out, prefix, "module", moduleName);
            }
            writeProperties(out, prefix, errorManager);
        }
    }

    /**
     * Writes a comment to the print stream. Prepends the comment with a {@code #}.
     *
     * @param out     the print stream to write to.
     * @param comment the comment to write.
     */
    private static void writePropertyComment(final PrintStream out, final String comment) {
        out.printf("%n# %s%n", comment);
    }

    /**
     * Writes a property to the print stream.
     *
     * @param out   the print stream to write to.
     * @param name  the name of the property.
     * @param value the value of the property.
     */
    private static void writeProperty(final PrintStream out, final String name, final String value) throws IOException {
        writeProperty(out, null, name, value);
    }

    /**
     * Writes a property to the print stream.
     *
     * @param out    the print stream to write to.
     * @param prefix the prefix for the name or {@code null} to use no prefix.
     * @param name   the name of the property.
     * @param value  the value of the property.
     */
    private static void writeProperty(final PrintStream out, final String prefix, final String name, final String value) throws IOException {
        if (prefix == null) {
            writeKey(out, name);
        } else {
            writeKey(out, String.format("%s%s", prefix, name));
        }
        writeValue(out, value);
        out.println();
    }

    /**
     * Writes a collection of properties to the print stream. Uses the {@link org.jboss.logmanager.config.PropertyConfigurable#getPropertyValueString(String)}
     * to extract the value.
     *
     * @param out                  the print stream to write to.
     * @param prefix               the prefix for the name or {@code null} to use no prefix.
     * @param propertyConfigurable the configuration to extract the property value from.
     */
    private static void writeProperties(final PrintStream out, final String prefix, final PropertyConfigurable propertyConfigurable) throws IOException {
        final List<String> names = propertyConfigurable.getPropertyNames();
        if (!names.isEmpty()) {
            final List<String> ctorProps = propertyConfigurable.getConstructorProperties();
            if (prefix == null) {
                writeProperty(out, "properties", toCsvString(names));
                if (!ctorProps.isEmpty()) {
                    writeProperty(out, "constructorProperties", toCsvString(ctorProps));
                }
                for (String name : names) {
                    writeProperty(out, name, propertyConfigurable.getPropertyValueString(name));
                }
            } else {
                writeProperty(out, prefix, "properties", toCsvString(names));
                if (!ctorProps.isEmpty()) {
                    writeProperty(out, prefix, "constructorProperties", toCsvString(ctorProps));
                }
                for (String name : names) {
                    writeProperty(out, prefix, name, propertyConfigurable.getPropertyValueString(name));
                }
            }
        }
    }

    /**
     * Parses the list and creates a comma delimited string of the names.
     * <p/>
     * <b>Notes:</b> empty names are ignored.
     *
     * @param names the names to process.
     *
     * @return a comma delimited list of the names.
     */
    private static String toCsvString(final List<String> names) {
        final StringBuilder result = new StringBuilder(1024);
        Iterator<String> iterator = names.iterator();
        while (iterator.hasNext()) {
            final String name = iterator.next();
            // No need to write empty names
            if (!name.isEmpty()) {
                result.append(name);
                if (iterator.hasNext()) {
                    result.append(",");
                }
            }
        }
        return result.toString();
    }

    /**
     * Configure the log manager from the given properties.
     *
     * @param properties the properties
     *
     * @throws IOException if an error occurs
     */
    public void configure(final Properties properties) throws IOException {
        synchronized (config) {
            try {
                // Start with the list of loggers to configure.  The root logger is always on the list.
                configureLogger(properties, "");
                // And, for each logger name, configure any filters, handlers, etc.
                for (String loggerName : getStringCsvArray(properties, "loggers")) {
                    configureLogger(properties, loggerName);
                }
                // Configure any declared handlers.
                for (String handlerName : getStringCsvArray(properties, "handlers")) {
                    configureHandler(properties, handlerName);
                }
                // Configure any declared filters.
                for (String filterName : getStringCsvArray(properties, "filters")) {
                    configureFilter(properties, filterName);
                }
                // Configure any declared formatters.
                for (String formatterName : getStringCsvArray(properties, "formatters")) {
                    configureFormatter(properties, formatterName);
                }
                // Configure any declared error managers.
                for (String errorManagerName : getStringCsvArray(properties, "errorManagers")) {
                    configureErrorManager(properties, errorManagerName);
                }
                config.commit();
            } finally {
                config.forget();
            }
        }
    }

    private void configureLogger(final Properties properties, final String loggerName) throws IOException {
        if (config.getLoggerConfiguration(loggerName) != null) {
            // duplicate
            return;
        }
        final LoggerConfiguration loggerConfiguration = config.addLoggerConfiguration(loggerName);

        // Get logger level
        final String levelName = getStringProperty(properties, getKey("logger", loggerName, "level"));
        if (levelName != null) {
            loggerConfiguration.setLevel(levelName);
        }

        // Get logger filter
        final String filterName = getStringProperty(properties, getKey("logger", loggerName, "filter"));
        if (filterName != null) {
            loggerConfiguration.setFilter(filterName);
            // Not yet supported LOGMGR-51
            // configureFilter(properties, filterName);
        }

        // Get logger handlers
        final String[] handlerNames = getStringCsvArray(properties, getKey("logger", loggerName, "handlers"));
        loggerConfiguration.setHandlerNames(handlerNames);
        for (String name : handlerNames) {
            configureHandler(properties, name);
        }

        // Get logger properties
        final String useParentHandlersString = getStringProperty(properties, getKey("logger", loggerName, "useParentHandlers"));
        if (useParentHandlersString != null) {
            loggerConfiguration.setUseParentHandlers(Boolean.valueOf(Boolean.parseBoolean(useParentHandlersString)));
        }
    }

    private void configureFilter(final Properties properties, final String filterName) throws IOException {
        if (config.getFilterConfiguration(filterName) != null) {
            // already configured!
            return;
        }
        final FilterConfiguration configuration = config.addFilterConfiguration(
                getStringProperty(properties, getKey("filter", filterName, "module")),
                getStringProperty(properties, getKey("filter", filterName)),
                filterName,
                getStringCsvArray(properties, getKey("filter", filterName, "constructorProperties")));
        configureProperties(properties, configuration, getKey("filter", filterName));
    }

    private void configureFormatter(final Properties properties, final String formatterName) throws IOException {
        if (config.getFormatterConfiguration(formatterName) != null) {
            // already configured!
            return;
        }
        final FormatterConfiguration configuration = config.addFormatterConfiguration(
                getStringProperty(properties, getKey("formatter", formatterName, "module")),
                getStringProperty(properties, getKey("formatter", formatterName)),
                formatterName,
                getStringCsvArray(properties, getKey("formatter", formatterName, "constructorProperties")));
        configureProperties(properties, configuration, getKey("formatter", formatterName));
    }

    private void configureErrorManager(final Properties properties, final String errorManagerName) throws IOException {
        if (config.getErrorManagerConfiguration(errorManagerName) != null) {
            // already configured!
            return;
        }
        final ErrorManagerConfiguration configuration = config.addErrorManagerConfiguration(
                getStringProperty(properties, getKey("errorManager", errorManagerName, "module")),
                getStringProperty(properties, getKey("errorManager", errorManagerName)),
                errorManagerName,
                getStringCsvArray(properties, getKey("errorManager", errorManagerName, "constructorProperties")));
        configureProperties(properties, configuration, getKey("errorManager", errorManagerName));
    }

    private void configureHandler(final Properties properties, final String handlerName) throws IOException {
        if (config.getHandlerConfiguration(handlerName) != null) {
            // already configured!
            return;
        }
        final HandlerConfiguration configuration = config.addHandlerConfiguration(
                getStringProperty(properties, getKey("handler", handlerName, "module")),
                getStringProperty(properties, getKey("handler", handlerName)),
                handlerName,
                getStringCsvArray(properties, getKey("handler", handlerName, "constructorProperties")));
        final String filter = getStringProperty(properties, getKey("handler", handlerName, "filter"));
        if (filter != null) {
            configuration.setFilter(filter);
        }
        final String levelName = getStringProperty(properties, getKey("handler", handlerName, "level"));
        if (levelName != null) {
            configuration.setLevel(levelName);
        }
        final String formatterName = getStringProperty(properties, getKey("handler", handlerName, "formatter"));
        if (formatterName != null) {
            configuration.setFormatterName(formatterName);
            configureFormatter(properties, formatterName);
        }
        final String encoding = getStringProperty(properties, getKey("handler", handlerName, "encoding"));
        if (encoding != null) {
            configuration.setEncoding(encoding);
        }
        final String errorManagerName = getStringProperty(properties, getKey("handler", handlerName, "errorManager"));
        if (errorManagerName != null) {
            configuration.setErrorManagerName(errorManagerName);
            configureErrorManager(properties, errorManagerName);
        }
        final String[] handlerNames = getStringCsvArray(properties, getKey("handler", handlerName, "handlers"));
        configuration.setHandlerNames(handlerNames);
        for (String name : handlerNames) {
            configureHandler(properties, name);
        }
        configureProperties(properties, configuration, getKey("handler", handlerName));
    }

    private void configureProperties(final Properties properties, final PropertyConfigurable configurable, final String prefix) throws IOException {
        final List<String> propertyNames = getStringCsvList(properties, getKey(prefix, "properties"));
        for (String propertyName : propertyNames) {
            final String valueString = getStringProperty(properties, getKey(prefix, propertyName));
            if (valueString != null) configurable.setPropertyValueString(propertyName, valueString);
        }
    }

    private static String getKey(final String prefix, final String objectName) {
        return objectName.length() > 0 ? prefix + "." + objectName : prefix;
    }

    private static String getKey(final String prefix, final String objectName, final String key) {
        return objectName.length() > 0 ? prefix + "." + objectName + "." + key : prefix + "." + key;
    }

    private static String getStringProperty(final Properties properties, final String key) {
        return properties.getProperty(key);
    }

    private static String[] getStringCsvArray(final Properties properties, final String key) {
        final String property = properties.getProperty(key, "");
        if (property == null) {
            return EMPTY_STRINGS;
        }
        final String value = property.trim();
        if (value.length() == 0) {
            return EMPTY_STRINGS;
        }
        return value.split("\\s*,\\s*");
    }

    private static List<String> getStringCsvList(final Properties properties, final String key) {
        return new ArrayList<String>(Arrays.asList(getStringCsvArray(properties, key)));
    }

    private static void writeValue(final PrintStream out, final String value) throws IOException {
        writeSanitized(out, value, false);
    }

    private static void writeKey(final PrintStream out, final String key) throws IOException {
        writeSanitized(out, key, true);
        out.append('=');
    }

    private static void writeSanitized(final Appendable out, final String string, final boolean escapeSpaces) throws IOException {
        for (int x = 0; x < string.length(); x++) {
            final char c = string.charAt(x);
            switch (c) {
                case ' ':
                    if (x == 0 || escapeSpaces)
                        out.append('\\');
                    out.append(c);
                    break;
                case '\t':
                    out.append('\\').append('t');
                    break;
                case '\n':
                    out.append('\\').append('n');
                    break;
                case '\r':
                    out.append('\\').append('r');
                    break;
                case '\f':
                    out.append('\\').append('f');
                    break;
                case '\\':
                case '=':
                case ':':
                case '#':
                case '!':
                    out.append('\\').append(c);
                    break;
                default:
                    out.append(c);
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
