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

import static org.jboss.as.connector.subsystems.datasources.Constants.AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.CREDENTIAL_REFERENCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_CREDENTIAL_REFERENCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRACKING;

import java.io.IOException;
import java.util.List;

import org.jboss.as.connector._private.Capabilities;
import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.AttributesPathAddressConfig;
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
        return "schema/wildfly-datasources_5_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[]{
                "/subsystem-templates/datasources.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }

    @Test
    public void testFullConfig() throws Exception {
        standardSubsystemTest("datasources-full.xml");
    }

    @Test
    public void testElytronConfig() throws Exception {
        standardSubsystemTest("datasources-elytron-enabled_5_0.xml");
    }

    @Test
    public void testExpressionConfig() throws Exception {
        standardSubsystemTest("datasources-full-expression.xml", "datasources-full.xml");
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        // Create a AdditionalInitialization.MANAGEMENT variant that has all the external
        // capabilities used by the various configs used in this test class
        return AdditionalInitialization.withCapabilities(
                Capabilities.AUTHENTICATION_CONTEXT_CAPABILITY + ".DsAuthCtxt",
                Capabilities.AUTHENTICATION_CONTEXT_CAPABILITY + ".CredentialAuthCtxt",
                CredentialReference.CREDENTIAL_STORE_CAPABILITY + ".test-store"
        );
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
    public void testTransformerEAP63() throws Exception {
        testTransformer("datasources-full.xml", ModelTestControllerVersion.EAP_6_3_0, ModelVersion.create(1, 3, 0));
    }

    @Test
    public void testTransformerExpressionEAP63() throws Exception {
        testTransformer("datasources-full-expression111.xml", ModelTestControllerVersion.EAP_6_3_0, ModelVersion.create(1, 3, 0));
    }

    @Test
    public void testTransformerEAP64() throws Exception {
        testTransformer("datasources-full.xml", ModelTestControllerVersion.EAP_6_4_0, ModelVersion.create(1, 3, 0));
    }

    @Test
    public void testTransformerExpressionEAP64() throws Exception {
        testTransformer("datasources-full-expression111.xml", ModelTestControllerVersion.EAP_6_4_0, ModelVersion.create(1, 3, 0));
    }

    @Test
    public void testTransformerElytronEnabledEAP64() throws Exception {
        testTransformerElytronEnabled("datasources-elytron-enabled_5_0.xml", ModelTestControllerVersion.EAP_6_4_0, ModelVersion.create(1, 3, 0));
    }
    @Test
    public void testTransformerEAP7() throws Exception {
        testTransformerEAP7FullConfiguration("datasources-full.xml");
    }

    @Test
    public void testRejectionsEAP7() throws Exception {
        testTransformerEAP7Rejection("datasources-no-connection-url.xml");
    }

    private KernelServices initialKernelServices(KernelServicesBuilder builder, ModelTestControllerVersion controllerVersion, final ModelVersion modelVersion) throws Exception {
        LegacyKernelServicesInitializer initializer = builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion);
        String mavenGroupId = controllerVersion.getMavenGroupId();
        String artifactId = "wildfly-connector";
        if (controllerVersion.isEap() && controllerVersion.getMavenGavVersion().equals(controllerVersion.getCoreVersion())) { // EAP 6
            artifactId = "jboss-as-connector";
        }
        initializer.addMavenResourceURL(mavenGroupId + ":" + artifactId + ":" + controllerVersion.getMavenGavVersion());
        initializer.addMavenResourceURL("org.jboss.ironjacamar:ironjacamar-spec-api:1.0.28.Final")
                .addMavenResourceURL("org.jboss.ironjacamar:ironjacamar-common-api:1.0.28.Final")
                .setExtensionClassName("org.jboss.as.connector.subsystems.datasources.DataSourcesExtension")
                .excludeFromParent(SingleClassFilter.createFilter(ConnectorLogger.class));

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        Assert.assertNotNull(legacyServices);
        return mainServices;
    }

    /**
     * Tests transformation of model from latest version into one passed into modelVersion parameter.
     *
     * @throws Exception
     */
    private void testTransformer(String subsystemXml, ModelTestControllerVersion controllerVersion, final ModelVersion modelVersion) throws Exception {
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);
        KernelServices mainServices = initialKernelServices(builder, controllerVersion, modelVersion);
        List<ModelNode> ops = builder.parseXmlResource(subsystemXml);
        PathAddress subsystemAddress = PathAddress.pathAddress(DataSourcesSubsystemRootDefinition.PATH_SUBSYSTEM);

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, new FailedOperationTransformationConfig()
                        .addFailedAttribute(subsystemAddress.append(DataSourceDefinition.PATH_DATASOURCE), new FailedOperationTransformationConfig.NewAttributesConfig(TRACKING))
                        .addFailedAttribute(subsystemAddress.append(XaDataSourceDefinition.PATH_XA_DATASOURCE), new FailedOperationTransformationConfig.NewAttributesConfig(TRACKING))
        );
    }

    /**
     * Tests transformation of model from latest version into one passed into modelVersion parameter.
     *
     * @throws Exception
     */
    private void testTransformerElytronEnabled(String subsystemXml, ModelTestControllerVersion controllerVersion, final ModelVersion modelVersion) throws Exception {
        //Use the non-runtime version of the extension which will happen on the HC
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        KernelServices mainServices = initialKernelServices(builder, controllerVersion, modelVersion);
        List<ModelNode> ops = builder.parseXmlResource(subsystemXml);
        PathAddress subsystemAddress = PathAddress.pathAddress(DataSourcesSubsystemRootDefinition.PATH_SUBSYSTEM);

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, new FailedOperationTransformationConfig()
                .addFailedAttribute(subsystemAddress.append(DataSourceDefinition.PATH_DATASOURCE),
                        new FailedOperationTransformationConfig.NewAttributesConfig(TRACKING, ELYTRON_ENABLED, AUTHENTICATION_CONTEXT, CREDENTIAL_REFERENCE))
                .addFailedAttribute(subsystemAddress.append(XaDataSourceDefinition.PATH_XA_DATASOURCE),
                        new FailedOperationTransformationConfig.NewAttributesConfig(TRACKING, ELYTRON_ENABLED, AUTHENTICATION_CONTEXT,
                                RECOVERY_ELYTRON_ENABLED, RECOVERY_AUTHENTICATION_CONTEXT, CREDENTIAL_REFERENCE, RECOVERY_CREDENTIAL_REFERENCE) {

                    @Override
                    protected boolean isAttributeWritable(String attributeName) {
                        return false;
                    }

                    @Override
                    protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
                        return attribute.isDefined();
                    }

                    @Override
                    protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
                        return new ModelNode();
                    }
                })
        );
    }

    /**
     * Tests transformation of model from latest version which works well in EAP 7.0.0 without setting up FailedOperationTransformationConfig.
     *
     * @throws Exception
     */
    private void testTransformerEAP7FullConfiguration(String subsystemXml) throws Exception {
        ModelTestControllerVersion eap7ControllerVersion = ModelTestControllerVersion.EAP_7_0_0;
        ModelVersion eap7ModelVersion = ModelVersion.create(4, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);
        KernelServices mainServices = initialKernelServices(builder, eap7ControllerVersion, eap7ModelVersion);
        List<ModelNode> ops = builder.parseXmlResource(subsystemXml);
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, eap7ModelVersion, ops, FailedOperationTransformationConfig.NO_FAILURES);
    }

    /**
     * Tests transformation of model from latest version which needs to be rejected in EAP 7.0.0
     *
     * @throws Exception
     */
    private void testTransformerEAP7Rejection(String subsystemXml) throws Exception {
        //Use the non-runtime version of the extension which will happen on the HC
        ModelTestControllerVersion eap7ControllerVersion = ModelTestControllerVersion.EAP_7_0_0;
        ModelVersion eap7ModelVersion = ModelVersion.create(4, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);
        KernelServices mainServices = initialKernelServices(builder, eap7ControllerVersion, eap7ModelVersion);
        List<ModelNode> ops = builder.parseXmlResource(subsystemXml);
        PathAddress subsystemAddress = PathAddress.pathAddress(DataSourcesSubsystemRootDefinition.PATH_SUBSYSTEM);

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, eap7ModelVersion, ops, new FailedOperationTransformationConfig()
                        .addFailedAttribute(subsystemAddress.append(DataSourceDefinition.PATH_DATASOURCE),
                                new RejectUndefinedAttribute(Constants.CONNECTION_URL.getName()))
        );
    }

    private static class RejectUndefinedAttribute extends AttributesPathAddressConfig<RejectUndefinedAttribute> {

        private RejectUndefinedAttribute(final String... attributes) {
            super(attributes);
        }

        @Override
        protected boolean isAttributeWritable(final String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(final String attrName, final ModelNode attribute, final boolean isWriteAttribute) {
            return !attribute.isDefined();
        }

        @Override
        protected ModelNode correctValue(final ModelNode toResolve, final boolean isWriteAttribute) {
            // Correct by providing a value
            return new ModelNode("token");
        }
    }
}
