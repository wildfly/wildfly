/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Security subsystem tests for the version 2.0 of the subsystem schema.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class SecurityDomainModelv20UnitTestCase extends AbstractSubsystemBaseTest {

    public SecurityDomainModelv20UnitTestCase() {
        super(SecurityExtension.SUBSYSTEM_NAME, new SecurityExtension());
    }

    private static String oldConfig;


    @BeforeClass
    public static void beforeClass() {
        try {
            File target = new File(SecurityDomainModelv20UnitTestCase.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
            File config = new File(target, "config");
            config.mkdir();
            oldConfig = System.setProperty("jboss.server.config.dir", config.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void afterClass() {
        if (oldConfig != null) {
            System.setProperty("jboss.server.config.dir", oldConfig);
        } else {
            System.clearProperty("jboss.server.config.dir");
        }
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("securitysubsystemv20.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-security_2_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/security.xml"
        };
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.put("jboss.server.config.dir", System.getProperty("java.io.tmpdir"));
        return properties;
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities("org.wildfly.clustering.infinispan.default-cache-configuration.security");
    }

    @Test
    public void testTransformersEAP64() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_6_4_0);
    }

    @Test
    public void testTransformersEAP70() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_0_0);
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion) throws Exception {

        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        ModelVersion version = ModelVersion.create(1, 3, 0);

        final String mavenGavVersion = controllerVersion.getMavenGavVersion();
        final String artifactId;
        if (controllerVersion.isEap() && mavenGavVersion.equals(controllerVersion.getCoreVersion())) {
            /* EAP 6 */
            artifactId = "jboss-as-security";
        } else {
            artifactId = "wildfly-security";
        }

        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, version)
                .addMavenResourceURL(controllerVersion.getMavenGroupId() + ":"+ artifactId +":" + mavenGavVersion);

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        // any elytron-related resources in the model should get rejected as those are not supported in model version 1.3.0.
        PathAddress subsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, getMainSubsystemName()));
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version,
                builder.parseXmlResource("securitysubsystemv20.xml"),
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(PathAddress.pathAddress(subsystemAddress, PathElement.pathElement(Constants.ELYTRON_REALM)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(PathAddress.pathAddress(subsystemAddress, PathElement.pathElement(Constants.ELYTRON_KEY_STORE)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(PathAddress.pathAddress(subsystemAddress, PathElement.pathElement(Constants.ELYTRON_TRUST_STORE)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(PathAddress.pathAddress(subsystemAddress, PathElement.pathElement(Constants.ELYTRON_KEY_MANAGER)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(PathAddress.pathAddress(subsystemAddress, PathElement.pathElement(Constants.ELYTRON_TRUST_MANAGER)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(
                                PathAddress.pathAddress(subsystemAddress,
                                        PathElement.pathElement(Constants.SECURITY_DOMAIN, "domain-with-custom-audit-provider"),
                                        SecurityExtension.PATH_AUDIT_CLASSIC,
                                        PathElement.pathElement(Constants.PROVIDER_MODULE,
                                                "org.myorg.security.MyCustomLogAuditProvider")),
                                new FailedOperationTransformationConfig.NewAttributesConfig(Constants.MODULE))
                        .addFailedAttribute(PathAddress.pathAddress(subsystemAddress, PathElement.pathElement(Constants.SECURITY_MANAGEMENT)),
                                new FailedOperationTransformationConfig.NewAttributesConfig(Constants.INITIALIZE_JACC)));
    }
}
