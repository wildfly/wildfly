/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.packaging.injection;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that lifecycle methods defined on classes in a different module to the component class
 * are called.
 */
@RunWith(Arquillian.class)
public class CrossModuleInjectionTestCase {

    private static final String ARCHIVE_NAME = "CrossModuleInjectionTestCase";

    @ArquillianResource
    private URL url;

    private static final String WEB_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            " \n" +
            "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\"\n" +
            "version=\"2.5\">\n" +
            " \n" +
            "    <servlet>\n" +
            "        <servlet-name>SimpleServlet</servlet-name>\n" +
            "        <servlet-class>"+SimpleServlet.class.getName()+"</servlet-class>\n" +
            "        <load-on-startup>1</load-on-startup>\n" +
            "    </servlet>\n" +
            " \n" +
            "    <servlet-mapping>\n" +
            "        <servlet-name>SimpleServlet</servlet-name>\n" +
            "        <url-pattern>/SimpleServlet</url-pattern>\n" +
            "    </servlet-mapping>\n" +
            "</web-app>";


    @Deployment(testable = false)
    public static Archive<?> deploy() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");
        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClass(SimpleServlet.class);
        ear.addAsLibrary(lib);
        JavaArchive module = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        module.addClasses(CrossModuleInjectionTestCase.class, BaseBean.class);
        ear.addAsModule(module);

        WebArchive war = ShrinkWrap.create(WebArchive.class,"simple.war");
        war.addAsWebInfResource(new StringAsset(WEB_XML),"web.xml");
        ear.addAsModule(war);
        return ear;
    }


    @Test
    public void testPostConstructCalled() throws Exception {
        Assert.assertEquals("Hello World", HttpRequest.get( url.toExternalForm() + "SimpleServlet", 2, TimeUnit.SECONDS));
    }
}
