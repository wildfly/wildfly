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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.runner.RunWith;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

/**
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class PojoEndpointTestCase extends BasicTests {

    @ArquillianResource
    URL baseUrl;

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        WebArchive pojoWar = ShrinkWrap.create(WebArchive.class, "jaxws-basic-pojo.war")
                .addClasses(EndpointIface.class, PojoEndpoint.class, HelloObject.class);
        if (SystemUtils.JAVA_VENDOR.startsWith("IBM")) {
            pojoWar.addAsManifestResource(createPermissionsXmlAsset(
                    // With IBM JDK + SecurityManager, PojoEndpoint#helloError needs accessClassInPackage permission for
                    // SOAPFactory.newInstance() invocation to access internal jaxp packages
                    new RuntimePermission("accessClassInPackage.com.sun.org.apache.xerces.internal.jaxp")), "permissions.xml");
        }
        return pojoWar;
    }

    @Before
    public void endpointLookup() throws Exception {
        QName serviceName = new QName("http://jbossws.org/basic", "POJOService");
        URL wsdlURL = new URL(baseUrl, "/jaxws-basic-pojo/POJOService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        proxy = service.getPort(EndpointIface.class);
    }
}
