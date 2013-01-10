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

package org.jboss.as.modcluster;

import java.io.IOException;

import junit.framework.Assert;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.fail;

import org.jboss.as.modcluster.CommonAttributes;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.modcluster.ModClusterExtension;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Jean-Frederic Clere.
 */
public class ModClusterSubsystemTestCase extends AbstractSubsystemBaseTest {

    public ModClusterSubsystemTestCase() {
        super(ModClusterExtension.SUBSYSTEM_NAME, new ModClusterExtension());
    }

    @Test
    public void testXsd10() throws Exception {
        standardSubsystemTest("subsystem_1_0.xml",false);
    }

    @Test
    public void testTransformers_1_2_0() throws Exception {
        String subsystemXml = readResource("subsystem_1_0.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 2, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXml(subsystemXml);

        builder.createLegacyKernelServicesBuilder(null, modelVersion)
        .addMavenResourceURL("org.jboss.as:jboss-as-modcluster:7.1.2.Final");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    @Test
    public void testExpressionsAreRejectedByVersion_1_2() throws Exception {
        String subsystemXml = readResource("subsystem.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 2, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        builder.createLegacyKernelServicesBuilder(null, modelVersion)
        .addMavenResourceURL("org.jboss.as:jboss-as-modcluster:7.1.2.Final");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        PathAddress rootAddr = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, ModClusterExtension.SUBSYSTEM_NAME));
        PathAddress confAddr = rootAddr.append(PathElement.pathElement(CommonAttributes.MOD_CLUSTER_CONFIG, CommonAttributes.CONFIGURATION));
        PathAddress simpAddr = confAddr.append(PathElement.pathElement(CommonAttributes.SIMPLE_LOAD_PROVIDER_FACTOR, CommonAttributes.CONFIGURATION));
        PathAddress dynaAddr = confAddr.append(PathElement.pathElement(CommonAttributes.DYNAMIC_LOAD_PROVIDER, CommonAttributes.CONFIGURATION));
        PathAddress metrAddr = dynaAddr.append(PathElement.pathElement(CommonAttributes.LOAD_METRIC, "*"));
        PathAddress custAddr = dynaAddr.append(PathElement.pathElement(CommonAttributes.CUSTOM_LOAD_METRIC, "*"));
        PathAddress sslAddr = confAddr.append(PathElement.pathElement(CommonAttributes.SSL, CommonAttributes.CONFIGURATION));
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, parse(subsystemXml),
                new FailedOperationTransformationConfig()
                    .addFailedAttribute(metrAddr,
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.CAPACITY, CommonAttributes.WEIGHT
                                    ))
                     .addFailedAttribute(custAddr,
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.CAPACITY, CommonAttributes.WEIGHT,
                                    CommonAttributes.CLASS))
                    .addFailedAttribute(dynaAddr,
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.DECAY, CommonAttributes.HISTORY))
                     .addFailedAttribute(simpAddr,
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.FACTOR))
                     .addFailedAttribute(sslAddr,
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(
                                    CommonAttributes.CIPHER_SUITE, CommonAttributes.KEY_ALIAS,
                                    CommonAttributes.PROTOCOL))
                    .addFailedAttribute(confAddr,
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(CommonAttributes.ADVERTISE,
                                    CommonAttributes.ADVERTISE_SOCKET, CommonAttributes.ADVERTISE_SOCKET,
                                    CommonAttributes.AUTO_ENABLE_CONTEXTS, CommonAttributes.FLUSH_PACKETS,
                                    CommonAttributes.PING,
                                    CommonAttributes.STICKY_SESSION, CommonAttributes.STICKY_SESSION_FORCE, CommonAttributes.STICKY_SESSION_REMOVE
                                    ))
                    );
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXml(String configId) throws IOException {
        return readResource(configId);
    }


    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

}
