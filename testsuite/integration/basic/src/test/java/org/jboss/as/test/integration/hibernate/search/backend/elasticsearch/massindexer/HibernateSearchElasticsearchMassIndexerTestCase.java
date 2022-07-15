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
package org.jboss.as.test.integration.hibernate.search.backend.elasticsearch.massindexer;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.core.api.annotation.Observer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.hibernate.search.backend.elasticsearch.util.ElasticsearchServerSetupObserver;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
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
@Observer(ElasticsearchServerSetupObserver.class)
public class HibernateSearchElasticsearchMassIndexerTestCase {
    private static final String NAME = HibernateSearchElasticsearchMassIndexerTestCase.class.getSimpleName();
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

        if (!AssumeTestGroupUtil.isDockerAvailable() || !AssumeTestGroupUtil.isSecurityManagerDisabled()) {
            return AssumeTestGroupUtil.emptyWar(WAR_ARCHIVE_NAME);
        }

        return ShrinkWrap
                .create(WebArchive.class, WAR_ARCHIVE_NAME)
                .addClasses(HibernateSearchElasticsearchMassIndexerTestCase.class,
                        Singer.class, SingersSingleton.class, AssumeTestGroupUtil.class)
                .addAsResource(persistenceXml(), "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    private static Asset persistenceXml() {
        String persistenceXml = Descriptors.create(PersistenceDescriptor.class)
                .version("2.0")
                .createPersistenceUnit()
                .name("cmt-test")
                .jtaDataSource("java:jboss/datasources/ExampleDS")
                .clazz(Singer.class.getName())
                .getOrCreateProperties()
                .createProperty().name("hibernate.hbm2ddl.auto").value("create-drop").up()
                .createProperty().name("hibernate.search.schema_management.strategy").value("drop-and-create-and-drop").up()
                .createProperty().name("hibernate.search.automatic_indexing.enabled").value("false").up()
                .createProperty().name("hibernate.search.backend.type").value("elasticsearch").up()
                .createProperty().name("hibernate.search.backend.hosts").value(ElasticsearchServerSetupObserver.getHttpHostAddress()).up()
                .up().up()
                .exportAsString();
        return new StringAsset(persistenceXml);
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


