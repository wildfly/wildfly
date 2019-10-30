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
package org.jboss.as.test.integration.ws.wsse.signencrypt;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.junit.Assert;
import org.apache.cxf.ws.security.SecurityConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ws.wsse.EJBEncryptServiceImpl;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jboss.as.test.integration.ws.wsse.KeystorePasswordCallback;
import org.jboss.as.test.integration.ws.wsse.ServiceIface;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.wsf.stack.cxf.client.UseNewBusFeature;

/**
 * Test WS sign + encrypt capability
 * <p>
 * Certificates can ge generated using keytool -genkey -keyalg RSA -storetype JKS
 * Public key can be extracted using keytool -export
 * Public key can be imported using keytool -import
 * Keystore can be listed using keytool -list -v
 *
 * @author Rostislav Svoboda
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EJBSignEncryptTestCase {

    @ArquillianResource
    URL baseUrl;

    @Deployment
    public static Archive<?> deployment() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jaxws-wsse-sign-encrypt.jar").
                addAsManifestResource(new StringAsset("Dependencies: org.jboss.ws.cxf.jbossws-cxf-client\n"), "MANIFEST.MF").
                addClasses(ServiceIface.class, EJBEncryptServiceImpl.class, KeystorePasswordCallback.class).
                addAsResource(ServiceIface.class.getPackage(), "bob.jks", "bob.jks").
                addAsResource(ServiceIface.class.getPackage(), "bob.properties", "bob.properties").
                addAsManifestResource(ServiceIface.class.getPackage(), "wsdl/SecurityService-ejb-sign-encrypt.wsdl", "wsdl/SecurityService.wsdl").
                addAsManifestResource(ServiceIface.class.getPackage(), "wsdl/SecurityService_schema1.xsd", "wsdl/SecurityService_schema1.xsd").
                addAsManifestResource(EJBSignEncryptTestCase.class.getPackage(), "jaxws-endpoint-config.xml", "jaxws-endpoint-config.xml");

        return jar;
    }

    @Test
    public void encryptedAndSignedRequest() throws Exception {
        QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy", "EJBEncryptSecurityService");
        URL wsdlURL = new URL(baseUrl, "/jaxws-wsse-sign-encrypt/EJBEncryptSecurityService?wsdl");

        Service service = Service.create(wsdlURL, serviceName, new UseNewBusFeature()); //use a new bus to avoid any possible clash with other tests
        ServiceIface proxy = (ServiceIface) service.getPort(ServiceIface.class);
        setupWsse(proxy);

        Assert.assertEquals("Secure Hello World!", proxy.sayHello());
    }

    private void setupWsse(ServiceIface proxy) throws MalformedURLException {
        ((BindingProvider) proxy).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ((BindingProvider) proxy).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES, "org/jboss/as/test/integration/ws/wsse/alice.properties");
        ((BindingProvider) proxy).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, "org/jboss/as/test/integration/ws/wsse/alice.properties");
        ((BindingProvider) proxy).getRequestContext().put(SecurityConstants.SIGNATURE_USERNAME, "alice");
        ((BindingProvider) proxy).getRequestContext().put(SecurityConstants.ENCRYPT_USERNAME, "bob");
    }
}
