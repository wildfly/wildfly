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

package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;

/**
 * @author Paul Ferraro
 */
public class SimpleMarshallingContext implements MarshallingContext {

    private final MarshallerFactory factory;
    private final VersionedMarshallingConfiguration configuration;
    private final WeakReference<ClassLoader> loader;

    public SimpleMarshallingContext(MarshallerFactory factory, VersionedMarshallingConfiguration configuration, ClassLoader loader) {
        this.factory = factory;
        this.configuration = configuration;
        this.loader = new WeakReference<>(loader);
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.loader.get();
    }

    @Override
    public int getCurrentVersion() {
        return this.configuration.getCurrentMarshallingVersion();
    }

    @Override
    public Unmarshaller createUnmarshaller(int version) throws IOException {
        return this.factory.createUnmarshaller(this.getMarshallingConfiguration(version));
    }

    @Override
    public Marshaller createMarshaller(int version) throws IOException {
        return this.factory.createMarshaller(this.getMarshallingConfiguration(version));
    }

    private MarshallingConfiguration getMarshallingConfiguration(int version) {
        return this.configuration.getMarshallingConfiguration(version);
    }
}
