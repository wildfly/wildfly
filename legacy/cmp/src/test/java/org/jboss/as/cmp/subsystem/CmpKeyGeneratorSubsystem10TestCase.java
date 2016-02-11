/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.cmp.subsystem;

import static org.jboss.as.cmp.subsystem.CmpSubsystemModel.BLOCK_SIZE;
import static org.jboss.as.cmp.subsystem.CmpSubsystemModel.CREATE_TABLE;
import static org.jboss.as.cmp.subsystem.CmpSubsystemModel.CREATE_TABLE_DDL;
import static org.jboss.as.cmp.subsystem.CmpSubsystemModel.DATA_SOURCE;
import static org.jboss.as.cmp.subsystem.CmpSubsystemModel.DROP_TABLE;
import static org.jboss.as.cmp.subsystem.CmpSubsystemModel.HILO_KEY_GENERATOR;
import static org.jboss.as.cmp.subsystem.CmpSubsystemModel.ID_COLUMN;
import static org.jboss.as.cmp.subsystem.CmpSubsystemModel.SEQUENCE_COLUMN;
import static org.jboss.as.cmp.subsystem.CmpSubsystemModel.SEQUENCE_NAME;
import static org.jboss.as.cmp.subsystem.CmpSubsystemModel.TABLE_NAME;
import static org.jboss.as.cmp.subsystem.CmpSubsystemModel.UUID_KEY_GENERATOR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CmpKeyGeneratorSubsystem10TestCase extends AbstractSubsystemBaseTest {

    public CmpKeyGeneratorSubsystem10TestCase() {
        super(CmpExtension.SUBSYSTEM_NAME, new CmpExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem-cmp-key-generators_1_0.xml");
    }

    @Override
    protected KernelServices standardSubsystemTest(String configId, boolean compareXml) throws Exception {
        return super.standardSubsystemTest(configId, false);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {

            @Override
            protected ProcessType getProcessType() {
                return ProcessType.HOST_CONTROLLER;
            }

            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.ADMIN_ONLY;
            }
        };
    }

    // TODO WFCORE-1353 means this doesn't have to always fail now; consider just deleting this
//    @Override
//    protected void validateDescribeOperation(KernelServices hc, AdditionalInitialization serverInit, ModelNode expectedModel) throws Exception {
//        final ModelNode operation = createDescribeOperation();
//        final ModelNode result = hc.executeOperation(operation);
//        Assert.assertTrue("The subsystem describe operation must fail",
//                result.hasDefined(ModelDescriptionConstants.FAILURE_DESCRIPTION));
//    }

    @Test
    public void testParseSubsystem() throws Exception {
        final List<ModelNode> operations = super.parse(getSubsystemXml("subsystem-cmp-key-generators_1_0.xml"));
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
