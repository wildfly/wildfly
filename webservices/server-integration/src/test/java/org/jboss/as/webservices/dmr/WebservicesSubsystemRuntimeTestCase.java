/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.dmr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.server.Services;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.config.ClientConfig;
import org.jboss.wsf.spi.metadata.config.EndpointConfig;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

/**
 * A test for checking the services that are created by the subsystem after boot.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com>Alessio Soldano</a>
 */
public class WebservicesSubsystemRuntimeTestCase extends AbstractSubsystemBaseTest {

    public WebservicesSubsystemRuntimeTestCase() {
        super(WSExtension.SUBSYSTEM_NAME, new WSExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("ws-subsystem20-rt.xml");
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {
            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.NORMAL;
            }

            @Override
            protected void addExtraServices(ServiceTarget target) {
                super.addExtraServices(target);
                target.addService(Services.JBOSS_SERVICE_MODULE_LOADER, new ServiceModuleLoader(null)).install();
            }
        };
    }

    @AfterClass
    public static void resetClassLoaderProvider() {
        //HACK: another hack required because we're not running in a valid modular environment, hence at the of the test
        //the WS ClassLoaderProvider is still configured to use a ModularClassLoaderProvider backed by the fake
        //module loader installed by this testcase. As a consequence other tests running after this might fail.
        //The issue can be prevented in various ways, but given this whole test is a hack as we're not running in a
        //proper environment, I prefer to do as below for now (after all, this is a testsuite issue only, WFLY-4122)
        ClassLoaderProvider.setDefaultProvider(new ClassLoaderProvider() {

            @Override
            public ClassLoader getWebServiceSubsystemClassLoader() {
                return Thread.currentThread().getContextClassLoader();
            }

            @Override
            public ClassLoader getServerJAXRPCIntegrationClassLoader() {
                return Thread.currentThread().getContextClassLoader();
            }

            @Override
            public ClassLoader getServerIntegrationClassLoader() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    @Test
    public void testSubsystem() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(getSubsystemXml());
        KernelServices mainServices = builder.build();
        if (!mainServices.isSuccessfulBoot()) {
            Assert.fail(mainServices.getBootError().toString());
        }

        //WSDL soap:address rewrite engine options test
        ServerConfig serverConfig = getMSCService(mainServices.getContainer(), WSServices.CONFIG_SERVICE, ServerConfig.class);
        Assert.assertTrue(serverConfig.isModifySOAPAddress());
        Assert.assertEquals("localhost", serverConfig.getWebServiceHost());
        Assert.assertEquals(9895, serverConfig.getWebServicePort());
        Assert.assertEquals(9944, serverConfig.getWebServiceSecurePort());
        Assert.assertEquals("https", serverConfig.getWebServiceUriScheme());

        //Client & Endpoint predefined configuration tests
        //HACK: we need to manually reload the client/endpoint configs as the ServerConfigService is actually not starting in this test;
        //the reason is that it requires services which are not installed here and even if those were available it would fail to start
        //because we're not running in a modular environment and hence it won't be able to detect the proper ws stack implementation.
        //Even if we made the subsystem work with a flat classloader (which would not make sense except for the tests here), we'd have
        //to deal a hell of ws specific maven dependencies here... so really not worth the effort.
        serverConfig.reloadClientConfigs();
        ClientConfig clCfg = serverConfig.getClientConfig("My-Client-Config");
        Assert.assertNotNull(clCfg);
        Assert.assertEquals(1, clCfg.getProperties().size());
        Assert.assertEquals("bar3", clCfg.getProperties().get("foo3"));
        Assert.assertEquals(2, clCfg.getPreHandlerChains().size());
        Map<String, UnifiedHandlerChainMetaData> map = new HashMap<String, UnifiedHandlerChainMetaData>();
        for (UnifiedHandlerChainMetaData uhc : clCfg.getPreHandlerChains()) {
            map.put(uhc.getId(), uhc);
        }
        Assert.assertTrue(map.get("my-handlers").getHandlers().isEmpty());
        Assert.assertEquals("##SOAP11_HTTP ##SOAP11_HTTP_MTOM ##SOAP12_HTTP ##SOAP12_HTTP_MTOM", map.get("my-handlers").getProtocolBindings());
        Assert.assertEquals(1, map.get("my-handlers2").getHandlers().size());
        Assert.assertEquals("MyHandler", map.get("my-handlers2").getHandlers().get(0).getHandlerName());
        Assert.assertEquals("org.jboss.ws.common.invocation.MyHandler", map.get("my-handlers2").getHandlers().get(0).getHandlerClass());
        Assert.assertEquals("##SOAP11_HTTP ##SOAP11_HTTP_MTOM ##SOAP12_HTTP ##SOAP12_HTTP_MTOM", map.get("my-handlers").getProtocolBindings());
        Assert.assertEquals(1, clCfg.getPostHandlerChains().size());
        Assert.assertEquals("my-handlers2", clCfg.getPostHandlerChains().get(0).getId());
        Assert.assertEquals(1, clCfg.getPostHandlerChains().get(0).getHandlers().size());
        Assert.assertEquals("MyHandler2", clCfg.getPostHandlerChains().get(0).getHandlers().get(0).getHandlerName());
        Assert.assertEquals("org.jboss.ws.common.invocation.MyHandler2", clCfg.getPostHandlerChains().get(0).getHandlers().get(0).getHandlerClass());
        Assert.assertEquals("##SOAP11_HTTP ##SOAP11_HTTP_MTOM ##SOAP12_HTTP ##SOAP12_HTTP_MTOM", clCfg.getPostHandlerChains().get(0).getProtocolBindings());

        serverConfig.reloadEndpointConfigs();
        EndpointConfig epCfg = serverConfig.getEndpointConfig("Standard-Endpoint-Config");
        Assert.assertNotNull(epCfg);
        Assert.assertTrue(epCfg.getProperties().isEmpty());
        Assert.assertTrue(epCfg.getPreHandlerChains().isEmpty());
        Assert.assertTrue(epCfg.getPostHandlerChains().isEmpty());
        epCfg = serverConfig.getEndpointConfig("Recording-Endpoint-Config");
        Assert.assertNotNull(epCfg);
        Assert.assertEquals(2, epCfg.getProperties().size());
        Assert.assertEquals("bar", epCfg.getProperties().get("foo"));
        Assert.assertEquals("bar2", epCfg.getProperties().get("foo2"));
        Assert.assertEquals(1, epCfg.getPreHandlerChains().size());
        Assert.assertEquals("recording-handlers", epCfg.getPreHandlerChains().get(0).getId());
        Assert.assertEquals(2, epCfg.getPreHandlerChains().get(0).getHandlers().size());
        Assert.assertEquals("RecordingHandler", epCfg.getPreHandlerChains().get(0).getHandlers().get(0).getHandlerName());
        Assert.assertEquals("org.jboss.ws.common.invocation.RecordingServerHandler", epCfg.getPreHandlerChains().get(0).getHandlers().get(0).getHandlerClass());
        Assert.assertEquals("AnotherRecordingHandler", epCfg.getPreHandlerChains().get(0).getHandlers().get(1).getHandlerName());
        Assert.assertEquals("org.jboss.ws.common.invocation.RecordingServerHandler", epCfg.getPreHandlerChains().get(0).getHandlers().get(1).getHandlerClass());
        Assert.assertEquals("##SOAP11_HTTP ##SOAP11_HTTP_MTOM ##SOAP12_HTTP ##SOAP12_HTTP_MTOM", epCfg.getPreHandlerChains().get(0).getProtocolBindings());
        Assert.assertEquals(1, epCfg.getPostHandlerChains().size());
        Assert.assertEquals("recording-handlers2", epCfg.getPostHandlerChains().get(0).getId());
        Assert.assertEquals(2, epCfg.getPostHandlerChains().get(0).getHandlers().size());
        Assert.assertEquals("RecordingHandler2", epCfg.getPostHandlerChains().get(0).getHandlers().get(0).getHandlerName());
        Assert.assertEquals("org.jboss.ws.common.invocation.RecordingServerHandler", epCfg.getPostHandlerChains().get(0).getHandlers().get(0).getHandlerClass());
        Assert.assertEquals("AnotherRecordingHandler2", epCfg.getPostHandlerChains().get(0).getHandlers().get(1).getHandlerName());
        Assert.assertEquals("org.jboss.ws.common.invocation.RecordingServerHandler", epCfg.getPostHandlerChains().get(0).getHandlers().get(1).getHandlerClass());
        Assert.assertEquals("##SOAP11_HTTP ##SOAP11_HTTP_MTOM ##SOAP12_HTTP ##SOAP12_HTTP_MTOM", epCfg.getPostHandlerChains().get(0).getProtocolBindings());
    }

    @SuppressWarnings("unchecked")
    public static <T> T getMSCService(final ServiceContainer container, final ServiceName serviceName, final Class<T> clazz) {
        ServiceController<T> service = (ServiceController<T>) container.getService(serviceName);
        return service != null ? service.getValue() : null;
    }
}
