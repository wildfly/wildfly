/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejbref.lookup;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Tomas Remes
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EjbRefLookupTestCase {

    private static final String EAR_DEPLOYMENT = "ejb-test-ear";
    private static final String WAR_DEPLOYMENT = "webapp-test";

    @ArquillianResource
    @OperateOnDeployment(WAR_DEPLOYMENT)
    private URL warUrl;

    @Deployment(order = 0, name = EAR_DEPLOYMENT)
    public static Archive<?> deployEar() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT + ".ear");
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar").addClasses(RemoteInterface.class, RemoteInterfaceBean.class);
        ear.addAsModule(ejbJar);
        return ear;
    }

    @Deployment(order = 1, name = WAR_DEPLOYMENT)
    public static Archive<?> deployWar() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addClasses(FirstRestService.class, FirstTestServlet.class, SecondRestService.class, SecondTestServlet.class);
        war.addAsManifestResource(new StringAsset("Dependencies: deployment.ejb-test-ear.ear.ejb.jar\n"), "MANIFEST.MF");
        war.addAsWebInfResource(EjbRefLookupTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    private String doGetReq(String urlPattern) throws Exception {
        return HttpRequest.get(warUrl + urlPattern, 4, TimeUnit.SECONDS);
    }

    @Test
    public void testEjbRefInServlet() throws Exception {
        Assert.assertEquals("1", doGetReq("test1"));
    }

    @Test
    public void testJBossEjbRefInServlet() throws Exception {
        Assert.assertEquals("1", doGetReq("test2"));
    }

    @Test
    public void testEjbRefInRest() throws Exception {
        Assert.assertEquals("1", doGetReq("rest/first/text"));
    }

    @Test
    public void testJBossEjbRefInRest() throws Exception {
        Assert.assertEquals("1", doGetReq("rest/second/text"));
    }
}
