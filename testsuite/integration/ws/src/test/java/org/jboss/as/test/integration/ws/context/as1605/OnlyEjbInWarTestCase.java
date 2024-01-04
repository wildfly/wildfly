/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.context.as1605;

import java.net.URL;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * [AS7-1605] jboss-web.xml ignored for web service root
 * <p>
 * This test case tests if only EJB3 endpoints are in war archive.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OnlyEjbInWarTestCase {

    @ArquillianResource
    URL baseUrl;

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "as1605-usecase2.war");
        war.addClass(EndpointIface.class);
        war.addClass(EJB3Endpoint.class);
        war.addAsWebInfResource(new StringAsset(
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                        "<!DOCTYPE jboss-web PUBLIC \"-//JBoss//DTD Web Application 2.4//EN\" \"http://www.jboss.org/j2ee/dtd/jboss-web_4_0.dtd\">\n" +
                        "<jboss-web>\n" +
                        "<context-root>/as1605-customized</context-root>\n" +
                        "</jboss-web>\n"), "jboss-web.xml");
        return war;
    }

    @Test
    public void testEJB3Endpoint() throws Exception {
        final QName serviceName = new QName("org.jboss.as.test.integration.ws.context.as1605", "EJB3EndpointService");
        final URL wsdlURL = new URL(baseUrl, "/as1605-customized/EJB3Endpoint?wsdl");
        final Service service = Service.create(wsdlURL, serviceName);
        final EndpointIface port = service.getPort(EndpointIface.class);
        final String result = port.echo("hello");
        Assert.assertEquals("EJB3 hello", result);
    }

}
