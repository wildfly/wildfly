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
import static org.wildfly.extension.undertow.ListenerResourceDefinition.ALLOW_UNESCAPED_CHARACTERS_IN_URL;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.RFC6265_COOKIE_VALIDATION;
import static org.wildfly.extension.undertow.filters.ModClusterDefinition.MAX_AJP_PACKET_SIZE;

import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;
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
public class UndertowTransformersTestCase extends AbstractSubsystemTest {
    private static final ModelVersion EAP7_0_0 = ModelVersion.create(3, 1, 0);
    private static final ModelVersion EAP7_1_0 = ModelVersion.create(4, 0, 0);

    public UndertowTransformersTestCase() {
        super(UndertowExtension.SUBSYSTEM_NAME, new UndertowExtension());
    }

    @Test
    public void testTransformersEAP_7_0_0() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_0_0, EAP7_0_0);
    }

    @Test
    public void testTransformersEAP_7_1_0() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_1_0, EAP7_1_0);
    }

    @Test
    public void testRejectTransformersEAP_7_0_0() throws Exception {
        PathAddress subsystemAddress = PathAddress.pathAddress(UndertowExtension.SUBSYSTEM_PATH);
        PathAddress serverAddress = subsystemAddress.append(UndertowExtension.SERVER_PATH);
        PathAddress hostAddress = serverAddress.append(UndertowExtension.HOST_PATH);
        PathAddress httpsAddress = serverAddress.append(UndertowExtension.HTTPS_LISTENER_PATH);
        PathAddress ajpAddress = serverAddress.append(UndertowExtension.AJP_LISTENER_PATH);
        PathAddress httpAddress = serverAddress.append(UndertowExtension.HTTP_LISTENER_PATH);
        PathAddress reverseProxy = subsystemAddress.append(UndertowExtension.PATH_HANDLERS).append(Constants.REVERSE_PROXY);
        PathAddress reverseProxyServerAddress = reverseProxy.append(Constants.HOST);
        PathAddress modClusterPath = subsystemAddress.append(UndertowExtension.PATH_FILTERS).append(Constants.MOD_CLUSTER);
        PathAddress servletContainer = subsystemAddress.append(UndertowExtension.PATH_SERVLET_CONTAINER);
        PathAddress byteBufferPath = subsystemAddress.append(UndertowExtension.BYTE_BUFFER_POOL_PATH);

        doRejectTest(ModelTestControllerVersion.EAP_7_0_0, EAP7_0_0, new FailedOperationTransformationConfig()
                .addFailedAttribute(byteBufferPath, FailedOperationTransformationConfig.REJECTED_RESOURCE)
                .addFailedAttribute(hostAddress, new FailedOperationTransformationConfig.NewAttributesConfig(HostDefinition.QUEUE_REQUESTS_ON_START))
                .addFailedAttribute(httpAddress,
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11,
                                HttpListenerResourceDefinition.PROXY_PROTOCOL,
                                HttpListenerResourceDefinition.ALLOW_UNESCAPED_CHARACTERS_IN_URL,
                                HttpListenerResourceDefinition.RFC6265_COOKIE_VALIDATION
                        )
                ).addFailedAttribute(httpsAddress,
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11,
                                HttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING,
                                HttpListenerResourceDefinition.CERTIFICATE_FORWARDING,
                                HttpsListenerResourceDefinition.SSL_CONTEXT,
                                HttpsListenerResourceDefinition.ALLOW_UNESCAPED_CHARACTERS_IN_URL,
                                HttpListenerResourceDefinition.PROXY_PROTOCOL,
                                HttpListenerResourceDefinition.RFC6265_COOKIE_VALIDATION

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
                .addFailedAttribute(servletContainer,
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                ServletContainerDefinition.DEFAULT_COOKIE_VERSION,
                                ServletContainerDefinition.FILE_CACHE_MAX_FILE_SIZE,
                                ServletContainerDefinition.FILE_CACHE_METADATA_SIZE,
                                ServletContainerDefinition.FILE_CACHE_TIME_TO_LIVE,
                                ServletContainerDefinition.DISABLE_FILE_WATCH_SERVICE,
                                ServletContainerDefinition.DISABLE_SESSION_ID_REUSE))
                .addFailedAttribute(ajpAddress,
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                ALLOW_UNESCAPED_CHARACTERS_IN_URL, RFC6265_COOKIE_VALIDATION))
        );
    }

    @Test
    public void testRejectTransformersEAP_7_1_0() throws Exception {
        PathAddress subsystemAddress = PathAddress.pathAddress(UndertowExtension.SUBSYSTEM_PATH);
        PathAddress serverAddress = subsystemAddress.append(UndertowExtension.SERVER_PATH);
        PathAddress hostAddress = serverAddress.append(UndertowExtension.HOST_PATH);
        PathAddress httpsAddress = serverAddress.append(UndertowExtension.HTTPS_LISTENER_PATH);
        PathAddress ajpAddress = serverAddress.append(UndertowExtension.AJP_LISTENER_PATH);
        PathAddress httpAddress = serverAddress.append(UndertowExtension.HTTP_LISTENER_PATH);
        PathAddress servletContainer = subsystemAddress.append(UndertowExtension.PATH_SERVLET_CONTAINER);
        PathAddress byteBufferPath = subsystemAddress.append(UndertowExtension.BYTE_BUFFER_POOL_PATH);

        doRejectTest(ModelTestControllerVersion.EAP_7_1_0, EAP7_1_0, new FailedOperationTransformationConfig()
                .addFailedAttribute(byteBufferPath, FailedOperationTransformationConfig.REJECTED_RESOURCE)
                .addFailedAttribute(hostAddress, new FailedOperationTransformationConfig.NewAttributesConfig(HostDefinition.QUEUE_REQUESTS_ON_START))
                .addFailedAttribute(httpAddress,
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                HttpListenerResourceDefinition.PROXY_PROTOCOL,
                                HttpListenerResourceDefinition.ALLOW_UNESCAPED_CHARACTERS_IN_URL
                        )
                ).addFailedAttribute(httpsAddress,
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                HttpListenerResourceDefinition.PROXY_PROTOCOL,
                                HttpsListenerResourceDefinition.ALLOW_UNESCAPED_CHARACTERS_IN_URL
                        )
                )
                .addFailedAttribute(servletContainer,
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                ServletContainerDefinition.DEFAULT_COOKIE_VERSION,
                                ServletContainerDefinition.FILE_CACHE_MAX_FILE_SIZE,
                                ServletContainerDefinition.FILE_CACHE_METADATA_SIZE,
                                ServletContainerDefinition.FILE_CACHE_TIME_TO_LIVE))
                .addFailedAttribute(ajpAddress,
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                ALLOW_UNESCAPED_CHARACTERS_IN_URL))
        );
    }

    @Test
    public void testConvertTransformersEAP_7_1_0() throws Exception {
        // https://issues.jboss.org/browse/WFLY-9675 Fix max-post-size LongRangeValidator min to 0.
        // Test Listener attribute max-post-size value 0 is converted to Long.MAX
        doConvertTest(ModelTestControllerVersion.EAP_7_1_0, EAP7_1_0);
    }

    private void doRejectTest(ModelTestControllerVersion controllerVersion, ModelVersion targetVersion, FailedOperationTransformationConfig config) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        LegacyKernelServicesInitializer init = builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, targetVersion)
                .configureReverseControllerCheck(createAdditionalInitialization(), null)
                //.skipReverseControllerCheck()
                .addSingleChildFirstClass(DefaultInitialization.class)
                .addMavenResourceURL(String.format("%s:wildfly-undertow:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .dontPersistXml();

        addExtraMavenUrls(controllerVersion, init);

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(targetVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        Assert.assertNotNull(legacyServices);

        List<ModelNode> ops = builder.parseXmlResource("undertow-reject.xml");

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, targetVersion, ops, config);
    }

    private void doConvertTest(ModelTestControllerVersion controllerVersion, ModelVersion targetVersion) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, targetVersion)
                .configureReverseControllerCheck(createAdditionalInitialization(), null)
                //.skipReverseControllerCheck()
                .addSingleChildFirstClass(DefaultInitialization.class)
                .addMavenResourceURL(UndertowDependencies.getUndertowDependencies(controllerVersion))
                .addMavenResourceURL(String.format("%s:wildfly-undertow:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(targetVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        Assert.assertNotNull(legacyServices);

        List<ModelNode> ops = builder.parseXmlResource("undertow-convert.xml");
        for (ModelNode op : ops) {
            if (op.hasDefined(Constants.MAX_POST_SIZE) && op.get(Constants.MAX_POST_SIZE).asLong() == 0L) {
                TransformedOperation transformedOperation = mainServices.transformOperation(targetVersion, op.clone());
                ModelNode transformed = transformedOperation.getTransformedOperation().get(Constants.MAX_POST_SIZE);
                Assert.assertEquals(Constants.MAX_POST_SIZE + " should be transformed for value 0.", Long.MAX_VALUE, transformed.asLong());
            }
        }
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion undertowVersion) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        final String eapVersion;
        switch (controllerVersion) {
            case EAP_7_0_0:
                eapVersion = "7.0";
                break;
            case EAP_7_1_0:
                eapVersion = "7.1";
                break;
            default:
                Assert.fail(controllerVersion + " not yet configured");
                return;
        }
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource(String.format("undertow-%s-transformers.xml", eapVersion));
        LegacyKernelServicesInitializer init = builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, undertowVersion)
                .addMavenResourceURL(String.format("%s:wildfly-undertow:%s", controllerVersion.getMavenGroupId(), controllerVersion.getMavenGavVersion()))
                .addSingleChildFirstClass(DefaultInitialization.class)
                .configureReverseControllerCheck(createAdditionalInitialization(), null)
                .dontPersistXml();

        addExtraMavenUrls(controllerVersion, init);

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(undertowVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, undertowVersion, null);
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AbstractUndertowSubsystemTestCase.DEFAULT;
    }

    private void addExtraMavenUrls(ModelTestControllerVersion controllerVersion, LegacyKernelServicesInitializer init) throws Exception {
        if (controllerVersion == ModelTestControllerVersion.EAP_7_1_0) {
            init.addMavenResourceURL(controllerVersion.getMavenGroupId() + ":wildfly-clustering-common:" + controllerVersion.getMavenGavVersion());
            init.addMavenResourceURL(controllerVersion.getMavenGroupId() + ":wildfly-web-common:" + controllerVersion.getMavenGavVersion());
            init.addMavenResourceURL("io.undertow:undertow-servlet:2.0.4.Final");
            init.addMavenResourceURL("io.undertow:undertow-core:2.0.4.Final");
        }
    }
}
