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

package org.jboss.as.test.integration.jpa.secondlevelcache;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JPA Second level cache tests
 *
 * @author Scott Marlow and Zbynek Roubalik
 */
@RunWith(Arquillian.class)
public class JPA2LCTestCase {

    private static final String ARCHIVE_NAME = "jpa_SecondLevelCacheTestCase";

    // cache region name prefix, use getCacheRegionName() method to get the value!
    private static String CACHE_REGION_NAME = null;

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(JPA2LCTestCase.class,
                Employee.class,
                SFSB1.class,
                SFSB2LC.class
        );

        jar.addAsManifestResource(JPA2LCTestCase.class.getPackage(), "persistence.xml", "persistence.xml");

        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    // Cache region name depends on the internal entity cache naming convention:
    // "fully application scoped persistence unit name" + "the entity class full name"
    // first part could be rewritten by property "hibernate.cache.region_prefix"
    // This method returns prefix + package name, the entity name needs to be appended
    public String getCacheRegionName() throws Exception {

        if (CACHE_REGION_NAME == null) {
            SFSB2LC sfsb = lookup("SFSB2LC", SFSB2LC.class);
            String prefix = sfsb.getCacheRegionName();

            assertNotNull("'hibernate.cache.region_prefix' is null.", prefix);
            CACHE_REGION_NAME = prefix + '.' + this.getClass().getPackage().getName() + '.';
        }

        return CACHE_REGION_NAME;
    }


    @Test
    @InSequence(1)
    public void testMultipleNonTXTransactionalEntityManagerInvocations() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.createEmployee("Kelly Smith", "Watford, England", 1000);
        sfsb1.createEmployee("Alex Scott", "London, England", 2000);
        sfsb1.getEmployeeNoTX(1000);
        sfsb1.getEmployeeNoTX(2000);

        DataSource ds = rawLookup("java:jboss/datasources/ExampleDS", DataSource.class);
        Connection conn = ds.getConnection();
        try {
            int deleted = conn.prepareStatement("delete from Employee").executeUpdate();
            // verify that delete worked (or test is invalid)
            assertTrue("was able to delete added rows.  delete count=" + deleted, deleted > 1);

        } finally {
            conn.close();
        }

        // read deleted data from second level cache
        Employee emp = sfsb1.getEmployeeNoTX(1000);

        assertTrue("was able to read deleted database row from second level cache", emp != null);
    }

    // When caching is disabled, no extra action is done or exception happens
    // even if the code marks an entity and/or a query as cacheable
    @Test
    @InSequence(2)
    public void testDisabledCache() throws Exception {

        SFSB2LC sfsb = lookup("SFSB2LC", SFSB2LC.class);
        String message = sfsb.disabled2LCCheck();

        if (!message.equals("OK")) {
            fail(message);
        }
    }

    // When entity caching is enabled, loading all entities at once
    // will put all entities in the cache. During the SAME session,
    // when looking up for the ID of an entity which was returned by
    // the original query, no SQL queries should be executed.
    //
    // Hibernate ORM 5.3 internally changed from 5.1.
    // Infinispan caches are now non-transactional, meaning that the Hibernate first level cache is
    // relied on inside of transactions for caching.
    // Note from Radim:
    // "
    // With (5.1) transactional caches, Infinispan was storing the fact that you've
    // stored some entities in transactional context and when you attempted to
    // read it from cache, it transparently provided the updated data. With
    // non-transactional caches the *Infinispan layer* (as opposed to *2LC
    // layer*) does not do that. Instead the 2LC provider registers JPA
    // synchronization to execute the update if the JPA transaction commits.
    // "
    //
    // In response to this change, the current sameSessionCheck doesn't make sense anymore,
    // as the "sameSession" refers to the same persistence context being used for the entire test.
    // The same persistence context being used, means that the persistence context first level cache (1lc),
    // is used, instead of the 2lc, which leads to emp2LCStats.getElementCountInMemory() being zero, which would
    // cause this test to fail.
    // Since this test, as currently written, doesn't really test the 2lc, we will ignore it.
    @Ignore
    @Test
    @InSequence(3)
    public void testEntityCacheSameSession() throws Exception {
        SFSB2LC sfsb = lookup("SFSB2LC", SFSB2LC.class);
        String message = sfsb.sameSessionCheck(getCacheRegionName());
        if (!message.equals("OK")) {
            fail(message);
        }
    }

    // When entity caching is enabled, loading all entities at once
    // will put all entities in the cache. During the SECOND session,
    // when looking up for the ID of an entity which was returned by
    // the original query, no SQL queries should be executed.

    @Ignore // see comment for testEntityCacheSameSession, ignored for same reason.
    @Test
    @InSequence(4)
    public void testEntityCacheSecondSession() throws Exception {

        SFSB2LC sfsb = lookup("SFSB2LC", SFSB2LC.class);
        String message = sfsb.secondSessionCheck(getCacheRegionName());

        if (!message.equals("OK")) {
            fail(message);
        }

    }

    // Check if evicting entity second level cache is working as expected
    @Ignore // see comment for testEntityCacheSameSession, ignored for same reason.
    @Test
    @InSequence(5)
    public void testEvictEntityCache() throws Exception {
        SFSB2LC sfsb = lookup("SFSB2LC", SFSB2LC.class);
        String message = sfsb.addEntitiesAndEvictAll(getCacheRegionName());

        if (!message.equals("OK")) {
            fail(message);
        }

        message = sfsb.evictedEntityCacheCheck(getCacheRegionName());

        if (!message.equals("OK")) {
            fail(message);
        }
    }

    // When query caching is enabled, running the same query twice
    // without any operations between them will perform SQL queries only once.
    @Test
    @InSequence(6)
    public void testSameQueryTwice() throws Exception {

        SFSB2LC sfsb = lookup("SFSB2LC", SFSB2LC.class);
        String id = "1";

        String message = sfsb.queryCacheCheck(id);

        if (!message.equals("OK")) {
            fail(message);
        }
    }


    //When query caching is enabled, running a query to return all entities of a class
    // and then adding one entity of such class would invalidate the cache
    @Test
    @InSequence(7)
    public void testInvalidateQuery() throws Exception {

        SFSB2LC sfsb = lookup("SFSB2LC", SFSB2LC.class);
        String id = "2";

        String message = sfsb.queryCacheCheck(id);

        if (!message.equals("OK")) {
            fail(message);
        }

        // invalidate the cache
        sfsb.createEmployee("Newman", "Paul", 400);

        message = sfsb.queryCacheCheck(id);

        if (!message.equals("OK")) {
            fail(message);
        }

    }

    // Check if evicting query cache is working as expected
    @Test
    @InSequence(8)
    public void testEvictQueryCache() throws Exception {
        SFSB2LC sfsb = lookup("SFSB2LC", SFSB2LC.class);
        String id = "3";
        String message = sfsb.queryCacheCheck(id);
        if (!message.equals("OK")) {
            fail(message);
        }
        // evict query cache
        sfsb.evictQueryCache();
        message = sfsb.queryCacheCheckIfEmpty(id);

        if (!message.equals("OK")) {
            fail(message);
        }

    }

}
