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

import static org.jboss.as.model.test.FailedOperationTransformationConfig.RejectExpressionsConfig;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
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
        testOperationTransformers_1_1_0("7.1.2.Final");
        testResourceTransformers_1_1_0("7.1.2.Final");
    }

    @Test
    public void testTransformers713() throws Exception {
        testOperationTransformers_1_1_0("7.1.3.Final");
        testResourceTransformers_1_1_0("7.1.3.Final");
    }


    private void testOperationTransformers_1_1_0(String version) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);


        //which is why we need to include the jboss-as-controller artifact.
        builder.createLegacyKernelServicesBuilder(null, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-security:" + version)
                .addMavenResourceURL("org.jboss.as:jboss-as-controller:" + version)
                .addParentFirstClassPattern("org.jboss.as.controller.*")
                .dontPersistXml();

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
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        //Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());
        checkSubsystemModelTransformation(mainServices, modelVersion);
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
