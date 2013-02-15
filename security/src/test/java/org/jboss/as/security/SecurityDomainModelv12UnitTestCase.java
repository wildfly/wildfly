/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */
package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.RejectExpressionsConfig;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * <p>
 * Security subsystem tests for the version 1.2 of the subsystem schema.
 * </p>
 */
public class SecurityDomainModelv12UnitTestCase extends AbstractSubsystemBaseTest {

    public SecurityDomainModelv12UnitTestCase() {
        super(SecurityExtension.SUBSYSTEM_NAME, new SecurityExtension());
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {
            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.NORMAL;
            }
        };
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("securitysubsystemv12.xml");
    }

    @Test
    public void testOrder() throws Exception {
        KernelServices service = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("securitysubsystemv12.xml")
                .build();
        PathAddress address = PathAddress.pathAddress().append("subsystem", "security").append("security-domain", "ordering");
        address = address.append("authentication", "classic");

        ModelNode writeOp = Util.createOperation("write-attribute", address);
        writeOp.get("name").set("login-modules");
        for (int i = 1; i <= 5; i++) {
            ModelNode module = writeOp.get("value").add();
            module.get("code").set("module-" + i);
            module.get("flag").set("optional");
            module.get("module-options");

        }
        service.executeOperation(writeOp);
        ModelNode readOp = Util.createOperation("read-attribute", address);
        readOp.get("name").set("login-modules");
        ModelNode result = service.executeForResult(readOp);
        List<ModelNode> modules = result.asList();
        Assert.assertEquals("There should be exactly 5 modules but there are not", 5, modules.size());
        for (int i = 1; i <= 5; i++) {
            ModelNode module = modules.get(i - 1);
            Assert.assertEquals(module.get("code").asString(), "module-" + i);
        }
    }

    @Test
    public void testTransformers712() throws Exception {
        testResourceTransformers_1_1_0("7.1.2.Final");
    }

    @Test
    public void testTransformers713() throws Exception {
        testResourceTransformers_1_1_0("7.1.3.Final");
    }


    @Test
    public void testRejectedTransformers712() throws Exception {
        testOperationTransformers_1_1_0("7.1.2.Final");
    }

    @Test
    public void testRejectedTransformers713() throws Exception {
        testOperationTransformers_1_1_0("7.1.3.Final");
    }


    private void testOperationTransformers_1_1_0(String version) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);


        //which is why we need to include the jboss-as-controller artifact.
        builder.createLegacyKernelServicesBuilder(null, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-security:" + version)
                .addMavenResourceURL("org.jboss.as:jboss-as-controller:" + version)
                .addParentFirstClassPattern("org.jboss.as.controller.*")
                .dontPersistXml()
                //TODO https://issues.jboss.org/browse/AS7-6534
                .skipReverseControllerCheck();


        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());
        ModelTestUtils.checkFailedTransformedBootOperations(
                mainServices,
                modelVersion,
                builder.parseXml(readResource("transformers.xml")),
                getConfig()
        );

    }

    private void testResourceTransformers_1_1_0(String version) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("transformers-noexpressions.xml");

        //which is why we need to include the jboss-as-controller artifact.
        builder.createLegacyKernelServicesBuilder(null, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-security:" + version)
                .addMavenResourceURL("org.jboss.as:jboss-as-controller:" + version)
                .addParentFirstClassPattern("org.jboss.as.controller.*")
                .dontPersistXml()
                //TODO https://issues.jboss.org/browse/AS7-6534
                .skipReverseControllerCheck();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());
        checkSubsystemModelTransformation(mainServices, modelVersion);

        testAddAndRemove_1_1_0(mainServices, modelVersion);
    }

    private void testAddAndRemove_1_1_0(KernelServices mainServices, ModelVersion version) throws Exception {
        final ModelNode mainModel = mainServices.readWholeModel();
        final ModelNode securityDomainParent = mainModel.get(SUBSYSTEM, getMainSubsystemName(), Constants.SECURITY_DOMAIN);

        for (String domainName : securityDomainParent.keys()) {
            ModelNode securityDomain = securityDomainParent.get(domainName);

            if (securityDomain.hasDefined(Constants.AUDIT)) {
                securityDomain.get(Constants.AUDIT).require(Constants.CLASSIC);
                testAddAndRemove_1_1_0(mainServices, version, mainModel, getSecurityDomainAddress(domainName).append(PathElement.pathElement(Constants.AUDIT, Constants.CLASSIC)), Constants.PROVIDER_MODULES, Constants.PROVIDER_MODULE);
            }
            if (securityDomain.hasDefined(Constants.AUTHENTICATION))
            {
                if (securityDomain.get(Constants.AUTHENTICATION).hasDefined(Constants.CLASSIC)) {
                    securityDomain.get(Constants.AUTHENTICATION).require(Constants.CLASSIC);
                    testAddAndRemove_1_1_0(mainServices, version, mainModel, getSecurityDomainAddress(domainName).append(PathElement.pathElement(Constants.AUTHENTICATION, Constants.CLASSIC)), Constants.LOGIN_MODULES, Constants.LOGIN_MODULE);
                }
                if (securityDomain.get(Constants.AUTHENTICATION).hasDefined(Constants.JASPI)) {
                    securityDomain.get(Constants.AUTHENTICATION).require(Constants.JASPI);
                    testAddAndRemove_1_1_0(mainServices, version, mainModel, getSecurityDomainAddress(domainName).append(PathElement.pathElement(Constants.AUTHENTICATION, Constants.JASPI)), Constants.AUTH_MODULES, Constants.AUTH_MODULE);

                    //TODO jaspi=>*
                }
            }
            if (securityDomain.hasDefined(Constants.AUTHORIZATION)) {
                securityDomain.get(Constants.AUTHORIZATION).require(Constants.CLASSIC);
                testAddAndRemove_1_1_0(mainServices, version, mainModel, getSecurityDomainAddress(domainName).append(PathElement.pathElement(Constants.AUTHORIZATION, Constants.CLASSIC)), Constants.POLICY_MODULES, Constants.POLICY_MODULE);
            }
            if (securityDomain.hasDefined(Constants.IDENTITY_TRUST)) {
                securityDomain.get(Constants.IDENTITY_TRUST).require(Constants.CLASSIC);
                testAddAndRemove_1_1_0(mainServices, version, mainModel, getSecurityDomainAddress(domainName).append(PathElement.pathElement(Constants.IDENTITY_TRUST, Constants.CLASSIC)), Constants.TRUST_MODULES, Constants.TRUST_MODULE);
            }
            if (securityDomain.hasDefined(Constants.MAPPING)) {
                securityDomain.get(Constants.MAPPING).require(Constants.CLASSIC);
                testAddAndRemove_1_1_0(mainServices, version, mainModel, getSecurityDomainAddress(domainName).append(PathElement.pathElement(Constants.MAPPING, Constants.CLASSIC)), Constants.MAPPING_MODULES, Constants.MAPPING_MODULE);
            }
        }
    }


    private void testAddAndRemove_1_1_0(KernelServices mainServices, ModelVersion modelVersion, ModelNode subsystemModel, PathAddress parentAddress, String attributeName, String resourceType) throws Exception {
        final ModelNode parentModel = ModelTestUtils.getSubModel(subsystemModel, parentAddress);
        Set<String> originalKeys = new HashSet<String>(parentModel.get(resourceType).keys());


        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        final List<ModelNode> originalAttribute = getLegacyAttribute(legacyServices, parentAddress, attributeName);
        Assert.assertEquals(originalKeys.size(), originalAttribute.size());

        // TODO Check that the attributes are similar
        checkSimilarEntries(originalAttribute, parentModel.get(resourceType));

        ModelNode add = Util.createAddOperation(parentAddress.append(PathElement.pathElement(resourceType, "new-added-by-test")));
        add.get(Constants.CODE).set("new-added-by-test");
        add.get(Constants.FLAG).set("required");
        if (resourceType.equals(Constants.MAPPING_MODULE)) {
            add.get(Constants.TYPE).set("role");
        }
        add.get("module-options", "password-stacking").set("useFirstPass");

        //We need to execute on the main server since its child resources will get used for the legacy service
        ModelTestUtils.checkOutcome(mainServices.executeOperation(add));
        ModelTestUtils.checkOutcome(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, add)));

        List<ModelNode> attributes = getLegacyAttribute(legacyServices, parentAddress, attributeName);
        Assert.assertEquals(originalKeys.size() + 1, attributes.size());

        //Remove the added attribute
        final ModelNode removeAdded = Util.createRemoveOperation(parentAddress.append(PathElement.pathElement(resourceType, "new-added-by-test")));
        ModelTestUtils.checkOutcome(mainServices.executeOperation(removeAdded));
        ModelTestUtils.checkOutcome(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, removeAdded)));
        attributes = getLegacyAttribute(legacyServices, parentAddress, attributeName);
        Assert.assertEquals(originalKeys.size(), attributes.size());
        checkSimilarEntries(attributes, parentModel.get(resourceType));

        //Now try to remove all the other attributes
        int i = originalKeys.size();
        for (String childName : originalKeys) {

            if (i-- == 1) {
                //TODO Without this break there, a remove of the last xxx-module resource becomes a remove of the parent so we get failures
                //when calling getLegacyAttribute() since the resource has been removed.
                break;
            }

            final ModelNode remove = Util.createRemoveOperation(parentAddress.append(PathElement.pathElement(resourceType, childName)));
            ModelTestUtils.checkOutcome(mainServices.executeOperation(remove));
            ModelTestUtils.checkOutcome(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, remove)));
            attributes = getLegacyAttribute(legacyServices, parentAddress, attributeName);
            Assert.assertEquals(i, attributes.size());
        }
    }

    private void checkSimilarEntries(List<ModelNode> attributes, ModelNode parentResource) {
        Assert.assertEquals(attributes.size(), parentResource.keys().size());
        for (ModelNode attr : attributes) {
            String code = attr.get(Constants.CODE).asString();
            ModelNode resource = parentResource.get(code);
            Assert.assertEquals(attr, resource);
        }
    }

    private List<ModelNode> getLegacyAttribute(KernelServices legacyServices, PathAddress parentAddress, String attributeName) throws Exception {
        return legacyServices.executeForResult(Util.getReadAttributeOperation(parentAddress, attributeName)).asList();
    }

    private PathAddress getSecurityDomainAddress(String securityDomainName) {
        return PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, getMainSubsystemName()), PathElement.pathElement(Constants.SECURITY_DOMAIN, securityDomainName));
    }

    private FailedOperationTransformationConfig getConfig() {
        PathAddress subsystemAddress = PathAddress.pathAddress(SecurityExtension.PATH_SUBSYSTEM);
        PathAddress securityDomain = subsystemAddress.append(SecurityExtension.SECURITY_DOMAIN_PATH);
        return new FailedOperationTransformationConfig()
                .addFailedAttribute(subsystemAddress, new RejectExpressionsConfig(SecuritySubsystemRootResourceDefinition.DEEP_COPY_SUBJECT_MODE))
                .addFailedAttribute(securityDomain, new RejectExpressionsConfig(SecurityDomainResourceDefinition.CACHE_TYPE))
                .addFailedAttribute(securityDomain.append(SecurityExtension.JSSE_PATH), new RejectExpressionsConfig(JSSEResourceDefinition.ADDITIONAL_PROPERTIES))
                .addFailedAttribute(subsystemAddress.append(SecurityExtension.VAULT_PATH), new RejectExpressionsConfig(VaultResourceDefinition.OPTIONS))
                ;
    }
}
