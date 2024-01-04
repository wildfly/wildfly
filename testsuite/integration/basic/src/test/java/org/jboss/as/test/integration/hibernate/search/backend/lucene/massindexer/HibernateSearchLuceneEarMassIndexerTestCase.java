/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.hibernate.search.backend.lucene.massindexer;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.application6.ApplicationDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@RunWith(Arquillian.class)
public class HibernateSearchLuceneEarMassIndexerTestCase {
    private static final String NAME = HibernateSearchLuceneEarMassIndexerTestCase.class.getSimpleName();
    private static final String EAR_ARCHIVE_NAME = NAME + ".ear";
    private static final String WAR_ARCHIVE_NAME = NAME + ".war";
    private static final String EJB_ARCHIVE_NAME = NAME + ".jar";

    @BeforeClass
    public static void securityManagerNotSupportedInHibernateSearch() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    @Deployment
    public static EnterpriseArchive createTestEAR() {
        JavaArchive ejb = ShrinkWrap
                .create(JavaArchive.class, EJB_ARCHIVE_NAME)
                .addAsResource(ejbManifest(), "META-INF/MANIFEST.MF")
                .addClasses(Singer.class, SingersSingleton.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, WAR_ARCHIVE_NAME)
                .addClasses(HibernateSearchLuceneEarMassIndexerTestCase.class)
                .addAsResource(warManifest(), "META-INF/MANIFEST.MF")
                .addAsResource(persistenceXml(), "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        return ShrinkWrap.create(EnterpriseArchive.class, EAR_ARCHIVE_NAME)
                .addAsModules(ejb)
                .addAsModule(war)
                .setApplicationXML(applicationXml());
    }

    private static Asset ejbManifest() {
        String manifest = Descriptors.create(ManifestDescriptor.class)
                .attribute("Dependencies", "org.hibernate.search.mapper.orm services,org.hibernate.search.backend.lucene services")
                .exportAsString();
        return new StringAsset(manifest);
    }

    private static Asset persistenceXml() {
        String persistenceXml = Descriptors.create(PersistenceDescriptor.class)
                .version("2.0")
                .createPersistenceUnit()
                .name("cmt-test")
                .jtaDataSource("java:jboss/datasources/ExampleDS")
                .clazz(Singer.class.getName())
                .getOrCreateProperties()
                .createProperty().name("hibernate.hbm2ddl.auto").value("create").up()
                .createProperty().name("hibernate.search.schema_management.strategy").value("drop-and-create-and-drop").up()
                .createProperty().name("hibernate.search.backend.type").value("lucene").up()
                .createProperty().name("hibernate.search.backend.lucene_version").value("LUCENE_CURRENT").up()
                .createProperty().name("hibernate.search.backend.directory.type").value("local-heap").up()
                .createProperty().name("hibernate.search.automatic_indexing.enabled").value("false").up()
                .up().up()
                .exportAsString();
        return new StringAsset(persistenceXml);
    }

    private static Asset applicationXml() {
        String applicationXml = Descriptors.create(ApplicationDescriptor.class)
                .applicationName(NAME)
                .createModule()
                .ejb(EJB_ARCHIVE_NAME)
                .getOrCreateWeb()
                .webUri(WAR_ARCHIVE_NAME)
                .contextRoot("test")
                .up().up()
                .exportAsString();
        return new StringAsset(applicationXml);
    }

    private static Asset warManifest() {
        String manifest = Descriptors.create(ManifestDescriptor.class)
                .addToClassPath(EJB_ARCHIVE_NAME)
                .exportAsString();
        return new StringAsset(manifest);
    }

    @Inject
    private SingersSingleton singersEjb;

    @Test
    public void testMassIndexerWorks() throws Exception {
        assertNotNull(singersEjb);
        singersEjb.insertContact("John", "Lennon");
        singersEjb.insertContact("Paul", "McCartney");
        singersEjb.insertContact("George", "Harrison");
        singersEjb.insertContact("Ringo", "Starr");

        assertEquals("Don't you know the Beatles?", 4, singersEjb.listAllContacts().size());
        assertEquals("Beatles should not yet be indexed", 0, singersEjb.searchAllContacts().size());
        assertTrue("Indexing the Beatles failed.", singersEjb.rebuildIndex());
        assertEquals("Now the Beatles should be indexed", 4, singersEjb.searchAllContacts().size());
    }
}


