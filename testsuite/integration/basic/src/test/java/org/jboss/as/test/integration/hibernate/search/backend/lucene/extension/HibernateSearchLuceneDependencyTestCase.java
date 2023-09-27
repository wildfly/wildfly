/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.hibernate.search.backend.lucene.extension;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test the ability for applications to use native Lucene classes,
 * provided they add a dependency to the Lucene module.
 * <p>
 * That's necessary because that module, while public, may be unsupported by product vendors
 * (because its APIs could change without prior notice).
 */
@RunWith(Arquillian.class)
public class HibernateSearchLuceneDependencyTestCase {

    @BeforeClass
    public static void securityManagerNotSupportedInHibernateSearch() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    @Deployment
    public static Archive<?> createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, HibernateSearchLuceneDependencyTestCase.class.getSimpleName() + ".war")
                .addClass(HibernateSearchLuceneDependencyTestCase.class)
                .addClasses(SearchBean.class, IndexedEntity.class)
                .addAsResource(manifest(), "META-INF/MANIFEST.MF")
                .addAsResource(persistenceXml(), "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    private static Asset manifest() {
        String manifest = Descriptors.create(ManifestDescriptor.class)
                // This import is absolutely required, that's on purpose:
                // Lucene is normally an internal module, which can be used at your own risk.
                .attribute("Dependencies", "org.apache.lucene")
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
                .up().up()
                .exportAsString();
        return new StringAsset(persistenceXml);
    }

    @Inject
    private SearchBean searchBean;

    @Test
    public void test() {
        assertEquals(0, searchBean.searchWithNativeQuery("mytoken").size());
        searchBean.create("This is MYToken");
        assertEquals(1, searchBean.searchWithNativeQuery("mytoken").size());
    }

    @ApplicationScoped
    @Transactional
    public static class SearchBean {

        @PersistenceContext
        EntityManager em;

        public void create(String text) {
            IndexedEntity entity = new IndexedEntity();
            entity.text = text;
            em.persist(entity);
        }

        public List<IndexedEntity> searchWithNativeQuery(String keyword) {
            return Search.session(em).search(IndexedEntity.class)
                    .extension(LuceneExtension.get())
                    .where(f -> f.fromLuceneQuery(new TermQuery(new Term("text", keyword))))
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
