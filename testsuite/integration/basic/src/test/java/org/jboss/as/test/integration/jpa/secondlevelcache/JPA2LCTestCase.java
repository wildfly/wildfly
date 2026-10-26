/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.secondlevelcache;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Jakarta Persistence Second level cache tests
 *
 * @author Scott Marlow and Zbynek Roubalik and Tommaso Borgato
 */
@ExtendWith(ArquillianExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JPA2LCTestCase {

    private static final String ARCHIVE_NAME = "jpa_SecondLevelCacheTestCase";

    private static final String CACHE_REGION_NAME = JPA2LCTestCase.class.getPackage().getName() + '.';;

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(JPA2LCTestCase.class,
                Employee.class,
                Company.class,
                SFSB1.class,
                SFSB2LC.class,
                SFSB3LC.class,
                SFSB4LC.class,
                RollbackException.class
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

    @Test
    @Order(1)
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
            assertTrue(deleted > 1, "was able to delete added rows.  delete count=" + deleted);

        } finally {
            conn.close();
        }

        // read deleted data from second level cache
        Employee emp = sfsb1.getEmployeeNoTX(1000);

        assertTrue(emp != null, "was able to read deleted database row from second level cache");
    }

    // When caching is disabled, no extra action is done or exception happens
    // even if the code marks an entity and/or a query as cacheable
    @Test
    @Order(2)
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
    // layer*) does not do that. Instead the 2LC provider registers Jakarta Persistence
    // synchronization to execute the update if the Jakarta Persistence transaction commits.
    // "
    //
    // In response to this change, the current sameSessionCheck doesn't make sense anymore,
    // as the "sameSession" refers to the same persistence context being used for the entire test.
    // The same persistence context being used, means that the persistence context first level cache (1lc),
    // is used, instead of the 2lc, which leads to emp2LCStats.getElementCountInMemory() being zero, which would
    // cause this test to fail.
    // Since this test, as currently written, doesn't really test the 2lc, we will ignore it.
    @Disabled
    @Test
    @Order(3)
    public void testEntityCacheSameSession() throws Exception {
        SFSB2LC sfsb = lookup("SFSB2LC", SFSB2LC.class);
        String message = sfsb.sameSessionCheck(CACHE_REGION_NAME);
        if (!message.equals("OK")) {
            fail(message);
        }
    }

    // When entity caching is enabled, loading all entities at once
    // will put all entities in the cache. During the SECOND session,
    // when looking up for the ID of an entity which was returned by
    // the original query, no SQL queries should be executed.

    @Disabled // see comment for testEntityCacheSameSession, ignored for same reason.
    @Test
    @Order(4)
    public void testEntityCacheSecondSession() throws Exception {

        SFSB2LC sfsb = lookup("SFSB2LC", SFSB2LC.class);
        String message = sfsb.secondSessionCheck(CACHE_REGION_NAME);

        if (!message.equals("OK")) {
            fail(message);
        }

    }

    // Check if evicting entity second level cache is working as expected
    @Disabled // see comment for testEntityCacheSameSession, ignored for same reason.
    @Test
    @Order(5)
    public void testEvictEntityCache() throws Exception {
        SFSB2LC sfsb = lookup("SFSB2LC", SFSB2LC.class);
        String message = sfsb.addEntitiesAndEvictAll(CACHE_REGION_NAME);

        if (!message.equals("OK")) {
            fail(message);
        }

        message = sfsb.evictedEntityCacheCheck(CACHE_REGION_NAME);

        if (!message.equals("OK")) {
            fail(message);
        }
    }

    // When query caching is enabled, running the same query twice
    // without any operations between them will perform SQL queries only once.
    @Test
    @Order(6)
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
    @Order(7)
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
    @Order(8)
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

    /**
     <p>
     Check that, when L2 cache is enabled, it does not prevent recovery in separate transactions;
     </p>
     <p>
     Note that all EJB calls are marked with <code>TransactionAttributeType.REQUIRES_NEW</code>;
     </p>
     <p>
     Test sequence:
     <ul>
     <li>{@link SFSB3LC} calls {@link SFSB4LC}</li>
     <li>{@link SFSB4LC} throws and exception</li>
     <li>{@link SFSB3LC} intercepts the exception and calls again {@link SFSB4LC} (hence creating a new transaction)</li>
     <li>this time {@link SFSB4LC} succeeds and {@link SFSB3LC} should succeed as well</li>
     </ul>
     </p>
     */
    @Test
    @Order(9)
    public void testTransaction() throws Exception {
        SFSB3LC sfsb = lookup("SFSB3LC", SFSB3LC.class);

        int id = 10_000;

        // create entity
        sfsb.createEmployee("Gisele Bundchen", "Milan, ITALY", id);

        String message = sfsb.entityCacheCheck(id);

        if (!message.equals("OK")) {
            fail(message);
        }

        // update entity
        message = sfsb.testL2CacheWithRollbackAndRetry(id, "Brookline, Massachusetts (MA), US");
        assertEquals("OK", message, "Expected message is OK");
    }

    /**
     * Verify that we can read Cache Region Statistics via cli
     */
    @Test
    @Order(10)
    @RunAsClient
    public void testStatistics() throws Exception {
        CLIWrapper cli = new CLIWrapper(true);
        cli.sendLine(String.format("/deployment=%s.jar:read-resource(include-runtime=true,recursive=true)", ARCHIVE_NAME));
        CLIOpResult opResult = cli.readAllAsOpResult();
        Assertions.assertTrue(opResult.isIsOutcomeSuccess());
    }

}
