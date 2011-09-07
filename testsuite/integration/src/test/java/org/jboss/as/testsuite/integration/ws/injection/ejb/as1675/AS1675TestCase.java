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

package org.jboss.as.testsuite.integration.ws.injection.ejb.as1675;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.testsuite.integration.ws.injection.ejb.as1675.shared.BeanIface;
import org.jboss.as.testsuite.integration.ws.injection.ejb.as1675.shared.BeanImpl;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;

/**
 * [AS7-1675] Problem with @Resource lookups on EJBs
 * 
 * https://issues.jboss.org/browse/AS7-1675
 * 
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AS1675TestCase {

    @Deployment
    public static Archive<?> deployment() {
        // construct shared jar
        JavaArchive sharedJar = ShrinkWrap.create(JavaArchive.class, "shared.jar");
        sharedJar.addClass(BeanIface.class);
        sharedJar.addClass(BeanImpl.class);
        sharedJar.addClass(EndpointIface.class);
        sharedJar.addClass(AbstractEndpointImpl.class);
        System.out.println(sharedJar.toString(true));
        // construct ejb3 jar
        JavaArchive ejb3Jar = ShrinkWrap.create(JavaArchive.class, "ejb3.jar");
        ejb3Jar.addClass(EJB3Bean.class);
        ejb3Jar.addClass(AS1675TestCase.class);
        ejb3Jar.addAsResource("as1675.jar/META-INF/ejb-jar.xml", "META-INF/ejb-jar.xml");
        System.out.println(ejb3Jar.toString(true));
        // construct ear
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "as1675.ear");
        ear.addAsModule(sharedJar);
        ear.addAsModule(ejb3Jar);
        System.out.println(ear.toString(true));
        // return ear
        return ear;
    }

    @Test
    public void testEjb3EndpointInjection() throws Exception {
        QName serviceName = new QName("http://jbossws.org/as1675", "EJB3Service");
        URL wsdlURL = new URL("http://localhost:8080/as1675/EJB3Service?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EndpointIface proxy = (EndpointIface) service.getPort(EndpointIface.class);
        Assert.assertEquals("Hello World!:EJB3Bean", proxy.echo("Hello World!"));
    }

}
