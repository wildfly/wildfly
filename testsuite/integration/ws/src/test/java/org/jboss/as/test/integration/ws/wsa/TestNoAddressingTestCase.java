/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsa;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

import javax.xml.namespace.QName;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;

import org.junit.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class TestNoAddressingTestCase {

    @ArquillianResource
    URL baseUrl;
    private static final String message = "no-addressing";
    private static final String expectedResponse = "Hello no-addressing!";

    @Deployment
    public static Archive<?> deployment() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxws-wsa.war").
                addClasses(ServiceIface.class, ServiceImplNoAddressing.class, WSHandler.class).
                addAsResource(WSHandler.class.getPackage(), "ws-handler.xml", "org/jboss/as/test/integration/ws/wsa/ws-handler.xml");
        return war;
    }

    @Test
    public void usingLocalWSDLWithoutAddressing() throws Exception {

        ServiceIface proxy = getServicePortFromWSDL("NoAddressingService.xml");

        Assert.assertEquals(expectedResponse, proxy.sayHello(message));
    }

    @Test
    public void usingLocalWSDLWithOptionalAddressing() throws Exception {

        ServiceIface proxy = getServicePortFromWSDL("OptionalAddressingService.xml");

        Assert.assertEquals(expectedResponse, proxy.sayHello(message));
    }

    @Test
    public void usingLocalWSDLWithAddressing() throws Exception {

        ServiceIface proxy = getServicePortFromWSDL("RequiredAddressingService.xml");

        try {
            proxy.sayHello(message);
            Assert.fail("Message Addressing is defined in local wsdl but shouldn't be used on deployed endpoint");
        } catch (WebServiceException e) {
            // Expected, Message Addressing Property is not present
        }
    }

    @Test
    public void usingWSDLFromDeployedEndpoint() throws Exception {

        QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wsaddressing", "AddressingService");
        URL wsdlURL = new URL(baseUrl, "/jaxws-wsa/AddressingService?wsdl");
        File wsdlFile = new File(this.getClass().getSimpleName() + ".wsdl");
        downloadWSDLToFile(wsdlURL, wsdlFile);

        Service service = Service.create(wsdlFile.toURI().toURL(), serviceName);
        ServiceIface proxy = (ServiceIface) service.getPort(ServiceIface.class);

        Assert.assertEquals(expectedResponse, proxy.sayHello(message));
        wsdlFile.delete();
    }

    private ServiceIface getServicePortFromWSDL(String wsdlFileName) throws MalformedURLException {
        QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wsaddressing", "AddressingService");
        File wsdlFile = new File(System.getProperty("jbossas.ts.submodule.dir") + "/src/test/java/org/jboss/as/test/integration/ws/wsa/" + wsdlFileName);
        URL wsdlURL = wsdlFile.toURI().toURL();

        Service service = Service.create(wsdlURL, serviceName);
        ServiceIface proxy = (ServiceIface) service.getPort(ServiceIface.class);

        BindingProvider bp = (BindingProvider) proxy;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, new URL(baseUrl, "/jaxws-wsa/AddressingService").toString());

        return proxy;
    }

    protected static void downloadWSDLToFile(URL wsdlURL, File wsdlFile) throws IOException {
        Files.copy(wsdlURL.openStream(), wsdlFile.toPath());
    }
}
