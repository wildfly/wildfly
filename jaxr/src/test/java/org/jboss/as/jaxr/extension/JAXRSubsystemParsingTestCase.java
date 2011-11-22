/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.extension;


import junit.framework.Assert;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.jaxr.service.JAXRConfiguration;
import org.jboss.as.jaxr.service.JAXRConfigurationService;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.junit.Test;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.jaxr.extension.JAXRConstants.Namespace;
import static org.jboss.as.jaxr.service.JAXRConfiguration.DEFAULT_CREATEONSTART;
import static org.jboss.as.jaxr.service.JAXRConfiguration.DEFAULT_DROPONSTART;
import static org.jboss.as.jaxr.service.JAXRConfiguration.DEFAULT_DROPONSTOP;


/**
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
public class JAXRSubsystemParsingTestCase extends AbstractSubsystemTest {

    public static final String SUBSYSTEM_XML =
            "<subsystem xmlns='" + Namespace.CURRENT.getUriString() + "'>" +
              "<datasource jndi-name='java:DataSource'/>" +
              "<connection-factory jndi-name='java:ConnectionFactory'/>" +
              "<flags drop-on-start='true' create-on-start='true' drop-on-stop='true'/>" +
            "</subsystem>";

    public JAXRSubsystemParsingTestCase() {
        super(JAXRConstants.SUBSYSTEM_NAME, new JAXRSubsystemExtension());
    }

    /**
     * Tests that the xml is parsed into the correct operations
     */
    @Test
    public void testParseSubsystem() throws Exception {
        //Parse the subsystem xml into operations
        List<ModelNode> operations = super.parse(SUBSYSTEM_XML);

        ///Check that we have the expected number of operations
        Assert.assertEquals(1, operations.size());
    }

    /**
     * Test that the model created from the xml looks as expected
     */
    @Test
    public void testInstallIntoController() throws Exception {

        //Parse the subsystem xml and install into the controller
        KernelServices services = super.installInController(SUBSYSTEM_XML);

        ServiceContainer container = services.getContainer();
        ServiceController<?> controller = container.getService(JAXRConfigurationService.SERVICE_NAME);
        JAXRConfiguration config = (JAXRConfiguration) controller.getService().getValue();

        Assert.assertEquals("java:DataSource", config.getDataSourceBinding());
        Assert.assertEquals("java:ConnectionFactory", config.getConnectionFactoryBinding());
        Assert.assertEquals(true, config.isDropOnStart());
        Assert.assertEquals(true, config.isCreateOnStart());
        Assert.assertEquals(true, config.isDropOnStop());

        //Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(JAXRConstants.SUBSYSTEM_NAME));
    }

    /**
     * Starts a controller with a given subsystem xml and then checks that a second
     * controller started with the xml marshalled from the first one results in the same model
     */
    @Test
    public void testParseAndMarshalModel() throws Exception {
        //Parse the subsystem xml and install into the first controller
        KernelServices servicesA = super.installInController(SUBSYSTEM_XML);
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = super.installInController(marshalled);
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    /**
     * Starts a controller with the given subsystem xml and then checks that a second
     * controller started with the operations from its describe action results in the same model
     */
    @Test
    public void testDescribeHandler() throws Exception {
        //Parse the subsystem xml and install into the first controller
        KernelServices servicesA = super.installInController(SUBSYSTEM_XML);
        //Get the model and the describe operations from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        ModelNode describeOp = new ModelNode();
        describeOp.get(OP).set(DESCRIBE);
        describeOp.get(OP_ADDR).set(
                PathAddress.pathAddress(
                        PathElement.pathElement(SUBSYSTEM, JAXRConstants.SUBSYSTEM_NAME)).toModelNode());
        List<ModelNode> operations = super.checkResultAndGetContents(servicesA.executeOperation(describeOp)).asList();

        //Install the describe options from the first controller into a second controller
        KernelServices servicesB = super.installInController(operations);
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);

    }
}
