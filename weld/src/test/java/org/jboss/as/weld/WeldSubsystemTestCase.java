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
package org.jboss.as.weld;

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class WeldSubsystemTestCase extends AbstractSubsystemBaseTest {

    public WeldSubsystemTestCase() {
        super(WeldExtension.SUBSYSTEM_NAME, new WeldExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_1_0.xml");
    }

    @Test
    public void testTransformersAS712() throws Exception {
        testTransformers10(ModelTestControllerVersion.V7_1_2_FINAL, false);
    }

    @Test
    public void testTransformersAS713() throws Exception {
        testTransformers10(ModelTestControllerVersion.V7_1_3_FINAL, false);
    }

    @Test
    public void testTransformersAS72() throws Exception {
        testTransformers10(ModelTestControllerVersion.V7_2_0_FINAL, false);
    }

    @Test
    public void testTransformersEAP600() throws Exception {
        testTransformers10(ModelTestControllerVersion.EAP_6_0_0, true);
    }

    @Test
    public void testTransformersEAP601() throws Exception {
        testTransformers10(ModelTestControllerVersion.EAP_6_0_1, true);
    }

    @Test
    public void testTransformersEAP610() throws Exception {
        testTransformers10(ModelTestControllerVersion.EAP_6_1_0, true);
    }

    @Test
    public void testTransformersEAP611() throws Exception {
        testTransformers10(ModelTestControllerVersion.EAP_6_1_1, true);
    }


    private void testTransformers10(ModelTestControllerVersion controllerVersion, boolean eap) throws Exception {
        if (eap) {
            ignoreThisTestIfEAPRepositoryIsNotReachable();
        }

        ModelVersion modelVersion = ModelVersion.create(1, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("subsystem_1_0.xml");
        //which is why we need to include the jboss-as-controller artifact.
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-weld:" + controllerVersion.getMavenGavVersion())
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }
}
