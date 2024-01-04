/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.hibernate.search.backend.elasticsearch.projectionconstructor;

import jakarta.inject.Inject;
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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Verify deployed applications can use Hibernate Search's @ProjectionConstructor with Lucene.
 */
@RunWith(Arquillian.class)
@Observer(ElasticsearchServerSetupObserver.class)
public class HibernateSearchElasticsearchProjectionConstructorTestCase {

    private static final String NAME = HibernateSearchElasticsearchProjectionConstructorTestCase.class.getSimpleName();
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
                .addClasses(HibernateSearchElasticsearchProjectionConstructorTestCase.class,
                        SearchBean.class, Book.class, Author.class, BookDTO.class, AuthorDTO.class,
                        AssumeTestGroupUtil.class)
                .addAsResource(persistenceXml(), "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    private static Asset persistenceXml() {
        String persistenceXml = Descriptors.create(PersistenceDescriptor.class)
                .version("2.0")
                .createPersistenceUnit()
                .name("jpa-search-test-pu")
                .jtaDataSource("java:jboss/datasources/ExampleDS")
                .clazz(Book.class.getName())
                .clazz(Author.class.getName())
                .getOrCreateProperties()
                .createProperty().name("hibernate.hbm2ddl.auto").value("create-drop").up()
                .createProperty().name("hibernate.search.schema_management.strategy").value("drop-and-create-and-drop").up()
                .createProperty().name("hibernate.search.automatic_indexing.synchronization.strategy").value("sync").up()
                .createProperty().name("hibernate.search.backend.type").value("elasticsearch").up()
                .createProperty().name("hibernate.search.backend.hosts").value(ElasticsearchServerSetupObserver.getHttpHostAddress()).up()
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
    public void testProjection() {
        BookDTO book1 = new BookDTO(1, "Hello world",
                Arrays.asList(new AuthorDTO("John", "Smith")));
        searchBean.storeNewBook(book1);
        BookDTO book2 = new BookDTO(2, "Hello planet Mars",
                Arrays.asList(new AuthorDTO("Jane", "Green"),
                        new AuthorDTO("John", "Doe")));
        searchBean.storeNewBook(book2);

        List<BookDTO> hits = searchBean.findByKeyword("world");
        assertEquals(1, hits.size());
        assertSearchHit(book1, hits.get(0));

        hits = searchBean.findByKeyword("mars");
        assertEquals(1, hits.size());
        assertSearchHit(book2, hits.get(0));
    }

    private static void assertSearchHit(BookDTO expected, BookDTO hit) {
        assertEquals(expected.id, hit.id);
        assertEquals(expected.title, hit.title);
        assertEquals(expected.authors.size(), hit.authors.size());
        for (int i = 0; i < expected.authors.size(); i++) {
            AuthorDTO expectedAuthor = expected.authors.get(i);
            AuthorDTO hitAuthor = hit.authors.get(i);
            assertEquals(expectedAuthor.firstName, hitAuthor.firstName);
            assertEquals(expectedAuthor.lastName, hitAuthor.lastName);
            // We use includeDepth = 1 on BookDTO#authors, so book.authors.books should be empty
            assertEquals(0, hitAuthor.books.size());
        }
    }

}
