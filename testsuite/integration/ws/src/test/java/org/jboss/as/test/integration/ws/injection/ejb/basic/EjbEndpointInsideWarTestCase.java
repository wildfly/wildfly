/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.injection.ejb.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ws.injection.ejb.basic.shared.BeanIface;
import org.jboss.as.test.integration.ws.injection.ejb.basic.shared.BeanImpl;
import org.jboss.as.test.integration.ws.injection.ejb.basic.shared.handlers.TestHandler;
import org.jboss.as.test.integration.ws.injection.ejb.basic.webservice.AbstractEndpointImpl;
import org.jboss.as.test.integration.ws.injection.ejb.basic.webservice.EJB3Bean;
import org.jboss.as.test.integration.ws.injection.ejb.basic.webservice.EndpointIface;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;
import java.net.URL;

/**
 * [AS7-3411] Test injection + handlers with EJB3 endpoint packaged inside a WAR.
 *
 * @author Robert Reimann
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EjbEndpointInsideWarTestCase {
    private static final Logger log = Logger.getLogger(EjbEndpointInsideWarTestCase.class);

    @ArquillianResource
    URL baseUrl;

    @Deployment
    public static Archive<?> deployment() {
        // construct shared jar
        JavaArchive sharedJar = ShrinkWrap.create(JavaArchive.class, "jaxws-injection.jar");
        sharedJar.addClass(BeanIface.class);
        sharedJar.addClass(BeanImpl.class);

        // construct ejb3 jar
        JavaArchive ejb3Jar = ShrinkWrap.create(JavaArchive.class, "jaxws-injection-ejb3.jar");
        ejb3Jar.addClass(EJB3Bean.class);
        ejb3Jar.addClass(TestHandler.class);
        ejb3Jar.addClass(AbstractEndpointImpl.class);
        ejb3Jar.addClass(EndpointIface.class);
        ejb3Jar.addAsResource(EJB3Bean.class.getPackage(), "jaxws-handler.xml", "org/jboss/as/test/integration/ws/injection/ejb/basic/webservice/jaxws-handler.xml");

        // construct war containing the ejb3 jar
        WebArchive ejb3War = ShrinkWrap.create(WebArchive.class, "jaxws-injection-ejb3-inside.war");
        ejb3War.addAsLibraries(sharedJar);
        ejb3War.addAsLibraries(ejb3Jar);
        ejb3War.addAsWebInfResource(EJB3Bean.class.getPackage(), "ejb-web.xml", "web.xml");
        return ejb3War;
    }

    @Test
    public void testEjb3InsideWarEndpoint() throws Exception {
        QName serviceName = new QName("http://jbossws.org/injection", "EJB3Service");
        URL wsdlURL = new URL(baseUrl, "/jaxws-injection-ejb3/EJB3Service?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EndpointIface proxy = service.getPort(EndpointIface.class);
        Assert.assertEquals("Hello World!:Inbound:TestHandler:EJB3Bean:Outbound:TestHandler", proxy.echo("Hello World!"));
    }

}
