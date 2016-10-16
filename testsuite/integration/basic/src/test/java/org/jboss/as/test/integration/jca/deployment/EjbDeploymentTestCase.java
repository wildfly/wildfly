/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
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
public class EjbDeploymentTestCase {

    static final String deploymentName = "raractivation.ear";
    static final String rarDeploymentName = "eis.rar";
    static final String insideRarDeploymentName = "inside-eis.rar";
    static final String webDeploymentName = "web.war";
    static final String ejbDeploymentName = "ejb.jar";

    @Deployment(name = "rar", order = 1)
    public static Archive<?> deploytRar() {
        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, rarDeploymentName);

        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        ja.addPackage(MultipleAdminObject1.class.getPackage());
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(EjbDeploymentTestCase.class.getPackage(), "ra.xml", "ra.xml");

        return raa;
    }

    public static Archive<?> getRar() {
        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, insideRarDeploymentName);

        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        ja.addPackage(MultipleAdminObject1.class.getPackage());
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(EjbDeploymentTestCase.class.getPackage(), "ra.xml", "ra.xml");

        return raa;
    }

    public static Archive<?> getEjb() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ejbDeploymentName);
        //jar.addClass(ITestStatelessEjb.class);
        jar.addClass(TestStatelessEjb.class);
        jar.addClass(TestStatelessEjbAO.class);

        //jar.addAsManifestResource(new StringAsset("Dependencies: deployment." + rarDeploymentName + "\n"), "MANIFEST.MF");

        return jar;
    }

    public static Archive<?> getWar() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, webDeploymentName);
        war.addClass(EjbTestServlet.class);
        war.addAsWebInfResource(EjbDeploymentTestCase.class.getPackage(), "ejb-web.xml", "web.xml");
        //war.addAsManifestResource(new StringAsset("Dependencies: deployment." + rarDeploymentName + "\n"), "MANIFEST.MF");

        return war;
    }

    @Deployment(name = "ear", order = 2)
    public static Archive<?> deployEar() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, deploymentName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        ja.addClass(ITestStatelessEjb.class);
        ja.addClass(ITestStatelessEjbAO.class);
        ear.addAsLibraries(ja);
        ear.addAsModule(getRar());
        ear.addAsModule(getWar());
        ear.addAsModule(getEjb());
        //ear.addAsManifestResource(EjbDeploymentTestCase.class.getPackage(), "application.xml", "application.xml");
        ear.addAsManifestResource(new StringAsset("Dependencies: deployment." + rarDeploymentName + "\n"), "MANIFEST.MF");

        return ear;
    }

    @ArquillianResource
    @OperateOnDeployment("ear")
    private URL earUrl;


    /**
     * Test EAR
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testEAR() throws Exception {
        String res = HttpRequest.get(earUrl.toExternalForm() + "EjbTestServlet", 4, TimeUnit.SECONDS);
        Assert.assertEquals("EjbTestServlet OK", res);
    }
}
