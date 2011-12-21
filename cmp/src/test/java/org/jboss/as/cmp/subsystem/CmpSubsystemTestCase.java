/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.subsystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import static org.jboss.as.cmp.subsystem.CmpConstants.BLOCK_SIZE;
import static org.jboss.as.cmp.subsystem.CmpConstants.CREATE_TABLE;
import static org.jboss.as.cmp.subsystem.CmpConstants.CREATE_TABLE_DDL;
import static org.jboss.as.cmp.subsystem.CmpConstants.DATA_SOURCE;
import static org.jboss.as.cmp.subsystem.CmpConstants.DROP_TABLE;
import static org.jboss.as.cmp.subsystem.CmpConstants.HILO_KEY_GENERATOR;
import static org.jboss.as.cmp.subsystem.CmpConstants.ID_COLUMN;
import static org.jboss.as.cmp.subsystem.CmpConstants.SEQUENCE_COLUMN;
import static org.jboss.as.cmp.subsystem.CmpConstants.SEQUENCE_NAME;
import static org.jboss.as.cmp.subsystem.CmpConstants.TABLE_NAME;
import static org.jboss.as.cmp.subsystem.CmpConstants.UUID_KEY_GENERATOR;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.dmr.ModelNode;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CmpSubsystemTestCase extends AbstractSubsystemBaseTest {

    public CmpSubsystemTestCase() {
        super(CmpExtension.SUBSYSTEM_NAME, new CmpExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return getSubsystemXml("subsystem-cmp.xml");
    }

    protected String getSubsystemXml(final String fileName) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(fileName);
        if (url == null) {
            throw new IllegalStateException(String.format("Failed to locate %s", fileName));
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(url.toURI())));
            StringWriter writer = new StringWriter();
            try {
                String line = reader.readLine();
                while (line != null) {
                    writer.write(line);
                    line = reader.readLine();
                }
            } finally {
                reader.close();
            }
            return writer.toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void testParseSubsystem() throws Exception {
        final List<ModelNode> operations = super.parse(getSubsystemXml("subsystem-cmp-key-generators.xml"));
        assertEquals(5, operations.size());
        assertOperation(operations.get(0), ADD, PathElement.pathElement(SUBSYSTEM, getMainSubsystemName()));
        assertOperation(operations.get(1), ADD, PathElement.pathElement(UUID_KEY_GENERATOR, "uuid1"));
        assertOperation(operations.get(2), ADD, PathElement.pathElement(UUID_KEY_GENERATOR, "uuid2"));

        final ModelNode hilo1 = operations.get(3);
        assertOperation(hilo1, ADD, PathElement.pathElement(HILO_KEY_GENERATOR, "hilo1"));
        assertEquals("java:/jdbc/DB1", hilo1.get(DATA_SOURCE).asString());
        assertEquals("HILOSEQUENCES1", hilo1.get(TABLE_NAME).asString());
        assertEquals("SEQUENCENAME1", hilo1.get(SEQUENCE_COLUMN).asString());
        assertEquals("HIGHVALUES1", hilo1.get(ID_COLUMN).asString());
        assertEquals("create table HILOSEQUENCES1", hilo1.get(CREATE_TABLE_DDL).asString());
        assertEquals("general1", hilo1.get(SEQUENCE_NAME).asString());
        assertEquals(true, hilo1.get(CREATE_TABLE).asBoolean());
        assertEquals(true, hilo1.get(DROP_TABLE).asBoolean());
        assertEquals(10, hilo1.get(BLOCK_SIZE).asLong());

        final ModelNode hilo2 = operations.get(4);
        assertOperation(hilo2, ADD, PathElement.pathElement(HILO_KEY_GENERATOR, "hilo2"));
        assertEquals("java:/jdbc/DB2", hilo2.get(DATA_SOURCE).asString());
        assertEquals("HILOSEQUENCES2", hilo2.get(TABLE_NAME).asString());
        assertEquals("SEQUENCENAME2", hilo2.get(SEQUENCE_COLUMN).asString());
        assertEquals("HIGHVALUES2", hilo2.get(ID_COLUMN).asString());
        assertEquals("create table HILOSEQUENCES2", hilo2.get(CREATE_TABLE_DDL).asString());
        assertEquals("general2", hilo2.get(SEQUENCE_NAME).asString());
        assertEquals(false, hilo2.get(CREATE_TABLE).asBoolean());
        assertEquals(false, hilo2.get(DROP_TABLE).asBoolean());
        assertEquals(11, hilo2.get(BLOCK_SIZE).asLong());
    }

    private void assertOperation(final ModelNode operation, final String operationName, final PathElement lastElement) {
        assertEquals(operationName, operation.get(OP).asString());
        final PathAddress addr = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathElement element = addr.getLastElement();
        assertEquals(lastElement.getKey(), element.getKey());
        assertEquals(lastElement.getValue(), element.getValue());
    }
}
