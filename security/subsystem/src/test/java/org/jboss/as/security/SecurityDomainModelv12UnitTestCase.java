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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.ReadResourceHandler;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.ChainedConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.NewAttributesConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.RejectExpressionsConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.model.test.SingleClassFilter;
import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * <p>
 * Security subsystem tests for the version 1.2 of the subsystem schema.
 * </p>
 */
public class SecurityDomainModelv12UnitTestCase extends AbstractSubsystemBaseTest {

    private static String oldConfig;
    @BeforeClass
    public static void beforeClass() {
        try {
            File target = new File(SecurityDomainModelv11UnitTestCase.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
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

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-security_1_2.xsd";
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
    public void testOrder() throws Exception {
        KernelServices service = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("securitysubsystemv12.xml")
                .build();
        PathAddress address = PathAddress.pathAddress().append("subsystem", "security").append("security-domain", "ordering");
        address = address.append("authentication", "classic");

        ModelNode writeOp = Util.createOperation("write-attribute", address);
        writeOp.get("name").set("login-modules");
        for (int i = 1; i <= 6; i++) {
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
        Assert.assertEquals("There should be exactly 6 modules but there are not", 6, modules.size());
        for (int i = 1; i <= 6; i++) {
            ModelNode module = modules.get(i - 1);
            Assert.assertEquals(module.get("code").asString(), "module-" + i);
        }
    }

    @Test
    public void testTransformers712() throws Exception {
        testResourceTransformers_1_1_0(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformers713() throws Exception {
        testResourceTransformers_1_1_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }


    @Test
    public void testRejectedTransformers712() throws Exception {
        testRejectedTransformers_1_1_0(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testRejectedTransformers713() throws Exception {
        testRejectedTransformers_1_1_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    @Test
    public void testTransformers720() throws Exception {
        testTransformers_1_2_x(ModelTestControllerVersion.V7_2_0_FINAL, 0);
    }

    private void testTransformers_1_2_x(ModelTestControllerVersion controllerVersion, int micro) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 2, micro);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("transformers.xml");


        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-security:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, null)
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion, new ModelFixer() {
            @Override
            public ModelNode fixModel(ModelNode modelNode) {
                //https://issues.jboss.org/browse/WFLY-2474 acl-module was wrongly called login-module in 7.2.0
                ModelNode node = modelNode.get("security-domain", "other", "acl", "classic").get("login-module");
                modelNode.get("security-domain", "other", "acl", "classic", "acl-modules").add(node.get("AclThingy"));
                return modelNode;
            }});

        ModelNode composite = Util.createEmptyOperation("composite", null);
        ModelNode steps = composite.get(STEPS);

        PathAddress secDomAddr = getSecurityDomainAddress("modules");
        steps.add(Util.createEmptyOperation("add", secDomAddr));
        steps.add(getSecurityDomainComponentAdd(secDomAddr.append(PathElement.pathElement(Constants.AUDIT, Constants.CLASSIC)), Constants.PROVIDER_MODULES));
        steps.add(getSecurityDomainComponentAdd(secDomAddr.append(PathElement.pathElement(Constants.AUTHENTICATION, Constants.CLASSIC)), Constants.LOGIN_MODULES));
        steps.add(getSecurityDomainComponentAdd(secDomAddr.append(PathElement.pathElement(Constants.AUTHENTICATION, Constants.JASPI)), Constants.AUTH_MODULES));
        steps.add(getSecurityDomainComponentAdd(secDomAddr.append(PathElement.pathElement(Constants.AUTHORIZATION, Constants.CLASSIC)), Constants.POLICY_MODULES));
        steps.add(getSecurityDomainComponentAdd(secDomAddr.append(PathElement.pathElement(Constants.IDENTITY_TRUST, Constants.CLASSIC)), Constants.TRUST_MODULES));
        steps.add(getSecurityDomainComponentAdd(secDomAddr.append(PathElement.pathElement(Constants.MAPPING, Constants.CLASSIC)), Constants.MAPPING_MODULES));

        ModelTestUtils.checkOutcome(mainServices.executeOperation(composite));
        ModelTestUtils.checkOutcome(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, composite)));

    }

    private void testRejectedTransformers_1_1_0(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);


        //which is why we need to include the jboss-as-controller artifact.
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-security:" + controllerVersion.getMavenGavVersion())
                .dontPersistXml()
                .excludeFromParent(SingleClassFilter.createFilter(SecurityLogger.class));


        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());
        ModelTestUtils.checkFailedTransformedBootOperations(
                mainServices,
                modelVersion,
                //Here we should really use the main subsystem xml, but since the operation transformers read from the model,
                //to create the composite add the framework needs beefing up to be able to correct the model as part of try/fail loop
                //TODO use a custom RejectExpressionsConfig for that?
                builder.parseXml(readResource("securitysubsystemv12.xml")),
                getConfig_1_1_0(mainServices)
        );

    }

    private void testResourceTransformers_1_1_0(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("transformers-noexpressions.xml");

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-security:" + controllerVersion.getMavenGavVersion())
                .dontPersistXml()
                .excludeFromParent(SingleClassFilter.createFilter(SecurityLogger.class));

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
            if (securityDomain.hasDefined(Constants.ACL)) {
                securityDomain.get(Constants.ACL).require(Constants.CLASSIC);
                testAddAndRemove_1_1_0(mainServices, version, mainModel, getSecurityDomainAddress(domainName).append(PathElement.pathElement(Constants.ACL, Constants.CLASSIC)), Constants.ACL_MODULES, Constants.ACL_MODULE);
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

        testAddAndRemoveJaspi_1_1(mainServices, version);
    }


    private void testAddAndRemove_1_1_0(KernelServices mainServices, ModelVersion modelVersion, ModelNode subsystemModel, PathAddress parentAddress, String attributeName, String resourceType) throws Exception {
        final ModelNode parentModel = ModelTestUtils.getSubModel(subsystemModel, parentAddress);
        Set<String> originalKeys = new HashSet<String>(parentModel.get(resourceType).keys());


        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        final List<ModelNode> originalAttribute = getLegacyAttribute(legacyServices, parentAddress, attributeName);
        Assert.assertEquals(originalKeys.size(), originalAttribute.size());

        checkSimilarEntries(originalAttribute, parentModel.get(resourceType));

        ModelNode add = Util.createAddOperation(parentAddress.append(PathElement.pathElement(resourceType, "new-added-by-test")));
        add.get(Constants.CODE).set("new-added-by-test");
        add.get(Constants.FLAG).set("required");
        if (resourceType.equals(Constants.MAPPING_MODULE)) {
            add.get(Constants.TYPE).set("role");
        }
        add.get("module-options", "password-stacking").set("useFirstPass");

        //We need to execute on the main server since its child resources will get used for the legacy service
        executeOpsInBothControllers(mainServices, modelVersion, add);

        List<ModelNode> attributes = getLegacyAttribute(legacyServices, parentAddress, attributeName);
        Assert.assertEquals(originalKeys.size() + 1, attributes.size());

        //Remove the added attribute
        final ModelNode removeAdded = Util.createRemoveOperation(parentAddress.append(PathElement.pathElement(resourceType, "new-added-by-test")));
        executeOpsInBothControllers(mainServices, modelVersion, removeAdded);
        attributes = getLegacyAttribute(legacyServices, parentAddress, attributeName);
        Assert.assertEquals(originalKeys.size(), attributes.size());
        checkSimilarEntries(attributes, parentModel.get(resourceType));

        //Now try to remove all the other attributes
        int i = originalKeys.size();
        for (String childName : originalKeys) {

            final ModelNode remove = Util.createRemoveOperation(parentAddress.append(PathElement.pathElement(resourceType, childName)));
            ModelTestUtils.checkOutcome(mainServices.executeOperation(remove));
            ModelTestUtils.checkOutcome(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, remove)));
            if (--i > 0) {
                //There are still xxx-module so we work fine
                legacyServices.executeForResult(Util.createOperation(ReadResourceHandler.DEFINITION, parentAddress));
                attributes = getLegacyAttribute(legacyServices, parentAddress, attributeName);
                Assert.assertEquals(i, attributes.size());
            } else {
                //Here the read-resource should fail since the resource no longer exists, removal of
                //the last xxx-module resource becomes a remove of the parent
                legacyServices.executeForFailure(Util.createOperation(ReadResourceHandler.DEFINITION, parentAddress));
                //Remove the main resource
                ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createRemoveOperation(parentAddress)));

            }
        }

        //Now that the resource is removed try to add it again, we will also need to add it to the main services since the
        //transformers use the model to decide what is sent across to the legacy controller
        ModelNode addResource = parentModel.clone();

        addResource.remove(resourceType);
        addResource.get(OP).set(ADD);
        addResource.get(OP_ADDR).set(parentAddress.toModelNode());

        ModelTestUtils.checkOutcome(mainServices.executeOperation(addResource));
        ModelTestUtils.checkOutcome(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, addResource)));
        executeOpsInBothControllers(mainServices, modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, parentAddress));

        //Remove the parent resource
        executeOpsInBothControllers(mainServices, modelVersion, Util.createRemoveOperation(parentAddress));

        //Do the add again in the different way
        addResource = parentModel.clone();
        addResource.remove(attributeName);
        List<Property> children = parentModel.clone().remove(resourceType).asPropertyList();
        addResource.get(OP).set(ADD);
        addResource.get(OP_ADDR).set(parentAddress.toModelNode());
        ModelTestUtils.checkOutcome(mainServices.executeOperation(addResource));
        ModelTestUtils.checkOutcome(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, addResource)));
        for (Property childProp : children) {
            ModelNode addChild = Util.createAddOperation(parentAddress.append(resourceType, childProp.getName()));
            for (String key : childProp.getValue().keys()) {
                addChild.get(key).set(childProp.getValue().get(key));
            }
            executeOpsInBothControllers(mainServices, modelVersion, addChild);
        }
        executeOpsInBothControllers(mainServices, modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, parentAddress));
        attributes = getLegacyAttribute(legacyServices, parentAddress, attributeName);
        Assert.assertEquals(originalKeys.size(), attributes.size());
        checkSimilarEntries(attributes, parentModel.get(resourceType));


        //Remove the parent resource
        executeOpsInBothControllers(mainServices, modelVersion, Util.createRemoveOperation(parentAddress));
    }

    private void testAddAndRemoveJaspi_1_1(KernelServices mainServices, ModelVersion modelVersion) throws Exception {
        //Create the operations we will need a bit later - the 'jaspi-test' security domain should still be hanging round
        final PathAddress securityDomainAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SecurityExtension.SUBSYSTEM_NAME),
                PathElement.pathElement(Constants.SECURITY_DOMAIN, "jaspi-test"));
        final PathAddress jaspiAuth = securityDomainAddress.append(SecurityExtension.PATH_JASPI_AUTH);
        final ModelNode addJaspiAuth = Util.createAddOperation(jaspiAuth);
        addJaspiAuth.protect();

        final PathAddress authModule = jaspiAuth.append(Constants.AUTH_MODULE, "org.jboss.Blah");
        final ModelNode addAuthModule = Util.createAddOperation(authModule);
        addAuthModule.get(Constants.CODE).set("org.jboss.Blah");
        addAuthModule.get(Constants.FLAG).set("optional");
        addAuthModule.protect();

        final PathAddress loginModuleStack = jaspiAuth.append(Constants.LOGIN_MODULE_STACK, "test");
        final ModelNode addLoginModuleStack = Util.createAddOperation(loginModuleStack);
        addLoginModuleStack.protect();

        final PathAddress loginModuleStackModule = loginModuleStack.append(Constants.LOGIN_MODULE, "UserRoles");
        final ModelNode addLoginModuleStackModule = Util.createAddOperation(loginModuleStackModule);
        addLoginModuleStackModule.get(Constants.CODE).set("UserRoles");
        addLoginModuleStackModule.get(Constants.FLAG).set("required");
        addLoginModuleStackModule.get(Constants.MODULE_OPTIONS).add("usersProperties", "testA");
        addLoginModuleStackModule.get(Constants.MODULE_OPTIONS).add("rolesProperties", "testB");
        addLoginModuleStackModule.protect();

        //Try to add jaspi resources in the 'wrong' order which breaks the plain ModulesToAttributeTransformer  - this is the same order as the reject test has
        executeOpsInBothControllers(mainServices, modelVersion, addJaspiAuth, addLoginModuleStack, addLoginModuleStackModule, addAuthModule);
        compareModules(mainServices, modelVersion, jaspiAuth, Constants.AUTH_MODULES, Constants.AUTH_MODULE);
        compareModules(mainServices, modelVersion, loginModuleStack, Constants.LOGIN_MODULES, Constants.LOGIN_MODULE);

        //Do a remove by deleting the last child resources
        //Remove the only jaspi login module - this should remove it in the legacy controller
        executeOpsInBothControllers(mainServices, modelVersion, Util.createRemoveOperation(loginModuleStackModule));
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack))));
        //Remove the only auth module - this should remove it in the legacy controller
        executeOpsInBothControllers(mainServices, modelVersion, Util.createRemoveOperation(authModule));
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, jaspiAuth)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, jaspiAuth))));
        //Clean up the remaining stuff in the main controller
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createRemoveOperation(jaspiAuth)));

        //Now add them in the 'right' order
        executeOpsInBothControllers(mainServices, modelVersion, addJaspiAuth, addAuthModule, addLoginModuleStack, addLoginModuleStackModule);
        compareModules(mainServices, modelVersion, jaspiAuth, Constants.AUTH_MODULES, Constants.AUTH_MODULE);
        compareModules(mainServices, modelVersion, loginModuleStack, Constants.LOGIN_MODULES, Constants.LOGIN_MODULE);

        //Remove the jaspi parent resource
        executeOpsInBothControllers(mainServices, modelVersion, Util.createRemoveOperation(jaspiAuth));
        ModelTestUtils.checkFailed(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, jaspiAuth)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, jaspiAuth))));

        //Add them using the collapsed form
        final ModelNode addJaspiAuthCollapsed = addJaspiAuth.clone();
        ModelNode modules = new ModelNode();
        modules.get(Constants.CODE).set("org.jboss.Blah");
        modules.get(Constants.FLAG).set("optional");
        addJaspiAuthCollapsed.get(Constants.AUTH_MODULES).add(modules);
        addJaspiAuthCollapsed.protect();

        final ModelNode addLoginStackCollapsed = addLoginModuleStack.clone();
        modules = new ModelNode();
        modules.get(Constants.CODE).set("UserRoles");
        modules.get(Constants.FLAG).set("required");
        modules.get(Constants.MODULE_OPTIONS).add("usersProperties", "testA");
        modules.get(Constants.MODULE_OPTIONS).add("rolesProperties", "testB");
        addLoginStackCollapsed.get(Constants.LOGIN_MODULES).add(modules);
        addLoginStackCollapsed.protect();

        executeOpsInBothControllers(mainServices, modelVersion, addJaspiAuthCollapsed, addLoginStackCollapsed);
        compareModules(mainServices, modelVersion, jaspiAuth, Constants.AUTH_MODULES, Constants.AUTH_MODULE);
        compareModules(mainServices, modelVersion, loginModuleStack, Constants.LOGIN_MODULES, Constants.LOGIN_MODULE);

        //Add some more modules
        final PathAddress authModule2 = jaspiAuth.append(Constants.AUTH_MODULE, "X");
        final ModelNode addAuthModule2 = Util.createAddOperation(authModule2);
        addAuthModule2.get(Constants.CODE).set("X");
        addAuthModule2.get(Constants.FLAG).set("optional");
        addAuthModule2.protect();
        executeOpsInBothControllers(mainServices, modelVersion, addAuthModule2);
        compareModules(mainServices, modelVersion, jaspiAuth, Constants.AUTH_MODULES, Constants.AUTH_MODULE);
        compareModules(mainServices, modelVersion, loginModuleStack, Constants.LOGIN_MODULES, Constants.LOGIN_MODULE);
        final PathAddress loginModuleStackModule2 = loginModuleStack.append(Constants.LOGIN_MODULE, "UserRoles2");
        final ModelNode addLoginModuleStackModule2 = Util.createAddOperation(loginModuleStackModule2);
        addLoginModuleStackModule2.get(Constants.CODE).set("UserRoles2");
        addLoginModuleStackModule2.get(Constants.FLAG).set("required");
        addLoginModuleStackModule2.get(Constants.MODULE_OPTIONS).add("usersProperties", "testA");
        addLoginModuleStackModule2.get(Constants.MODULE_OPTIONS).add("rolesProperties", "testB");
        addLoginModuleStackModule2.protect();
        executeOpsInBothControllers(mainServices, modelVersion, addLoginModuleStackModule2);
        compareModules(mainServices, modelVersion, jaspiAuth, Constants.AUTH_MODULES, Constants.AUTH_MODULE);
        compareModules(mainServices, modelVersion, loginModuleStack, Constants.LOGIN_MODULES, Constants.LOGIN_MODULE);
        //Now remove all the modules to see if this can work
        //TODO - not sure how to solve the following:
        //Removing the last module removes the parent resource in the legacy model. This is fine if you first remove all
        //the login-modules which removes the stack (which is a child of authentication=jaspi) and then all auth-modules,
        //which removes authentication=jaspi element. However, doing this the other way round will not work as expected,
        //if you remove all auth-modules, authentication=jaspi will disappear taking withit all login module stacks and children
        executeOpsInBothControllers(mainServices, modelVersion, Util.createRemoveOperation(loginModuleStackModule));
        compareModules(mainServices, modelVersion, jaspiAuth, Constants.AUTH_MODULES, Constants.AUTH_MODULE);
        compareModules(mainServices, modelVersion, loginModuleStack, Constants.LOGIN_MODULES, Constants.LOGIN_MODULE);

        executeOpsInBothControllers(mainServices, modelVersion, Util.createRemoveOperation(loginModuleStackModule2));
        compareModules(mainServices, modelVersion, jaspiAuth, Constants.AUTH_MODULES, Constants.AUTH_MODULE);
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack))));

        executeOpsInBothControllers(mainServices, modelVersion, Util.createRemoveOperation(authModule));
        compareModules(mainServices, modelVersion, jaspiAuth, Constants.AUTH_MODULES, Constants.AUTH_MODULE);
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack))));

        executeOpsInBothControllers(mainServices, modelVersion, Util.createRemoveOperation(authModule2));
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, jaspiAuth)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, jaspiAuth))));

        //cleanup the empty resources in the main controller
        mainServices.executeOperation(Util.createRemoveOperation(loginModuleStack));
        mainServices.executeOperation(Util.createRemoveOperation(jaspiAuth));


        //Now add empty ones and use write-attribute to set the modules via the alias, and make sure this shows up in the legacy controller
        executeOpsInBothControllers(mainServices, modelVersion, addJaspiAuth, addLoginModuleStack);
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack))));
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, jaspiAuth)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, jaspiAuth))));

        //First do write-attribute in the 'right' order
        modules = new ModelNode();
        modules.get(Constants.CODE).set("org.jboss.Blah");
        modules.get(Constants.FLAG).set("optional");
        ModelNode temp = modules.clone();
        modules.clear().add(temp.clone());
        final ModelNode writeJaspiAuthAuthModules = Util.getWriteAttributeOperation(jaspiAuth, Constants.AUTH_MODULES, modules);
        writeJaspiAuthAuthModules.protect();
        executeOpsInBothControllers(mainServices, modelVersion, writeJaspiAuthAuthModules);
        compareModules(mainServices, modelVersion, jaspiAuth, Constants.AUTH_MODULES, Constants.AUTH_MODULE);

        modules = new ModelNode();
        modules.get(Constants.CODE).set("UserRoles");
        modules.get(Constants.FLAG).set("required");
        modules.get(Constants.MODULE_OPTIONS).add("usersProperties", "testA");
        modules.get(Constants.MODULE_OPTIONS).add("rolesProperties", "testB");
        temp = modules.clone();
        modules.clear().add(temp.clone());
        final ModelNode writeLoginModuleStackModules = Util.getWriteAttributeOperation(loginModuleStack, Constants.LOGIN_MODULES, modules);
        writeLoginModuleStackModules.protect();
        executeOpsInBothControllers(mainServices, modelVersion, writeLoginModuleStackModules);
        compareModules(mainServices, modelVersion, loginModuleStack, Constants.LOGIN_MODULES, Constants.LOGIN_MODULE);

        //Add another one to each
        final ModelNode writeJaspiAuthAuthModules2 = writeJaspiAuthAuthModules.clone();
        modules = new ModelNode();
        modules.get(Constants.CODE).set("org.jboss.Blah2");
        modules.get(Constants.FLAG).set("optional");
        writeJaspiAuthAuthModules2.get(Constants.VALUE).add(modules);
        executeOpsInBothControllers(mainServices, modelVersion, writeJaspiAuthAuthModules2);
        compareModules(mainServices, modelVersion, jaspiAuth, Constants.AUTH_MODULES, Constants.AUTH_MODULE);
        final ModelNode writeLoginModuleStackModules2 = writeLoginModuleStackModules.clone();
        modules = new ModelNode();
        modules.get(Constants.CODE).set("UserRoles");
        modules.get(Constants.FLAG).set("required");
        modules.get(Constants.MODULE_OPTIONS).add("usersProperties", "testA");
        modules.get(Constants.MODULE_OPTIONS).add("rolesProperties", "testB");
        writeJaspiAuthAuthModules2.get(Constants.VALUE).add(modules);
        executeOpsInBothControllers(mainServices, modelVersion, writeLoginModuleStackModules2);
        compareModules(mainServices, modelVersion, loginModuleStack, Constants.LOGIN_MODULES, Constants.LOGIN_MODULE);

        //Remove by undefining the attribute
        executeOpsInBothControllers(mainServices, modelVersion, Util.getUndefineAttributeOperation(loginModuleStack, Constants.LOGIN_MODULES));
        compareModules(mainServices, modelVersion, jaspiAuth, Constants.AUTH_MODULES, Constants.AUTH_MODULE);
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack))));
        executeOpsInBothControllers(mainServices, modelVersion, Util.getUndefineAttributeOperation(jaspiAuth, Constants.AUTH_MODULES));
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, jaspiAuth)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, jaspiAuth))));

        //cleanup the empty resources in the main controller
        mainServices.executeOperation(Util.createRemoveOperation(loginModuleStack));
        mainServices.executeOperation(Util.createRemoveOperation(jaspiAuth));

        //Now add empty ones and do write-attribute in 'wrong' order
        executeOpsInBothControllers(mainServices, modelVersion, addJaspiAuth, addLoginModuleStack);
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack))));
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, jaspiAuth)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, jaspiAuth))));
        executeOpsInBothControllers(mainServices, modelVersion, writeLoginModuleStackModules);
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack))));
        executeOpsInBothControllers(mainServices, modelVersion, writeJaspiAuthAuthModules);
        compareModules(mainServices, modelVersion, jaspiAuth, Constants.AUTH_MODULES, Constants.AUTH_MODULE);
        compareModules(mainServices, modelVersion, loginModuleStack, Constants.LOGIN_MODULES, Constants.LOGIN_MODULE);

        //Remove by writing an undefined attribute
        executeOpsInBothControllers(mainServices, modelVersion, Util.getWriteAttributeOperation(loginModuleStack, Constants.LOGIN_MODULES, new ModelNode()));
        compareModules(mainServices, modelVersion, jaspiAuth, Constants.AUTH_MODULES, Constants.AUTH_MODULE);
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, loginModuleStack))));
        executeOpsInBothControllers(mainServices, modelVersion, Util.getWriteAttributeOperation(jaspiAuth, Constants.AUTH_MODULES, new ModelNode()));
        ModelTestUtils.checkOutcome(mainServices.executeOperation(Util.createOperation(ReadResourceHandler.DEFINITION, jaspiAuth)));
        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, Util.createOperation(ReadResourceHandler.DEFINITION, jaspiAuth))));

        //cleanup the empty resources in the main controller
        mainServices.executeOperation(Util.createRemoveOperation(loginModuleStack));
        mainServices.executeOperation(Util.createRemoveOperation(jaspiAuth));
    }

    private void compareModules(KernelServices mainServices, ModelVersion modelVersion, PathAddress address, String attrName, String resourceType) throws Exception {
        ModelNode parentModel = ModelTestUtils.getSubModel(mainServices.readWholeModel(false), address);
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        ModelNode attributes = ModelTestUtils.getSubModel(legacyServices.readWholeModel(), address);
        checkSimilarEntries(attributes.get(attrName).asList(), parentModel.get(resourceType));
    }

    private void executeOpsInBothControllers(KernelServices mainServices, ModelVersion modelVersion, ModelNode...ops) throws Exception{
        for (ModelNode op : ops) {
            ModelTestUtils.checkOutcome(mainServices.executeOperation(op.clone()));
            ModelTestUtils.checkOutcome(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, op.clone())));
        }
    }

    private void checkSimilarEntries(List<ModelNode> attributes, ModelNode parentResource) {
        Assert.assertEquals(attributes.size(), parentResource.keys().size());
        for (ModelNode attr : attributes) {
            String code = attr.get(Constants.CODE).asString();
            ModelNode resource = parentResource.get(code);
            ModelTestUtils.compare(attr, resource, true);
        }
    }

    private List<ModelNode> getLegacyAttribute(KernelServices legacyServices, PathAddress parentAddress, String attributeName) throws Exception {
        return legacyServices.executeForResult(Util.getReadAttributeOperation(parentAddress, attributeName)).asList();
    }

    private PathAddress getSecurityDomainAddress(String securityDomainName) {
        return PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, getMainSubsystemName()), PathElement.pathElement(Constants.SECURITY_DOMAIN, securityDomainName));
    }

    private ModelNode getSecurityDomainComponentAdd(PathAddress componentAddr, String modulesListAttribute) {
        ModelNode add = Util.createEmptyOperation("add", componentAddr);
        ModelNode modules = new ModelNode();
        modules.get(Constants.CODE).set("new-added-by-test");
        modules.get(Constants.FLAG).set("required");
        if (modulesListAttribute.equals(Constants.MAPPING_MODULES)) {
            modules.get(Constants.TYPE).set("role");
        }
        modules.get("module-options", "password-stacking").set("useFirstPass");
        modules.get(modulesListAttribute).add(modules);
        return add;
    }

    private FailedOperationTransformationConfig getConfig_1_1_0(KernelServices mainServices) {
        PathAddress subsystemAddress = PathAddress.pathAddress(SecurityExtension.PATH_SUBSYSTEM);
        PathAddress securityDomain = subsystemAddress.append(SecurityExtension.SECURITY_DOMAIN_PATH);
        PathAddress securityDomainOther = subsystemAddress.append(PathElement.pathElement(SecurityExtension.SECURITY_DOMAIN_PATH.getKey(), "other"));

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();

        config.addFailedAttribute(subsystemAddress, new RejectExpressionsConfig(SecuritySubsystemRootResourceDefinition.DEEP_COPY_SUBJECT_MODE));
        config.addFailedAttribute(securityDomain, new RejectExpressionsConfig(SecurityDomainResourceDefinition.CACHE_TYPE));
        config.addFailedAttribute(securityDomainOther.append(SecurityExtension.JSSE_PATH), new RejectExpressionsConfig(JSSEResourceDefinition.ADDITIONAL_PROPERTIES));
        config.addFailedAttribute(subsystemAddress.append(SecurityExtension.VAULT_PATH), new RejectExpressionsConfig(VaultResourceDefinition.OPTIONS));

        PathAddress securityDomainOtherClassicAuthentication = securityDomainOther.append(SecurityExtension.PATH_CLASSIC_AUTHENTICATION);

        PathAddress securityDomainOtherClassicAuthenticationLoginRemoting = securityDomainOtherClassicAuthentication.append(PathElement.pathElement(Constants.LOGIN_MODULE, "Remoting"));
        config.addFailedAttribute(securityDomainOtherClassicAuthenticationLoginRemoting,
                createCorrectModelRejectExpressionsConfig(mainServices, securityDomainOtherClassicAuthenticationLoginRemoting, Constants.FLAG, Constants.MODULE_OPTIONS));

        PathAddress securityDomainOtherJaspiAuthentication = securityDomainOtherClassicAuthentication.append(PathElement.pathElement(Constants.LOGIN_MODULE, "lm"));
        config.addFailedAttribute(securityDomainOtherJaspiAuthentication,
                createCorrectModelRejectExpressionsConfig(mainServices, securityDomainOtherJaspiAuthentication, Constants.FLAG, Constants.MODULE_OPTIONS));

        PathAddress securityDomainOtherClassicAuthenticationLoginRealmUsersRoles = securityDomainOtherClassicAuthentication.append(PathElement.pathElement(Constants.LOGIN_MODULE, "RealmUsersRoles"));
        config.addFailedAttribute(securityDomainOtherClassicAuthenticationLoginRealmUsersRoles,
                createCorrectModelRejectExpressionsConfig(mainServices, securityDomainOtherClassicAuthenticationLoginRealmUsersRoles, Constants.MODULE_OPTIONS));

        PathAddress securityDomainOtherClassicAuthorizationPolicyDenyAll = securityDomainOther.append(SecurityExtension.PATH_AUTHORIZATION_CLASSIC, PathElement.pathElement(Constants.POLICY_MODULE, "DenyAll"));
        config.addFailedAttribute(securityDomainOtherClassicAuthorizationPolicyDenyAll,
                createCorrectModelRejectExpressionsConfig(mainServices, securityDomainOtherClassicAuthorizationPolicyDenyAll, Constants.FLAG, Constants.MODULE_OPTIONS));

        PathAddress securityDomainOtherClassicAcl = securityDomainOther.append(SecurityExtension.ACL_PATH, PathElement.pathElement(Constants.ACL_MODULE, "acl"));
        config.addFailedAttribute(securityDomainOtherClassicAcl,
                createCorrectModelRejectExpressionsConfig(mainServices, securityDomainOtherClassicAcl, Constants.FLAG, Constants.MODULE_OPTIONS));

        PathAddress securityDomainOtherMappingClassicMapping = securityDomainOther.append(SecurityExtension.PATH_MAPPING_CLASSIC, PathElement.pathElement(Constants.MAPPING_MODULE, "test"));
        config.addFailedAttribute(securityDomainOtherMappingClassicMapping,
                createCorrectModelRejectExpressionsConfig(mainServices, securityDomainOtherMappingClassicMapping, Constants.TYPE, Constants.MODULE_OPTIONS));

        PathAddress securityDomainOtherAudit = securityDomainOther.append(SecurityExtension.PATH_AUDIT_CLASSIC, PathElement.pathElement(Constants.PROVIDER_MODULE, "customModule"));
        config.addFailedAttribute(securityDomainOtherAudit,
                createCorrectModelRejectExpressionsConfig(mainServices, securityDomainOtherAudit, Constants.MODULE_OPTIONS));

        PathAddress securityDomainOtherIdentity = securityDomainOther.append(SecurityExtension.PATH_IDENTITY_TRUST_CLASSIC, PathElement.pathElement(Constants.TRUST_MODULE, "IdentityThingy"));
        config.addFailedAttribute(securityDomainOtherIdentity,
                createCorrectModelRejectExpressionsConfig(mainServices, securityDomainOtherIdentity, Constants.FLAG, Constants.MODULE_OPTIONS));

        PathAddress jaspiAuthenticationAuthModule = subsystemAddress.append(
                PathElement.pathElement(Constants.SECURITY_DOMAIN, "jaspi-test"),SecurityExtension.PATH_JASPI_AUTH, PathElement.pathElement(Constants.AUTH_MODULE, "org.jboss.as.web.security.jaspi.modules.HTTPBasicServerAuthModule"));
        config.addFailedAttribute(jaspiAuthenticationAuthModule,
                ChainedConfig.createBuilder(Constants.FLAG, Constants.MODULE_OPTIONS, Constants.MODULE)
                    .addConfig(new CorrectModelConfig(mainServices, jaspiAuthenticationAuthModule, Constants.FLAG))
                    .addConfig(new CorrectModelConfig(mainServices, jaspiAuthenticationAuthModule, Constants.MODULE_OPTIONS))
                    .addConfig(new NewAttributesConfig(Constants.MODULE))
                    .build());

        return config;
    }

    private ChainedConfig createCorrectModelRejectExpressionsConfig(KernelServices kernelServices, PathAddress address, String...attributes) {
        ChainedConfig.Builder builder = ChainedConfig.createBuilder(attributes);
        for (String attr : attributes) {
            builder.addConfig(new CorrectModelConfig(kernelServices, address, attr));
        }
        return builder.build();
    }

    private class CorrectModelConfig extends RejectExpressionsConfig {
        private final KernelServices mainServices;
        private final PathAddress address;
        private final String attribute;

        public CorrectModelConfig(KernelServices mainServices, PathAddress address, String attribute) {
            super(attribute);
            this.mainServices = mainServices;
            this.address = address;
            this.attribute = attribute;
        }
        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            ModelNode resolved = super.correctValue(toResolve, isWriteAttribute);

            //Update the value in the model, the transformer uses the child resource to create the attribute in the parent resource
            ModelNode write = Util.getWriteAttributeOperation(address, attribute, resolved);
            ModelTestUtils.checkOutcome(mainServices.executeOperation(write));
            return resolved;
        }
    }
}
