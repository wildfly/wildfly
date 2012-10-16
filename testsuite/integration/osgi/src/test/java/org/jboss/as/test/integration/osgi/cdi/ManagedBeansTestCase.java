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

package org.jboss.as.test.integration.osgi.cdi;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.annotation.ManagedBean;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.osgi.api.PaymentProvider;
import org.jboss.as.test.integration.osgi.cdi.impl.ComplexBeanServlet;
import org.jboss.as.test.integration.osgi.cdi.impl.ComplexManagedBean;
import org.jboss.as.test.integration.osgi.cdi.impl.PaymentProviderActivatorPaypal;
import org.jboss.as.test.integration.osgi.cdi.impl.PaymentProviderActivatorVisa;
import org.jboss.as.test.integration.osgi.cdi.impl.SimpleBeanServlet;
import org.jboss.as.test.integration.osgi.cdi.impl.SimpleManagedBean;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tests CDI deployments with OSGi metadata
 *
 * @author Thomas.Diesler@jboss.com
 * @since 09-Jul-2012
 */
@RunWith(Arquillian.class)
public class ManagedBeansTestCase {

    private static final String SIMPLE_EAR = "simple.ear";
    private static final String SIMPLE_WAR = "simple.war";
    private static final String SIMPLE_CDI_JAR = "simple-cdi.jar";

    private static final String COMPLEX_EAR = "complex.ear";
    private static final String COMPLEX_WAR = "complex.war";
    private static final String COMPLEX_CDI_JAR = "complex-cdi.jar";
    private static final String VISA_PROVIDER_BUNDLE = "visa-bundle.jar";
    private static final String PAYPAL_PROVIDER_BUNDLE = "paypal-bundle.jar";

    @ArquillianResource
    PackageAdmin packageAdmin;

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "osgi-cdi-test");
        jar.addClasses(HttpRequest.class);
        jar.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(jar.getName());
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        return jar;
    }

    @Deployment(name = SIMPLE_EAR, testable = false)
    public static Archive<?> getSimpleEar() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, SIMPLE_WAR);
        war.addClasses(SimpleBeanServlet.class);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SIMPLE_CDI_JAR);
        jar.addClasses(SimpleManagedBean.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, SIMPLE_EAR);
        ear.addAsModule(war);
        ear.addAsModule(jar);
        return ear;
    }

    @Deployment(name = COMPLEX_EAR, testable = false)
    public static Archive<?> getComplexEar() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, COMPLEX_WAR);
        war.addClasses(ComplexBeanServlet.class);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, COMPLEX_CDI_JAR);
        jar.addClasses(ComplexManagedBean.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(COMPLEX_CDI_JAR);
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(PaymentProvider.class);
                builder.addImportPackages(BundleContext.class, ServiceTracker.class, ManagedBean.class);
                return builder.openStream();
            }
        });
        JavaArchive visa = ShrinkWrap.create(JavaArchive.class, VISA_PROVIDER_BUNDLE);
        visa.addClasses(PaymentProviderActivatorVisa.class, PaymentProvider.class);
        visa.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(VISA_PROVIDER_BUNDLE);
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(PaymentProviderActivatorVisa.class);
                builder.addExportPackages(PaymentProvider.class);
                builder.addImportPackages(PaymentProvider.class, BundleActivator.class);
                return builder.openStream();
            }
        });
        JavaArchive paypal = ShrinkWrap.create(JavaArchive.class, PAYPAL_PROVIDER_BUNDLE);
        paypal.addClasses(PaymentProviderActivatorPaypal.class, PaymentProvider.class);
        paypal.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(PAYPAL_PROVIDER_BUNDLE);
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(PaymentProviderActivatorPaypal.class);
                builder.addExportPackages(PaymentProvider.class);
                builder.addImportPackages(PaymentProvider.class, BundleActivator.class);
                return builder.openStream();
            }
        });
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, COMPLEX_EAR);
        ear.addAsModule(war);
        ear.addAsModule(jar);
        ear.addAsModule(visa);
        ear.addAsModule(paypal);
        return ear;
    }

    @Test
    public void testSimpleEar() throws Exception {
        Assert.assertEquals("[Paypal, Visa]", performCall("/simple/servlet"));
    }

    @Test
    public void testComplexEar() throws Exception {
        Assert.assertEquals("[Paypal, Visa]", performCall("/complex/servlet"));

        Bundle visaBundle = packageAdmin.getBundles(VISA_PROVIDER_BUNDLE, null)[0];
        Bundle paypalBundle = packageAdmin.getBundles(PAYPAL_PROVIDER_BUNDLE, null)[0];

        visaBundle.stop();
        Assert.assertEquals("[Paypal]", performCall("/complex/servlet"));

        paypalBundle.stop();
        Assert.assertEquals("[]", performCall("/complex/servlet"));

        visaBundle.start();
        Assert.assertEquals("[Visa]", performCall("/complex/servlet"));

        paypalBundle.start();
        Assert.assertEquals("[Paypal, Visa]", performCall("/complex/servlet"));
    }

    private String performCall(String path) throws Exception {
        String urlspec = managementClient.getWebUri() + path;
        return HttpRequest.get(urlspec, 5, TimeUnit.SECONDS);
    }
}
