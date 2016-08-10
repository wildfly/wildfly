/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.mockprovider.classtransformer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Hibernate "hibernate.ejb.use_class_enhancer" test that causes hibernate to add a
 * javax.persistence.spi.ClassTransformer to the pu.
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class ClassFileTransformerTestCase {

    private static final String ARCHIVE_NAME = "jpa_classTransformerTestWithMockProvider";

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive persistenceProvider = ShrinkWrap.create(JavaArchive.class, "testpersistenceprovider.jar");
        persistenceProvider.addClasses(
                TestClassTransformer.class,
                TestEntityManagerFactory.class,
                TestPersistenceProvider.class,
                TestAdapter.class
        );

        // META-INF/services/javax.persistence.spi.PersistenceProvider
        persistenceProvider.addAsResource(new StringAsset("org.jboss.as.test.integration.jpa.mockprovider.classtransformer.TestPersistenceProvider"),
                "META-INF/services/javax.persistence.spi.PersistenceProvider");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");

        JavaArchive ejbjar = ShrinkWrap.create(JavaArchive.class, "ejbjar.jar");
        ejbjar.addAsManifestResource(emptyEjbJar(), "ejb-jar.xml");
        ejbjar.addClasses(ClassFileTransformerTestCase.class,
                SFSB1.class
        );
        ejbjar.addAsManifestResource(ClassFileTransformerTestCase.class.getPackage(), "persistence.xml", "persistence.xml");

        ear.addAsModule(ejbjar);        // add ejbjar to root of ear

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(Employee.class, ClassFileTransformerTestCase.class);
        ear.addAsLibraries(lib, persistenceProvider);
        return ear;

    }

    private static StringAsset emptyEjbJar() {
        return new StringAsset(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\" \n" +
                        "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                        "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"\n" +
                        "         version=\"3.0\">\n" +
                        "   \n" +
                        "</ejb-jar>");
    }

    @ArquillianResource
    private InitialContext iniCtx;

    @Test
    public void test_use_class_enhancer() throws Exception {
        try {
            assertTrue("entity classes are enhanced", TestClassTransformer.getTransformedClasses().size() > 0);
        } finally {
            TestClassTransformer.clearTransformedClasses();
        }
    }

    @Test
    public void test_persistenceUnitInfoURLS() throws Exception {
        try {
            assertTrue("testing that PersistenceUnitInfo.getPersistenceUnitRootUrl() url is vfs based, failed because getPersistenceUnitRootUrl is " +
                            TestPersistenceProvider.getPersistenceUnitInfo("mypc").getPersistenceUnitRootUrl().getProtocol(),
                    "vfs".equals(TestPersistenceProvider.getPersistenceUnitInfo("mypc").getPersistenceUnitRootUrl().getProtocol()));
            InputStream inputStream = TestPersistenceProvider.getPersistenceUnitInfo("mypc").getPersistenceUnitRootUrl().openStream();
            assertNotNull("getPersistenceUnitRootUrl().openStream() returned non-null value", inputStream);

            assertTrue("getPersistenceUnitRootUrl returned a JarInputStream", inputStream instanceof JarInputStream);

            JarInputStream jarInputStream = (JarInputStream) inputStream;
            ZipEntry entry = jarInputStream.getNextEntry();
            assertNotNull("got zip entry from getPersistenceUnitRootUrl", entry);

            while (entry != null && !entry.getName().contains("persistence.xml")) {
                entry = jarInputStream.getNextEntry();
            }
            assertNotNull("didn't find persistence.xml in getPersistenceUnitRootUrl, details=" +
                            urlOpenStreamDetails(TestPersistenceProvider.getPersistenceUnitInfo("mypc").getPersistenceUnitRootUrl().openStream()),
                    entry);
        } finally {
            TestPersistenceProvider.clearLastPersistenceUnitInfo();
        }
    }

    @Test
    public void test_persistenceProviderAdapterInitialized() {
        try {
            assertTrue("persistence unit adapter was initialized", TestAdapter.wasInitialized());
        } finally {
            TestAdapter.clearInitialized();
        }
    }

    private String urlOpenStreamDetails(InputStream urlStream) {
        String result = null;
        try {
            JarInputStream jarInputStream = (JarInputStream) urlStream;
            ZipEntry entry = jarInputStream.getNextEntry();
            while (entry != null) {
                result += entry.getName() + ", ";
                entry = jarInputStream.getNextEntry();
            }
        } catch (IOException e) {
            return "couldn't get content, caught error " + e.getMessage();
        }
        return result;

    }

}
