/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.hibernate.search.backend.lucene.projectionconstructor;

import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
public class HibernateSearchLuceneProjectionConstructorTestCase {

    private static final String NAME = HibernateSearchLuceneProjectionConstructorTestCase.class.getSimpleName();
    private static final String JAR_ARCHIVE_NAME = NAME + ".jar";

    @BeforeClass
    public static void securityManagerNotSupportedInHibernateSearch() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
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

    @Deployment
    public static Archive<?> deploy() throws Exception {

        // TODO maybe just use managed=false and deploy in the @BeforeClass / undeploy in an @AfterClass
        //   see HibernateSearchLuceneSimpleTestCase
        if (AssumeTestGroupUtil.isSecurityManagerEnabled()) {
            return AssumeTestGroupUtil.emptyJar(JAR_ARCHIVE_NAME);
        }

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_ARCHIVE_NAME);
        // add Jakarta Persistence configuration
        jar.addAsManifestResource(HibernateSearchLuceneProjectionConstructorTestCase.class.getPackage(),
                "persistence.xml", "persistence.xml");
        // add testing Bean and entities
        jar.addClasses(SearchBean.class, Book.class, Author.class, BookDTO.class, AuthorDTO.class,
                HibernateSearchLuceneProjectionConstructorTestCase.class);

        return jar;
    }

}
