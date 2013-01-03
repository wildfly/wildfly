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

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;
import static java.lang.Character.isWhitespace;

import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.as.logging.LoggingMessages;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.config.ErrorManagerConfiguration;
import org.jboss.logmanager.config.FilterConfiguration;
import org.jboss.logmanager.config.FormatterConfiguration;
import org.jboss.logmanager.config.HandlerConfiguration;
import org.jboss.logmanager.config.LogContextConfiguration;
import org.jboss.logmanager.config.LoggerConfiguration;
import org.jboss.logmanager.filters.AcceptAllFilter;
import org.jboss.logmanager.filters.AllFilter;
import org.jboss.logmanager.filters.AnyFilter;
import org.jboss.logmanager.filters.DenyAllFilter;
import org.jboss.logmanager.filters.InvertFilter;
import org.jboss.logmanager.filters.LevelChangingFilter;
import org.jboss.logmanager.filters.LevelFilter;
import org.jboss.logmanager.filters.LevelRangeFilter;
import org.jboss.logmanager.filters.RegexFilter;
import org.jboss.logmanager.filters.SubstituteFilter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class LogContextConfigurationImpl implements LogContextConfiguration {

    private final LogContext logContext;

    private final Map<String, LoggerConfigurationImpl> loggers = new HashMap<String, LoggerConfigurationImpl>();
    private final Map<String, HandlerConfigurationImpl> handlers = new HashMap<String, HandlerConfigurationImpl>();
    private final Map<String, FormatterConfigurationImpl> formatters = new HashMap<String, FormatterConfigurationImpl>();
    private final Map<String, FilterConfigurationImpl> filters = new HashMap<String, FilterConfigurationImpl>();
    private final Map<String, ErrorManagerConfigurationImpl> errorManagers = new HashMap<String, ErrorManagerConfigurationImpl>();
    private final Map<String, Logger> loggerRefs = new HashMap<String, Logger>();
    private final Map<String, Handler> handlerRefs = new HashMap<String, Handler>();
    private final Map<String, Filter> filterRefs = new HashMap<String, Filter>();
    private final Map<String, Formatter> formatterRefs = new HashMap<String, Formatter>();
    private final Map<String, ErrorManager> errorManagerRefs = new HashMap<String, ErrorManager>();

    private final Deque<ConfigAction<?>> transactionState = new ArrayDeque<ConfigAction<?>>();
    private final Set<String> log4jAppendersName = new HashSet<String>();

    private boolean prepared = false;

    private static final ObjectProducer ACCEPT_PRODUCER = new SimpleObjectProducer(AcceptAllFilter.getInstance());
    private static final ObjectProducer DENY_PRODUCER = new SimpleObjectProducer(DenyAllFilter.getInstance());

    LogContextConfigurationImpl(final LogContext logContext) {
        this.logContext = logContext;
    }

    public LogContext getLogContext() {
        return logContext;
    }

    public LoggerConfiguration addLoggerConfiguration(final String loggerName) {
        if (loggers.containsKey(loggerName)) {
            throw LoggingMessages.MESSAGES.loggerAlreadyExists(loggerName);
        }
        final LoggerConfigurationImpl loggerConfiguration = new LoggerConfigurationImpl(loggerName, this);
        loggers.put(loggerName, loggerConfiguration);
        transactionState.addLast(new ConfigAction<Logger>() {
            public Logger validate() throws IllegalArgumentException {
                return logContext.getLogger(loggerName);
            }

            public void applyPreCreate(final Logger param) {
                loggerRefs.put(loggerName, param);
            }

            public void applyPostCreate(Logger param) {
            }

            public void rollback() {
                loggers.remove(loggerName);
            }
        });
        return loggerConfiguration;
    }

    public boolean removeLoggerConfiguration(final String loggerName) {
        final LoggerConfigurationImpl removed = loggers.remove(loggerName);
        if (removed != null) {
            transactionState.addLast(removed.getRemoveAction());
            removed.setRemoved();
            return true;
        } else {
            return false;
        }
    }

    public LoggerConfiguration getLoggerConfiguration(final String loggerName) {
        return loggers.get(loggerName);
    }

    public List<String> getLoggerNames() {
        return new ArrayList<String>(loggers.keySet());
    }

    public HandlerConfiguration addHandlerConfiguration(final String moduleName, final String className, final String handlerName, final String... constructorProperties) {
        if (handlers.containsKey(handlerName)) {
            throw new IllegalArgumentException(LoggingMessages.MESSAGES.handlerAlreadyDefined(handlerName));
        }
        final HandlerConfigurationImpl handlerConfiguration = new HandlerConfigurationImpl(this, handlerName, moduleName, className, constructorProperties);
        handlers.put(handlerName, handlerConfiguration);
        addAction(handlerConfiguration.getConstructAction());
        return handlerConfiguration;
    }

    public boolean removeHandlerConfiguration(final String handlerName) {
        final HandlerConfigurationImpl removed = handlers.remove(handlerName);
        if (removed != null) {
            transactionState.addLast(removed.getRemoveAction());
            removed.setRemoved();
            return true;
        } else {
            return false;
        }
    }

    public HandlerConfiguration getHandlerConfiguration(final String handlerName) {
        return handlers.get(handlerName);
    }

    public List<String> getHandlerNames() {
        return new ArrayList<String>(handlers.keySet());
    }

    public FormatterConfiguration addFormatterConfiguration(final String moduleName, final String className, final String formatterName, final String... constructorProperties) {
        if (formatters.containsKey(formatterName)) {
            throw LoggingMessages.MESSAGES.formatterAlreadyExists(formatterName);
        }
        final FormatterConfigurationImpl formatterConfiguration = new FormatterConfigurationImpl(this, formatterName, moduleName, className, constructorProperties);
        formatters.put(formatterName, formatterConfiguration);
        addAction(formatterConfiguration.getConstructAction());
        return formatterConfiguration;
    }

    public boolean removeFormatterConfiguration(final String formatterName) {
        final FormatterConfigurationImpl removed = formatters.remove(formatterName);
        if (removed != null) {
            transactionState.addLast(removed.getRemoveAction());
            removed.setRemoved();
            return true;
        } else {
            return false;
        }
    }

    public FormatterConfiguration getFormatterConfiguration(final String formatterName) {
        return formatters.get(formatterName);
    }

    public List<String> getFormatterNames() {
        return new ArrayList<String>(formatters.keySet());
    }

    public FilterConfiguration addFilterConfiguration(final String moduleName, final String className, final String filterName, final String... constructorProperties) {
        if (filters.containsKey(filterName)) {
            throw LoggingMessages.MESSAGES.filterAlreadyExists(filterName);
        }
        final FilterConfigurationImpl filterConfiguration = new FilterConfigurationImpl(this, filterName, moduleName, className, constructorProperties);
        filters.put(filterName, filterConfiguration);
        addAction(filterConfiguration.getConstructAction());
        return filterConfiguration;
    }

    public boolean removeFilterConfiguration(final String filterName) {
        final FilterConfigurationImpl removed = filters.remove(filterName);
        if (removed != null) {
            transactionState.addLast(removed.getRemoveAction());
            removed.setRemoved();
            return true;
        } else {
            return false;
        }
    }

    public FilterConfiguration getFilterConfiguration(final String filterName) {
        return filters.get(filterName);
    }

    public List<String> getFilterNames() {
        return new ArrayList<String>(filters.keySet());
    }

    public ErrorManagerConfiguration addErrorManagerConfiguration(final String moduleName, final String className, final String errorManagerName, final String... constructorProperties) {
        if (errorManagers.containsKey(errorManagerName)) {
            throw LoggingMessages.MESSAGES.errorManagerAlreadyExists(errorManagerName);
        }
        final ErrorManagerConfigurationImpl errorManagerConfiguration = new ErrorManagerConfigurationImpl(this, errorManagerName, moduleName, className, constructorProperties);
        errorManagers.put(errorManagerName, errorManagerConfiguration);
        addAction(errorManagerConfiguration.getConstructAction());
        return errorManagerConfiguration;
    }

    public boolean removeErrorManagerConfiguration(final String errorManagerName) {
        final ErrorManagerConfigurationImpl removed = errorManagers.remove(errorManagerName);
        if (removed != null) {
            transactionState.addLast(removed.getRemoveAction());
            removed.setRemoved();
            return true;
        } else {
            return false;
        }
    }

    public ErrorManagerConfiguration getErrorManagerConfiguration(final String errorManagerName) {
        return errorManagers.get(errorManagerName);
    }

    public List<String> getErrorManagerNames() {
        return new ArrayList<String>(errorManagers.keySet());
    }

    public void prepare() {
        List<Object> items = new ArrayList<Object>();
        for (ConfigAction<?> action : transactionState) {
            items.add(action.validate());
        }
        Iterator<Object> iterator = items.iterator();
        for (ConfigAction<?> action : transactionState) {
            doApplyPreCreate(action, iterator.next());
        }
        iterator = items.iterator();
        for (ConfigAction<?> action : transactionState) {
            doApplyPostCreate(action, iterator.next());
        }
        for (String name : log4jAppendersName) {
            handlers.get(name).activate();
        }
        prepared = true;
    }

    public void commit() {
        if (!prepared) {
            prepare();
        }
        prepared = false;
        log4jAppendersName.clear();
        transactionState.clear();
    }

    @SuppressWarnings("unchecked")
    private static <T> void doApplyPreCreate(ConfigAction<T> action, Object arg) {
        try {
            action.applyPreCreate((T) arg);
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void doApplyPostCreate(ConfigAction<T> action, Object arg) {
        try {
            action.applyPostCreate((T) arg);
        } catch (Throwable ignored) {
        }
    }

    public void forget() {
        final Iterator<ConfigAction<?>> iterator = transactionState.descendingIterator();
        while (iterator.hasNext()) {
            final ConfigAction<?> action = iterator.next();
            try {
                action.rollback();
            } catch (Throwable ignored) {
            }
        }
        prepared = false;
        log4jAppendersName.clear();
        transactionState.clear();
    }

    void addAction(final ConfigAction<?> action) {
        transactionState.addLast(action);
    }

    void addAppenderName(final String name) {
        log4jAppendersName.add(name);
    }

    ObjectProducer getValue(final Class<?> objClass, final String propertyName, final Class<?> paramType, final String valueString, final boolean immediate) {
        final String replaced = PropertyHelper.resolveValue(valueString);
        if (valueString == null) {
            if (paramType.isPrimitive()) {
                throw LoggingMessages.MESSAGES.cannotAssignNullToPrimitive(propertyName, objClass);
            }
            return ObjectProducer.NULL_PRODUCER;
        }
        if (paramType == String.class) {
            return new SimpleObjectProducer(replaced);
        } else if (paramType == Handler.class) {
            if (!handlers.containsKey(replaced) || immediate && !handlerRefs.containsKey(replaced)) {
                throw LoggingMessages.MESSAGES.handlerNotFound(replaced);
            }
            if (immediate) {
                return new SimpleObjectProducer(handlerRefs.get(replaced));
            } else {
                return new RefProducer(replaced, handlerRefs);
            }
        } else if (paramType == Filter.class) {
            return parseFilterExpression(replaced, immediate);
        } else if (paramType == Formatter.class) {
            if (!formatters.containsKey(replaced) || immediate && !formatterRefs.containsKey(replaced)) {
                throw LoggingMessages.MESSAGES.formatterNotFound(replaced);
            }
            if (immediate) {
                return new SimpleObjectProducer(formatterRefs.get(replaced));
            } else {
                return new RefProducer(replaced, formatterRefs);
            }
        } else if (paramType == ErrorManager.class) {
            if (!errorManagers.containsKey(replaced) || immediate && !errorManagerRefs.containsKey(replaced)) {
                throw LoggingMessages.MESSAGES.errorManagerNotFound(replaced);
            }
            if (immediate) {
                return new SimpleObjectProducer(errorManagerRefs.get(replaced));
            } else {
                return new RefProducer(replaced, errorManagerRefs);
            }
        } else if (paramType == Level.class) {
            return new SimpleObjectProducer(LogContext.getSystemLogContext().getLevelForName(replaced));
        } else if (paramType == java.util.logging.Logger.class) {
            return new SimpleObjectProducer(LogContext.getSystemLogContext().getLogger(replaced));
        } else if (paramType == boolean.class || paramType == Boolean.class) {
            return new SimpleObjectProducer(Boolean.valueOf(replaced));
        } else if (paramType == byte.class || paramType == Byte.class) {
            return new SimpleObjectProducer(Byte.valueOf(replaced));
        } else if (paramType == short.class || paramType == Short.class) {
            return new SimpleObjectProducer(Short.valueOf(replaced));
        } else if (paramType == int.class || paramType == Integer.class) {
            return new SimpleObjectProducer(Integer.valueOf(replaced));
        } else if (paramType == long.class || paramType == Long.class) {
            return new SimpleObjectProducer(Long.valueOf(replaced));
        } else if (paramType == float.class || paramType == Float.class) {
            return new SimpleObjectProducer(Float.valueOf(replaced));
        } else if (paramType == double.class || paramType == Double.class) {
            return new SimpleObjectProducer(Double.valueOf(replaced));
        } else if (paramType == char.class || paramType == Character.class) {
            return new SimpleObjectProducer(Character.valueOf(replaced.length() > 0 ? replaced.charAt(0) : 0));
        } else if (paramType == TimeZone.class) {
            return new SimpleObjectProducer(TimeZone.getTimeZone(replaced));
        } else if (paramType == Charset.class) {
            return new SimpleObjectProducer(Charset.forName(replaced));
        } else if (paramType.isEnum()) {
            return new SimpleObjectProducer(Enum.valueOf(paramType.asSubclass(Enum.class), replaced));
        } else {
            throw LoggingMessages.MESSAGES.unknownParameterType(paramType, propertyName, objClass);
        }
    }

    Map<String, Filter> getFilterRefs() {
        return filterRefs;
    }

    Map<String, FilterConfigurationImpl> getFilterConfigurations() {
        return filters;
    }

    Map<String, ErrorManager> getErrorManagerRefs() {
        return errorManagerRefs;
    }

    Map<String, ErrorManagerConfigurationImpl> getErrorManagerConfigurations() {
        return errorManagers;
    }

    Map<String, Handler> getHandlerRefs() {
        return handlerRefs;
    }

    Map<String, HandlerConfigurationImpl> getHandlerConfigurations() {
        return handlers;
    }

    Map<String, Formatter> getFormatterRefs() {
        return formatterRefs;
    }

    Map<String, FormatterConfigurationImpl> getFormatterConfigurations() {
        return formatters;
    }

    Map<String, Logger> getLoggerRefs() {
        return loggerRefs;
    }

    Map<String, LoggerConfigurationImpl> getLoggerConfigurations() {
        return loggers;
    }

    private static List<String> tokens(String source) {
        final List<String> tokens = new ArrayList<String>();
        final int length = source.length();
        int idx = 0;
        while (idx < length) {
            int ch;
            ch = source.codePointAt(idx);
            if (isWhitespace(ch)) {
                ch = source.codePointAt(idx);
                idx = source.offsetByCodePoints(idx, 1);
            } else if (isJavaIdentifierStart(ch)) {
                int start = idx;
                do {
                    idx = source.offsetByCodePoints(idx, 1);
                } while (idx < length && isJavaIdentifierPart(ch = source.codePointAt(idx)));
                tokens.add(source.substring(start, idx));
            } else if (ch == '"') {
                final StringBuilder b = new StringBuilder();
                // tag token as a string
                b.append('"');
                idx = source.offsetByCodePoints(idx, 1);
                while (idx < length && (ch = source.codePointAt(idx)) != '"') {
                    ch = source.codePointAt(idx);
                    if (ch == '\\') {
                        idx = source.offsetByCodePoints(idx, 1);
                        if (idx == length) {
                            throw LoggingMessages.MESSAGES.truncatedFilterExpression();
                        }
                        ch = source.codePointAt(idx);
                        switch (ch) {
                            case '\\':
                                b.append('\\');
                                break;
                            case '\'':
                                b.append('\'');
                                break;
                            case '"':
                                b.append('"');
                                break;
                            case 'b':
                                b.append('\b');
                                break;
                            case 'f':
                                b.append('\f');
                                break;
                            case 'n':
                                b.append('\n');
                                break;
                            case 'r':
                                b.append('\r');
                                break;
                            case 't':
                                b.append('\t');
                                break;
                            default:
                                throw LoggingMessages.MESSAGES.invalidEscapeFoundInFilterExpression();
                        }
                    } else {
                        b.appendCodePoint(ch);
                    }
                    idx = source.offsetByCodePoints(idx, 1);
                }
                idx = source.offsetByCodePoints(idx, 1);
                tokens.add(b.toString());
            } else {
                int start = idx;
                idx = source.offsetByCodePoints(idx, 1);
                tokens.add(source.substring(start, idx));
            }
        }
        return tokens;
    }

    private ObjectProducer parseFilterExpression(Iterator<String> iterator, boolean outermost, final boolean immediate) {
        if (!iterator.hasNext()) {
            if (outermost) {
                return ObjectProducer.NULL_PRODUCER;
            }
            throw LoggingMessages.MESSAGES.unexpectedEnd();
        }
        final String token = iterator.next();
        if ("accept".equals(token)) {
            return ACCEPT_PRODUCER;
        } else if ("deny".equals(token)) {
            return DENY_PRODUCER;
        } else if ("not".equals(token)) {
            expect("(", iterator);
            final ObjectProducer nested = parseFilterExpression(iterator, false, immediate);
            expect(")", iterator);
            return new ObjectProducer() {
                public Object getObject() {
                    return new InvertFilter((Filter) nested.getObject());
                }
            };
        } else if ("all".equals(token)) {
            expect("(", iterator);
            final List<ObjectProducer> producers = new ArrayList<ObjectProducer>();
            do {
                producers.add(parseFilterExpression(iterator, false, immediate));
            } while (expect(",", ")", iterator));
            return new ObjectProducer() {
                public Object getObject() {
                    final int length = producers.size();
                    final Filter[] filters = new Filter[length];
                    for (int i = 0; i < length; i++) {
                        filters[i] = (Filter) producers.get(i).getObject();
                    }
                    return new AllFilter(filters);
                }
            };
        } else if ("any".equals(token)) {
            expect("(", iterator);
            final List<ObjectProducer> producers = new ArrayList<ObjectProducer>();
            do {
                producers.add(parseFilterExpression(iterator, false, immediate));
            } while (expect(",", ")", iterator));
            return new ObjectProducer() {
                public Object getObject() {
                    final int length = producers.size();
                    final Filter[] filters = new Filter[length];
                    for (int i = 0; i < length; i++) {
                        filters[i] = (Filter) producers.get(i).getObject();
                    }
                    return new AnyFilter(filters);
                }
            };
        } else if ("levelChange".equals(token)) {
            expect("(", iterator);
            final String levelName = expectName(iterator);
            final Level level = logContext.getLevelForName(levelName);
            expect(")", iterator);
            return new SimpleObjectProducer(new LevelChangingFilter(level));
        } else if ("levels".equals(token)) {
            expect("(", iterator);
            final Set<Level> levels = new HashSet<Level>();
            do {
                levels.add(logContext.getLevelForName(expectName(iterator)));
            } while (expect(",", ")", iterator));
            return new SimpleObjectProducer(new LevelFilter(levels));
        } else if ("levelRange".equals(token)) {
            final boolean minInclusive = expect("[", "(", iterator);
            final Level minLevel = logContext.getLevelForName(expectName(iterator));
            expect(",", iterator);
            final Level maxLevel = logContext.getLevelForName(expectName(iterator));
            final boolean maxInclusive = expect("]", ")", iterator);
            return new SimpleObjectProducer(new LevelRangeFilter(minLevel, minInclusive, maxLevel, maxInclusive));
        } else if ("match".equals(token)) {
            expect("(", iterator);
            final String pattern = expectString(iterator);
            expect(")", iterator);
            return new SimpleObjectProducer(new RegexFilter(pattern));
        } else if ("substitute".equals(token)) {
            expect("(", iterator);
            final String pattern = expectString(iterator);
            expect(",", iterator);
            final String replacement = expectString(iterator);
            expect(")", iterator);
            return new SimpleObjectProducer(new SubstituteFilter(pattern, replacement, false));
        } else if ("substituteAll".equals(token)) {
            expect("(", iterator);
            final String pattern = expectString(iterator);
            expect(",", iterator);
            final String replacement = expectString(iterator);
            expect(")", iterator);
            return new SimpleObjectProducer(new SubstituteFilter(pattern, replacement, true));
        } else {
            final String name = expectName(iterator);
            if (!filters.containsKey(name) || immediate && !filterRefs.containsKey(name)) {
                throw LoggingMessages.MESSAGES.filterNotFound(name);
            }
            if (immediate) {
                return new SimpleObjectProducer(filterRefs.get(name));
            } else {
                return new RefProducer(name, filterRefs);
            }
        }
    }

    private static String expectName(Iterator<String> iterator) {
        if (iterator.hasNext()) {
            final String next = iterator.next();
            if (isJavaIdentifierStart(next.codePointAt(0))) {
                return next;
            }
        }
        throw LoggingMessages.MESSAGES.expectedIdentifier();
    }

    private static String expectString(Iterator<String> iterator) {
        if (iterator.hasNext()) {
            final String next = iterator.next();
            if (next.codePointAt(0) == '"') {
                return next.substring(1);
            }
        }
        throw LoggingMessages.MESSAGES.expectedString();
    }

    private static boolean expect(String trueToken, String falseToken, Iterator<String> iterator) {
        final boolean hasNext = iterator.hasNext();
        final String next = hasNext ? iterator.next() : null;
        final boolean result;
        if (!hasNext || !((result = trueToken.equals(next)) || falseToken.equals(next))) {
            throw LoggingMessages.MESSAGES.expected(trueToken, falseToken);
        }
        return result;
    }

    private static void expect(String token, Iterator<String> iterator) {
        if (!iterator.hasNext() || !token.equals(iterator.next())) {
            throw LoggingMessages.MESSAGES.expected(token);
        }
    }

    private ObjectProducer parseFilterExpression(String expression, final boolean immediate) {
        if (expression == null) {
            return ObjectProducer.NULL_PRODUCER;
        }
        final Iterator<String> iterator = tokens(expression).iterator();
        final ObjectProducer result = parseFilterExpression(iterator, true, immediate);
        if (iterator.hasNext()) {
            throw LoggingMessages.MESSAGES.extraData();
        }
        return result;
    }

    ObjectProducer parseFilterExpression(String expression) {
        return parseFilterExpression(expression, false);
    }
}
