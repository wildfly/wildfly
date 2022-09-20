/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.hibernate.search.v5migrationhelper.simple;

import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.hibernate.search.backend.lucene.simple.HibernateSearchLuceneSimpleTestCase;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Verify deployed applications can use the default Hibernate Search module via Jakarta Persistence APIs
 * and through the V5 migration helper provided as an application library.
 * <p>
 * This test is similar to {@link HibernateSearchLuceneSimpleTestCase},
 * but uses old APIs thanks to the V5 migration helper JARs that are packaged with the application.
 * Those are not provided as WildFly modules because they shouldn't be used in production.
 */
@RunWith(Arquillian.class)
public class HibernateSearchV5MigrationHelperTestCase {
    private static final String NAME = HibernateSearchV5MigrationHelperTestCase.class.getSimpleName();
    private static final String WAR_ARCHIVE_NAME = NAME + ".war";

    @BeforeClass
    public static void securityManagerNotSupportedInHibernateSearch() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    @Deployment
    public static WebArchive createArchive() {

        // TODO maybe just use managed=false and deploy in the @BeforeClass / undeploy in an @AfterClass
        if (AssumeTestGroupUtil.isSecurityManagerEnabled()) {
            return AssumeTestGroupUtil.emptyWar(WAR_ARCHIVE_NAME);
        }

        return ShrinkWrap
                .create(WebArchive.class, WAR_ARCHIVE_NAME)
                .addClasses(HibernateSearchV5MigrationHelperTestCase.class,
                        SearchBean.class, Book.class, AnalysisConfigurer.class)
                .addAsResource(warManifest(), "META-INF/MANIFEST.MF")
                .addAsResource(persistenceXml(), "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                // These JARs are copied to target/ using the maven-dependency-plugin; see pom.xml.
                .addAsLibraries(new File("target/testlib/hibernate-search-v5migrationhelper-engine.jar"),
                        new File("target/testlib/hibernate-search-v5migrationhelper-orm.jar"));
    }

    private static Asset warManifest() {
        String manifest = Descriptors.create(ManifestDescriptor.class)
                // We're not using Hibernate Search 6 @Indexed annotations,
                // so we need to depend on org.hibernate.search.orm and to import services.
                // The V5 migration helper leaks Lucene APIs, so we need a dependency to org.apache.lucene.
                .attribute("Dependencies", "org.hibernate.search.orm services,org.apache.lucene")
                .exportAsString();
        return new StringAsset(manifest);
    }

    private static Asset persistenceXml() {
        String persistenceXml = Descriptors.create(PersistenceDescriptor.class)
                .version("2.0")
                .createPersistenceUnit()
                .name("jpa-search-test-pu")
                .jtaDataSource("java:jboss/datasources/ExampleDS")
                .clazz(Book.class.getName())
                // Configuration properties need to be those of Hibernate Search 6;
                // the migration helper doesn't change that.
                .getOrCreateProperties()
                .createProperty().name("hibernate.hbm2ddl.auto").value("create-drop").up()
                .createProperty().name("hibernate.search.schema_management.strategy").value("drop-and-create-and-drop").up()
                .createProperty().name("hibernate.search.backend.type").value("lucene").up()
                .createProperty().name("hibernate.search.backend.lucene_version").value("LUCENE_CURRENT").up()
                .createProperty().name("hibernate.search.backend.directory.type").value("local-heap").up()
                .createProperty().name("hibernate.search.backend.analysis.configurer").value(AnalysisConfigurer.class.getName()).up()
                .up().up()
                .exportAsString();
        return new StringAsset(persistenceXml);
    }

    @Inject
    private SearchBean searchBean;

    @Before
    @After
    public void cleanupDatabase() {
        searchBean.deleteAll();
    }

    @Test
    public void testFullTextQuery() {
        searchBean.storeNewBook("Hello");
        searchBean.storeNewBook("Hello world");
        searchBean.storeNewBook("Hello planet Mars");
        assertEquals(3, searchBean.findByKeyword("hello").size());
        assertEquals(1, searchBean.findByKeyword("mars").size());
        // Search should be case-insensitive thanks to the default analyzer
        assertEquals(3, searchBean.findByKeyword("HELLO").size());
    }

    @Test
    public void testAnalysisConfiguration() {
        searchBean.storeNewBook("Hello");
        searchBean.storeNewBook("Hello world");
        searchBean.storeNewBook("Hello planet Mars");
        // This search relies on a custom analyzer configured in AnalysisConfigurationProvider;
        // if it works, then our custom analysis configuration was taken into account.
        assertEquals(3, searchBean.findAutocomplete("he").size());
        assertEquals(1, searchBean.findAutocomplete("he wo").size());
        assertEquals(1, searchBean.findAutocomplete("he pl").size());
    }

}
