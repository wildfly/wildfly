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
package org.jboss.as.test.integration.hibernate.search.backend.elasticsearch.simple;

import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.core.api.annotation.Observer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.hibernate.search.backend.elasticsearch.massindexer.Singer;
import org.jboss.as.test.integration.hibernate.search.backend.elasticsearch.util.ElasticsearchServerSetupObserver;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Verify deployed applications can use the default Hibernate Search module via Jakarta Persistence APIs.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@Observer(ElasticsearchServerSetupObserver.class)
public class HibernateSearchElasticsearchSimpleTestCase {

    private static final String NAME = HibernateSearchElasticsearchSimpleTestCase.class.getSimpleName();
    private static final String WAR_ARCHIVE_NAME = NAME + ".war";

    @BeforeClass
    public static void testRequiresDocker() {
        AssumeTestGroupUtil.assumeDockerAvailable();
    }

    @BeforeClass
    public static void securityManagerNotSupportedInHibernateSearch() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    @Deployment
    public static WebArchive createArchive() {

        // TODO maybe just use managed=false and deploy in the @BeforeClass / undeploy in an @AfterClass
        if (!AssumeTestGroupUtil.isDockerAvailable() || AssumeTestGroupUtil.isSecurityManagerEnabled()) {
            return AssumeTestGroupUtil.emptyWar(WAR_ARCHIVE_NAME);
        }

        return ShrinkWrap
                .create(WebArchive.class, WAR_ARCHIVE_NAME)
                .addClasses(HibernateSearchElasticsearchSimpleTestCase.class,
                        SearchBean.class, Book.class, HibernateSearchElasticsearchSimpleTestCase.class,
                        AnalysisConfigurer.class, AssumeTestGroupUtil.class)
                .addAsResource(persistenceXml(), "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    private static Asset persistenceXml() {
        String persistenceXml = Descriptors.create(PersistenceDescriptor.class)
                .version("2.0")
                .createPersistenceUnit()
                .name("jpa-search-test-pu")
                .jtaDataSource("java:jboss/datasources/ExampleDS")
                .clazz(Singer.class.getName())
                .getOrCreateProperties()
                .createProperty().name("hibernate.hbm2ddl.auto").value("create-drop").up()
                .createProperty().name("hibernate.search.schema_management.strategy").value("drop-and-create-and-drop").up()
                .createProperty().name("hibernate.search.automatic_indexing.synchronization.strategy").value("sync").up()
                .createProperty().name("hibernate.search.backend.type").value("elasticsearch").up()
                .createProperty().name("hibernate.search.backend.hosts").value(ElasticsearchServerSetupObserver.getHttpHostAddress()).up()
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
