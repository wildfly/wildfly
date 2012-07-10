/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.osgi.resource;

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
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * An example of OSGi resource injection.
 *
 * @author thomas.diesler@jboss.com
 * @since 10-Jul-2012
 */
@RunAsClient
@RunWith(Arquillian.class)
public class BundleContextInjectionTestCase {

    private static final String WEBAPP_WAR = "webapp.war";
    private static final String SUB_WEBAPP_EAR = "sub-webapp.ear";
    private static final String SUB_WEBAPP_WAR = "sub-webapp.war";
    private static final String MANAGED_BEAN_EAR = "managed.ear";
    private static final String MANAGED_BEAN_WAR = "managed-webapp.war";
    private static final String MANAGED_BEAN_JAR = "managed-beans.jar";

    @ArquillianResource
    URL targetURL;

    @Deployment(name = WEBAPP_WAR, testable = false)
    public static Archive<?> getSimpleWar() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, WEBAPP_WAR);
        war.addClasses(SimpleServlet.class);
        return war;
    }

    @Deployment(name = SUB_WEBAPP_EAR, testable = false)
    public static Archive<?> getSimpleEar() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, SUB_WEBAPP_WAR);
        war.addClasses(SimpleServlet.class);
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, SUB_WEBAPP_EAR);
        ear.addAsModule(war);
        return ear;
    }

    @Deployment(name = MANAGED_BEAN_EAR, testable = false)
    public static Archive<?> getManagedBeanEar() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MANAGED_BEAN_WAR);
        war.addClasses(SimpleBeanServlet.class);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MANAGED_BEAN_JAR);
        jar.addClasses(SimpleManagedBean.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, MANAGED_BEAN_EAR);
        ear.addAsModules(jar, war);
        return ear;
    }

    @Test
    @OperateOnDeployment(WEBAPP_WAR)
    public void testSimpleWar() throws Exception {
        String result = performCall("simple", null);
        Assert.assertEquals("system.bundle:0.0.0", result);
    }

    @Test
    @OperateOnDeployment(SUB_WEBAPP_EAR)
    public void testSimpleEar() throws Exception {
        String result = performCall("simple", null);
        Assert.assertEquals("system.bundle:0.0.0", result);
    }

    @Test
    @OperateOnDeployment(MANAGED_BEAN_EAR)
    public void testManagedBeanEar() throws Exception {
        String result = performCall("simple", null);
        Assert.assertEquals("system.bundle:0.0.0", result);
    }

    private String performCall(String pattern, String param) throws Exception {
        return performCall(null,  pattern, param);
    }

    private String performCall(String context, String pattern, String param) throws Exception {
        String urlspec = targetURL.toExternalForm();
        URL url = new URL(urlspec + pattern + "?input=" + param);
        return HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
    }
}