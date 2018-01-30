/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import static org.junit.Assert.assertTrue;
import static org.wildfly.extension.undertow.HttpsListenerResourceDefinition.SSL_CONTEXT;
import static org.wildfly.extension.undertow.filters.ModClusterDefinition.MAX_AJP_PACKET_SIZE;

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
import org.wildfly.extension.undertow.filters.ModClusterDefinition;
import org.wildfly.extension.undertow.handlers.ReverseProxyHandler;

/**
 * This is the barebone test example that tests subsystem
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class UndertowTransformersTestCase extends AbstractSubsystemBaseTest {
    private static ModelVersion EAP7_0_0 = ModelVersion.create(3, 1, 0);

    public UndertowTransformersTestCase() {
        super(UndertowExtension.SUBSYSTEM_NAME, new UndertowExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("undertow-transformers.xml");
    }

    @Test
    public void testTransformersEAP_7_0_0() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_0_0, EAP7_0_0);
    }

    @Test
    public void testRejectTransformersEAP_7_0_0() throws Exception {
        doRejectTest(ModelTestControllerVersion.EAP_7_0_0, EAP7_0_0);
    }


    private void doRejectTest(ModelTestControllerVersion controllerVersion, ModelVersion targetVersion) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, targetVersion)
                .configureReverseControllerCheck(createAdditionalInitialization(), null)
                //.skipReverseControllerCheck()
                .addSingleChildFirstClass(DefaultInitialization.class)
                .addMavenResourceURL(String.format("%s:wildfly-undertow:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(targetVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        Assert.assertNotNull(legacyServices);

        List<ModelNode> ops = builder.parseXmlResource("undertow-reject.xml");
        PathAddress subsystemAddress = PathAddress.pathAddress(UndertowExtension.SUBSYSTEM_PATH);
        PathAddress serverAddress = subsystemAddress.append(UndertowExtension.SERVER_PATH);
        PathAddress hostAddress = serverAddress.append(UndertowExtension.HOST_PATH);
        PathAddress httpsAddress = serverAddress.append(UndertowExtension.HTTPS_LISTENER_PATH);
        PathAddress httpAddress = serverAddress.append(UndertowExtension.HTTP_LISTENER_PATH);
        PathAddress reverseProxy = subsystemAddress.append(UndertowExtension.PATH_HANDLERS).append(Constants.REVERSE_PROXY);
        PathAddress reverseProxyServerAddress = reverseProxy.append(Constants.HOST);
        PathAddress modClusterPath = subsystemAddress.append(UndertowExtension.PATH_FILTERS).append(Constants.MOD_CLUSTER);

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, targetVersion, ops, new FailedOperationTransformationConfig()
                .addFailedAttribute(httpAddress,
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11
                        )
                ).addFailedAttribute(httpsAddress,
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11.getName(),
                                HttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING.getName(),
                                HttpListenerResourceDefinition.CERTIFICATE_FORWARDING.getName(),
                                HttpsListenerResourceDefinition.SSL_CONTEXT.getName()
                        )
                )
                .addFailedAttribute(reverseProxy, new FailedOperationTransformationConfig.NewAttributesConfig(ReverseProxyHandler.MAX_RETRIES))
                .addFailedAttribute(reverseProxyServerAddress, new FailedOperationTransformationConfig.NewAttributesConfig(Constants.SSL_CONTEXT))
                .addFailedAttribute(hostAddress.append(UndertowExtension.PATH_HTTP_INVOKER), FailedOperationTransformationConfig.REJECTED_RESOURCE)
                .addFailedAttribute(subsystemAddress.append(UndertowExtension.PATH_APPLICATION_SECURITY_DOMAIN), FailedOperationTransformationConfig.DISCARDED_RESOURCE)
                .addFailedAttribute(modClusterPath, new FailedOperationTransformationConfig.RejectExpressionsConfig(MAX_AJP_PACKET_SIZE))
                .addFailedAttribute(modClusterPath, FailedOperationTransformationConfig.ChainedConfig.createBuilder(SSL_CONTEXT, ModClusterDefinition.MAX_RETRIES, ModClusterDefinition.FAILOVER_STRATEGY, MAX_AJP_PACKET_SIZE)
                        .addConfig(new FailedOperationTransformationConfig.RejectExpressionsConfig(MAX_AJP_PACKET_SIZE))
                        .addConfig(new FailedOperationTransformationConfig.NewAttributesConfig(SSL_CONTEXT, ModClusterDefinition.MAX_RETRIES, ModClusterDefinition.FAILOVER_STRATEGY))
                        .build())
                .addFailedAttribute(subsystemAddress.append(UndertowExtension.PATH_APPLICATION_SECURITY_DOMAIN).append(UndertowExtension.PATH_SSO), FailedOperationTransformationConfig.REJECTED_RESOURCE)
                .addFailedAttribute(subsystemAddress.append(UndertowExtension.PATH_APPLICATION_SECURITY_DOMAIN), FailedOperationTransformationConfig.REJECTED_RESOURCE)

        );
    }


    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion undertowVersion) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("undertow-transformers.xml");
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, undertowVersion)
                .addMavenResourceURL(String.format("%s:wildfly-undertow:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .addSingleChildFirstClass(DefaultInitialization.class)
                .configureReverseControllerCheck(createAdditionalInitialization(), null)
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(undertowVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, undertowVersion, null);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AbstractUndertowSubsystemTestCase.DEFAULT;
    }

    @Override
    public void testSchema() throws Exception {
    }
}
