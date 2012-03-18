/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jaxrs.cfg.applicationclasses.HelloWorldApplication;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Tests for RESTEasy configuration parameter 'resteasy.scan.resources'
 *
 * @author Pavel Janousek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ResteasyScanResourcesTestCase {

    private static final String depNameTrue = "dep_true";
    private static final String depNameFalse = "dep_false";
    private static final String depNameFalseAS7_3034 = "dep_false_as7_3034";
    private static final String depNameInvalid = "dep_invalid";

    @Deployment(name = depNameTrue, managed = true)
    public static Archive<?> deploy_true() {
        return ShrinkWrap.create(WebArchive.class, depNameTrue + ".war")
                .addPackage(ResteasyScanResourcesTestCase.class.getPackage())
                .setWebXML(webXml("resteasy.scan.resources", "true"));
    }

    @Deployment(name = depNameFalse, managed = true)
    public static Archive<?> deploy_false() {
        return ShrinkWrap.create(WebArchive.class, depNameFalse + ".war")
                .addPackage(ResteasyScanResourcesTestCase.class.getPackage())
                .setWebXML(webXml("resteasy.scan.resources", "false"));
    }

    @Deployment(name = depNameFalseAS7_3034, managed = false)
    public static Archive<?> deploy_false_as7_3034() {
        return ShrinkWrap.create(WebArchive.class, depNameFalseAS7_3034 + ".war")
                .addPackage(ResteasyScanResourcesTestCase.class.getPackage())
                .addPackage(HelloWorldApplication.class.getPackage());
    }

    @Deployment(name = depNameInvalid, managed = false)
    public static Archive<?> deploy_invalid() {
        return ShrinkWrap.create(WebArchive.class, depNameInvalid + ".war")
                .addPackage(ResteasyScanResourcesTestCase.class.getPackage())
                .setWebXML(webXml("resteasy.scan.resources", "blah"));

    }

    private static StringAsset webXml(String paramName, String paramValue) {
        return WebXml.get("<servlet-mapping>\n"
                + "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
                + "        <url-pattern>/myjaxrs/*</url-pattern>\n"
                + "</servlet-mapping>\n"
                + "\n"
                + "<context-param>\n"
                + "        <param-name>" + paramName + "</param-name>\n"
                + "        <param-value>" + paramValue + "</param-value>\n"
                + "</context-param>\n"
                + "\n");
    }

    @ArquillianResource
    private Deployer deployer;

    @Test
    @OperateOnDeployment(depNameTrue)
    public void testDeployTrue1(@ArquillianResource URL url) throws Exception {
        String result = HttpRequest.get(url.toExternalForm() + "myjaxrs/helloworld", 10, TimeUnit.SECONDS);
        assertEquals("Hello World!", result);
    }

    @Test
    @OperateOnDeployment(depNameFalse)
    public void testDeployFalse1(@ArquillianResource URL url) throws Exception {
        try {
            @SuppressWarnings("unused")
            String result = HttpRequest.get(url.toExternalForm() + "myjaxrs/helloworld", 10, TimeUnit.SECONDS);
            Assert.fail("Scan of Resources is disabled so we should not pass to there - HTTP 404 must occur!");
        } catch (Exception e) {
        }
    }

    @Test
    public void testDeployFalse_AS7_3043() throws Exception {
        try {
            deployer.deploy(depNameFalseAS7_3034);
            Assert.fail("Test should not go there - invalid deployment (duplicated javax.ws.rs.core.Application)! Possible regression of AS7-3034 found");
        } catch (Exception e) {
        }
    }

    @Test
    public void testDeployInvalid() throws Exception {
        try {
            deployer.deploy(depNameInvalid);
            Assert.fail("Test should not go here - invalid deployment (invalid value of resteasy.scan.resources)!");
        } catch (Exception e) {
        }
    }

}
