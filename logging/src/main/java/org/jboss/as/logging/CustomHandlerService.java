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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Handler;
import java.util.logging.Level;

import static org.jboss.as.logging.CommonAttributes.CUSTOM_HANDLER;

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
    private Map<String, Method> methods;

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
        methods = Collections.emptyMap();
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
                methods = findSetterMethods(handlerClass);
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
            setProperty(handler, property);
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

    /**
     * Set all the properties from the list.
     * <p/>
     * Note this method is not synchronized and should be called in a synchronized block or method.
     *
     * @param handler    the handler to set the properties for.
     * @param properties the properties to set.
     */
    private void setProperties(final Handler handler, final List<Property> properties) {
        for (Property property : properties) {
            setProperty(handler, property);
        }
    }

    /**
     * Sets the property on the handler.
     * <p/>
     * Note this method is not synchronized and should be called in a synchronized block or method.
     *
     * @param property the property to set.
     */
    private void setProperty(final Handler handler, final Property property) {
        final String setterMethod = setterName(property.getName());
        try {
            if (methods.containsKey(setterMethod)) {
                final Method method = methods.get(setterMethod);
                final Object arg = getArgument(method, property.getName(), property.getValue().asString());
                method.invoke(handler, arg);
                ROOT_LOGGER.debugf("Set property '%s' with value of '%s' on handler '%s'.", property.getName(), property.getValue().asString(), handler.getClass().getName());
            } else {
                ROOT_LOGGER.warnf("Unknown property '%s' on '%s'.", property.getName(), handler.getClass().getName());
            }
        } catch (final Throwable t) {
            ROOT_LOGGER.warnf(t, "An error occurred trying to set the property '%s' on handler '%s'.", property.getName(), handler.getClass().getName());
        }
    }

    /**
     * Creates the argument for the setter method.
     *
     * @param method       the method to check the type parameter of.
     * @param propertyName the property name.
     * @param propValue    the property value.
     *
     * @return the argument for the property.
     *
     * @throws IllegalArgumentException if an error occurs.
     */
    private Object getArgument(final Method method, final String propertyName, final String propValue) throws IllegalArgumentException {
        final Class<?> objClass = method.getDeclaringClass();
        final Object argument;
        final Class<?> paramType = method.getParameterTypes()[0];
        if (paramType == String.class) {
            argument = propValue;
        } else if (paramType == boolean.class || paramType == Boolean.class) {
            argument = Boolean.valueOf(propValue);
        } else if (paramType == byte.class || paramType == Byte.class) {
            argument = Byte.valueOf(propValue);
        } else if (paramType == short.class || paramType == Short.class) {
            argument = Short.valueOf(propValue);
        } else if (paramType == int.class || paramType == Integer.class) {
            argument = Integer.valueOf(propValue);
        } else if (paramType == long.class || paramType == Long.class) {
            argument = Long.valueOf(propValue);
        } else if (paramType == float.class || paramType == Float.class) {
            argument = Float.valueOf(propValue);
        } else if (paramType == double.class || paramType == Double.class) {
            argument = Double.valueOf(propValue);
        } else if (paramType == char.class || paramType == Character.class) {
            argument = Character.valueOf(propValue.length() > 0 ? propValue.charAt(0) : 0);
        } else if (paramType == BigDecimal.class) {
            argument = new BigDecimal(propValue);
        } else if (paramType == File.class) {
            argument = new File(propValue);
        } else if (paramType == Level.class) {
            argument = Level.parse(propValue);
        } else if (paramType == TimeZone.class) {
            argument = TimeZone.getTimeZone(propValue);
        } else if (paramType == Charset.class) {
            argument = Charset.forName(propValue);
        } else if (paramType.isEnum()) {
            argument = Enum.valueOf(paramType.asSubclass(Enum.class), propValue);
        } else {
            throw new IllegalArgumentException(String.format("Unknown parameter type for property %s on %s", propertyName, objClass));
        }
        return argument;
    }

    /**
     * Loads all the setter methods found in the handler class.
     *
     * @param handlerClass the handler class to find the setter methods for.
     *
     * @return all public setter methods.
     */
    private static Map<String, Method> findSetterMethods(final Class<?> handlerClass) {
        final Map<String, Method> result = new HashMap<String, Method>();
        for (Method method : handlerClass.getMethods()) {
            final int modifiers = method.getModifiers();
            if (Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                continue;
            }
            if (!method.getName().startsWith("set")) {
                continue;
            }
            final Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                continue;
            }
            if (method.getReturnType() != void.class) {
                continue;
            }
            result.put(method.getName(), method);
        }
        return result;
    }

    /**
     * Create the setter name for the property.
     *
     * @param propertyName the property name.
     *
     * @return the property name prefixed with set.
     */
    private static String setterName(final String propertyName) {
        final StringBuilder sb = new StringBuilder("set");
        sb.append(Character.toUpperCase(propertyName.charAt(0)));
        sb.append(propertyName.substring(1));
        return sb.toString();
    }
}
