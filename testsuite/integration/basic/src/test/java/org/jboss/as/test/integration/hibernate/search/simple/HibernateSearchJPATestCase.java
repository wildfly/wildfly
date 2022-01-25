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
package org.jboss.as.test.integration.hibernate.search.simple;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import javax.ejb.EJB;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.impl.MutableSearchFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verify deployed applications can use the default Hibernate Search module via Jakarta Persistence APIs.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 */
@RunWith(Arquillian.class)
public class HibernateSearchJPATestCase {

    private static final String NAME = HibernateSearchJPATestCase.class.getSimpleName();
    private static final String JAR_ARCHIVE_NAME = NAME + ".jar";

    @EJB(mappedName = "java:module/SearchBean")
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

    @Test
    public void testCustomConfigurationApplied() {
        SearchFactory searchFactory = searchBean.retrieveHibernateSearchEngine();
        MutableSearchFactory internalSearchEngine = searchFactory.unwrap( MutableSearchFactory.class );
        Assert.assertTrue(internalSearchEngine.isIndexUninvertingAllowed());
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_ARCHIVE_NAME);
        // add Jakarta Persistence configuration
        jar.addAsManifestResource(HibernateSearchJPATestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        // add testing Bean and entities
        jar.addClasses(SearchBean.class, Book.class, HibernateSearchJPATestCase.class, AnalysisConfigurationProvider.class);
        // WFLY-10195: temporary - should be possible to remove after upgrade to Lucene 6
        jar.addAsManifestResource(createPermissionsXmlAsset(
                new RuntimePermission("accessDeclaredMembers")
        ), "permissions.xml");

        return jar;
    }

}
