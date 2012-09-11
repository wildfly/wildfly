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

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.osgi.OSGiFrameworkUtils;
import org.jboss.as.test.smoke.osgi.jpa.Employee;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Test simple OSGi Persistence deployment
 *
 * @author thomas.diesler@jboss.com
 * @since 31-Aug-2012
 */
@RunWith(Arquillian.class)
public class SimplePersistenceTestCase {

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "simple-jpa-bundle");
        archive.addClasses(Employee.class, OSGiFrameworkUtils.class);
        archive.addAsResource(Employee.class.getPackage(), "simple-persistence.xml", "META-INF/persistence.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                // The Meta-Persistence header may include zero or more comma-separated jar-paths.
                // Each a path to a Persistence Descriptor resource in the bundle.
                builder.addManifestHeader("Meta-Persistence", "");
                builder.addImportPackages(EntityManagerFactory.class, ServiceTracker.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testEntityManagerFactoryService() throws Exception {
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
    }

}
