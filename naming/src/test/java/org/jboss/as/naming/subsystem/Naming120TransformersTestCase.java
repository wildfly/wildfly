/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.naming.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.naming.subsystem.NamingExtension.SUBSYSTEM_NAME;
import static org.jboss.as.naming.subsystem.NamingExtension.VERSION_1_2_0;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING_TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.CLASS;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.ENVIRONMENT;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.EXTERNAL_CONTEXT;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.MODULE;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test case that verifies functionality of Naming 1.2.0 Transformers.
 *
 * @author Stuart Douglas
 */
@Ignore("It looks like 8.0 transformer tests have not been setup yet?")
public class Naming120TransformersTestCase extends AbstractSubsystemBaseTest {

    public Naming120TransformersTestCase() {
        super(SUBSYSTEM_NAME, new NamingExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

    @Test
    public void testTransformers_AS712() throws Exception {
        testTransformers(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformers_AS713() throws Exception {
        testTransformers(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testTransformers(ModelTestControllerVersion version) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("subsystem.xml");

        builder.createLegacyKernelServicesBuilder(null, version, VERSION_1_2_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-naming:" + version.getMavenGavVersion())
                .skipReverseControllerCheck();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(VERSION_1_2_0);
        assertNotNull(legacyServices);

        checkExternalContextEnvironmentBindingTransformation(mainServices, VERSION_1_2_0);
    }



    private void checkExternalContextEnvironmentBindingTransformation(KernelServices mainServices, ModelVersion version)
            throws OperationFailedException {

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, SUBSYSTEM_NAME);
        address.add(BINDING, "java:global/as75140-7");
        final ModelNode bindingAdd = new ModelNode();
        bindingAdd.get(OP).set(ADD);
        bindingAdd.get(OP_ADDR).set(address);
        bindingAdd.get(BINDING_TYPE).set(EXTERNAL_CONTEXT);
        bindingAdd.get(MODULE).set("org.jboss.as.naming");
        bindingAdd.get(CLASS).set("javax.naming.InitialContext");
        bindingAdd.get(ENVIRONMENT).set(new ModelNode().add("a", "a"));

        ModelNode resultNode = mainServices.executeOperation(version,
                mainServices.transformOperation(version, bindingAdd));
        Assert.assertTrue(resultNode.get(FAILURE_DESCRIPTION).isDefined());
    }


}
