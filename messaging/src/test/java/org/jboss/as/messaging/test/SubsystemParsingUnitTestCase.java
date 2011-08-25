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

package org.jboss.as.messaging.test;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.messaging.MessagingSubsystemParser;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

import javax.swing.plaf.OptionPaneUI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
public class SubsystemParsingUnitTestCase  extends AbstractSubsystemTest {

    private static final String namespace = "urn:jboss:domain:messaging:1.0";
    private static final MessagingSubsystemParser parser = MessagingSubsystemParser.getInstance();

//    private static File tmpDir;
//
//    @BeforeClass
//    public static void createDataDir() {
//        File tmp =  new File(System.getProperty("java.io.tmpdir"));
//        tmpDir = new File(tmp, SubsystemParsingUnitTestCase.class.getSimpleName());
//    }
//
//    @AfterClass
//    public static void deleteDataDir() {
//        cleanDir(tmpDir);
//    }
//
//    private static void cleanDir(File file) {
//        if (file.isDirectory()) {
//            for (File child : file.listFiles()) {
//                cleanDir(child);
//            }
//        }
//
//        if (file.exists() && !file.delete()) {
//            file.deleteOnExit();
//        }
//    }

    public SubsystemParsingUnitTestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }


    @Test
    public void testParseAndMarshalModel() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml = readResource("subsystem.xml");

        AdditionalInitialization additionalInit = new AdditionalInitialization(){

            @Override
            protected OperationContext.Type getType() {
                return OperationContext.Type.MANAGEMENT;
            }

//            @Override
//            public void setupController(ControllerInitializer controllerInitializer) {
//                controllerInitializer.addSocketBinding("messaging", 12345);
//                controllerInitializer.addSocketBinding("messaging-throughput", 12346);
//                File dataDir = new File(tmpDir, "testParseAndMarshalModel");
//                controllerInitializer.addPath("jboss.server.data.dir", dataDir.getAbsolutePath(), null);
//            }
        };

        KernelServices servicesA = super.installInController(additionalInit, subsystemXml);
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        System.out.println(marshalled);

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = super.installInController(additionalInit, marshalled);
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);

        // test the describe operation
        final ModelNode operation = createDescribeOperation();
        final ModelNode result = servicesB.executeOperation(operation);
        final List<ModelNode> operations = result.get("result").asList();

        servicesB.shutdown();

        KernelServices servicesC = super.installInController(additionalInit, operations);
        ModelNode modelC = servicesC.readWholeModel();

        super.compare(modelA, modelC);
    }

    static ModelNode createDescribeOperation() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "messaging");

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.DESCRIBE);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        return operation;
    }

}
