/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.extension.undertow.filters.PredicateHandlerWrapper;

public abstract class AbstractUndertowSubsystemTestCase extends AbstractSubsystemSchemaTest<UndertowSubsystemSchema> {
    final Map<ServiceName, Supplier<Object>> values = new ConcurrentHashMap<>();
    protected final UndertowSubsystemSchema schema;

    AbstractUndertowSubsystemTestCase(UndertowSubsystemSchema schema) {
        super(UndertowExtension.SUBSYSTEM_NAME, new UndertowExtension(), schema, UndertowSubsystemSchema.CURRENT);
        this.schema = schema;
    }

    @Before
    public void setUp() {
        System.setProperty("server.data.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        System.setProperty("jboss.server.server.dir", System.getProperty("java.io.tmpdir"));
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.put("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        properties.put("jboss.server.server.dir", System.getProperty("java.io.tmpdir"));
        properties.put("server.data.dir", System.getProperty("java.io.tmpdir"));
        return properties;
    }

    @Test
    public void testRuntime() throws Exception {
        // Skip runtime tests for old versions - since legacy SSO is only allowed in admin-only mode
        if (!this.schema.since(UndertowSubsystemSchema.VERSION_14_0)) return;

        KernelServicesBuilder builder = createKernelServicesBuilder(new RuntimeInitialization(this.values, this.schema)).setSubsystemXml(getSubsystemXml());
        KernelServices mainServices = builder.build();

        if (!mainServices.isSuccessfulBoot()) {
            Throwable t = mainServices.getBootError();
            Assert.fail("Boot unsuccessful: " + (t != null ? t.toString() : "no boot error provided"));
        }

        PredicateHandlerWrapper connectionLimiterService = (PredicateHandlerWrapper) this.values.get(UndertowService.FILTER.append("limit-connections")).get();
        HttpHandler connectionLimiterHandler = connectionLimiterService.wrap(Predicates.truePredicate(), new PathHandler());
        Assert.assertNotNull("handler should have been created", connectionLimiterHandler);

        PredicateHandlerWrapper headersService = (PredicateHandlerWrapper) this.values.get(UndertowService.FILTER.append("headers")).get();
        HttpHandler headerHandler = headersService.wrap(Predicates.truePredicate(), new PathHandler());
        Assert.assertNotNull("handler should have been created", headerHandler);

        PredicateHandlerWrapper modClusterService = (PredicateHandlerWrapper) this.values.get(UndertowService.FILTER.append("mod-cluster")).get();
        Assert.assertNotNull(modClusterService);

        HttpHandler modClusterHandler = modClusterService.wrap(Predicates.truePredicate(), new PathHandler());
        Assert.assertNotNull("handler should have been created", modClusterHandler);

        UndertowService undertowService = (UndertowService) this.values.get(UndertowRootDefinition.UNDERTOW_CAPABILITY.getCapabilityServiceName()).get();
        Assert.assertEquals("some-id", undertowService.getInstanceId());
        Assert.assertTrue(undertowService.isStatisticsEnabled());
        Assert.assertEquals("some-server", undertowService.getDefaultServer());
        Assert.assertEquals("myContainer", undertowService.getDefaultContainer());
        Assert.assertEquals("default-virtual-host", undertowService.getDefaultVirtualHost());

        // Don't verify servers until we know they are registered
        Assert.assertEquals(1, undertowService.getServers().size());
        Server server = undertowService.getServers().iterator().next();
        Assert.assertEquals("other-host", server.getDefaultHost());

        Host host = (Host) this.values.get(HostDefinition.HOST_CAPABILITY.getCapabilityServiceName("some-server", "other-host")).get();

        // Don't verify hosts until we know that they are all registered
        Assert.assertEquals(2, server.getHosts().size());
        Assert.assertEquals("some-server", server.getName());

        Assert.assertEquals(3, host.getAllAliases().size());
        Assert.assertTrue(host.getAllAliases().contains("default-alias"));

        LocationService locationService = (LocationService) this.values.get(LocationDefinition.LOCATION_CAPABILITY.getCapabilityServiceName("some-server", "default-virtual-host", "/")).get();
        Assert.assertNotNull(locationService);

        JSPConfig jspConfig = ((ServletContainerService) this.values.get(ServletContainerDefinition.SERVLET_CONTAINER_CAPABILITY.getCapabilityServiceName("myContainer")).get()).getJspConfig();
        Assert.assertNotNull(jspConfig);
        Assert.assertNotNull(jspConfig.createJSPServletInfo());

        UndertowFilter gzipFilterRef = (UndertowFilter) this.values.get(UndertowService.filterRefName("some-server", "other-host", "/", "static-gzip")).get();
        HttpHandler gzipHandler = gzipFilterRef.wrap(new PathHandler());
        Assert.assertNotNull("handler should have been created", gzipHandler);

        Assert.assertEquals(1, host.getFilters().size());

        ModelNode op = Util.createOperation("write-attribute",
                PathAddress.pathAddress(UndertowRootDefinition.PATH_ELEMENT)
                        .append("servlet-container", "myContainer")
                        .append("setting", "websockets")
        );
        op.get("name").set("buffer-pool");
        op.get("value").set("default");

        ModelNode res = ModelTestUtils.checkOutcome(mainServices.executeOperation(op));
        Assert.assertNotNull(res);

        // WFLY-14648 Check expression in enabled attribute is resolved.
        op = Util.createOperation("write-attribute",
                PathAddress.pathAddress(UndertowRootDefinition.PATH_ELEMENT)
                        .append("server", "some-server")
                        .append("http-listener", "default")
        );
        op.get("name").set("enabled");
        op.get("value").set("${env.val:true}");

        res = ModelTestUtils.checkOutcome(mainServices.executeOperation(op));
        Assert.assertNotNull(res);

        Host defaultHost = (Host) this.values.get(UndertowService.DEFAULT_HOST).get();
        Assert.assertNotNull("Default host should exist", defaultHost);

        Server defaultServer = (Server) this.values.get(UndertowService.DEFAULT_SERVER).get();
        Assert.assertNotNull("Default host should exist", defaultServer);

        AccessLogService accessLogService = (AccessLogService) this.values.get(AccessLogDefinition.ACCESS_LOG_CAPABILITY.getCapabilityServiceName("some-server", "default-virtual-host")).get();
        Assert.assertNotNull(accessLogService);
        Assert.assertFalse(accessLogService.isRotate());

        if (this.schema.since(UndertowSubsystemSchema.VERSION_13_0)) {
            PathAddress address = PathAddress.pathAddress(UndertowRootDefinition.PATH_ELEMENT, PathElement.pathElement(Constants.APPLICATION_SECURITY_DOMAIN, "other"), SingleSignOnDefinition.PATH_ELEMENT);
            ModelNode result = mainServices.executeOperation(Util.getWriteAttributeOperation(address, SingleSignOnDefinition.Attribute.PATH.getName(), new ModelNode("/modified-path")));
            assertEquals(ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asString());
            assertTrue("It is expected that reload is required after the operation.", result.get(ModelDescriptionConstants.RESPONSE_HEADERS).get(ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD).asBoolean());
        }
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new DefaultInitialization(this.schema);
    }
}
