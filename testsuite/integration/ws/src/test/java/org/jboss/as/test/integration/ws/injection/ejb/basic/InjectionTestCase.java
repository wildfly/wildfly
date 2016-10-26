/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import org.jboss.as.test.integration.ws.injection.ejb.basic.webservice.POJOBean;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;

/**
 * Migrated testcase from AS6, injection + handlers are tested with POJO and EJB3 endpoint
 *
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class InjectionTestCase {
    private static final Logger log = Logger.getLogger(InjectionTestCase.class);

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
        ejb3Jar.addAsManifestResource(EJB3Bean.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        ejb3Jar.addAsManifestResource(new StringAsset("Dependencies: deployment.jaxws-injection.ear.jaxws-injection.jar"), "MANIFEST.MF");

        // construct pojo war
        WebArchive pojoWar = ShrinkWrap.create(WebArchive.class, "jaxws-injection-pojo.war");
        pojoWar.addClass(POJOBean.class);
        pojoWar.addClass(TestHandler.class);
        pojoWar.addClass(AbstractEndpointImpl.class);
        pojoWar.addClass(EndpointIface.class);
        pojoWar.addAsResource(POJOBean.class.getPackage(), "jaxws-handler.xml", "org/jboss/as/test/integration/ws/injection/ejb/basic/webservice/jaxws-handler.xml");
        pojoWar.addAsWebInfResource(POJOBean.class.getPackage(), "web.xml", "web.xml");
        pojoWar.addAsManifestResource(new StringAsset("Dependencies: deployment.jaxws-injection.ear.jaxws-injection.jar"), "MANIFEST.MF");

        // construct ear
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "jaxws-injection.ear");
        ear.addAsModule(sharedJar);
        ear.addAsModule(ejb3Jar);
        ear.addAsModule(pojoWar);

        return ear;
    }

    @Test
    public void testPojoEndpoint() throws Exception {

        QName serviceName = new QName("http://jbossws.org/injection", "POJOService");
        URL wsdlURL = new URL(baseUrl, "/jaxws-injection-pojo/POJOService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EndpointIface proxy = service.getPort(EndpointIface.class);
        Assert.assertEquals("Hello World!:Inbound:TestHandler:POJOBean:Outbound:TestHandler", proxy.echo("Hello World!"));
    }

    @Test
    public void testEjb3Endpoint() throws Exception {

        QName serviceName = new QName("http://jbossws.org/injection", "EJB3Service");
        URL wsdlURL = new URL(baseUrl, "/jaxws-injection-ejb3/EJB3Service?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EndpointIface proxy = service.getPort(EndpointIface.class);
        Assert.assertEquals("Hello World!:Inbound:TestHandler:EJB3Bean:Outbound:TestHandler", proxy.echo("Hello World!"));
    }

}
