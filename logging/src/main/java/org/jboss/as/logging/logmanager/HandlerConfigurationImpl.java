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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.as.logging.LoggingMessages;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.config.HandlerConfiguration;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class HandlerConfigurationImpl extends AbstractPropertyConfiguration<Handler, HandlerConfigurationImpl> implements HandlerConfiguration {

    private final List<String> handlerNames = new ArrayList<String>(0);

    private String formatterName;
    private String level;
    private String filter;
    private String encoding;
    private String errorManagerName;
    private boolean requiresActivation;

    HandlerConfigurationImpl(final LogContextConfigurationImpl configuration, final String name, final String moduleName, final String className, final String[] constructorProperties) {
        super(Handler.class, configuration, configuration.getHandlerRefs(), configuration.getHandlerConfigurations(), name, moduleName, className, constructorProperties);
    }

    public String getFormatterName() {
        return formatterName;
    }

    public void setFormatterName(final String formatterName) {
        final String oldFormatterName = this.formatterName;
        this.formatterName = formatterName;
        final LogContextConfigurationImpl configuration = getConfiguration();
        configuration.addAction(new ConfigAction<Void>() {
            public Void validate() throws IllegalArgumentException {
                if (formatterName != null && configuration.getFormatterConfiguration(formatterName) == null) {
                    throw LoggingMessages.MESSAGES.formatterNotFound(formatterName);
                }
                return null;
            }

            public void applyPreCreate(final Void param) {
            }

            public void applyPostCreate(final Void param) {
                configuration.getHandlerRefs().get(getName()).setFormatter(formatterName == null ? null : configuration.getFormatterRefs().get(formatterName));
            }

            public void rollback() {
                HandlerConfigurationImpl.this.formatterName = oldFormatterName;
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
                configuration.getHandlerRefs().get(getName()).setLevel(param);
            }

            public void rollback() {
                HandlerConfigurationImpl.this.level = oldLevel;
            }
        });
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
                HandlerConfigurationImpl.this.filter = oldFilterName;
            }
        });
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(final String encoding) {
        final String oldEncoding = this.encoding;
        this.encoding = encoding;
        final LogContextConfigurationImpl configuration = getConfiguration();
        configuration.addAction(new ConfigAction<Void>() {
            public Void validate() throws IllegalArgumentException {
                if (encoding != null) {
                    try {
                        Charset.forName(encoding);
                    } catch (Throwable t) {
                        throw LoggingMessages.MESSAGES.unsupportedCharSet(encoding);
                    }
                }
                return null;
            }

            public void applyPreCreate(final Void param) {
            }

            public void applyPostCreate(final Void param) {
                try {
                    configuration.getHandlerRefs().get(getName()).setEncoding(encoding);
                } catch (UnsupportedEncodingException e) {
                    throw LoggingMessages.MESSAGES.failedToSetHandlerEncoding(e, encoding);
                }
            }

            public void rollback() {
                HandlerConfigurationImpl.this.encoding = oldEncoding;
            }
        });
    }

    public String getErrorManagerName() {
        return errorManagerName;
    }

    public void setErrorManagerName(final String errorManagerName) {
        final String oldErrorManagerName = this.errorManagerName;
        this.errorManagerName = errorManagerName;
        final LogContextConfigurationImpl configuration = getConfiguration();
        configuration.addAction(new ConfigAction<Void>() {
            public Void validate() throws IllegalArgumentException {
                if (errorManagerName != null && configuration.getErrorManagerConfiguration(errorManagerName) == null) {
                    throw LoggingMessages.MESSAGES.errorManagerNotFound(errorManagerName);
                }
                return null;
            }

            public void applyPreCreate(final Void param) {
            }

            public void applyPostCreate(final Void param) {
                configuration.getHandlerRefs().get(getName()).setErrorManager(errorManagerName == null ? null : configuration.getErrorManagerRefs().get(errorManagerName));
            }

            public void rollback() {
                HandlerConfigurationImpl.this.errorManagerName = oldErrorManagerName;
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
        if (! ExtHandler.class.isAssignableFrom(getActualClass())) {
            if (names.length == 0) {
                return;
            }
            throw LoggingMessages.MESSAGES.nestedHandlersNotSupported(getActualClass());
        }
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
                final ExtHandler handler = (ExtHandler) handlerRefs.get(getName());
                final int length = stringsArray.length;
                final Handler[] handlers = new Handler[length];
                for (int i = 0; i < length; i ++) {
                    handlers[i] = handlerRefs.get(stringsArray[i]);
                }
                handler.setHandlers(handlers);
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
        if (! ExtHandler.class.isAssignableFrom(getActualClass())) {
            throw LoggingMessages.MESSAGES.nestedHandlersNotSupported(getActualClass());
        }
        if (handlerNames.contains(name)) {
            return false;
        }
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
                final ExtHandler handler = (ExtHandler) handlerRefs.get(getName());
                handler.addHandler(handlerRefs.get(name));
            }

            public void rollback() {
                handlerNames.remove(name);
            }
        });
        return true;
    }

    public boolean removeHandlerName(final String name) {
        final LogContextConfigurationImpl configuration = getConfiguration();
        if (! ExtHandler.class.isAssignableFrom(getActualClass())) {
            return false;
        }
        if (! handlerNames.contains(name)) {
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
                final ExtHandler handler = (ExtHandler) handlerRefs.get(getName());
                handler.removeHandler(handlerRefs.get(name));
            }

            public void rollback() {
                handlerNames.add(index, name);
            }
        });
        return true;
    }

    String getDescription() {
        return "handler";
    }


    @Override
    ConfigAction<Void> getRemoveAction() {
        return new ConfigAction<Void>() {
            @Override
            public Void validate() throws IllegalArgumentException {
                return null;
            }

            @Override
            public void applyPreCreate(final Void param) {
                final Handler handler = refs.remove(getName());
                if (handler != null) {
                    handler.close();
                }
            }

            @Override
            public void applyPostCreate(final Void param) {
            }

            @Override
            public void rollback() {
                configs.put(getName(), HandlerConfigurationImpl.this);
                clearRemoved();
            }
        };
    }
}
