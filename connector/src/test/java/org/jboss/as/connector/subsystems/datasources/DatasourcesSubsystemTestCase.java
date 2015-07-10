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
package org.jboss.as.connector.subsystems.datasources;

import java.io.IOException;
import java.util.List;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.model.test.SingleClassFilter;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="stefano.maestri@redhat.com>Stefano Maestri</a>
 */
public class DatasourcesSubsystemTestCase extends AbstractSubsystemBaseTest {

    public DatasourcesSubsystemTestCase() {
        super(DataSourcesExtension.SUBSYSTEM_NAME, new DataSourcesExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        //test configuration put in standalone.xml
        return readResource("datasources-minimal.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-datasources_3_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[]{
                "/subsystem-templates/datasources.xml"
        };
    }

    @Test
    public void testFullConfig() throws Exception {
        standardSubsystemTest("datasources-full.xml");
    }

    @Test
    public void testExpressionConfig() throws Exception {
        standardSubsystemTest("datasources-full-expression.xml", "datasources-full.xml");
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

    @Test
    public void testTransformerEAP62() throws Exception {
        testTransformer("datasources-full.xml", ModelTestControllerVersion.EAP_6_2_0, ModelVersion.create(1, 2, 0));
    }

    @Test
    public void testTransformerExpressionEAP62() throws Exception {
        testTransformer("datasources-full-expression111.xml", ModelTestControllerVersion.EAP_6_2_0, ModelVersion.create(1, 2, 0));
    }


    @Test
    public void testTransformerWF8() throws Exception {
        testTransformer("datasources-full.xml", ModelTestControllerVersion.WILDFLY_8_0_0_FINAL, ModelVersion.create(2, 0, 0));
    }


    /**
     * Tests transformation of model from latest version into one passed into modelVersion parameter.
     *
     * @throws Exception
     */
    private void testTransformer(String subsystemXml, ModelTestControllerVersion controllerVersion, final ModelVersion modelVersion) throws Exception {
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);


        // Add legacy subsystems
        LegacyKernelServicesInitializer initializer = builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion);
        if (controllerVersion.equals(ModelTestControllerVersion.WILDFLY_8_0_0_FINAL)) {
            initializer.addMavenResourceURL("org.wildfly:wildfly-connector:" + controllerVersion.getMavenGavVersion());
        } else {
            initializer.addMavenResourceURL("org.jboss.as:jboss-as-connector:" + controllerVersion.getMavenGavVersion());
        }
        initializer.addMavenResourceURL("org.jboss.ironjacamar:ironjacamar-spec-api:1.1.4.Final")
                .addMavenResourceURL("org.jboss.ironjacamar:ironjacamar-common-api:1.1.4.Final")

                .setExtensionClassName("org.jboss.as.connector.subsystems.datasources.DataSourcesExtension")
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        Assert.assertNotNull(legacyServices);
        List<ModelNode> ops = builder.parseXmlResource(subsystemXml);
        PathAddress subsystemAddress = PathAddress.pathAddress(DataSourcesSubsystemRootDefinition.PATH_SUBSYSTEM);

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, new FailedOperationTransformationConfig()
                        .addFailedAttribute(subsystemAddress.append(DataSourceDefinition.PATH_DATASOURCE), new FailedOperationTransformationConfig.NewAttributesConfig(Constants.TRACKING))
                        .addFailedAttribute(subsystemAddress.append(XaDataSourceDefinition.PATH_XA_DATASOURCE), new FailedOperationTransformationConfig.NewAttributesConfig(Constants.TRACKING))
        );
    }
}
