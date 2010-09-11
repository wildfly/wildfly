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

package org.jboss.as.osgi.deployment;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.as.deployment.DeploymentService;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.osgi.AbstractOSGiSubsystemTest;
import org.jboss.as.osgi.OSGiSubsystemSupport;
import org.jboss.as.osgi.deployment.a.A;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.resolver.XModuleBuilder;
import org.jboss.osgi.resolver.XResolverFactory;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Test various OSGi subsystem deployment types.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
public class OSGiDeploymentTestCase extends AbstractOSGiSubsystemTest {

    private static OSGiSubsystemSupport support;

    @BeforeClass
    public static void beforeClass() throws Exception {
        support = new OSGiSubsystemSupport();
        DeploymentChain deploymentChain = support.getDeploymentChain();
        deploymentChain.addProcessor(new OSGiManifestDeploymentProcessor(), 10);
        deploymentChain.addProcessor(new OSGiXServicesDeploymentProcessor(), 20);
        deploymentChain.addProcessor(new OSGiAttachmentsDeploymentProcessor(), Integer.MAX_VALUE);
    }

    @AfterClass
    public static void afterClass() {
        if (support != null) {
            support.shutdown();
            support = null;
        }
    }

    @Override
    protected OSGiSubsystemSupport getSubsystemSupport() {
        return support;
    }

    @Test
    public void testBundleDeployUndeploy() throws Exception {

        JavaArchive archive = getBundleArchive();
        Bundle bundle = executeDeploy(archive);

        assertNotNull("Bundle not null", bundle);
        assertBundleState(Bundle.INSTALLED, bundle.getState());

        assertServiceUp(DeploymentService.SERVICE_NAME.append(archive.getName()));
        assertServiceUp(OSGiDeploymentService.SERVICE_NAME.append(archive.getName()));

        executeUndeploy(archive);

        assertServiceDown(DeploymentService.SERVICE_NAME.append(archive.getName()));
        assertServiceDown(OSGiDeploymentService.SERVICE_NAME.append(archive.getName()));
        assertBundleState(Bundle.UNINSTALLED, bundle.getState());
    }

    @Test
    public void testBundleDeployUninstall() throws Exception {

        JavaArchive archive = getBundleArchive();
        Bundle bundle = executeDeploy(archive);

        assertNotNull("Bundle not null", bundle);
        assertBundleState(Bundle.INSTALLED, bundle.getState());

        final CountDownLatch latch = new CountDownLatch(1);
        AbstractServiceListener<Object> serviceListener = new AbstractServiceListener<Object>() {
            @Override
            public void serviceRemoved(ServiceController<?> controller) {
                latch.countDown();
            }
        };

        ServiceName serviceName = DeploymentService.SERVICE_NAME.append(archive.getName());
        ServiceController<?> controller = getServiceContainer().getRequiredService(serviceName);
        controller.addListener(serviceListener);

        bundle.uninstall();
        assertBundleState(Bundle.UNINSTALLED, bundle.getState());

        latch.await(5L, TimeUnit.SECONDS);
        if (latch.getCount() != 0)
            fail("Did not remove service within 5 seconds.");

        assertServiceDown(DeploymentService.SERVICE_NAME.append(archive.getName()));
        assertServiceDown(OSGiDeploymentService.SERVICE_NAME.append(archive.getName()));
    }

    @Test
    public void testModuleWithXServiceProperties() throws Exception {

        JavaArchive archive = getModuleArchive();
        Bundle bundle = executeDeploy(archive);

        assertNotNull("Bundle not null", bundle);
        assertBundleState(Bundle.INSTALLED, bundle.getState());

        bundle.start();
        assertBundleState(Bundle.ACTIVE, bundle.getState());

        assertLoadClass(bundle, A.class.getName());
        URL entryURL = bundle.getEntry("META-INF/jbosgi-xservice.properties");
        assertNotNull("META-INF/jbosgi-xservice.properties not null", entryURL);

        bundle.uninstall();
        assertBundleState(Bundle.UNINSTALLED, bundle.getState());
    }

    @Test
    public void testAttachedOSGiMetaData() throws Exception {

        final JavaArchive archive = getSimpleArchive("module-two");

        // Add a processor that attaches fabricated {@link OSGiMetaData}
        DeploymentUnitProcessor processor = new DeploymentUnitProcessor() {
            @Override
            public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
                OSGiMetaDataBuilder builder = OSGiMetaDataBuilder.createBuilder(archive.getName(),
                        Version.parseVersion("1.0.0"));
                OSGiMetaDataAttachment.attachOSGiMetaData(context, builder.getOSGiMetaData());
            }
        };
        getDeploymentChain().addProcessor(processor, 30);

        Bundle bundle = executeDeploy(archive);

        assertNotNull("Bundle not null", bundle);
        assertBundleState(Bundle.INSTALLED, bundle.getState());

        bundle.start();
        assertBundleState(Bundle.ACTIVE, bundle.getState());
        assertLoadClass(bundle, A.class.getName());

        bundle.uninstall();
        assertBundleState(Bundle.UNINSTALLED, bundle.getState());

        getDeploymentChain().removeProcessor(processor, 30);
    }

    @Test
    public void testAttachedXModule() throws Exception {

        final JavaArchive archive = getSimpleArchive("module-with-xmodule");

        // Add a processor that attaches fabricated {@link OSGiMetaData}
        DeploymentUnitProcessor processor = new DeploymentUnitProcessor() {
            @Override
            public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
                XResolverFactory factory = XResolverFactory.getInstance(getClass().getClassLoader());
                XModuleBuilder builder = factory.newModuleBuilder();
                builder.createModule(archive.getName(), Version.parseVersion("1.0.0"), 0);
                builder.addBundleCapability("module-with-xmodule", Version.parseVersion("1.0.0"));
                XModuleAttachment.attachXModule(context, builder.getModule());
            }
        };
        getDeploymentChain().addProcessor(processor, 40);

        Bundle bundle = executeDeploy(archive);

        assertNotNull("Bundle not null", bundle);
        assertBundleState(Bundle.INSTALLED, bundle.getState());

        bundle.start();
        assertBundleState(Bundle.ACTIVE, bundle.getState());
        assertLoadClass(bundle, A.class.getName());

        bundle.uninstall();
        assertBundleState(Bundle.UNINSTALLED, bundle.getState());

        getDeploymentChain().removeProcessor(processor, 40);
    }

    private JavaArchive getBundleArchive() throws Exception {
        String uniqueName = support.getUniqueName("bundle");
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, uniqueName);
        archive.addClass(A.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleVersion("1.0.0");
                return builder.openStream();
            }
        });
        return archive;
    }

    private JavaArchive getModuleArchive() throws Exception {
        String uniqueName = support.getUniqueName("module");
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, uniqueName);
        archive.addClass(A.class);
        archive.addResource("deployment/jbosgi-xservice.properties", "META-INF/jbosgi-xservice.properties");
        return archive;
    }

    private JavaArchive getSimpleArchive(String name) throws Exception {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, name);
        archive.addClass(A.class);
        return archive;
    }
}
