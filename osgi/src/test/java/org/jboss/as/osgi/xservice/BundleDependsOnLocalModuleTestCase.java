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

package org.jboss.as.osgi.xservice;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;

import javax.transaction.TransactionManager;

import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.osgi.AbstractOSGiSubsystemTest;
import org.jboss.as.osgi.OSGiSubsystemSupport;
import org.jboss.as.osgi.deployment.OSGiAttachmentsDeploymentProcessor;
import org.jboss.as.osgi.deployment.OSGiManifestDeploymentProcessor;
import org.jboss.as.osgi.deployment.b.TxLoaderActicator;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * A test that shows how a bundle can have a dependency on a local module.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Oct-2010
 */
public class BundleDependsOnLocalModuleTestCase extends AbstractOSGiSubsystemTest {

    private OSGiSubsystemSupport support;

    @Before
    public void setUp() throws Exception {
        support = new OSGiSubsystemSupport();
        DeploymentChain deploymentChain = support.getDeploymentChain();
        deploymentChain.addProcessor(new OSGiManifestDeploymentProcessor(), 20);
        deploymentChain.addProcessor(new OSGiAttachmentsDeploymentProcessor(), Integer.MAX_VALUE);
    }

    @After
    public void tearDown() {
        if (support != null) {
            support.shutdown();
        }
    }

    @Override
    protected OSGiSubsystemSupport getSubsystemSupport() {
        return support;
    }

    @Test
    public void testBundleMissingImport() throws Exception {

        JavaArchive archive = getBundleArchive();

        Bundle bundle = executeDeploy(archive);
        assertBundleState(Bundle.INSTALLED, bundle.getState());

        try {
            bundle.start();
            fail("BundleException expected");
        } catch (BundleException ex) {
            // ignore
        }

        bundle.uninstall();
        assertBundleState(Bundle.UNINSTALLED, bundle.getState());
    }

    @Test
    public void testBundleDependencyOnSystemModule() throws Exception {

        ModuleIdentifier moduleId = ModuleIdentifier.create("javax.transaction.api");
        assertNotNull("Module not null", loadModule(moduleId));

        Bundle txBundle = getBundleManager().installBundle(moduleId);
        assertBundleState(Bundle.INSTALLED, txBundle.getState());

        JavaArchive archive = getBundleArchive();
        Bundle bundle = executeDeploy(archive);
        assertBundleState(Bundle.INSTALLED, bundle.getState());

        bundle.start();
        assertBundleState(Bundle.ACTIVE, bundle.getState());

        bundle.uninstall();
        assertBundleState(Bundle.UNINSTALLED, bundle.getState());

        txBundle.uninstall();
        assertBundleState(Bundle.UNINSTALLED, txBundle.getState());
    }

    private JavaArchive getBundleArchive() throws Exception {
        String uniqueName = support.getUniqueName("bundle");
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, uniqueName);
        archive.addClass(TxLoaderActicator.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleVersion("1.0.0");
                builder.addBundleActivator(TxLoaderActicator.class);
                builder.addImportPackages(TransactionManager.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}
