/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.deployment.classloading.ear.subdeployments;

import java.net.URL;
import javax.naming.InitialContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.ejb.EJBBusinessInterface;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.ejb.SimpleSLSB;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet.EjbInvokeClassloaderToStringServlet;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet.EjbInvokingServlet;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet.HelloWorldServlet;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet.ServletInOtherWar;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests various scenarios for class access between subdeployments within a .ear.
 *
 * @see https://issues.jboss.org/browse/AS7-306
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SubDeploymentAvailableInClassPathTestCase {

    private static final Logger logger = Logger.getLogger(SubDeploymentAvailableInClassPathTestCase.class);

    private static final String OTHER_WEB_APP_CONTEXT = "other-war";

    @Deployment(name = "ear-with-single-war", testable = false)
    public static EnterpriseArchive createEar() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejbJar.addClasses(EJBBusinessInterface.class, SimpleSLSB.class);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, "war-access-to-ejb.war");
        war.addClasses(HelloWorldServlet.class, EjbInvokingServlet.class);

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "war-access-to-ejb.ear");
        ear.addAsModule(ejbJar);
        ear.addAsModule(war);

        return ear;
    }

    @Deployment(name = "ear-with-exploded-war", testable = false)
    public static EnterpriseArchive createEarWithExplodedWar() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejbJar.addClasses(EJBBusinessInterface.class, SimpleSLSB.class);
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "exploded.ear");
        ear.addAsModule(ejbJar);
        ear.add(new StringAsset("OK!"), "exploded.war/index.jsp");
        ear.add(new ClassAsset(EjbInvokingServlet.class), "exploded.war/WEB-INF/classes/" + EjbInvokingServlet.class.getName().replace('.', '/') + ".class");
        final JavaArchive servletJar = ShrinkWrap.create(JavaArchive.class, "servlet.jar");
        servletJar.addClass(HelloWorldServlet.class);
        ear.add(servletJar, "exploded.war/WEB-INF/lib", ZipExporter.class);
        return ear;
    }

    @Deployment(name = "ear-with-multiple-wars", testable = false)
    public static EnterpriseArchive createEarWithMultipleWars() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejbJar.addClasses(EJBBusinessInterface.class, SimpleSLSB.class);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, "war-access-to-war.war");
        war.addClasses(HelloWorldServlet.class, EjbInvokingServlet.class);

        final WebArchive otherWar = ShrinkWrap.create(WebArchive.class, OTHER_WEB_APP_CONTEXT + ".war");
        otherWar.addClass(ServletInOtherWar.class);

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "war-access-to-war.ear");
        ear.addAsModule(ejbJar);
        ear.addAsModule(war);
        ear.addAsModule(otherWar);

        return ear;
    }

    @Deployment(name = "ear-with-war-and-jar", testable = false)
    public static EnterpriseArchive createEarWithWarJarAndLib() {
        final JavaArchive servletLogicJar = ShrinkWrap.create(JavaArchive.class, "servlet-logic.jar");
        servletLogicJar.addClasses(ClassInJar.class);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, "war-access-to-jar.war");
        war.addClasses(EjbInvokeClassloaderToStringServlet.class);
        war.addAsManifestResource(new StringAsset("Class-Path: servlet-logic.jar"), "MANIFEST.MF");

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "war-access-to-jar.ear");
        ear.addAsModule(servletLogicJar);
        ear.addAsModule(war);

        JavaArchive libraryJar = ShrinkWrap.create(JavaArchive.class, "javax-naming-test-impl.jar");
        libraryJar.addClass(InitialContext.class);
        ear.addAsLibrary(libraryJar);

        return ear;
    }

    /**
     * Tests that for a .ear like this one:
     * myapp.ear
     * |
     * |--- web.war
     * |
     * |--- ejb.jar
     * <p/>
     * the classes within the web.war have access to the classes in the ejb.jar
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("ear-with-single-war")
    public void testEjbClassAvailableInServlet(@ArquillianResource(HelloWorldServlet.class) URL baseUrl) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final String message = "JBossAS7";

            final String requestURL = baseUrl.toURI() + HelloWorldServlet.URL_PATTERN + "?" + HelloWorldServlet.PARAMETER_NAME + "=" + message;
            final HttpGet request = new HttpGet(requestURL);
            final HttpResponse response = httpClient.execute(request);
            final HttpEntity entity = response.getEntity();
            Assert.assertNotNull("Response message from servlet was null", entity);
            final String responseMessage = EntityUtils.toString(entity);
            Assert.assertEquals("Unexpected echo message from servlet", message, responseMessage);
        }
    }

    /**
     * Tests that for a .ear like this one:
     * myapp.ear
     * |
     * |--- web.war/WEB-INF/classes
     * |--- web.war/index.jsp
     * |
     * |--- ejb.jar
     * <p/>
     * <p/>
     * the classes within the web.war have access to the classes in the ejb.jar
     * web.war is a folder, not an archive.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("ear-with-exploded-war")
    public void testExplodedWarInEar(@ArquillianResource(HelloWorldServlet.class) URL baseUrl) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String message = "OK!";
            String requestURL = baseUrl.toURI() + "/index.jsp";
            HttpGet request = new HttpGet(requestURL);
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            Assert.assertNotNull("Response message from servlet was null", entity);
            String responseMessage = EntityUtils.toString(entity);
            Assert.assertEquals("Unexpected echo message from servlet", message, responseMessage);
            message = "JBossAS7";
            requestURL = baseUrl.toURI() + HelloWorldServlet.URL_PATTERN + "?" + HelloWorldServlet.PARAMETER_NAME + "=" + message;
            request = new HttpGet(requestURL);
            response = httpClient.execute(request);
            entity = response.getEntity();
            Assert.assertNotNull("Response message from servlet was null", entity);
            responseMessage = EntityUtils.toString(entity);
            Assert.assertEquals("Unexpected echo message from servlet", message, responseMessage);
        }
    }

    /**
     * Tests that for a .ear like this one:
     * myapp.ear
     * |
     * |--- web.war
     * |
     * |--- ejb.jar
     * <p/>
     * <p/>
     * the classes within the ejb.jar *don't* have access to the classes in the web.war
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("ear-with-single-war")
    public void testServletClassNotAvailableToEjbInEar(@ArquillianResource(EjbInvokingServlet.class) URL baseUrl) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final String classInWar = HelloWorldServlet.class.getName();
            final String requestURL = baseUrl.toURI() + EjbInvokingServlet.URL_PATTERN + "?" + EjbInvokingServlet.CLASS_IN_WAR_PARAMETER + "=" + classInWar;
            final HttpGet request = new HttpGet(requestURL);
            final HttpResponse response = httpClient.execute(request);
            final HttpEntity entity = response.getEntity();
            Assert.assertNotNull("Response message from servlet was null", entity);
            final String responseMessage = EntityUtils.toString(entity);
            Assert.assertEquals("Unexpected echo message from servlet", EjbInvokingServlet.SUCCESS_MESSAGE, responseMessage);
        }
    }


    /**
     * Tests that for a .ear like this one:
     * myapp.ear
     * |
     * |--- lib/javax-naming-test-impl.jar this jar contains override for javax.naming.Binding class
     * |                                   which has to be resolved from implicit modules
     * |
     * |--- war-access-to-jar.war
     * |
     * |--- servlet-logic.jar
     * <p/>
     * the classes within the war-access-to-jar.war have access to the classes in the servlet-logic.jar
     * and servlet-logic.jar has implicit module dependencies first in class path
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("ear-with-war-and-jar")
    public void testWarSeeJarAndJarSeeImplicitModulesFirst(@ArquillianResource(EjbInvokeClassloaderToStringServlet.class) URL baseUrl) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final String requestURL = baseUrl.toURI() + EjbInvokeClassloaderToStringServlet.URL_PATTERN
                    + "?" + EjbInvokeClassloaderToStringServlet.CLASS_NAME_PARAMETER + "=javax.naming.InitialContext";
            final HttpGet request = new HttpGet(requestURL);
            final HttpResponse response = httpClient.execute(request);
            final HttpEntity entity = response.getEntity();
            Assert.assertNotNull("Response message from servlet was null", entity);
            final String responseMessage = EntityUtils.toString(entity);

            // expected is that class is loaded from implicit module
            // app's class loader toString returns: ModuleClassLoader for Module deployment.war-access-to-jar.ear:main from Service Module Loader
            Assert.assertEquals("Unexpected response from servlet", "bootstrap class loader", responseMessage);
        }
    }

    /**
     * Tests that for a .ear like this one:
     * myapp.ear
     * |
     * |--- web-one.war
     * |
     * |--- web-two.war
     * <p/>
     * <p/>
     * the classes within the web-one.war *don't* have access to the classes in the web-two.war
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("ear-with-multiple-wars")
    public void testWarsDontSeeEachOtherInEar(@ContainerResource ManagementClient managementClient) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final String classInOtherWar = HelloWorldServlet.class.getName();
            final String requestURL = managementClient.getWebUri() + "/" + OTHER_WEB_APP_CONTEXT + ServletInOtherWar.URL_PATTERN +
                    "?" + ServletInOtherWar.CLASS_IN_OTHER_WAR_PARAMETER + "=" + classInOtherWar;
            final HttpGet request = new HttpGet(requestURL);
            final HttpResponse response = httpClient.execute(request);
            final HttpEntity entity = response.getEntity();
            Assert.assertNotNull("Response message from servlet was null", entity);
            final String responseMessage = EntityUtils.toString(entity);
            Assert.assertEquals("Unexpected echo message from servlet", ServletInOtherWar.SUCCESS_MESSAGE, responseMessage);
        }
    }

}
