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

package org.jboss.as.logging.handlers.custom;

import static org.jboss.as.logging.LoggingMessages.MESSAGES;
import static org.jboss.as.logging.handlers.custom.PropertiesConfigurator.setProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Handler;

import org.jboss.as.logging.handlers.HandlerService;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service for custom handlers.
 * <p/>
 * Date: 03.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class CustomHandlerService extends HandlerService<Handler> {
    private final String className;
    private final String moduleName;
    private final List<Property> properties;

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
    protected Handler createHandler() throws StartException {
        final Handler handler;
        final ModuleLoader moduleLoader = Module.forClass(CustomHandlerService.class).getModuleLoader();
        final ModuleIdentifier id = ModuleIdentifier.create(moduleName);
        try {
            final Class<?> handlerClass = Class.forName(className, false, moduleLoader.loadModule(id).getClassLoader());
            if (Handler.class.isAssignableFrom(handlerClass)) {
                handler = (Handler) handlerClass.newInstance();
            } else {
                throw MESSAGES.invalidType(className, Handler.class);
            }
        } catch (ClassNotFoundException e) {
            throw MESSAGES.classNotFound(e, className);
        } catch (ModuleLoadException e) {
            throw MESSAGES.cannotLoadModule(e, moduleName);
        } catch (InstantiationException e) {
            throw MESSAGES.cannotInstantiateClass(e, className);
        } catch (IllegalAccessException e) {
            throw MESSAGES.cannotAccessClass(e, className);
        }
        return handler;
    }

    @Override
    protected void start(final StartContext context, final Handler handler) throws StartException {
        // Set the properties
        setProperties(handler, properties);
    }

    @Override
    protected void stop(final StopContext context, final Handler handler) {
        properties.clear();
    }

    public synchronized void addProperty(final Property property) {
        properties.add(property);
        final Handler handler = getValue();
        if (handler != null) {
            setProperties(handler, this.properties);
        }
    }


    public synchronized void addProperties(final Collection<Property> properties) {
        this.properties.addAll(properties);
        final Handler handler = getValue();
        if (handler != null) {
            setProperties(handler, this.properties);
        }
    }
}
