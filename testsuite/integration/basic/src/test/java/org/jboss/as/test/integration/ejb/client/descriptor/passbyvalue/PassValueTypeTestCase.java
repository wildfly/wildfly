/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.client.descriptor.passbyvalue;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author baranowb
 */
@RunWith(Arquillian.class)
@RunAsClient
public class PassValueTypeTestCase {

    public static final String WAR_FULL_DEPLOYMENT = "TestWAR";
    public static final String WAR_STRIPPED_DEPLOYMENT = "TestWAR-stripped";

    @ArquillianResource
    @OperateOnDeployment(WAR_FULL_DEPLOYMENT)
    private URL warUrl;

    @ArquillianResource
    @OperateOnDeployment(WAR_STRIPPED_DEPLOYMENT)
    private URL warStrippedUrl;

    @Deployment(name = WAR_FULL_DEPLOYMENT)
    public static Archive<?> deployWar() {
        return createWAR(true, WAR_FULL_DEPLOYMENT);
    }

    @Deployment(name = WAR_STRIPPED_DEPLOYMENT)
    public static Archive<?> deployStrippedWar() {
        return createWAR(false, WAR_STRIPPED_DEPLOYMENT);
    }

    public static Archive<?> createWAR(final boolean includeJbossEJBClientXML, final String name) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, name + ".war");
        war.addClasses(ClientPassByValueTestServlet.class, TestEJB.class, TestEJBRemote.class, DummySerializableObject.class);
        if (includeJbossEJBClientXML) {
            war.addAsManifestResource(PassValueTypeTestCase.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        }
        war.addAsWebInfResource(PassValueTypeTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    @Test
    public void testFullWar() throws Exception {
        test(warUrl);
    }

    @Test
    public void testStrippedWar() throws Exception {
        test(warStrippedUrl);
    }

    private void test(final URL url) throws IOException, ExecutionException, TimeoutException {
        final String result2 = HttpRequest.get(url + "test?flip=false", 4, TimeUnit.SECONDS);
        final String result = HttpRequest.get(url + "test?flip=true", 4, TimeUnit.SECONDS);
        Assert.assertNotEquals(result, result2);
    }
}
