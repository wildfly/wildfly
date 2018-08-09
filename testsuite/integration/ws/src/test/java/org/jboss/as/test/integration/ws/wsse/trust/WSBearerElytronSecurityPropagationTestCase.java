/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.wsse.trust;

import static org.junit.Assert.assertEquals;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.ws.WrapThreadContextClassLoader;
import org.jboss.as.test.integration.ws.wsse.trust.bearer.BearerIface;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

/**
 * Test for WFLY-10480 with Elytron security domain
 *
 */
@RunWith(Arquillian.class)
@ServerSetup(WSTrustTestCaseElytronSecuritySetupTask.class)
public class WSBearerElytronSecurityPropagationTestCase {
    private static final String BEARER_STS_DEP = "jaxws-samples-wsse-policy-trust-sts-bearer";
    private static final String BEARER_SERVER_DEP = "jaxws-samples-wsse-policy-trust-bearer";
    @Rule
    public TestRule watcher = new WrapThreadContextClassLoaderWatcher();
    @ArquillianResource
    private URL serviceURL;


    @Deployment(name = BEARER_STS_DEP, testable = false)
    public static WebArchive createBearerSTSDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, BEARER_STS_DEP + ".war");
        archive
                .setManifest(new StringAsset("Manifest-Version: 1.0\n"
                        + "Dependencies: org.jboss.ws.cxf.jbossws-cxf-client,org.jboss.ws.cxf.sts annotations\n"))
                .addClass(org.jboss.as.test.integration.ws.wsse.trust.stsbearer.STSBearerCallbackHandler.class)
                .addClass(org.jboss.as.test.integration.ws.wsse.trust.stsbearer.SampleSTSBearer.class)
                .addClass(org.jboss.as.test.integration.ws.wsse.trust.shared.WSTrustAppUtils.class)
                .addAsWebInfResource(createFilteredAsset("WEB-INF/wsdl/bearer-ws-trust-1.4-service.wsdl"), "wsdl/bearer-ws-trust-1.4-service.wsdl")
                .addAsWebInfResource(WSTrustTestCase.class.getPackage(), "WEB-INF/stsstore.jks", "classes/stsstore.jks")
                .addAsWebInfResource(WSTrustTestCase.class.getPackage(), "WEB-INF/stsKeystore.properties", "classes/stsKeystore.properties")
                .addAsManifestResource(WSTrustTestCase.class.getPackage(), "META-INF/permissions.xml", "permissions.xml")
                .setWebXML(WSTrustTestCase.class.getPackage(), "WEB-INF/bearer/web.xml");
        return archive;
    }

    @Deployment(name = BEARER_SERVER_DEP, testable = false)
    public static WebArchive createBearerServerDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, BEARER_SERVER_DEP + ".war");
        archive
                .setManifest(new StringAsset("Manifest-Version: 1.0\n"
                        + "Dependencies: org.jboss.ws.cxf.jbossws-cxf-client,org.apache.cxf.impl\n"))
                .addClass(org.jboss.as.test.integration.ws.wsse.trust.SayHello.class)
                .addClass(org.jboss.as.test.integration.ws.wsse.trust.SayHelloResponse.class)
                .addClass(org.jboss.as.test.integration.ws.wsse.trust.bearer.BearerIface.class)
                .addClass(org.jboss.as.test.integration.ws.wsse.trust.bearer.BearerEJBImpl.class)
                .addClass(org.jboss.as.test.integration.ws.wsse.trust.SamlSecurityContextInInterceptor.class)
                .addClass(org.jboss.as.test.integration.ws.wsse.trust.ContextProvider.class)
                .addClass(org.jboss.as.test.integration.ws.wsse.trust.ContextProviderBean.class)
                .addAsWebInfResource(WSTrustTestCase.class.getPackage(), "WEB-INF/jboss-web-elytron.xml", "jboss-web.xml")
                .addAsWebInfResource(WSTrustTestCase.class.getPackage(), "WEB-INF/jboss-ejb3-elytron.xml", "jboss-ejb3.xml")
                .addAsWebInfResource(createFilteredAsset("WEB-INF/wsdl/BearerService.wsdl"), "wsdl/BearerService.wsdl")
                .addAsWebInfResource(createFilteredAsset("WEB-INF/wsdl/BearerService_schema1.xsd"), "wsdl/BearerService_schema1.xsd")
                .addAsWebInfResource(WSTrustTestCase.class.getPackage(), "WEB-INF/servicestore.jks", "classes/servicestore.jks")
                .addAsWebInfResource(WSTrustTestCase.class.getPackage(), "WEB-INF/serviceKeystore.properties", "classes/serviceKeystore.properties");
        return archive;
    }


    @Test
    @RunAsClient
    @OperateOnDeployment(BEARER_SERVER_DEP)
    @WrapThreadContextClassLoader
    public void testBearer() throws Exception {
        Bus bus = BusFactory.newInstance().createBus();
        try {
            BusFactory.setThreadDefaultBus(bus);

            final QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/bearerwssecuritypolicy", "BearerService");
            Service service = Service.create(new URL(serviceURL + "BearerService?wsdl"), serviceName);
            BearerIface proxy = (BearerIface) service.getPort(BearerIface.class);

            WSTrustTestUtils.setupWsseAndSTSClientBearer((BindingProvider) proxy, bus);
            assertEquals("alice&alice", proxy.sayHello());

        } catch (Exception e) {
            throw e;
        } finally {
            bus.shutdown(true);
        }
    }


    private static String replaceNodeAddress(String resourceName) {
        String content = null;
        try {
            content = IOUtils.toString(WSTrustTestCase.class.getResourceAsStream(resourceName), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("Exception during replacing node address in resource", e);
        }
        return content.replaceAll("@node0@", NetworkUtils.formatPossibleIpv6Address(System.getProperty("node0", "127.0.0.1")));
    }

    private static StringAsset createFilteredAsset(String resourceName) {
        return new StringAsset(replaceNodeAddress(resourceName));
    }
    /**
     * @return comma- or space-separated list of absolute paths to client jars
     */
    private String getClientJarPaths() throws IOException {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jaxws-samples-wsse-policy-trust-client.jar");
        jar.addManifest()
                .addAsManifestResource(WSTrustTestCase.class.getPackage(), "META-INF/clientKeystore.properties", "clientKeystore.properties")
                .addAsManifestResource(WSTrustTestCase.class.getPackage(), "META-INF/clientstore.jks", "clientstore.jks");
        File jarFile = new File(TestSuiteEnvironment.getTmpDir(), "jaxws-samples-wsse-policy-trust-client.jar");
        jar.as(ZipExporter.class).exportTo(jarFile, true);
        return jarFile.getAbsolutePath();
    }
    class WrapThreadContextClassLoaderWatcher extends TestWatcher {

        private ClassLoader classLoader = null;

        protected void starting(Description description) {
            try {
                final String cjp = getClientJarPaths();

                if (cjp == null || cjp.trim().isEmpty()) {
                    return;
                }
                if (description.getAnnotation(WrapThreadContextClassLoader.class) != null) {

                    classLoader = Thread.currentThread().getContextClassLoader();

                    StringTokenizer st = new StringTokenizer(cjp, ", ");
                    URL[] archives = new URL[st.countTokens()];

                    for (int i = 0; i < archives.length; i++) {
                        archives[i] = new File(st.nextToken()).toURI().toURL();
                    }

                    URLClassLoader cl = new URLClassLoader(archives, classLoader);
                    Thread.currentThread().setContextClassLoader(cl);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        protected void finished(Description description) {

            if (classLoader != null && description.getAnnotation(WrapThreadContextClassLoader.class) != null) {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
        }
    }
}
