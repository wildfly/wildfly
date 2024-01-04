/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.basic;

import java.net.URL;
import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.runner.RunWith;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

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
        if (SystemUtils.JAVA_VENDOR.startsWith("IBM")) {
            jar.addAsManifestResource(createPermissionsXmlAsset(
                    // With IBM JDK + SecurityManager, EJBEndpoint#helloError needs accessClassInPackage permission for
                    // SOAPFactory.newInstance() invocation to access internal jaxp packages
                    new RuntimePermission("accessClassInPackage.com.sun.org.apache.xerces.internal.jaxp")), "permissions.xml");
        }
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
