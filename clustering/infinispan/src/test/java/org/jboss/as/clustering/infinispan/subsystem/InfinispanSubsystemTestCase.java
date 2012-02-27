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
package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.clustering.subsystem.ClusteringSubsystemTest;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationConfiguration;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(value = Parameterized.class)
public class InfinispanSubsystemTestCase extends ClusteringSubsystemTest {

    String xmlFile = null ;
    int operations = 0 ;

    public InfinispanSubsystemTestCase(String xmlFile, int operations) {
        super(InfinispanExtension.SUBSYSTEM_NAME, new InfinispanExtension(), xmlFile);
        this.xmlFile = xmlFile ;
        this.operations = operations ;
    }

    @Parameters
    public static Collection<Object[]> data() {
      Object[][] data = new Object[][] { { "subsystem-infinispan_1_0.xml", 31 },
                                         { "subsystem-infinispan_1_1.xml", 31 },
                                         { "subsystem-infinispan_1_2.xml", 31 }
                                       };
      return Arrays.asList(data);
    }


    @Override
    protected ValidationConfiguration getModelValidationConfiguration() {
        // use this configuration to report any exceptional cases for DescriptionProviders
        return new ValidationConfiguration();
    }

    /**
     * Tests that the xml is parsed into the correct operations
     */
    @Test
    public void testParseSubsystem() throws Exception {
       // Parse the subsystem xml into operations
       List<ModelNode> operations = super.parse(getSubsystemXml());

       // print the operations
       /*
       System.out.println("List of operations");
       for (ModelNode op : operations) {
           System.out.println("operation = " + op.toString());
       }
       */
       // Check that we have the expected number of operations
       // one for each resource instance
       Assert.assertEquals(this.operations, operations.size());

       // Check that each operation has the correct content
       ModelNode addSubsystem = operations.get(0);
       Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
       PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
       Assert.assertEquals(1, addr.size());
       PathElement element = addr.getElement(0);
       Assert.assertEquals(SUBSYSTEM, element.getKey());
       Assert.assertEquals(getMainSubsystemName(), element.getValue());
    }

    /**
     * Test that the model created from the xml looks as expected
     */
    @Test
    public void testInstallIntoController() throws Exception {
       // Parse the subsystem xml and install into the controller
       KernelServices services = super.installInController(getSubsystemXml());

       // Read the whole model and make sure it looks as expected
       ModelNode model = services.readWholeModel();

       // System.out.println("model = " + model.asString());

       Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(getMainSubsystemName()));
    }

    /**
     * Starts a controller with a given subsystem xml and then checks that a second controller
     * started with the xml marshalled from the first one results in the same model
     */
    @Test
    public void testParseAndMarshalModel() throws Exception {
       // Parse the subsystem xml and install into the first controller

       KernelServices servicesA = super.installInController(getSubsystemXml());

       // Get the model and the persisted xml from the first controller
       ModelNode modelA = servicesA.readWholeModel();
       String marshalled = servicesA.getPersistedSubsystemXml();

       // Install the persisted xml from the first controller into a second controller
       KernelServices servicesB = super.installInController(marshalled);
       ModelNode modelB = servicesB.readWholeModel();

       // Make sure the models from the two controllers are identical
       super.compare(modelA, modelB);
    }

    /**
     * Starts a controller with the given subsystem xml and then checks that a second controller
     * started with the operations from its describe action results in the same model
     */
    @Test
    public void testDescribeHandler() throws Exception {
       // Parse the subsystem xml and install into the first controller
       KernelServices servicesA = super.installInController(getSubsystemXml());
       // Get the model and the describe operations from the first controller
       ModelNode modelA = servicesA.readWholeModel();
       ModelNode describeOp = new ModelNode();
       describeOp.get(OP).set(DESCRIBE);
       describeOp.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, getMainSubsystemName())).toModelNode());
       List<ModelNode> operations = super.checkResultAndGetContents(servicesA.executeOperation(describeOp)).asList();

       // Install the describe options from the first controller into a second controller
       KernelServices servicesB = super.installInController(operations);
       ModelNode modelB = servicesB.readWholeModel();

       // Make sure the models from the two controllers are identical
       super.compare(modelA, modelB);

    }
}
