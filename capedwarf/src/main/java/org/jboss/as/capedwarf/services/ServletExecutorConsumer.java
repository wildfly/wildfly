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

package org.jboss.as.capedwarf.services;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.servlet.ServletRequest;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * JMS consumer for servlet executor.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class ServletExecutorConsumer implements MessageListener {

    private static final String PREFIX = "org.jboss.capedwarf.jms.";

    private final Map<ClassLoader, Map<String, Object>> cache = new HashMap<ClassLoader, Map<String, Object>>();
    private final ModuleLoader loader;

    public ServletExecutorConsumer(ModuleLoader loader) {
        this.loader = loader;
    }

    protected String getValue(final Message message, final String key) throws Exception {
        final String value = message.getStringProperty(PREFIX + key);
        if (value == null)
            throw new IllegalArgumentException("Null value for key: " + key);
        return value;
    }

    protected ModuleIdentifier parseModuleIdentifier(Message msg) throws Exception {
        String mi = getValue(msg, "module");
        return ModuleIdentifier.create(mi);
    }

    private ServletRequest createServletRequest(Message message, ClassLoader cl) throws Exception {
        final String factoryClass = getValue(message, "factory");
        Object factory;
        synchronized (cache) {
            Map<String, Object> factories = cache.get(cl);
            if (factories == null) {
                factories = new HashMap<String, Object>();
                cache.put(cl, factories);
            }
            factory = factories.get(factoryClass);
            if (factory == null) {
                Class<?> clazz = cl.loadClass(factoryClass);
                factory = clazz.newInstance();
                factories.put(factoryClass, factory);
            }
        }
        Method m = factory.getClass().getMethod("createServletRequest", Message.class);
        return (ServletRequest) m.invoke(factory, message);
    }

    public void onMessage(Message message) {
        try {
            final ClassLoader previous = Thread.currentThread().getContextClassLoader();
            final Module module = loader.loadModule(parseModuleIdentifier(message));
            final ClassLoader cl = module.getClassLoader();
            Thread.currentThread().setContextClassLoader(cl);
            try {
                final String appId = getValue(message, "appId");
                final String path = getValue(message, "path");
                final ServletRequest request = createServletRequest(message, cl);
                ServletExecutor.dispatch(appId, path, request);
            } finally {
                Thread.currentThread().setContextClassLoader(previous);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void removeClassLoader(ClassLoader classLoader) {
        synchronized (cache) {
            cache.remove(classLoader);
        }
    }
}
