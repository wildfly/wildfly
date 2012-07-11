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

package org.jboss.as.test.smoke.osgi;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.annotation.ManagedBean;
import javax.sql.DataSource;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.smoke.osgi.bundleA.SimpleBeanServlet;
import org.jboss.as.test.smoke.osgi.bundleB.SimpleManagedBean;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests CDI deployments with OSGi metadata
 *
 * @author Thomas.Diesler@jboss.com
 * @since 09-Jul-2012
 */
@RunAsClient
@RunWith(Arquillian.class)
public class SimpleManagedBeanTestCase {

    private static final String SIMPLE_EAR = "simple.ear";
    private static final String SIMPLE_WAR = "simple.war";
    private static final String SIMPLE_CDI_JAR = "simple-cdi.jar";

    private static final String COMPLEX_EAR = "complex.ear";
    private static final String COMPLEX_WAR = "complex.war";
    private static final String COMPLEX_CDI_JAR = "complex-cdi.jar";

    @ArquillianResource
    URL targetURL;

    @Deployment(name = SIMPLE_EAR, testable = false)
    public static Archive<?> getSimpleEar() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, SIMPLE_WAR);
        war.addClasses(SimpleBeanServlet.class);

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SIMPLE_CDI_JAR);
        jar.addClasses(SimpleManagedBean.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, SIMPLE_EAR);
        ear.addAsModule(war);
        ear.addAsModule(jar);
        return ear;
    }

    @Deployment(name = COMPLEX_EAR, testable = false)
    public static Archive<?> getComplexEar() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, COMPLEX_WAR);
        war.addClasses(SimpleBeanServlet.class);

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, COMPLEX_CDI_JAR);
        jar.addClasses(SimpleManagedBean.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(jar.getName());
                builder.addBundleManifestVersion(2);
                builder.addExportPackages(SimpleManagedBean.class);
                builder.addImportPackages(ManagedBean.class, DataSource.class);
                return builder.openStream();
            }
        });

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, COMPLEX_EAR);
        ear.addAsModule(war);
        ear.addAsModule(jar);
        return ear;
    }

    @Test
    @OperateOnDeployment(SIMPLE_EAR)
    public void testSimpleEar() throws Exception {
        String result = performCall("simple", null);
        Assert.assertEquals("H2 JDBC Driver", result);
    }

    @Test
    @OperateOnDeployment(COMPLEX_EAR)
    public void testComplexEar() throws Exception {
        String result = performCall("simple", null);
        Assert.assertEquals("H2 JDBC Driver", result);
    }

    private String performCall(String pattern, String param) throws Exception {
        String urlspec = targetURL.toExternalForm();
        URL url = new URL(urlspec + pattern + (param != null ? "?input=" + param : ""));
        return HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
    }
}
