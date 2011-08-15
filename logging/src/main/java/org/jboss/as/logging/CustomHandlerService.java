/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;

import static org.jboss.as.logging.CommonAttributes.CUSTOM_HANDLER;
import static org.jboss.as.logging.LogHandlerPropertiesConfigurator.setProperties;

/**
 * Service for custom handlers.
 * <p/>
 * Date: 03.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class CustomHandlerService implements Service<Handler> {
    private static final Logger ROOT_LOGGER = Logger.getLogger(CustomHandlerService.class.getPackage().getName());
    private final String className;
    private final String moduleName;
    private final List<Property> properties;

    private AbstractFormatterSpec formatterSpec;
    private Level level;
    private String encoding;
    private Handler value;

    /**
     * Creates a new custom handler service.
     *
     * @param className  the handler class name.
     * @param moduleName the module name the handler class is dependent on.
     */
    public CustomHandlerService(final String className, final String moduleName) {
        this.className = className;
        this.moduleName = moduleName;
        properties = new ArrayList<Property>();
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final Handler handler;
        final ModuleLoader moduleLoader = Module.forClass(CustomHandlerService.class).getModuleLoader();
        final ModuleIdentifier id = ModuleIdentifier.create(moduleName);
        try {
            final Class<?> handlerClass = Class.forName(className, false, moduleLoader.loadModule(id).getClassLoader());
            if (Handler.class.isAssignableFrom(handlerClass)) {
                handler = (Handler) handlerClass.newInstance();
            } else {
                throw new StartException(String.format("%s %s is not a valid %s.", CUSTOM_HANDLER, className, Handler.class.getName()));
            }
        } catch (ClassNotFoundException e) {
            throw new StartException(e);
        } catch (ModuleLoadException e) {
            throw new StartException(String.format("%s %s is not a valid %s.", CUSTOM_HANDLER, className, Handler.class.getName()), e);
        } catch (InstantiationException e) {
            throw new StartException(String.format("%s %s is not a valid %s.", CUSTOM_HANDLER, className, Handler.class.getName()), e);
        } catch (IllegalAccessException e) {
            throw new StartException(String.format("%s %s is not a valid %s.", CUSTOM_HANDLER, className, Handler.class.getName()), e);
        }
        formatterSpec.apply(handler);
        if (level != null) handler.setLevel(level);
        try {
            handler.setEncoding(encoding);
        } catch (UnsupportedEncodingException e) {
            throw new StartException(e);
        }
        // Set the properties
        setProperties(handler, properties);
        value = handler;
    }

    @Override
    public synchronized void stop(final StopContext context) {
        final Handler handler = value;
        handler.close();
        properties.clear();
        value = null;
    }

    public synchronized void addProperty(final Property property) {
        properties.add(property);
        final Handler handler = value;
        if (handler != null) {
            setProperties(handler, this.properties);
        }
    }


    public synchronized void addProperties(final Collection<Property> properties) {
        this.properties.addAll(properties);
        final Handler handler = value;
        if (handler != null) {
            setProperties(handler, this.properties);
        }
    }

    @Override
    public synchronized Handler getValue() throws IllegalStateException {
        return value;
    }

    public synchronized Level getLevel() {
        return level;
    }

    public synchronized void setLevel(final Level level) {
        this.level = level;
        final Handler handler = value;
        if (handler != null) handler.setLevel(level);
    }


    public synchronized AbstractFormatterSpec getFormatterSpec() {
        return formatterSpec;
    }

    public synchronized void setFormatterSpec(final AbstractFormatterSpec formatterSpec) {
        this.formatterSpec = formatterSpec;
        final Handler handler = value;
        if (handler != null) formatterSpec.apply(handler);
    }

    public synchronized String getEncoding() {
        return encoding;
    }

    public synchronized void setEncoding(final String encoding) throws UnsupportedEncodingException {
        final Handler handler = value;
        if (handler != null) handler.setEncoding(encoding);
        this.encoding = encoding;
    }
}
