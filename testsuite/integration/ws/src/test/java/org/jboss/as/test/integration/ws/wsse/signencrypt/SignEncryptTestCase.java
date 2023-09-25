/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.signencrypt;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;

import org.junit.Assert;
import org.apache.cxf.ws.security.SecurityConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.wsf.stack.cxf.client.UseNewBusFeature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jboss.as.test.integration.ws.wsse.KeystorePasswordCallback;
import org.jboss.as.test.integration.ws.wsse.POJOEncryptServiceImpl;
import org.jboss.as.test.integration.ws.wsse.ServiceIface;

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
public class SignEncryptTestCase {

    private static Logger log = Logger.getLogger(SignEncryptTestCase.class.getName());
    @ArquillianResource
    URL baseUrl;

    @Deployment
    public static Archive<?> deployment() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxws-wsse-sign-encrypt.war").
                addAsManifestResource(new StringAsset("Dependencies: org.jboss.ws.cxf.jbossws-cxf-client\n"), "MANIFEST.MF").
                addClasses(ServiceIface.class, POJOEncryptServiceImpl.class, KeystorePasswordCallback.class).
                addAsResource(ServiceIface.class.getPackage(), "bob.jks", "bob.jks").
                addAsResource(ServiceIface.class.getPackage(), "bob.properties", "bob.properties").
                addAsWebInfResource(ServiceIface.class.getPackage(), "wsdl/SecurityService-sign-encrypt.wsdl", "wsdl/SecurityService.wsdl").
                addAsWebInfResource(ServiceIface.class.getPackage(), "wsdl/SecurityService_schema1.xsd", "wsdl/SecurityService_schema1.xsd").
                addAsWebInfResource(SignEncryptTestCase.class.getPackage(), "jaxws-endpoint-config.xml", "jaxws-endpoint-config.xml");

        return war;
    }

    @Test
    public void encryptedAndSignedRequest() throws Exception {
        QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy", "EncryptSecurityService");
        URL wsdlURL = new URL(baseUrl.toString() + "EncryptSecurityService?wsdl");

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
