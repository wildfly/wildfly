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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;

import org.apache.log4j.Appender;
import org.jboss.as.logging.LoggingMessages;
import org.jboss.logmanager.config.ObjectConfigurable;
import org.jboss.logmanager.config.PropertyConfigurable;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * Handles setting generic properties.
 * <p/>
 * <b>Note:</b> This class does have some specific handling for log4j appenders. Ideally this would be in the {@link
 * HandlerConfigurationImpl}, but only works here.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class AbstractPropertyConfiguration<T, C extends AbstractPropertyConfiguration<T, C>> extends AbstractBasicConfiguration<T, C> implements ObjectConfigurable, PropertyConfigurable {
    private final Class<? extends T> actualClass;
    private final Class<? extends Appender> appenderClass;
    private final String moduleName;
    private final String className;
    private final String[] constructorProperties;
    private final Map<String, String> properties = new LinkedHashMap<String, String>(0);

    protected AbstractPropertyConfiguration(final Class<T> baseClass, final LogContextConfigurationImpl configuration, final Map<String, T> refs, final Map<String, C> configs, final String name, final String moduleName, final String className, final String[] constructorProperties) {
        super(name, configuration, refs, configs);
        if (className == null) {
            throw LoggingMessages.MESSAGES.nullVar("className");
        }
        this.constructorProperties = constructorProperties;
        final ClassLoader classLoader;
        if (moduleName != null) try {
            classLoader = ModuleFinder.getClassLoader(moduleName);
        } catch (Throwable e) {
            throw LoggingMessages.MESSAGES.cannotLoadModule(e, moduleName, getDescription(), name);
        }
        else {
            classLoader = getClass().getClassLoader();
        }
        final Class<? extends T> actualClass;
        Class<?> temp;
        try {
            temp = Class.forName(className, true, classLoader);
            if (baseClass == Handler.class && Appender.class.isAssignableFrom(temp)) {
                actualClass = Log4jAppenderHandler.class.asSubclass(baseClass);
                appenderClass = temp.asSubclass(Appender.class);
            } else {
                actualClass = temp.asSubclass(baseClass);
                appenderClass = null;
            }
        } catch (Exception e) {
            throw LoggingMessages.MESSAGES.failedToLoadClass(e, className, getDescription(), name);
        }
        this.moduleName = moduleName;
        this.className = className;
        this.actualClass = actualClass;
    }

    ConfigAction<T> getConstructAction() {
        return new ConstructAction();
    }

    abstract String getDescription();

    class ConstructAction implements ConfigAction<T> {

        public T validate() throws IllegalArgumentException {
            final int length = constructorProperties.length;
            final Class<?>[] paramTypes = new Class<?>[length];
            for (int i = 0; i < length; i++) {
                final String property = constructorProperties[i];
                final Class<?> type = getConstructorPropertyType(actualClass, property);
                if (type == null) {
                    throw LoggingMessages.MESSAGES.invalidProperty(property, getDescription(), getName(), appenderClass);
                }
                paramTypes[i] = type;
            }
            final Constructor<? extends T> constructor;
            final Constructor<? extends Appender> appenderConstructor;
            try {
                // If this is a log4j appender, this requires special construction
                if (appenderClass != null) {
                    constructor = actualClass.getConstructor(Appender.class);
                    appenderConstructor = appenderClass.getConstructor(paramTypes);
                } else {
                    constructor = actualClass.getConstructor(paramTypes);
                    appenderConstructor = null;
                }
            } catch (Exception e) {
                throw LoggingMessages.MESSAGES.failedToLocateConstructor(e, className, getDescription(), getName());
            }
            final Object[] params = new Object[length];
            final Class<?> c = (appenderClass == null ? actualClass : appenderClass);
            for (int i = 0; i < length; i++) {
                final String property = constructorProperties[i];
                if (!properties.containsKey(property)) {
                    throw LoggingMessages.MESSAGES.invalidProperty(property, getDescription(), getName(), c);
                }
                final String valueString = properties.get(property);
                final Object value = getConfiguration().getValue(c, property, paramTypes[i], valueString, true).getObject();
                params[i] = value;
            }
            try {
                if (appenderConstructor != null) {
                    getConfiguration().addAppenderName(getName());
                    return constructor.newInstance(appenderConstructor.newInstance(params));
                }
                return constructor.newInstance(params);
            } catch (Exception e) {
                throw LoggingMessages.MESSAGES.cannotInstantiateClass(e, className, getDescription(), getName());
            }
        }

        public void applyPreCreate(final T param) {
            getRefs().put(getName(), param);
        }

        public void applyPostCreate(T param) {
        }

        public void rollback() {
            getConfigs().remove(getName());
        }
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public String getClassName() {
        return className;
    }

    /**
     * Special case for log4j appenders
     */
    void activate() {
        if (isLog4jAppender()) {
            ((Log4jAppenderHandler) getRefs().get(getName())).activate();
        }
    }

    /**
     * Checks to see if this is a log4j appender.
     *
     * @return {@code true} if this is a log4j appender, otherwise {@code false}
     */
    boolean isLog4jAppender() {
        return actualClass == Log4jAppenderHandler.class;
    }

    static boolean contains(Object[] array, Object val) {
        for (Object o : array) {
            if (o.equals(val)) return true;
        }
        return false;
    }

    @Override
    public void setPropertyValueString(final String propertyName, final String value) throws IllegalArgumentException {
        if (isRemoved()) {
            throw LoggingMessages.MESSAGES.cannotSetRemovedProperty(propertyName, getDescription(), getName());
        }
        if (propertyName == null) {
            throw LoggingMessages.MESSAGES.nullVar("propertyName");
        }
        final boolean replacement = properties.containsKey(propertyName);
        final boolean constructorProp = contains(constructorProperties, propertyName);
        final Class<?> useClass = (appenderClass == null ? actualClass : appenderClass);
        final Method setter = getPropertySetter(useClass, propertyName);
        if (setter == null && !constructorProp) {
            throw LoggingMessages.MESSAGES.propertySetterNotFound(propertyName, getDescription(), getName());
        }
        final String oldValue = properties.put(propertyName, value);
        getConfiguration().addAction(new ConfigAction<ObjectProducer>() {
            public ObjectProducer validate() throws IllegalArgumentException {
                if (setter == null) {
                    return ObjectProducer.NULL_PRODUCER;
                }
                final Class<?> propertyType = getPropertyType(useClass, propertyName);
                if (propertyType == null) {
                    throw LoggingMessages.MESSAGES.propertyTypeNotFound(propertyName, getDescription(), getName());
                }
                return getConfiguration().getValue(useClass, propertyName, propertyType, value, false);
            }

            public void applyPreCreate(final ObjectProducer param) {
            }

            public void applyPostCreate(final ObjectProducer param) {
                if (setter != null) {
                    final T instance = getRefs().get(getName());
                    if (isLog4jAppender()) {
                        try {
                            setter.invoke(((Log4jAppenderHandler) instance).getAppender(), param.getObject());
                        } catch (Throwable e) {
                            // TODO for now this will do, we can't assume loggers are available
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            setter.invoke(instance, param.getObject());
                        } catch (Throwable e) {
                            // TODO for now this will do, we can't assume loggers are available
                            e.printStackTrace();
                        }
                    }
                }
            }

            public void rollback() {
                if (replacement) {
                    properties.put(propertyName, oldValue);
                } else {
                    properties.remove(propertyName);
                }
            }
        });
    }

    @Override
    public String getPropertyValueString(final String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public boolean hasProperty(final String propertyName) {
        return properties.containsKey(propertyName);
    }

    @Override
    public boolean removeProperty(final String propertyName) {
        if (isRemoved()) {
            throw LoggingMessages.MESSAGES.propertyAlreadyRemoved(propertyName, getDescription(), getName());
        }
        try {
            return properties.containsKey(propertyName);
        } finally {
            properties.remove(propertyName);
        }
    }

    @Override
    public List<String> getPropertyNames() {
        return new ArrayList<String>(properties.keySet());
    }

    @Override
    public boolean hasConstructorProperty(final String propertyName) {
        return contains(constructorProperties, propertyName);
    }

    Class<? extends T> getActualClass() {
        return actualClass;
    }

    @Override
    public List<String> getConstructorProperties() {
        return Arrays.asList(constructorProperties);
    }

    static Class<?> getPropertyType(Class<?> clazz, String propertyName) {
        final Method setter = getPropertySetter(clazz, propertyName);
        return setter != null ? setter.getParameterTypes()[0] : null;
    }

    static Class<?> getConstructorPropertyType(Class<?> clazz, String propertyName) {
        final Method getter = getPropertyGetter(clazz, propertyName);
        return getter != null ? getter.getReturnType() : getPropertyType(clazz, propertyName);
    }

    static Method getPropertySetter(Class<?> clazz, String propertyName) {
        final String upperPropertyName = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        final String set = "set" + upperPropertyName;
        for (Method method : clazz.getMethods()) {
            if ((method.getName().equals(set) && Modifier.isPublic(method.getModifiers())) && method.getParameterTypes().length == 1) {
                return method;
            }
        }
        return null;
    }

    static Method getPropertyGetter(Class<?> clazz, String propertyName) {
        final String upperPropertyName = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        final String get = "get" + upperPropertyName;
        for (Method method : clazz.getMethods()) {
            if ((method.getName().equals(get) && Modifier.isPublic(method.getModifiers())) && method.getParameterTypes().length == 0) {
                return method;
            }
        }
        return null;
    }

    static class ModuleFinder {

        private ModuleFinder() {
        }

        static ClassLoader getClassLoader(final String moduleName) throws Exception {
            ModuleLoader moduleLoader = ModuleLoader.forClass(ModuleFinder.class);
            if (moduleLoader == null) {
                moduleLoader = Module.getBootModuleLoader();
            }
            return moduleLoader.loadModule(ModuleIdentifier.create(moduleName)).getClassLoader();
        }
    }
}
