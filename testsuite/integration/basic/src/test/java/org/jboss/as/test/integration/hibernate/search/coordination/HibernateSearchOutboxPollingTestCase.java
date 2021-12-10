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
package org.jboss.as.test.integration.hibernate.search.coordination;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test the ability for applications to use outbox-polling coordination,
 * provided they bundle the appropriate Hibernate Search JAR with their application
 * and add a few module dependencies,
 * because that feature is still considered incubating and thus not included in WildFly.
 */
@RunWith(Arquillian.class)
public class HibernateSearchOutboxPollingTestCase {

    @Deployment
    public static Archive<?> createTestArchive() throws Exception {
        return ShrinkWrap.create(WebArchive.class, HibernateSearchOutboxPollingTestCase.class.getSimpleName() + ".war")
                .addClass(HibernateSearchOutboxPollingTestCase.class)
                .addClasses(SearchBean.class, IndexedEntity.class, TimeoutUtil.class)
                .addAsResource(manifest(), "META-INF/MANIFEST.MF")
                .addAsResource(persistenceXml(), "META-INF/persistence.xml")
                // This JAR is copied to target/ using the maven-dependency-plugin; see pom.xml.
                .addAsLibraries(new File("target/testlib/hibernate-search-mapper-orm-coordination-outbox-polling.jar"))
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    private static Asset manifest() {
        String manifest = Descriptors.create(ManifestDescriptor.class)
                .attribute("Dependencies", "org.apache.avro")
                .exportAsString();
        return new StringAsset(manifest);
    }

    private static Asset persistenceXml() {
        String persistenceXml = Descriptors.create(PersistenceDescriptor.class)
                .version("2.0")
                .createPersistenceUnit()
                .name("primary")
                .jtaDataSource("java:jboss/datasources/ExampleDS")
                .getOrCreateProperties()
                .createProperty().name("hibernate.hbm2ddl.auto").value("create-drop").up()
                .createProperty().name("hibernate.search.schema_management.strategy").value("drop-and-create-and-drop").up()
                .createProperty().name("hibernate.search.backend.type").value("lucene").up()
                .createProperty().name("hibernate.search.backend.lucene_version").value("LUCENE_CURRENT").up()
                .createProperty().name("hibernate.search.backend.directory.type").value("local-heap").up()
                .createProperty().name("hibernate.search.coordination.strategy").value("outbox-polling").up()
                .up().up()
                .exportAsString();
        return new StringAsset(persistenceXml);
    }

    @Inject
    private SearchBean searchBean;

    @Test
    public void test() throws InterruptedException {
        // Check that we ARE using outbox-polling coordination
        awaitAssertion(() -> assertEquals(1, searchBean.findAgentNames().size()));

        // Check that indexing through the outbox works correctly
        assertEquals(0, searchBean.search("mytoken").size());
        searchBean.create("This is MYToken");
        awaitAssertion(() -> assertEquals(1, searchBean.search("mytoken").size()));
    }

    private static void awaitAssertion(Runnable assertion) throws InterruptedException {
        long end = System.currentTimeMillis() + TimeoutUtil.adjust(5000);
        AssertionError lastError;
        do {
            try {
                assertion.run();
                return; // The assertion passed
            } catch (AssertionError e) {
                lastError = e;
            }
            Thread.sleep(100);
        }
        while (System.currentTimeMillis() < end);

        throw lastError;
    }

    @Singleton
    @Transactional
    public static class SearchBean {

        @PersistenceContext
        EntityManager em;

        @SuppressWarnings("unchecked")
        public List<String> findAgentNames() {
            return em.createNativeQuery("select name from HSEARCH_AGENT")
                    .getResultList();
        }

        public void create(String text) {
            IndexedEntity entity = new IndexedEntity();
            entity.text = text;
            em.persist(entity);
        }

        public List<IndexedEntity> search(String keyword) {
            return Search.session(em).search(IndexedEntity.class)
                    .extension(LuceneExtension.get())
                    .where(f -> f.match().field("text").matching(keyword))
                    .fetchAllHits();
        }
    }

    @Entity
    @Indexed
    public static class IndexedEntity {

        @Id
        @GeneratedValue
        Long id;

        @FullTextField
        String text;

    }

}
