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

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

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
    //TODO These cannot work due to problems in the legacydescription
    //This can only work if we use the legacy classloader to load up everything
    //There is a problem with how the descriptions for ObjectTypeDefinition play with the current ones which are used for testing
    //The old resource bundle misses a key which was never used in the old version, while the tests use the new ObjectTypeDefinition
    //which try to look for that key
//    @Test
//    public void testBoot712() throws Exception {
//        //Override the core model version to make sure that our custom transformer for model version 1.0.0 running on 7.1.2 kicks in
//        testBootOldVersion("7.1.2.Final");
//    }
//
//    @Test
//    public void testBoot713() throws Exception {
//        //The model version has not changed, make sure we can boot 7.1.3 with the current config
//        testBootOldVersion("7.1.3.Final");
//    }
//
//    private void testBootOldVersion(String asVersion) throws Exception {
//
//        String subsystemXml = readResource("subsystem.xml");
//        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
//        //Use the non-runtime version of the extension which will happen on the HC
//        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
//                .setSubsystemXml(subsystemXml);
//
//        // Add legacy subsystems
//        builder.createLegacyKernelServicesBuilder(new AdditionalInitialization() {
//                    @Override
//                    protected RunningMode getRunningMode() {
//                        return RunningMode.ADMIN_ONLY;
//                    }
//
//                    @Override
//                    protected boolean isValidateOperations() {
//                        //There is a problem with how the descriptions for ObjectTypeDefinition play with the current ones which are used for testing
//                        //The old resource bundle misses a key which was never used in the old version, while the tests use the new ObjectTypeDefinition
//                        //which try to look for that key
//                        return false;
//                    }
//
//                }, modelVersion)
//                .addMavenResourceURL("org.jboss.as:jboss-as-xts:" + asVersion);
//
//        KernelServices mainServices = builder.build();
//        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
//        Assert.assertTrue(mainServices.isSuccessfulBoot());
//        Assert.assertTrue(legacyServices.isSuccessfulBoot());
//
//        checkSubsystemModelTransformation(mainServices, modelVersion);
//    }
}
