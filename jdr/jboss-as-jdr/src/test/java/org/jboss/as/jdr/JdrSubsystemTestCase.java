
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
package org.jboss.as.jdr;

import java.io.IOException;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.Test;


/**
 * Performs basic parsing and configuration testing of the JDR subsystem.
 *
 * @author Mike M. Clark
 */
public class JdrSubsystemTestCase extends AbstractSubsystemBaseTest {

    public JdrSubsystemTestCase() {
        super(JdrReportExtension.SUBSYSTEM_NAME, new JdrReportExtension());
    }

    @Test(expected = XMLStreamException.class)
    public void testParseSubsystemWithBadChild() throws Exception {
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\"><invalid/></subsystem>";
        super.parse(subsystemXml);
    }

    @Test(expected = XMLStreamException.class)
    public void testParseSubsystemWithBadAttribute() throws Exception {
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\" attr=\"wrong\"/>";
        super.parse(subsystemXml);
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-jdr_1_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[]{
                "/subsystem-templates/jdr.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }

    //todo not sure how much sense does it make to test this as model version is exactly the same as in current version
    /*@Test
    public void testTransformersEAP620() throws Exception {
        testJdrTransformers(ModelTestControllerVersion.EAP_6_2_0, ModelVersion.create(1, 2, 0));
    }

    @Test
    public void testTransformersEAP630() throws Exception {
        testJdrTransformers(ModelTestControllerVersion.EAP_6_3_0, ModelVersion.create(1, 2, 0));
    }

    @Test
    public void testTransformersEAP640() throws Exception {
        testJdrTransformers(ModelTestControllerVersion.EAP_6_4_0, ModelVersion.create(1, 2, 0));
    }

    private void testJdrTransformers(ModelTestControllerVersion controllerVersion, ModelVersion modelVersion) throws Exception {
        String subsystemXml = "subsystem.xml";
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-jdr:" + controllerVersion.getMavenGavVersion());

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(mainServices);
        Assert.assertNotNull(legacyServices);
        checkSubsystemModelTransformation(mainServices, modelVersion);
    }*/

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {
            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.NORMAL;
            }
        };
    }
}
