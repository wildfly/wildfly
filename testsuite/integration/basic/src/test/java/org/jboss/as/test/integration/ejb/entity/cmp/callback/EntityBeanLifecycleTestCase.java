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
 * Testing entity bean lifecycle as it's defined at 10.1.3  Instance Life Cycle (p.293)
 * The test rely on a server without entity-pool. ejbPassivate can not be tested ATM
 *
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EntityBeanLifecycleTestCase {
    private static final Logger log = Logger.getLogger(EntityBeanLifecycleTestCase.class);

    private static final String ENTITY_BEAN_ARCHIVE_NAME = "cmp-lifecycle";
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
                .addAsManifestResource(EntityBeanLifecycleTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml")
                .addAsManifestResource(EntityBeanLifecycleTestCase.class.getPackage(), "jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
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
     * Create an EntityBean and check whether the callback methods ejbCreate, ejbPostCreate and ejbStore are called.
     */
    public void testJustCreate() throws Exception {
        SingletonTestResults results = getResultsSingleton();
        results.resetAll();

        final UserTransaction ut = EJBClient.getUserTransaction(nodeName);
        ut.begin();

        // getting home interface
        SimpleEntityRemoteHome home = getEntityBeanHome();

        final long pKey = 200;
        // creating entity
        SimpleEntityRemote entityBeanCreated = home.create(pKey, "Entity "+pKey);
        ut.commit();

        try {
            Assert.assertTrue("ejbCreate was not called! " + results.showAll(), 1 == results.getNumberOfCalls(ENTITY_NAME, String.valueOf(pKey), "ejbCreate"));
            Assert.assertTrue("ejbPostCreate was not called!", 1 == results.getNumberOfCalls(ENTITY_NAME, String.valueOf(pKey), "ejbPostCreate"));
            Assert.assertTrue("ejbStore was not called after create!", 1 == results.getNumberOfCalls(ENTITY_NAME, String.valueOf(pKey), "ejbStore"));
            Assert.assertTrue("ejbActivate should not run during creation according to the spec!", 0 == results.getNumberOfCalls(ENTITY_NAME, String.valueOf(pKey), "ejbActivate"));
        } finally {
            // removing entity at the end
            entityBeanCreated.remove();
        }
    }


    /**
     * Create an EntityBean and call a business method in the same transaction.
     * Check whether the callback methods ejbCreate, ejbPostCreate and ejbStore are called and the ejbActivate NOT.
     */
    public void testCreateAndWithBusinessMethod() throws Exception {
        SingletonTestResults results = getResultsSingleton();
        results.resetAll();

        final UserTransaction ut = EJBClient.getUserTransaction(nodeName);
        ut.begin();

        // getting home interface
        SimpleEntityRemoteHome home = getEntityBeanHome();

        // creating entity
        final long pKey = 200;
        // creating entity
        SimpleEntityRemote entityBeanCreated = home.create(pKey, "Entity "+pKey);

        try {
            // calling business method in another transaction
            entityBeanCreated.getName();
            ut.commit();

            Assert.assertTrue("ejbCreate was not called! " + results.showAll(), results.isCalled(ENTITY_NAME, String.valueOf(pKey), "ejbCreate"));
            Assert.assertTrue("ejbPostCreate was not called! "+results.showAll(), results.isCalled(ENTITY_NAME, String.valueOf(pKey), "ejbPostCreate"));
            Assert.assertTrue("ejbStore was not called after create! "+results.showAll(), results.isCalled(ENTITY_NAME, String.valueOf(pKey), "ejbStore"));
            Assert.assertFalse("ejbActivate should not run if a business method is called after create according to the spec! "+results.showAll(), results.isCalled(ENTITY_NAME, String.valueOf(pKey), "ejbActivate"));
        } finally {
            // removing entity at the end
            entityBeanCreated.remove();
        }
    }

    /**
     * Check the callback methods invoked if the Entity is loaded by findByPrimaryKey.
     */
    @Test
    public void testFindByPrimaryKeyAndCallBusinessMethod() throws Exception {
        SingletonTestResults results = getResultsSingleton();
        results.resetAll();

        final UserTransaction ut = EJBClient.getUserTransaction(nodeName);
        ut.begin();

        // getting home interface
        SimpleEntityRemoteHome home = getEntityBeanHome();

        // creating entity
        final long pKey = 200;
        // creating entity
        SimpleEntityRemote entityBeanCreated = home.create(pKey, "Entity "+pKey);
        ut.commit();

        // clearing invocation history - we are interested now in what the findByPK will do
        results.resetAll();

        try {
            ut.begin();
            SimpleEntityRemote entityBeanFound = home.findByPrimaryKey(pKey);
            // check callback invocation direct after finder
            Assert.assertFalse("ejbActivate should not invoked if only a findByPrimaryKey is called!", results.isCalled(ENTITY_NAME, String.valueOf(pKey), "ejbActivate"));
            Assert.assertFalse("ejbLoad should not invoked if only a findByPrimaryKey is called! " + results.showAll(), results.isCalled(ENTITY_NAME, String.valueOf(pKey), "ejbLoad"));
            entityBeanFound.getName();
            ut.commit();

            Assert.assertTrue("ejbActivate was not invoked after a business method is called after finder! "+results.showAll(), results.isCalled(ENTITY_NAME, String.valueOf(pKey), "ejbActivate"));
            Assert.assertTrue("ejbLoad was not invoked after a business method is called after finder! "+results.showAll(), results.isCalled(ENTITY_NAME, String.valueOf(pKey), "ejbLoad"));
        } finally {
            // removing entity at the end
            entityBeanCreated.remove();
        }
    }

    /**
     * Check the callback methods invoked if the Entity is loaded by findByPrimaryKey.
     */
    @Test
    public void testFindByIdAndCallBusinessMethod() throws Exception {
        SingletonTestResults results = getResultsSingleton();
        results.resetAll();

        final UserTransaction ut = EJBClient.getUserTransaction(nodeName);
        ut.begin();

        // getting home interface
        SimpleEntityRemoteHome home = getEntityBeanHome();

        // creating entity
        final long pKey = 200;
        // creating entity
        SimpleEntityRemote entityBeanCreated = home.create(pKey, "Entity "+pKey);
        ut.commit();

        // clearing invocation history - we are interested now in what the findByPK will do
        results.resetAll();

        try {
            ut.begin();
            SimpleEntityRemote entityBeanFound = home.findById(pKey);
            Assert.assertFalse("ejbActivate should not invoked if only a findByPrimaryKey is called! " + results.showAll(), results.isCalled(ENTITY_NAME, String.valueOf(pKey), "ejbActivate"));
            Assert.assertFalse("ejbLoad should not run if only a findByPrimaryKey is called! " + results.showAll(), results.isCalled(ENTITY_NAME, String.valueOf(pKey), "ejbLoad"));
            entityBeanFound.getName();
            ut.commit();

            Assert.assertTrue("ejbActivate was not invoked after a business method is called after finder! "+results.showAll(), results.isCalled(ENTITY_NAME, String.valueOf(pKey), "ejbActivate"));
            Assert.assertTrue("ejbLoad was not invoked after a business method is called after finder! "+results.showAll(), results.isCalled(ENTITY_NAME, String.valueOf(pKey), "ejbLoad"));
        } finally {
            // removing entity at the end
            entityBeanCreated.remove();
        }
    }
}
