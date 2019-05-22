/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.cache;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.wildfly.clustering.marshalling.spi.Serializer;
import org.wildfly.clustering.web.IdentifierSerializerProvider;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public enum SessionIdentifierSerializer implements Serializer<String> {
    INSTANCE;

    private final Serializer<String> serializer = loadSerializer();

    private static Serializer<String> loadSerializer() {
        Iterator<IdentifierSerializerProvider> providers = load(IdentifierSerializerProvider.class).iterator();
        if (!providers.hasNext()) {
            throw new ServiceConfigurationError(IdentifierSerializerProvider.class.getName());
        }
        return providers.next().getSerializer();
    }

    private static <T> Iterable<T> load(Class<T> providerClass) {
        PrivilegedAction<Iterable<T>> action = new PrivilegedAction<Iterable<T>>() {
            @Override
            public Iterable<T> run() {
                return ServiceLoader.load(providerClass, providerClass.getClassLoader());
            }
        };
        return WildFlySecurityManager.doUnchecked(action);
    }

    @Override
    public void write(DataOutput output, String value) throws IOException {
        this.serializer.write(output, value);
    }

    @Override
    public String read(DataInput input) throws IOException {
        return this.serializer.read(input);
    }
}
