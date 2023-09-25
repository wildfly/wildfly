/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.anonymouspojos;

import java.net.URL;
import org.jboss.logging.Logger;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * [JBWS-3276] Tests anonymous POJO in web archive that is missing web.xml.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class Usecase2TestCase {

    @ArquillianResource
    URL baseUrl;

    private static final Logger log = Logger.getLogger(Usecase2TestCase.class.getName());

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "anonymous-pojo-usecase2.war");
        war.addPackage(AnonymousPOJO.class.getPackage());
        war.addClass(AnonymousPOJO.class);
        return war;
    }

    @Test
    public void testAnonymousEndpoint() throws Exception {
        final QName serviceName = new QName("org.jboss.as.test.integration.ws.anonymouspojos", "AnonymousPOJOService");
        final URL wsdlURL = new URL(baseUrl, "/anonymous-pojo-usecase2/AnonymousPOJOService?wsdl");
        final Service service = Service.create(wsdlURL, serviceName);
        final POJOIface port = service.getPort(POJOIface.class);
        final String result = port.echo("hello");
        Assert.assertEquals("hello from anonymous POJO", result);
    }

}
