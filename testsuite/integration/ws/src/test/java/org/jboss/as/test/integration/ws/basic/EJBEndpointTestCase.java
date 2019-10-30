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
package org.jboss.as.test.integration.ws.basic;

import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.runner.RunWith;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EJBEndpointTestCase extends BasicTests {

    @ArquillianResource
    URL baseUrl;

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jaxws-basic-ejb.jar")
                .addClasses(EndpointIface.class, EJBEndpoint.class, HelloObject.class);
        // EJBEndpoint#helloError needs getClassLoader permission for SOAPFactory.newInstance() invocation which is not supposed(??? at least it seems so) to be called from deployments
        jar.addAsManifestResource(createPermissionsXmlAsset(new RuntimePermission("getClassLoader")), "permissions.xml");
        return jar;
    }

    @Before
    public void endpointLookup() throws Exception {
        QName serviceName = new QName("http://jbossws.org/basic", "EJB3Service");
        URL wsdlURL = new URL(baseUrl, "/jaxws-basic-ejb3/EJB3Service?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        proxy = service.getPort(EndpointIface.class);
    }

}
