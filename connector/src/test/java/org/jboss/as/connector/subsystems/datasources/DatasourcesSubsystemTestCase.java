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

import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTION_SORTER_MODULE;
import static org.jboss.as.connector.subsystems.datasources.Constants.STALE_CONNECTION_CHECKER_MODULE;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALID_CONNECTION_CHECKER_MODULE;

import java.io.IOException;
import java.util.List;

import org.jboss.as.connector._private.Capabilities;
import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.model.test.SingleClassFilter;
import org.jboss.as.naming.service.NamingService;
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
        return "schema/wildfly-datasources_7_0.xsd";
    }

    @Test
    public void testFullConfig() throws Exception {
        standardSubsystemTest("datasources-full.xml");
    }

    @Test
    public void testElytronConfig() throws Exception {
        standardSubsystemTest("datasources-elytron-enabled.xml");
    }

    @Test
    public void testExpressionConfig() throws Exception {
        standardSubsystemTest("datasources-full-expression.xml", "datasources-full.xml");
    }

    @Test
    public void testElytronExpressionConfig() throws Exception {
        standardSubsystemTest("datasources-elytron-enabled-expression.xml", "datasources-elytron-enabled.xml");
    }

    @Test
    public void testRejectionsEAP74() throws Exception {
        testTransformerEAP74Rejection("datasources-validation-custom-modules-reject.xml");
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        // Create a AdditionalInitialization.MANAGEMENT variant that has all the external
        // capabilities used by the various configs used in this test class
        return AdditionalInitialization.MANAGEMENT.withCapabilities(
                Capabilities.AUTHENTICATION_CONTEXT_CAPABILITY + ".DsAuthCtxt",
                Capabilities.AUTHENTICATION_CONTEXT_CAPABILITY + ".CredentialAuthCtxt",
                CredentialReference.CREDENTIAL_STORE_CAPABILITY + ".test-store",
                NamingService.CAPABILITY_NAME,
                ConnectorServices.TRANSACTION_INTEGRATION_CAPABILITY_NAME
        );
    }

    private void testTransformerEAP74Rejection(String subsystemXml) throws Exception {
        ModelTestControllerVersion eap74ControllerVersion = ModelTestControllerVersion.EAP_7_4_0;
        ModelVersion eap74ModelVersion = ModelVersion.create(6, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        KernelServices mainServices = initialKernelServices(builder, eap74ControllerVersion, eap74ModelVersion);

        List<ModelNode> ops = builder.parseXmlResource(subsystemXml);

        PathAddress subsystemAddress = PathAddress.pathAddress(DataSourcesSubsystemRootDefinition.PATH_SUBSYSTEM);
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, eap74ModelVersion, ops, new FailedOperationTransformationConfig()
                .addFailedAttribute(subsystemAddress.append(DataSourceDefinition.PATH_DATASOURCE),
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                EXCEPTION_SORTER_MODULE,
                                VALID_CONNECTION_CHECKER_MODULE,
                                STALE_CONNECTION_CHECKER_MODULE))
                .addFailedAttribute(subsystemAddress.append(XaDataSourceDefinition.PATH_XA_DATASOURCE),
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                EXCEPTION_SORTER_MODULE,
                                VALID_CONNECTION_CHECKER_MODULE,
                                STALE_CONNECTION_CHECKER_MODULE))
                );
    }

    private KernelServices initialKernelServices(KernelServicesBuilder builder, ModelTestControllerVersion controllerVersion, final ModelVersion modelVersion) throws Exception {
        LegacyKernelServicesInitializer initializer = builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, modelVersion);
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
}

