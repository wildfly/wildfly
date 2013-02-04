/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.entity.cmp.callback;

import javax.ejb.ObjectNotFoundException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientTransactionContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <p>
 * Check whether the findByPrimaryKey or other finder will not find a removed entity. This is done in a different or the same
 * transaction.
 * </p>
 * <p>
 * Check whether ejbCreate ejbPostCreate and ejbStore is called during creating an entity bean. Check if the findByPrimaryKey is
 * optimized to not call DB sync by check whether the ejbStore is called if the finder is executed. The creation and the test
 * will be called in different transactions to ensure that the entity bean was passivated and is taken out of the instance pool
 * to check that the pooled instance is correct reset for reuse.
 * </p>
 * <p>
 * Check whether ejbStore is called during creating an entity bean. Validate if the findByPrimaryKey is optimized to not call DB
 * sync by check whether the ejbStore is called if the findByPrimaryKey is executed. And ensure that ejbStore is called if a
 * finder different to findByPrimaryKey is called. The creation and the test will be called within the same transaction.
 * </p>
 *
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CheckFinderAndFlushTestCase {
    private static final Logger log = Logger.getLogger(CheckFinderAndFlushTestCase.class);

    private static final String ENTITY_BEAN_ARCHIVE_NAME = "cmp-findBy";
    private static final String ENTITY_NAME = "SimpleEntity";

    private static String nodeName;

    @ArquillianResource
    private InitialContext initialContext;


    @Deployment
    public static Archive<?> deploymentEntityBean()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ENTITY_BEAN_ARCHIVE_NAME + ".jar")
                .addClass(SimpleEntityBean.class)
                .addClass(SimpleEntityRemoteHome.class)
                .addClass(SimpleEntityRemote.class)
                .addClass(SingletonTestResults.class)
                .addClass(SingletonTestResultsBean.class)
                .addAsManifestResource(CheckFinderAndFlushTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml")
                .addAsManifestResource(CheckFinderAndFlushTestCase.class.getPackage(), "jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
        log.info(jar.toString(true));
        return jar;
    }

    private SingletonTestResults getResultsSingleton() throws NamingException {
        return (SingletonTestResults) initialContext.lookup("ejb:/" + ENTITY_BEAN_ARCHIVE_NAME + "//" + SingletonTestResultsBean.class.getSimpleName() + "!" + SingletonTestResults.class.getName());
    }

    private SimpleEntityRemoteHome getEntityBeanHome() throws NamingException {
        return (SimpleEntityRemoteHome) initialContext.lookup("ejb:/" + ENTITY_BEAN_ARCHIVE_NAME + "//" + ENTITY_NAME + "!" + SimpleEntityRemoteHome.class.getName());
    }

    /**
     * Create and setup the remoting connection
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeTestClass() throws Exception {
        // the node name that the test methods can use
        nodeName = EJBManagementUtil.getNodeName();
        log.info("Using node name " + nodeName);
    }

    /**
     * Needed for transaction ctx to be correctly used and propagated to AS.
     */
    @Before
    public void beforeTest() throws Exception {
        // Create and setup the EJB client context backed by the remoting receiver
        final EJBClientTransactionContext localUserTxContext = EJBClientTransactionContext.createLocal();
        // set the tx context
        EJBClientTransactionContext.setGlobalContext(localUserTxContext);
    }

    /**
     * From the spec a call to a finder must flush all entities to the database to ensure a correct result for the query.
     * This is not necessary for the 'findByPrimaryKey' method and will be a performance improvement.
     *
     * The test checks whether the entity is not flushed during calls of findByPrimaryKey.
     * This can be checked with the ejbStore callback invocation.
     */
    @Test
    public void testDbSync() throws Exception {
        SingletonTestResults results = getResultsSingleton();
        results.resetAll();

        final UserTransaction ut = EJBClient.getUserTransaction(nodeName);
        ut.begin();

        // getting home interface
        SimpleEntityRemoteHome home = getEntityBeanHome();

        SimpleEntityRemote e1 = home.create(10001l, "Entity 10001");
        Assert.assertTrue("ejbStore not called as expected after create!", results.isCalled(ENTITY_NAME, "10001", "ejbStore"));

        SimpleEntityRemote e2 = home.create(10002l, "Entity 10002");

        results.resetAll();
        // now change the entity to be sure it must be stored
        e1.setName("Entity 10001 changed");
        Assert.assertFalse("ejbStore called unexpected after change!", results.isCalled(ENTITY_NAME, "10001", "ejbStore"));

        // now call a finder ByPrimaryKey
        e2 = home.findByPrimaryKey(10002l);
        Assert.assertFalse("Entity #10001 was unexpected flushed to the DB", results.isCalled(ENTITY_NAME, "10001", "ejbStore"));

        // now call a finder and check that ejbStore is called as the entity must be synced to the database according to the spec
        e2 = home.findById(10002l);
        Assert.assertTrue("Entity #10001 was not flushed to the DB", results.isCalled(ENTITY_NAME, "10001", "ejbStore"));

        ut.commit();
        e1.remove();
        e2.remove();
    }

    /**
     * From the spec a call to a finder must flush all entities to the database to ensure
     * a correct result for the query.
     * This is not necessary for the 'findByPrimaryKey' method and will be a performance improvement.
     *
     * The test checks whether the entity is not flushed during calls of findByPrimaryKey if the entities are
     * created in a different Tx.
     */
    @Test
    public void testDbSyncDifferentTx() throws Exception {
        SingletonTestResults results = getResultsSingleton();
        results.resetAll();

        final UserTransaction ut = EJBClient.getUserTransaction(nodeName);
        final SimpleEntityRemoteHome home = getEntityBeanHome();

        // prepare, create two entities in a transaction
        ut.begin();
        SimpleEntityRemote e1Created = home.create(1l, "Entity #1");
        Assert.assertEquals("ejbCreate must be called for Entity#1", 1, results.getNumberOfCalls(ENTITY_NAME, "1", "ejbCreate"));
        Assert.assertEquals("ejbPostCreate must be called for Entity#1", 1, results.getNumberOfCalls(ENTITY_NAME, "1", "ejbPostCreate"));
        Assert.assertEquals("ejbStore must be called for Entity#1", 1, results.getNumberOfCalls(ENTITY_NAME, "1", "ejbStore"));
        SimpleEntityRemote e2Created = home.create(2l, "Entity #2");
        Assert.assertEquals("ejbCreate must be called for Entity#2", 1, results.getNumberOfCalls(ENTITY_NAME, "2", "ejbCreate"));
        Assert.assertEquals("ejbPostCreate must be called for Entity#2", 1, results.getNumberOfCalls(ENTITY_NAME, "2", "ejbPostCreate"));
        Assert.assertEquals("ejbStore must be called for Entity#2", 1, results.getNumberOfCalls(ENTITY_NAME, "2", "ejbStore"));
        ut.commit();

        // reset the invocation statistic
        results.resetAll();

        ut.begin();
        try {
            log.debug("read Enity #1");
            SimpleEntityRemote e1 = home.findByPrimaryKey(1l);
            Assert.assertFalse("ejbStore unexpected called after findByPrimaryKey!", results.isCalled(ENTITY_NAME, "1", "ejbStore"));
            // only call a findByPKey as trigger
            home.findByPrimaryKey(2l);

            // change the entity to force flush
            e1.setName("Entity #1 changed");
            Assert.assertFalse("ejbStore called unexpected after change!", results.isCalled(ENTITY_NAME, "1", "ejbStore"));

            // check that this did not trigger a flush
            home.findByPrimaryKey(2l);
            Assert.assertFalse("Entity #1 was unexpected flushed to the DB",results.isCalled(ENTITY_NAME, "1", "ejbStore"));

            // check that this finder trigger a flush according to the spec
            home.findById(2l);
            Assert.assertEquals("Entity #1 was not flushed to the DB ", 1, results.getNumberOfCalls(ENTITY_NAME, "1", "ejbStore"));
        }finally{
            ut.commit();
        }

        e1Created.remove();
        e2Created.remove();
    }


    /**
     * Check whether the created entity was not found after removing it within the same Tx. This is to avoid that the entity is
     * deleted from the database but will be returned out of the cache.
     *
     * @throws Exception
     */
    @Test
    public void testFinderAfterRemoveSameTransaction() throws Exception {
        SingletonTestResults results = getResultsSingleton();
        results.resetAll();

        final UserTransaction ut = EJBClient.getUserTransaction(nodeName);
        ut.begin();
        SimpleEntityRemoteHome home = getEntityBeanHome();

        SimpleEntityRemote e1 = home.create(10010l, "Entity #10010");
        e1.remove();

        try {
            home.findByPrimaryKey(10010l);
            Assert.fail("Entity was unexpected found with findByPrimaryKey() after remove");
        } catch (ObjectNotFoundException e) {
            log.info("Entity not found after remove");
        }
        try {
            home.findById(10010l);
            Assert.fail("Entity was unexpected found with finder query after remove");
        } catch (ObjectNotFoundException e) {
            log.info("Entity not found after remove by finder query");
        }
        ut.commit();
    }

    /**
     * Check whether the created and removed entity was not found after removing it in a different Tx.
     * This is to avoid that the entity is deleted from the database but will be returned out of the cache.
     */
    @Test
    public void testFinderAfterRemoveDifferentTx() throws Exception {
        SingletonTestResults results = getResultsSingleton();
        results.resetAll();

        final UserTransaction ut = EJBClient.getUserTransaction(nodeName);
        final SimpleEntityRemoteHome home = getEntityBeanHome();

        ut.begin();
        SimpleEntityRemote e1 = home.create(10l, "Entity #10");
        e1.remove();
        ut.commit();

        try {
            home.findByPrimaryKey(10l);
            Assert.fail("Entity was unexpected found with findByPrimaryKey() after remove");
        }catch(ObjectNotFoundException e) {
            log.info("Entity not found after remove");
        }
        try {
            home.findById(10l);
            Assert.fail("Entity was unexpected found with finder after remove");
        }catch(ObjectNotFoundException e) {
            log.info("Entity not found after remove by finder");
        }
    }
}
