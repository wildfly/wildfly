/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.wildfly.extension.elytron.oidc.OidcTestCase.DefaultInitializer;

@RunWith(value = Parameterized.class)
public class ElytronOidcClientSubsystemTransformerTestCase extends AbstractSubsystemSchemaTest<ElytronOidcSubsystemSchema> {

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return List.<Object[]>of(
                new Object[] { ModelTestControllerVersion.EAP_8_0_0, ElytronOidcClientSubsystemModel.VERSION_2_0_0.getVersion() }
        );
    }
    private final ModelTestControllerVersion controllerVersion;
    private final ModelVersion modelVersion;
    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, ElytronOidcExtension.SUBSYSTEM_NAME);


    public ElytronOidcClientSubsystemTransformerTestCase(ModelTestControllerVersion controllerVersion, ModelVersion version) {
        super(ElytronOidcExtension.SUBSYSTEM_NAME, new ElytronOidcExtension(), ElytronOidcSubsystemSchema.VERSION_3_0_COMMUNITY, ElytronOidcSubsystemSchema.CURRENT.get(Stability.COMMUNITY));
        this.controllerVersion = controllerVersion;
        this.modelVersion = version;
    }

    @Test
    public void testTransformations() throws Exception {
        final ModelVersion version = modelVersion;
        KernelServices services = this.buildKernelServices(controllerVersion, version);

        checkSubsystemModelTransformation(services, version, null, false);
        ModelNode transformed = services.readTransformedModel(version);
        Assert.assertTrue(transformed.isDefined());
    }

    @Test
    public void testRejection() throws Exception {
        testRejectingTransformers(ModelTestControllerVersion.EAP_8_0_0, "elytron-oidc-client-reject.xml", new FailedOperationTransformationConfig()
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT, "wildfly-reject-deployment-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.SCOPE)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.SCOPE))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_SERVER, "wildfly-reject-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.SCOPE)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.SCOPE))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT, "wildfly-reject-deployment-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_SERVER, "wildfly-reject-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT, "wildfly-reject-deployment-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_ENCRYPTION_ALG_VALUE))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_SERVER, "wildfly-reject-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_ENCRYPTION_ALG_VALUE))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT, "wildfly-reject-deployment-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_ENCRYPTION_ENC_VALUE))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_SERVER, "wildfly-reject-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_ENCRYPTION_ENC_VALUE))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT, "wildfly-reject-deployment-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_ALGORITHM))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_SERVER, "wildfly-reject-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_ALGORITHM))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT, "wildfly-reject-deployment-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEY_ALIAS))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_SERVER, "wildfly-reject-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEY_ALIAS))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT, "wildfly-reject-deployment-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEY_PASSWORD))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_SERVER, "wildfly-reject-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEY_PASSWORD))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT, "wildfly-reject-deployment-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_SERVER, "wildfly-reject-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT, "wildfly-reject-deployment-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_SERVER, "wildfly-reject-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT, "wildfly-reject-deployment-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE))
                .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_SERVER, "wildfly-reject-with-scope.war"), PathElement.pathElement(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT)),
                        new FailedOperationTransformationConfig.NewAttributesConfig(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE))
        );
    }

    private void testRejectingTransformers(ModelTestControllerVersion controllerVersion, final String subsystemXmlFile, final FailedOperationTransformationConfig config) throws Exception {
        ModelVersion version = modelVersion;
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.withCapabilities(
                RuntimeCapability.buildDynamicCapabilityName("org.wildfly.security", "elytron")));

        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.withCapabilities(
                RuntimeCapability.buildDynamicCapabilityName("org.wildfly.security", "elytron")), controllerVersion, version)
                .addMavenResourceURL(controllerVersion.createGAV("wildfly-elytron-oidc-client-subsystem"))
                .skipReverseControllerCheck()
                .addParentFirstClassPattern("org.jboss.as.controller.logging.ControllerLogger*")
                .addParentFirstClassPattern("org.jboss.as.controller.PathAddress")
                .addParentFirstClassPattern("org.jboss.as.controller.PathElement")
                .addParentFirstClassPattern("org.jboss.as.server.logging.*")
                .addParentFirstClassPattern("org.jboss.logging.*")
                .addParentFirstClassPattern("org.jboss.dmr.*")
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(ModelTestControllerVersion.MASTER + " boot failed", services.isSuccessfulBoot());
        Assert.assertTrue(controllerVersion.getMavenGavVersion() + " boot failed", services.getLegacyServices(version).isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource(subsystemXmlFile);
        ModelTestUtils.checkFailedTransformedBootOperations(services, version, ops, config);
    }

    private KernelServices buildKernelServices(ModelTestControllerVersion controllerVersion, ModelVersion version) throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder(new DefaultInitializer(this.getSubsystemSchema().getStability()))
                .setSubsystemXmlResource("elytron-oidc-client-transform.xml");
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC, controllerVersion, version)
                .addMavenResourceURL(controllerVersion.createGAV("wildfly-elytron-oidc-client-subsystem"))
                .skipReverseControllerCheck()
                .addParentFirstClassPattern("org.jboss.as.controller.logging.ControllerLogger*")
                .addParentFirstClassPattern("org.jboss.as.controller.PathAddress")
                .addParentFirstClassPattern("org.jboss.as.controller.PathElement")
                .addParentFirstClassPattern("org.jboss.as.server.logging.*")
                .addParentFirstClassPattern("org.jboss.logging.*")
                .addParentFirstClassPattern("org.jboss.dmr.*")
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(ModelTestControllerVersion.MASTER + " boot failed", services.isSuccessfulBoot());
        Assert.assertTrue(controllerVersion.getMavenGavVersion() + " boot failed", services.getLegacyServices(version).isSuccessfulBoot());

        return services;
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) {
        //
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("elytron-oidc-client-transform.xml");
    }
}
