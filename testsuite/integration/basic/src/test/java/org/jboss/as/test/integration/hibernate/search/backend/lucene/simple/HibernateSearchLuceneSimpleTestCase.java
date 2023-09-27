/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.hibernate.search.backend.lucene.simple;

import static org.junit.Assert.assertEquals;

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

/**
 * Verify deployed applications can use the default Hibernate Search module via Jakarta Persistence APIs.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 */
@RunWith(Arquillian.class)
public class HibernateSearchLuceneSimpleTestCase {

    private static final String NAME = HibernateSearchLuceneSimpleTestCase.class.getSimpleName();
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

    @Deployment
    public static Archive<?> deploy() throws Exception {

        // TODO maybe just use managed=false and deploy in the @BeforeClass / undeploy in an @AfterClass
        if (AssumeTestGroupUtil.isSecurityManagerEnabled()) {
            return AssumeTestGroupUtil.emptyJar(JAR_ARCHIVE_NAME);
        }

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_ARCHIVE_NAME);
        // add Jakarta Persistence configuration
        jar.addAsManifestResource(HibernateSearchLuceneSimpleTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        // add testing Bean and entities
        jar.addClasses(SearchBean.class, Book.class, HibernateSearchLuceneSimpleTestCase.class, AnalysisConfigurer.class);

        return jar;
    }

}
