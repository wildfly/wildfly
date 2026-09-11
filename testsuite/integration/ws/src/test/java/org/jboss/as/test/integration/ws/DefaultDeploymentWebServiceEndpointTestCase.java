/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws;

import jakarta.xml.ws.Service;
import java.net.URL;
import javax.xml.namespace.QName;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test webservice endpoint packaged in ROOT.war
 *
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
@RunWith(Arquillian.class)
public class DefaultDeploymentWebServiceEndpointTestCase {
    @ArquillianResource
    URL baseUrl;

    private static final Logger log = Logger.getLogger(DefaultDeploymentWebServiceEndpointTestCase.class.getName());

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ROOT.war");
        war.addPackage(SimpleWebserviceEndpointImpl.class.getPackage());
        war.addClass(SimpleWebserviceEndpointImpl.class);
        war.addAsWebInfResource(DefaultDeploymentWebServiceEndpointTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    @Test
    @RunAsClient
    public void testDefaultDeployment() throws Exception {
        final QName serviceName = new QName("org.jboss.as.test.integration.ws", "SimpleService");
        final URL wsdlURL = new URL(baseUrl, "/SimpleService?wsdl");
        Assert.assertEquals("wsdlURL is expected under the root path", "/SimpleService", wsdlURL.getPath());
        final Service service = Service.create(wsdlURL, serviceName);
        final SimpleWebserviceEndpointIface port = service.getPort(SimpleWebserviceEndpointIface.class);
        final String result = port.echo("hello");
        Assert.assertEquals("hello", result);
    }
}
