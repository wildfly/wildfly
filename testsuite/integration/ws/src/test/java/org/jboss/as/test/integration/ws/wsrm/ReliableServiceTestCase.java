/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsrm;

import java.net.URL;
import javax.xml.namespace.QName;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;

import org.junit.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ws.wsrm.generated.ReliableService;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.wsf.stack.cxf.client.UseNewBusFeature;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ReliableServiceTestCase {

    private static final Logger log = Logger.getLogger(ReliableServiceTestCase.class);
    @ArquillianResource
    URL baseUrl;

    @Deployment(testable = false)
    public static Archive mainDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ws-reliable-messaging-example.war").
                addPackage(ReliableService.class.getPackage()).
                addClasses(ReliableServiceImpl.class, ReliableCheckHandler.class).
                addAsWebInfResource(ReliableServiceTestCase.class.getPackage(), "ReliableService.wsdl", "wsdl/ReliableService.wsdl").
                addAsResource(ReliableCheckHandler.class.getPackage(), "ws-handler.xml", "org/jboss/as/test/integration/ws/wsrm/ws-handler.xml");
        return war;
    }

    @Test
    public void runTests() throws Exception {
        consumeHelloService();
        consumeOneWayService();
    }

    private void consumeOneWayService() throws Exception {
        QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wsrm", "ReliableService");
        URL wsdlURL = new URL(baseUrl, "ReliableService?wsdl");
        Service service = Service.create(wsdlURL, serviceName, new UseNewBusFeature());
        ReliableService proxy = (ReliableService) service.getPort(ReliableService.class);

        BindingProvider bp = (BindingProvider) proxy;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, new URL(baseUrl, "ReliableService").toString());

        proxy.writeLogMessage();
    }

    private void consumeHelloService() throws Exception {
        QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wsrm", "ReliableService");
        URL wsdlURL = new URL(baseUrl, "ReliableService?wsdl");
        Service service = Service.create(wsdlURL, serviceName, new UseNewBusFeature());
        ReliableService proxy = (ReliableService) service.getPort(ReliableService.class);

        BindingProvider bp = (BindingProvider) proxy;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, new URL(baseUrl, "ReliableService").toString());

        Assert.assertEquals("Hello Rosta!", proxy.sayHello("Rosta"));
    }
}
