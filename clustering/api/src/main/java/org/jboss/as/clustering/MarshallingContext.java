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
    private final VersionedMarshallingConfiguration configuration;

    public MarshallingContext(MarshallerFactory factory, VersionedMarshallingConfiguration configuration) {
        this.factory = factory;
        this.configuration = configuration;
    }

    public int getCurrentVersion() {
        return this.configuration.getCurrentMarshallingVersion();
    }

    public Unmarshaller createUnmarshaller(int version) throws IOException {
        return this.factory.createUnmarshaller(this.getMarshallingConfiguration(version));
    }

    public Marshaller createMarshaller(int version) throws IOException {
        return this.factory.createMarshaller(this.getMarshallingConfiguration(version));
    }

    // AS7-2496 Workaround
    public ClassLoader getContextClassLoader(int version) {
        final ClassResolver resolver = this.getMarshallingConfiguration(version).getClassResolver();
        return (resolver instanceof ClassLoaderProvider) ? ((ClassLoaderProvider) resolver).getClassLoader() : null;
    }

    private MarshallingConfiguration getMarshallingConfiguration(int version) {
        return this.configuration.getMarshallingConfiguration(version);
    }
}
