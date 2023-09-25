/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.deployment;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jca.rar.MultipleAdminObject1;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for servlet activations
 *
 * @author <a href="jpederse@redhat.com">Jesper Pedersen</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WarServletDeploymentTestCase {
    static final String deploymentName = "raractivation.ear";
    static final String rarDeploymentName = "eis.rar";
    static final String webDeploymentName = "web.war";

    @Deployment(name = "rar", order = 1)
    public static Archive<?> getRar() {
        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, rarDeploymentName);

        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        ja.addPackage(MultipleAdminObject1.class.getPackage());
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(WarServletDeploymentTestCase.class.getPackage(), "ra.xml", "ra.xml");

        return raa;
    }

    @Deployment(name = "web", order = 2)
    public static Archive<?> getWar() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, webDeploymentName);
        war.addClass(RARServlet.class);
        war.addAsWebInfResource(WarServletDeploymentTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsManifestResource(new StringAsset("Dependencies: deployment." + rarDeploymentName + "\n"), "MANIFEST.MF");

        return war;
    }


    @ArquillianResource
    @OperateOnDeployment("web")
    private URL webUrl;


    /**
     * Test web
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testWeb() throws Exception {
        String res = HttpRequest.get(webUrl.toExternalForm() + "RARServlet", 4, TimeUnit.SECONDS);
        Assert.assertEquals("RARServlet OK", res);
    }


}
