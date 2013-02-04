/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.fail;

import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.ejb.entity.cmp.AbstractCmpTest;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <p>
 * Check whether ejbStore is called during creating an entity bean. Validate if the findByPrimaryKey is optimized to not call DB
 * sync by check whether the ejbStore is called if the findByPrimaryKey is executed. And ensure that ejbStore is called if a
 * finder different to findByPrimaryKey is called. The creation and the test will be called within the same transaction.
 * </p>
 * <p>
 * Second test ensure that the entity is not longer found if remove and findByPrimaryKey will be called in the same transaction.
 *
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 */
@RunWith(CmpTestRunner.class)
public class CheckSameTxUnitTestCase extends AbstractCmpTest {
    private static Logger log = Logger.getLogger(CheckSameTxUnitTestCase.class);

    private static final String ARCHIVE_NAME = "cmp2-findByPK.jar";
    private static final String ENTITY_LOOKUP = "java:module/SimpleEntity!"+SimpleEntityLocalHome.class.getName();

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME);
        jar.addPackage(CheckSameTxUnitTestCase.class.getPackage());
        jar.addAsManifestResource(CheckSameTxUnitTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(CheckSameTxUnitTestCase.class.getPackage(), "jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
        AbstractCmpTest.addDeploymentAssets(jar);
        return jar;
    }

    private SimpleEntityLocalHome getHome() {
        try {
            return (SimpleEntityLocalHome) iniCtx.lookup(ENTITY_LOOKUP);
        } catch (Exception e) {
            log.error("failed", e);
            fail("Exception in getHome: " + e.getMessage());
        }
        return null;
    }

    /**
     * From the spec a call to a finder must flush all entities to the database to ensure a correct result for the query. This
     * is not necessary for the 'findByPrimaryKey' method and will be a performance improvement.
     *
     * The test checks whether the entity is not flushed during calls of findByPrimaryKey.
     *
     * @throws FinderException
     */
    @Test
    public void testDbSync() throws Exception {
        SimpleEntityLocalHome simpleHome = getHome();

        log.debug("create Enity #10001");
        SimpleEntityLocal e1 = simpleHome.create(10001l, "Entity 10001");
        Assert.assertTrue("ejbStore not called after create!", 1 == TestResults.getNumberOfCalls("SimpleEntity#10001.ejbStore"));
        log.debug("create Enity #10002");
        SimpleEntityLocal e2 = simpleHome.create(10002l, "Entity 10002");

        log.debug("change Enity #10001");
        e1.setName("Entity 10001 changed");
        Assert.assertTrue("ejbStore called unexpected after change!",
                1 == TestResults.getNumberOfCalls("SimpleEntity#10001.ejbStore"));

        log.debug("findByPKey Enity #10002");
        e2 = simpleHome.findByPrimaryKey(10002l);
        Assert.assertTrue("Entity #1 was unexpected flushed to the DB",
                1 == TestResults.getNumberOfCalls("SimpleEntity#10001.ejbStore"));

        log.debug("findById Enity #10002");
        e2 = simpleHome.findById(10002l);
        Assert.assertTrue("Entity #1 was not flushed to the DB",
                2 == TestResults.getNumberOfCalls("SimpleEntity#10001.ejbStore"));
        log.debug("leaving test");
    }

    /**
     * Check whether the created entity was not found after removing it within the same Tx. This is to avoid that the entity is
     * deleted from the database but will be returned out of the cache.
     *
     * @throws Exception
     */
    @Test
    public void testFinderAfterRemove() throws Exception {
        SimpleEntityLocalHome simpleHome = getHome();

        SimpleEntityLocal e1 = simpleHome.create(10010l, "Entity #10010");

        e1.remove();

        try {
            simpleHome.findByPrimaryKey(10010l);
            Assert.fail("Entity was unexpected found with findByPrimaryKey() after remove");
        } catch (ObjectNotFoundException e) {
            log.info("Entity not found after remove");
        }
        try {
            simpleHome.findById(10010l);
            Assert.fail("Entity was unexpected found with finder query after remove");
        } catch (ObjectNotFoundException e) {
            log.info("Entity not found after remove by finder query");
        }
    }
}