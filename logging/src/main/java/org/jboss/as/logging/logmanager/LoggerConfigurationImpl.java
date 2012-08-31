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

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.as.logging.LoggingMessages;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.config.LoggerConfiguration;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class LoggerConfigurationImpl extends AbstractBasicConfiguration<Logger, LoggerConfigurationImpl> implements LoggerConfiguration {
    private String filter;
    private Boolean useParentHandlers;
    private String level;
    private final List<String> handlerNames = new ArrayList<String>(0);

    LoggerConfigurationImpl(final String name, final LogContextConfigurationImpl configuration) {
        super(name, configuration, configuration.getLoggerRefs(), configuration.getLoggerConfigurations());
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(final String filter) {
        final String oldFilterName = this.filter;
        this.filter = filter;
        final LogContextConfigurationImpl configuration = getConfiguration();
        configuration.addAction(new ConfigAction<ObjectProducer>() {
            public ObjectProducer validate() throws IllegalArgumentException {
                return configuration.parseFilterExpression(filter);
            }

            public void applyPreCreate(final ObjectProducer param) {
            }

            public void applyPostCreate(final ObjectProducer param) {
                configuration.getHandlerRefs().get(getName()).setFilter((Filter) param.getObject());
            }

            public void rollback() {
                LoggerConfigurationImpl.this.filter = oldFilterName;
            }
        });
    }


    public Boolean getUseParentHandlers() {
        return useParentHandlers;
    }

    public void setUseParentHandlers(final Boolean useParentHandlers) {
        final Boolean oldUseParentHandlers = this.useParentHandlers;
        this.useParentHandlers = useParentHandlers;
        final LogContextConfigurationImpl configuration = getConfiguration();
        configuration.addAction(new ConfigAction<Void>() {
            public Void validate() throws IllegalArgumentException {
                return null;
            }

            public void applyPreCreate(final Void param) {
            }

            public void applyPostCreate(final Void param) {
                if (useParentHandlers != null)
                    configuration.getLoggerRefs().get(getName()).setUseParentHandlers(useParentHandlers.booleanValue());
            }

            public void rollback() {
                LoggerConfigurationImpl.this.useParentHandlers = oldUseParentHandlers;
            }
        });
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(final String level) {
        final String oldLevel = this.level;
        final String resolvedLevel = PropertyHelper.resolveValue(level);
        this.level = resolvedLevel;
        final LogContextConfigurationImpl configuration = getConfiguration();
        configuration.addAction(new ConfigAction<Level>() {
            public Level validate() throws IllegalArgumentException {
                return resolvedLevel == null ? null : configuration.getLogContext().getLevelForName(resolvedLevel);
            }

            public void applyPreCreate(final Level param) {
            }

            public void applyPostCreate(final Level param) {
                configuration.getLoggerRefs().get(getName()).setLevel(param);
            }

            public void rollback() {
                LoggerConfigurationImpl.this.level = oldLevel;
            }
        });
    }

    public List<String> getHandlerNames() {
        return new ArrayList<String>(handlerNames);
    }

    public void setHandlerNames(final String... names) {
        final String[] oldHandlerNames = handlerNames.toArray(new String[handlerNames.size()]);
        handlerNames.clear();
        final LinkedHashSet<String> strings = new LinkedHashSet<String>(asList(names));
        handlerNames.addAll(strings);
        final String[] stringsArray = strings.toArray(new String[strings.size()]);
        final LogContextConfigurationImpl configuration = getConfiguration();
        configuration.addAction(new ConfigAction<Void>() {
            public Void validate() throws IllegalArgumentException {
                for (String name : stringsArray) {
                    if (configuration.getHandlerConfiguration(name) == null) {
                        throw LoggingMessages.MESSAGES.handlerNotFound(name);
                    }
                }
                return null;
            }

            public void applyPreCreate(final Void param) {
            }

            public void applyPostCreate(final Void param) {
                final Map<String, Handler> handlerRefs = configuration.getHandlerRefs();
                final Map<String, Logger> loggerRefs = configuration.getLoggerRefs();
                final Logger logger = loggerRefs.get(getName());
                final int length = stringsArray.length;
                final Handler[] handlers = new Handler[length];
                for (int i = 0; i < length; i++) {
                    handlers[i] = handlerRefs.get(stringsArray[i]);
                }
                logger.setHandlers(handlers);
            }

            public void rollback() {
                handlerNames.clear();
                handlerNames.addAll(asList(oldHandlerNames));
            }
        });
    }

    public void setHandlerNames(final Collection<String> names) {
        setHandlerNames(names.toArray(new String[names.size()]));
    }

    public boolean addHandlerName(final String name) {
        final LogContextConfigurationImpl configuration = getConfiguration();
        if (handlerNames.contains(name)) {
            return false;
        }
        handlerNames.add(name);
        configuration.addAction(new ConfigAction<Void>() {
            public Void validate() throws IllegalArgumentException {
                if (configuration.getHandlerConfiguration(name) == null) {
                    throw LoggingMessages.MESSAGES.handlerNotFound(name);
                }
                return null;
            }

            public void applyPreCreate(final Void param) {
            }

            public void applyPostCreate(final Void param) {
                final Map<String, Handler> handlerRefs = configuration.getHandlerRefs();
                final Map<String, Logger> loggerRefs = configuration.getLoggerRefs();
                final Logger logger = loggerRefs.get(getName());
                logger.addHandler(handlerRefs.get(name));
            }

            public void rollback() {
                handlerNames.remove(name);
            }
        });
        return true;
    }

    public boolean removeHandlerName(final String name) {
        final LogContextConfigurationImpl configuration = getConfiguration();
        if (!handlerNames.contains(name)) {
            return false;
        }
        final int index = handlerNames.indexOf(name);
        handlerNames.remove(index);
        configuration.addAction(new ConfigAction<Void>() {
            public Void validate() throws IllegalArgumentException {
                return null;
            }

            public void applyPreCreate(final Void param) {
            }

            public void applyPostCreate(final Void param) {
                final Map<String, Handler> handlerRefs = configuration.getHandlerRefs();
                final Map<String, Logger> loggerRefs = configuration.getLoggerRefs();
                final Logger logger = (Logger) loggerRefs.get(getName());
                logger.removeHandler(handlerRefs.get(name));
            }

            public void rollback() {
                handlerNames.add(index, name);
            }
        });
        return true;
    }
}
