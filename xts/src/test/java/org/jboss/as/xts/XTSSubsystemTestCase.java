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
package org.jboss.as.xts;

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.OperationFixer;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class XTSSubsystemTestCase extends AbstractSubsystemBaseTest {

    public XTSSubsystemTestCase() {
        super(XTSExtension.SUBSYSTEM_NAME, new XTSExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-xts_2_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/xts.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }

    @Test
    public void testBootEAP640() throws Exception {
        testBoot1_1_0(ModelTestControllerVersion.EAP_6_4_0);
    }


    private void testBoot1_1_0(ModelTestControllerVersion controllerVersion) throws Exception {

        String subsystemXml = readResource("subsystem.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml);


        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, null, new OperationFixer() {
                    @Override
                    public ModelNode fixOperation(ModelNode operation) {
                        String name = operation.get(ModelDescriptionConstants.OP).asString();
                        PathAddress addr = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
                        if (name.equals(ModelDescriptionConstants.ADD) && addr.size() == 1 && addr.getElement(0).equals(XTSExtension.SUBSYSTEM_PATH)) {
                            operation.get(ModelDescriptionConstants.HOST).set("default-host");
                            operation.get(XTSSubsystemDefinition.DEFAULT_CONTEXT_PROPAGATION.getName()).set(false);
                        }
                        return operation;
                    }
                })
                .addMavenResourceURL("org.jboss.as:jboss-as-xts:" + controllerVersion.getMavenGavVersion());

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }
}
