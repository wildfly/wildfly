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

package org.jboss.as.clustering.infinispan;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.lang.reflect.Field;

import org.infinispan.CacheException;
import org.infinispan.marshall.VersionAwareMarshaller;
import org.infinispan.marshall.jboss.JBossMarshaller;
import org.jboss.marshalling.Marshalling;

/**
 * @author Paul Ferraro
 */
public class StreamingMarshaller extends VersionAwareMarshaller {
    private static final String MARSHALLER = "river";

    public StreamingMarshaller() {
        try {
            Field field = VersionAwareMarshaller.class.getDeclaredField("defaultMarshaller");
            field.setAccessible(true);
            field.set(this, new Marshaller());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static class Marshaller extends JBossMarshaller {

        private ThreadLocal<org.jboss.marshalling.Marshaller> localMarshaller = new ThreadLocal<org.jboss.marshalling.Marshaller>() {
            @Override
            protected org.jboss.marshalling.Marshaller initialValue() {
                try {
                    return Marshaller.this.createMarshaller();
                } catch (IOException e) {
                    throw new CacheException(e);
                }
            }
        };

        private ThreadLocal<org.jboss.marshalling.Unmarshaller> localUnmarshaller = new ThreadLocal<org.jboss.marshalling.Unmarshaller>() {
            @Override
            protected org.jboss.marshalling.Unmarshaller initialValue() {
                try {
                    return Marshaller.this.createUnmarshaller();
                } catch (IOException e) {
                    throw new CacheException(e);
                }
            }
        };

        Marshaller() {
            super();
            if (this.factory == null) {
                this.factory = Marshalling.getMarshallerFactory(MARSHALLER, VersionAwareMarshaller.class.getClassLoader());
                if (this.factory == null) {
                    throw new IllegalStateException("Failed to Marshalling.getMarshallerFactory(\"river\") returned null!!");
                }
            }
        }

        org.jboss.marshalling.Marshaller createMarshaller() throws IOException {
            return this.factory.createMarshaller(this.configuration);
        }

        org.jboss.marshalling.Unmarshaller createUnmarshaller() throws IOException {
            return this.factory.createUnmarshaller(this.configuration);
        }

        @Override
        public ObjectOutput startObjectOutput(OutputStream os, boolean isReentrant) throws IOException {
            org.jboss.marshalling.Marshaller marshaller = isReentrant ? this.createMarshaller() : this.localMarshaller.get();
            marshaller.start(Marshalling.createByteOutput(os));
            return marshaller;
        }

        @Override
        public ObjectInput startObjectInput(InputStream is, boolean isReentrant) throws IOException {
            org.jboss.marshalling.Unmarshaller unmarshaller = isReentrant ? this.createUnmarshaller() : this.localUnmarshaller.get();
            unmarshaller.start(Marshalling.createByteInput(is));
            return unmarshaller;
        }
    }
}
