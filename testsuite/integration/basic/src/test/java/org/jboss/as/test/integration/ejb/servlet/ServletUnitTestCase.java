/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.servlet;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;

/**
 * A servlet that accesses an EJB and tests whether the call argument is serialized.
 * Part of migration AS5 testsuite to AS7 [JIRA JBQA-5275].
 *
 * @author William DeCoste, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ServletUnitTestCase {
    private static final Logger log = Logger.getLogger(ServletUnitTestCase.class.getName());

    @Deployment(name = "ejb", order = 2)
    public static Archive<?> deployEjbs() {
        JavaArchive jar = getEjbs("ejb3-servlet-ejbs.jar");
        jar.addAsManifestResource(new StringAsset("Dependencies: deployment.ejb3-servlet-client.jar \n"), "MANIFEST.MF");
        return jar;
    }

    @Deployment(name = "client", order = 1)
    public static Archive<?> deployClient() {
        JavaArchive jar = getClient("ejb3-servlet-client.jar");
        return jar;
    }

    @ArquillianResource
    @OperateOnDeployment("servlet")
    private URL ejbServletUrl;

    @Deployment(name = "servlet", order = 3)
    public static Archive<?> deployServlet() {
        WebArchive war = getServlet("ejb3-servlet.war");
        war.addClass(EJBServlet.class);
        war.addClass(Util.class);
        war.addAsWebInfResource(ServletUnitTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.addAsWebInfResource(ServletUnitTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsManifestResource(new StringAsset("Dependencies: deployment.ejb3-servlet-ejbs.jar \n"), "MANIFEST.MF");
        war.addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("getSecurityDomain")), "permissions.xml");
        return war;
    }

    @ArquillianResource
    @OperateOnDeployment("ear")
    private URL servletUrl;

    @Deployment(name = "ear", order = 4)
    public static Archive<?> deployEar() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ejb3-ear-servlet.ear");

        ear.addAsModule(getClient("ejb3-ear-servlet-client.jar"));
        ear.addAsModule(getEjbs("ejb3-ear-servlet-ejbs.jar"));

        WebArchive war = getServlet("ejb3-ear-servlet.war");
        war.addAsWebInfResource(ServletUnitTestCase.class.getPackage(), "jboss-web-ear.xml", "jboss-web.xml");
        war.addAsWebInfResource(ServletUnitTestCase.class.getPackage(), "web-ear.xml", "web.xml");
        war.addClass(EJBServletEar.class);
        war.addClass(Util.class);
        ear.addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("getSecurityDomain")), "permissions.xml");
        ear.addAsModule(war);

        ear.addAsManifestResource(ServletUnitTestCase.class.getPackage(), "application.xml", "application.xml");
        return ear;
    }

    private static JavaArchive getEjbs(String archiveName) {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, archiveName);
        jar.addClasses(
                Session30.class,
                Session30Bean.class,
                Session30BusinessLocal.class,
                Session30BusinessRemote.class,
                Session30Home.class,
                Session30Local.class,
                Session30LocalHome.class,
                Session30Remote.class,
                StatefulBean.class,
                StatefulLocal.class,
                StatefulRemote.class,
                StatelessBean.class,
                StatelessLocal.class,
                TestObject.class);
        jar.addAsManifestResource(ServletUnitTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

    private static JavaArchive getClient(String archiveName) {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, archiveName);
        jar.addClasses(WarTestObject.class);
        return jar;
    }

    private static WebArchive getServlet(String archiveName) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, archiveName);
        war.addClasses(EJBServletHelper.class);
        return war;
    }

    @Test
    public void testEJBServletEar() throws Exception {
        String res = HttpRequest.get( servletUrl.toExternalForm() + "EJBServlet", 4, TimeUnit.SECONDS);
        Assert.assertEquals("EJBServlet OK", res);
    }

    @Test
    public void testEJBServlet() throws Exception {
        String res = HttpRequest.get( ejbServletUrl.toExternalForm() + "EJBServlet", 4, TimeUnit.SECONDS);
        Assert.assertEquals("EJBServlet OK", res);
    }
}
