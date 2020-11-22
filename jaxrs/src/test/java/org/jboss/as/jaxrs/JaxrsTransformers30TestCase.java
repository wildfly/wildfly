/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxrs;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
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
 * @author <a href="mailto:ema@rehdat.com>Jim Ma</a>
 * @author <a href="mailto:alessio.soldano@jboss.com>Alessio Soldano</a>
 * @author <a href="rsigal@redhat.com>Ron Sigal</a>
 */
public class JaxrsTransformers30TestCase extends AbstractSubsystemBaseTest {

    public JaxrsTransformers30TestCase() {
        super(JaxrsExtension.SUBSYSTEM_NAME, new JaxrsExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("jaxrs-3.0.xml");
    }

    @Test
    public void testTransformers740() throws Exception {
        testTransformers_3_0_to_2_0(ModelTestControllerVersion.MASTER);
    }

    @Test
    public void testRejections_2_0_0() throws Exception {
        testRejections_3_0_0(ModelTestControllerVersion.MASTER);
    }

    /**
     * Tests transformation of model from version 3.0 to version 2.0.
     */
    private void testTransformers_3_0_to_2_0(ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXmlResource("jaxrs-3.0.xml");

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, JaxrsExtension.MODEL_VERSION_2_0_0)
        .addMavenResourceURL("org.jboss.eap:wildfly-jaxrs:" + controllerVersion.getMavenGavVersion())
        .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(JaxrsExtension.MODEL_VERSION_2_0_0);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, JaxrsExtension.MODEL_VERSION_2_0_0);
    }

    /**
     * Tests rejections of attributes introduced in model version 3.0.
     */
    private void testRejections_3_0_0(ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, JaxrsExtension.MODEL_VERSION_2_0_0)
        .addMavenResourceURL("org.jboss.eap:wildfly-jaxrs:" + controllerVersion.getMavenGavVersion())
        .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, null)
        .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(JaxrsExtension.MODEL_VERSION_2_0_0);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXmlResource("jaxrs-3.0.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, JaxrsExtension.MODEL_VERSION_2_0_0, xmlOps, getFailedTransformationConfig());
    }

    private FailedOperationTransformationConfig getFailedTransformationConfig() {
        PathAddress subsystemAddress = PathAddress.pathAddress(JaxrsExtension.SUBSYSTEM_PATH);
        return new FailedOperationTransformationConfig()
              .addFailedAttribute(subsystemAddress,
                    new FailedOperationTransformationConfig.NewAttributesConfig(
                          JaxrsAttribute.RESTEASY_ORIGINAL_WEBAPPLICATIONEXCEPTION_BEHAVIOR
                          ));
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {
            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.ADMIN_ONLY;
            }
        };
    }

}
