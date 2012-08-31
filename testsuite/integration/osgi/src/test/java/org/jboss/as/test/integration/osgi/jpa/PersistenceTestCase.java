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
package org.jboss.as.test.integration.osgi.jpa;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import javax.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.test.osgi.OSGiFrameworkUtils;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Test OSGi Persistence deployments
 *
 * @author thomas.diesler@jboss.com
 * @since 31-Aug-2012
 */
@RunWith(Arquillian.class)
public class PersistenceTestCase {

    static final String BUNDLE_A_JAR = "bundle-a.jar";

    @ArquillianResource
    public Deployer deployer;

    @Inject
    public BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "jpa-test-bundle");
        archive.addClasses(OSGiFrameworkUtils.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(ServerDeploymentHelper.class, ModelControllerClient.class, DeploymentPlanBuilder.class);
                builder.addImportPackages(PackageAdmin.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    @Ignore("[AS7-3694] Allow management client to associate metadata with DeploymentUnit")
    public void testExplicitStart() throws Exception {

        InputStream input = deployer.getDeployment(BUNDLE_A_JAR);
        ServerDeploymentHelper server = new ServerDeploymentHelper(getModelControllerClient());
        String runtimeName = server.deploy(BUNDLE_A_JAR, input);
        try {
            Bundle bundle = OSGiFrameworkUtils.getDeployedBundle(context, runtimeName, null);
            assertEquals("Bundle INSTALLED", Bundle.INSTALLED, bundle.getState());

            bundle.start();
            assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

            /*
            EntityManagerFactory emf = null;
            try {
                emf = OSGiFrameworkUtils.waitForService(bundle.getBundleContext(), EntityManagerFactory.class);
                Assert.assertNotNull("EntityManagerFactory not null", emf);

                Employee emp = new Employee();
                emp.setId(100);
                emp.setAddress("Sesame Street");
                emp.setName("Kermit");

                EntityManager em = emf.createEntityManager();
                em.persist(emp);

                emp = em.find(Employee.class, 100);
                Assert.assertNotNull("Employee not null", emp);

                em.remove(emp);

            } finally {
                if (emf != null) {
                    emf.close();
                }
            }
            */
        } finally {
            server.undeploy(runtimeName);
        }
    }

    private ModelControllerClient getModelControllerClient() {
        ServiceReference sref = context.getServiceReference(ModelControllerClient.class.getName());
        return (ModelControllerClient) context.getService(sref);
    }

    @Deployment(name = BUNDLE_A_JAR, managed = false, testable = false)
    public static Archive<?> getPersistenceBundle() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_A_JAR);
        archive.addAsResource(PersistenceTestCase.class.getPackage(), "persistence-a.xml", "META-INF/persistence.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addManifestHeader("Meta-Persistence", "");
                return builder.openStream();
            }
        });
        return archive;
    }
}
