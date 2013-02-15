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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
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

    //@Test
    public void testFullConfig() throws Exception {
        standardSubsystemTest("datasources-full.xml");
    }

    //@Test
    public void testExpressionConfig() throws Exception {
        standardSubsystemTest("datasources-full-expression.xml", "datasources-full.xml");
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

    @Test
    public void testTransformerAS712() throws Exception {
        testTransformer1_1_0("7.1.2.Final", "datasources-full110.xml", ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformerAS713() throws Exception {
        testTransformer1_1_0("7.1.3.Final", "datasources-full110.xml", ModelTestControllerVersion.V7_1_3_FINAL);
    }

    @Test
    public void tesExpressionsAS712() throws Exception {
        //this file contain expression for all supported fields except reauth-plugin-properties, exception-sorter-properties,
        // stale-connection-checker-properties, valid-connection-checker-properties, recovery-plugin-properties
        // for a limitation in test suite not permitting to have expression in type LIST or OBJECT for legacyServices
        testRejectTransformers1_1_0("7.1.2.Final", "datasources-full-expression110.xml", ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testExpressionsAS713() throws Exception {
        //this file contain expression for all supported fields except reauth-plugin-properties, exception-sorter-properties,
        // stale-connection-checker-properties, valid-connection-checker-properties, recovery-plugin-properties
        // for a limitation in test suite not permitting to have expression in type LIST or OBJECT for legacyServices
        testRejectTransformers1_1_0("7.1.3.Final", "datasources-full-expression110.xml", ModelTestControllerVersion.V7_1_3_FINAL);
    }


    /**
     * Tests transformation of model from 1.1.1 version into 1.1.0 version.
     *
     * @throws Exception
     */
    private void testTransformer1_1_0(String mavenVersion, String subsystemXml, ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0); //The old model version
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource(subsystemXml);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null,controllerVersion,  modelVersion)
                              .addMavenResourceURL("org.jboss.as:jboss-as-connector:" + mavenVersion)
                              .setExtensionClassName("org.jboss.as.connector.subsystems.datasources.DataSourcesExtension");

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        Assert.assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }


    public void testRejectTransformers1_1_0(String mavenVersion, String subsystemXml, ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0); //The old model version
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-connector:" + mavenVersion)
                .setExtensionClassName("org.jboss.as.connector.subsystems.datasources.DataSourcesExtension");

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource(subsystemXml);
        PathAddress subsystemAddress = PathAddress.pathAddress(DataSourcesSubsystemRootDefinition.PATH_SUBSYSTEM);
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, new FailedOperationTransformationConfig()
                .addFailedAttribute(subsystemAddress.append(JdbcDriverDefinition.PATH_DRIVER),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(Constants.DRIVER_MINOR_VERSION, Constants.DRIVER_MAJOR_VERSION) {
                            @Override
                            protected boolean isAttributeWritable(String attributeName) {
                                return false;
                            }
                        })
                .addFailedAttribute(subsystemAddress.append(DataSourceDefinition.PATH_DATASOURCE),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(Constants.DATASOURCE_PROPERTIES_ATTRIBUTES))
                .addFailedAttribute(subsystemAddress.append(XaDataSourceDefinition.PATH_XA_DATASOURCE),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(Constants.DATASOURCE_PROPERTIES_ATTRIBUTES))
        );
    }

}
