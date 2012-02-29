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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.ejb.EJBBusinessInterface;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.ejb.SimpleSLSB;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet.EjbInvokingServlet;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet.HelloWorldServlet;
import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet.ServletInOtherWar;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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
 *      User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SubDeploymentAvailableInClassPathTestCase {

    private static final Logger logger = Logger.getLogger(SubDeploymentAvailableInClassPathTestCase.class);

    private static final String WEB_APP_CONTEXT_ONE = "war-access-to-ejb";

    private static final String WEB_APP_CONTEXT_TWO = "war-access-to-war";

    private static final String OTHER_WEB_APP_CONTEXT = "other-war";

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment(name = "ear-with-single-war", testable = false)
    public static EnterpriseArchive createEar() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejbJar.addClasses(EJBBusinessInterface.class, SimpleSLSB.class);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_APP_CONTEXT_ONE + ".war");
        war.addClasses(HelloWorldServlet.class, EjbInvokingServlet.class);

        // TODO: Currently, due to an issue in AS7 integration with Arquillian, the web-app context and the
        // .ear name should be the same or else you run into test deployment failures like:
        // Caused by: java.lang.IllegalStateException: Error launching test at
        // http://127.0.0.1:8080/<earname>/ArquillianServletRunner?outputMode=serializedObject&className=org.jboss.as.test.spec.ear.classpath.unit.SubDeploymentAvailableInClassPathTestCase&methodName=testEjbClassAvailableInServlet. Kept on getting 404s.
        // @see https://issues.jboss.org/browse/AS7-367
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, WEB_APP_CONTEXT_ONE + ".ear");
        ear.addAsModule(ejbJar);
        ear.addAsModule(war);

        logger.info(ear.toString(true));
        return ear;
    }

    @Deployment(name = "ear-with-multiple-wars", testable = false)
    public static EnterpriseArchive createEarWithMultipleWars() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejbJar.addClasses(EJBBusinessInterface.class, SimpleSLSB.class);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_APP_CONTEXT_TWO + ".war");
        war.addClasses(HelloWorldServlet.class, EjbInvokingServlet.class);

        final WebArchive otherWar = ShrinkWrap.create(WebArchive.class, OTHER_WEB_APP_CONTEXT + ".war");
        otherWar.addClass(ServletInOtherWar.class);

        // TODO: Currently, due to an issue in AS7 integration with Arquillian, the web-app context and the
        // .ear name should be the same or else you run into test deployment failures like:
        // Caused by: java.lang.IllegalStateException: Error launching test at
        // http://127.0.0.1:8080/<earname>/ArquillianServletRunner?outputMode=serializedObject&className=org.jboss.as.test.spec.ear.classpath.unit.SubDeploymentAvailableInClassPathTestCase&methodName=testEjbClassAvailableInServlet. Kept on getting 404s.
        // @see https://issues.jboss.org/browse/AS7-367
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, WEB_APP_CONTEXT_TWO + ".ear");
        ear.addAsModule(ejbJar);
        ear.addAsModule(war);
        ear.addAsModule(otherWar);

        logger.info(ear.toString(true));
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
    public void testEjbClassAvailableInServlet() throws Exception {
        final HttpClient httpClient = new DefaultHttpClient();
        final String message = "JBossAS7";

        final String requestURL = managementClient.getWebUri() + "/" + WEB_APP_CONTEXT_ONE + HelloWorldServlet.URL_PATTERN + "?" + HelloWorldServlet.PARAMETER_NAME + "=" + message;
        final HttpGet request = new HttpGet(requestURL);
        final HttpResponse response = httpClient.execute(request);
        final HttpEntity entity = response.getEntity();
        Assert.assertNotNull("Response message from servlet was null", entity);
        final String responseMessage = EntityUtils.toString(entity);
        Assert.assertEquals("Unexpected echo message from servlet", message, responseMessage);

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
    public void testServletClassNotAvailableToEjbInEar() throws Exception {
        final HttpClient httpClient = new DefaultHttpClient();
        final String classInWar = HelloWorldServlet.class.getName();
        final String requestURL = managementClient.getWebUri() + "/"  + WEB_APP_CONTEXT_ONE + EjbInvokingServlet.URL_PATTERN + "?" + EjbInvokingServlet.CLASS_IN_WAR_PARAMETER + "=" + classInWar;
        final HttpGet request = new HttpGet(requestURL);
        final HttpResponse response = httpClient.execute(request);
        final HttpEntity entity = response.getEntity();
        Assert.assertNotNull("Response message from servlet was null", entity);
        final String responseMessage = EntityUtils.toString(entity);
        Assert.assertEquals("Unexpected echo message from servlet", EjbInvokingServlet.SUCCESS_MESSAGE, responseMessage);
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
    public void testWarsDontSeeEachOtherInEar() throws Exception {
        final HttpClient httpClient = new DefaultHttpClient();
        final String classInOtherWar = HelloWorldServlet.class.getName();
        final String requestURL = managementClient.getWebUri() + "/"  + OTHER_WEB_APP_CONTEXT + ServletInOtherWar.URL_PATTERN +
                "?" + ServletInOtherWar.CLASS_IN_OTHER_WAR_PARAMETER + "=" + classInOtherWar;
        final HttpGet request = new HttpGet(requestURL);
        final HttpResponse response = httpClient.execute(request);
        final HttpEntity entity = response.getEntity();
        Assert.assertNotNull("Response message from servlet was null", entity);
        final String responseMessage = EntityUtils.toString(entity);
        Assert.assertEquals("Unexpected echo message from servlet", ServletInOtherWar.SUCCESS_MESSAGE, responseMessage);

    }

}
