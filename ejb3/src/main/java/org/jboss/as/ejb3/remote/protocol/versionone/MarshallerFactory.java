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

package org.jboss.as.ejb3.remote.protocol.versionone;

import org.jboss.logging.Logger;
import org.jboss.marshalling.AbstractClassResolver;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;

/**
 * TODO: Use the one from the ejb-client API project once the contract is settled
 * <p/>
 * User: jpai
 */
class MarshallerFactory {

    private static final Logger logger = Logger.getLogger(MarshallerFactory.class);

    public static Marshaller createMarshaller(final String marshallerType) throws IOException {
        if ("river".equals(marshallerType)) {
            return new JBossMarshaller(marshallerType);
        }
        if ("java-serial".equals(marshallerType)) {
            return new JavaSerialMarshaller();
        }
        throw new IllegalArgumentException("Unknown marshaller type " + marshallerType);
    }

    public static UnMarshaller createUnMarshaller(final String marshallerType) throws IOException {
        if ("river".equals(marshallerType)) {
            return new JBossUnMarshaller(marshallerType);
        }
        if ("java-serial".equals(marshallerType)) {
            return new JavaSerialUnMarshaller();
        }
        throw new IllegalArgumentException("Unknown marshaller type " + marshallerType);
    }

    private static class JBossMarshaller implements Marshaller {

        private final org.jboss.marshalling.Marshaller delegate;

        JBossMarshaller(final String marshallerType) throws IOException {
            final MarshallingConfiguration marshallingConfiguration = new MarshallingConfiguration();
            marshallingConfiguration.setClassTable(new ProtocolV1ClassTable());
            marshallingConfiguration.setVersion(2);
            org.jboss.marshalling.MarshallerFactory factory = Marshalling.getProvidedMarshallerFactory(marshallerType);
            this.delegate = factory.createMarshaller(marshallingConfiguration);
        }

        @Override
        public void start(final DataOutput output) throws IOException {
            final OutputStream outputStream = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    final int byteToWrite = b & 0xff;
                    output.write(byteToWrite);
                }
            };
            final ByteOutput byteOutput = Marshalling.createByteOutput(outputStream);
            this.delegate.start(byteOutput);
        }

        @Override
        public void writeObject(Object object) throws IOException {
            this.delegate.writeObject(object);
        }

        @Override
        public void finish() throws IOException {
            this.delegate.finish();
        }
    }


    private static class JBossUnMarshaller implements UnMarshaller {

        private final String marshallerType;

        private Unmarshaller delegate;

        JBossUnMarshaller(final String marshallerType) throws IOException {
            this.marshallerType = marshallerType;
        }

        @Override
        public void start(final DataInput input, final ClassLoaderProvider classLoaderProvider) throws IOException {
            final MarshallingConfiguration marshallingConfiguration = new MarshallingConfiguration();
            marshallingConfiguration.setVersion(2);
            marshallingConfiguration.setClassTable(new ProtocolV1ClassTable());
            marshallingConfiguration.setClassResolver(new LazyClassLoaderClassResolver(classLoaderProvider));
            org.jboss.marshalling.MarshallerFactory factory = Marshalling.getProvidedMarshallerFactory(marshallerType);
            this.delegate = factory.createUnmarshaller(marshallingConfiguration);
            final InputStream is = new InputStream() {
                @Override
                public int read() throws IOException {
                    try {

                        final int b = input.readByte();
                        return b & 0xff;
                    } catch (EOFException eof) {
                        return -1;
                    }
                }
            };
            final ByteInput byteInput = Marshalling.createByteInput(is);
            this.delegate.start(byteInput);
        }

        @Override
        public Object readObject() throws IOException, ClassNotFoundException {
            if (this.delegate == null) {
                throw new IllegalStateException("Unmarshalling hasn't yet been marked for start");
            }
            return this.delegate.readObject();
        }

        @Override
        public void finish() throws IOException {
            if (this.delegate == null) {
                throw new IllegalStateException("Unmarshalling hasn't yet been marked for start");
            }
            this.delegate.finish();
        }

        private class LazyClassLoaderClassResolver extends AbstractClassResolver {

            private final ClassLoaderProvider classLoaderProvider;

            LazyClassLoaderClassResolver(final ClassLoaderProvider classLoaderProvider) {
                this.classLoaderProvider = classLoaderProvider;
            }

            @Override
            protected ClassLoader getClassLoader() {
                return this.classLoaderProvider.provideClassLoader();
            }
        }
    }

    private static class JavaSerialMarshaller implements Marshaller {

        private DataOutput dataOutput;

        @Override
        public void start(final DataOutput output) throws IOException {
            this.dataOutput = output;
        }

        @Override
        public void writeObject(Object object) throws IOException {
            final OutputStream os = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    final int byteToWrite = b & 0xff;
                    JavaSerialMarshaller.this.dataOutput.write(byteToWrite);
                }
            };
            final ObjectOutputStream objectOutputStream = new ObjectOutputStream(os);
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
        }

        @Override
        public void finish() throws IOException {
            this.dataOutput = null;
        }
    }

    private static class JavaSerialUnMarshaller implements UnMarshaller {

        private DataInput dataInput;

        private ClassLoaderProvider classLoaderProvider;

        @Override
        public void start(final DataInput input, final ClassLoaderProvider classLoaderProvider) throws IOException {
            this.classLoaderProvider = classLoaderProvider;
            this.dataInput = input;
        }

        @Override
        public Object readObject() throws ClassNotFoundException, IOException {
            final InputStream is = new InputStream() {
                @Override
                public int read() throws IOException {
                    try {
                        final int b = JavaSerialUnMarshaller.this.dataInput.readByte();
                        return b & 0xff;
                    } catch (EOFException eof) {
                        return -1;
                    }
                }
            };
            final ObjectInputStream objectInputStream = new ObjectInputStream(is) {
                @Override
                protected Class resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    String name = desc.getName();
                    try {
                        // Use Class.forName instead of ClassLoader.loadClass to avoid issues with loading arrays
                        return Class.forName(name, false, JavaSerialUnMarshaller.this.classLoaderProvider.provideClassLoader());
                    } catch (ClassNotFoundException e) {
                        return super.resolveClass(desc);
                    }
                }
            };
            return objectInputStream.readObject();
        }

        @Override
        public void finish() throws IOException {
            this.dataInput = null;
        }
    }
}
