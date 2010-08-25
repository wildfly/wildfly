/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.model.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;

import junit.framework.TestCase;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.staxmapper.XMLMapper;

/**
 * Base class for unit-tests of {@link AbstractModelElement} subclasses.
 * 
 * @author Brian Stansberry
 */
public abstract class DomainModelElementTestBase extends TestCase {

    private XMLMapper mapper;
    
    /**
     * @param name
     */
    public DomainModelElementTestBase(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mapper = createXMLMapper();        
    }
    
    /**
     * Tests that the element can be serialized, a new instance can be 
     * deserialized, and that the two objects will be equivalent.
     * @throws Exception
     */
    public abstract void testSerializationDeserialization() throws Exception;
    
    /**
     * Test that the object can be parsed from XML, written back to XML, a new
     * instance reparsed from that output, and that the two parsed objects will
     * be equivalent.
     * 
     * @throws Exception
     */
//    public abstract void testXMLRoundTrip() throws Exception;
    
    
    protected XMLMapper getXMLMapper() {
        return mapper;
    }
    
    /**
     * Create the XMLMapper that will be returned by {@link #getXMLMapper()}
     * 
     * @return
     * @throws Exception
     */
    protected abstract XMLMapper createXMLMapper() throws Exception;

    /**
     * Gets the namespace that will be used in tests.
     * 
     * @return
     */
    protected abstract String getTargetNamespace();

    /**
     * Gets the schema location of the namespace that will be used in tests.
     * 
     * @return
     */
    protected abstract String getTargetNamespaceLocation();
    
    protected static byte[] serialize(AbstractModelElement<?> element) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        final Checksum chksum = new Adler32();
        final CheckedOutputStream cos = new CheckedOutputStream(baos, chksum);
        final Marshaller marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
        try {
            marshaller.start(Marshalling.createByteOutput(cos));
            marshaller.writeObject(element);
            marshaller.finish();
            marshaller.close();
            return baos.toByteArray();
        } finally {
            safeClose(marshaller);
        }
    }

    protected static <T extends AbstractModelElement<?>> T deserialize(byte[] message, Class<T> type) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(message);
        final Unmarshaller unmarshaller = MARSHALLER_FACTORY.createUnmarshaller(CONFIG);
        try {
            unmarshaller.start(Marshalling.createByteInput(byteArrayInputStream));
            final T element = unmarshaller.readObject(type);
            unmarshaller.finish();
            unmarshaller.close();
            return element;
        } finally {
            safeClose(unmarshaller);
        }
    }

    private static final MarshallerFactory MARSHALLER_FACTORY;
    private static final MarshallingConfiguration CONFIG;

    static {
        MARSHALLER_FACTORY = Marshalling.getMarshallerFactory("river", Thread.currentThread().getContextClassLoader());
        final MarshallingConfiguration config = new MarshallingConfiguration();
        CONFIG = config;
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

}
