/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.crypto;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.crypto.dsig.XMLSignatureFactory;

import org.junit.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A testcase for verifying that XMLSignatureFactory can be properly created
 *
 * @author alessio.soldano@jboss.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class XMLSignatureFactoryTestCase {

    private static Logger log = Logger.getLogger(XMLSignatureFactoryTestCase.class.getName());
    @ArquillianResource
    URL baseUrl;

    @Deployment
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "crypto.war").addClasses(TestServlet.class);
        return war;
    }

    @Test
    public void signedRequest() throws Exception {
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        Assert.assertNotNull(fac);

        BufferedReader br = new BufferedReader(new InputStreamReader(baseUrl.openStream(), StandardCharsets.UTF_8));
        try {
            Assert.assertEquals("OK", br.readLine());
        } finally {
            br.close();
        }
    }
}
