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

package org.jboss.as.clustering;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;

/**
 * @author Paul Ferraro
 */
public class MarshallingContext {
    private final MarshallerFactory factory;
    private final MarshallingConfiguration configuration;

    public MarshallingContext(MarshallerFactory factory, MarshallingConfiguration configuration) {
        this.factory = factory;
        this.configuration = configuration;
    }

    public Unmarshaller createUnmarshaller() throws IOException {
        return this.factory.createUnmarshaller(this.configuration);
    }

    public Marshaller createMarshaller() throws IOException {
        return this.factory.createMarshaller(this.configuration);
    }

    // AS7-2496 Workaround
    public ClassLoader getContextClassLoader() {
        final ClassResolver resolver = configuration.getClassResolver();
        if (resolver != null) {
            PrivilegedAction<Method> action = new PrivilegedAction<Method>() {
                @Override
                public Method run() {
                    try {
                        Method method = resolver.getClass().getDeclaredMethod("getClassLoader");
                        method.setAccessible(true);
                        return method;
                    } catch (NoSuchMethodException e) {
                        return null;
                    }
                }
            };
            Method method = AccessController.doPrivileged(action);
            if (method != null) {
                try {
                    return (ClassLoader) method.invoke(resolver);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        return null;
    }
}
