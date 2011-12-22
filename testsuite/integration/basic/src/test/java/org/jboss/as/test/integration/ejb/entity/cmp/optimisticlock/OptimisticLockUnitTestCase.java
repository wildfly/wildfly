/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.ejb.entity.cmp.AbstractCmpTest;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.bug1006723.testentity.EntityABean;
import org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.bug1006723.testsession.TestSession;
import org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.bug1006723.testsession.TestSessionHome;
import org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.interfaces.CmpEntityLocal;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * This class tests optimistic locking with different strategies.
 *
 * @author <a href="mailto:aloubyansky@hotmail.com">Alex Loubyansky</a>
 */
@RunWith(CmpTestRunner.class)
public class OptimisticLockUnitTestCase extends AbstractCmpTest {
    // Constants -------------------------------------
    private static final String ENTITY_GROUP_LOCKING = "java:module/EntityGroupLocking!org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.interfaces.CmpEntityLocalHome";
    private static final String ENTITY_MODIFIED_LOCKING = "java:module/EntityModifiedLocking!org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.interfaces.CmpEntityLocalHome";
    private static final String ENTITY_READ_LOCKING = "java:module/EntityReadLocking!org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.interfaces.CmpEntityLocalHome";
    private static final String ENTITY_VERSION_LOCKING = "java:module//EntityVersionLocking!org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.interfaces.CmpEntityLocalHome";
    private static final String ENTITY_EXPLICIT_VERSION_LOCKING = "java:module//EntityExplicitVersionLocking!org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.interfaces.CmpEntityLocalHome";
    private static final String ENTITY_TIMESTAMP_LOCKING = "java:module//EntityTimestampLocking!org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.interfaces.CmpEntityLocalHome";
    private static final String ENTITY_KEYGEN_LOCKING = "java:module//EntityKeyGeneratorLocking!org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.interfaces.CmpEntityLocalHome";

    // Attributes ------------------------------------
    private FacadeBean facade;
    /**
     * entity primary key value
     */
    private static final Integer id = 1;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "cmp-optimisticlock.jar");
        jar.addPackage(OptimisticLockUnitTestCase.class.getPackage());
        jar.addPackage(EntityABean.class.getPackage());
        jar.addPackage(TestSession.class.getPackage());
        jar.addPackage(CmpEntityBean.class.getPackage());
        jar.addPackage(CmpEntityLocal.class.getPackage());
        jar.addAsManifestResource("ejb/entity/cmp/optimisticlock/ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource("ejb/entity/cmp/optimisticlock/jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
        AbstractCmpTest.addDeploymentAssets(jar);
        return jar;
    }

    // Tests -----------------------------------------
    @Test
    public void testBug1006723() throws Exception {
        TestSessionHome testSessionRemoteHome = (TestSessionHome)iniCtx.lookup("java:global/cmp-optimisticlock/TestSession!org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.bug1006723.testsession.TestSessionHome");
        TestSession testSessionRemote = testSessionRemoteHome.create();
        Long oID = testSessionRemote.setup();
        testSessionRemote.test(oID);
    }

    @Test
    public void testNullLockedFields() throws Exception {
        FacadeBean facade = getFacade();
        facade.createCmpEntity(ENTITY_MODIFIED_LOCKING, id,
                null, new Integer(1), null, "str2", null, new Double(2.2));
        try {
            facade.testNullLockedFields(ENTITY_MODIFIED_LOCKING, id);
        } finally {
            tearDown(ENTITY_MODIFIED_LOCKING);
        }
    }

    @Test
    public void testKeygenStrategyPass() throws Exception {
        setup(ENTITY_KEYGEN_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testKeygenStrategyPass(ENTITY_KEYGEN_LOCKING, id);
        } finally {
            tearDown(ENTITY_KEYGEN_LOCKING);
        }
    }

    @Test
    public void testKeygenStrategyFail() throws Exception {
        setup(ENTITY_KEYGEN_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testKeygenStrategyFail(ENTITY_KEYGEN_LOCKING, id);
            fail("Should have failed to update.");
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            tearDown(ENTITY_KEYGEN_LOCKING);
        }
    }

    @Test
    public void testTimestampStrategyPass() throws Exception {
        setup(ENTITY_TIMESTAMP_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testTimestampStrategyPass(ENTITY_TIMESTAMP_LOCKING, id);
        } finally {
            tearDown(ENTITY_TIMESTAMP_LOCKING);
        }
    }

    @Test
    public void testTimestampStrategyFail() throws Exception {
        setup(ENTITY_TIMESTAMP_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testTimestampStrategyFail(ENTITY_TIMESTAMP_LOCKING, id);
            fail("Should have failed to update.");
        } catch (Exception e) {
        } finally {
            tearDown(ENTITY_TIMESTAMP_LOCKING);
        }
    }

    @Test
    public void testVersionStrategyPass() throws Exception {
        setup(ENTITY_VERSION_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testVersionStrategyPass(ENTITY_VERSION_LOCKING, id);
        } finally {
            tearDown(ENTITY_VERSION_LOCKING);
        }
    }

    @Test
    public void testVerionStrategyFail() throws Exception {
        setup(ENTITY_VERSION_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testVersionStrategyFail(ENTITY_VERSION_LOCKING, id);
            fail("Should have failed to update.");
        } catch (Exception e) {
        } finally {
            tearDown(ENTITY_VERSION_LOCKING);
        }
    }

    @Test
    public void testExplicitVersionStrategyPass() throws Exception {
        setup(ENTITY_EXPLICIT_VERSION_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testVersionStrategyPass(ENTITY_EXPLICIT_VERSION_LOCKING, id);
        } finally {
            tearDown(ENTITY_EXPLICIT_VERSION_LOCKING);
        }
    }

    @Test
    public void testExplicitVerionStrategyFail() throws Exception {
        setup(ENTITY_EXPLICIT_VERSION_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testVersionStrategyFail(ENTITY_EXPLICIT_VERSION_LOCKING, id);
            fail("Should have failed to update.");
        } catch (Exception e) {
        } finally {
            tearDown(ENTITY_EXPLICIT_VERSION_LOCKING);
        }
    }

    @Test
    public void testExplicitVersionUpdateOnSync() throws Exception {
        setup(ENTITY_EXPLICIT_VERSION_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testExplicitVersionUpdateOnSync(ENTITY_EXPLICIT_VERSION_LOCKING, id);
        } catch (Exception e) {
            fail("Locked fields are not updated on sync: " + e.getMessage());
        } finally {
            tearDown(ENTITY_EXPLICIT_VERSION_LOCKING);
        }
    }

    @Test
    public void testGroupStrategyPass() throws Exception {
        setup(ENTITY_GROUP_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testGroupStrategyPass(ENTITY_GROUP_LOCKING, id);
        } finally {
            tearDown(ENTITY_GROUP_LOCKING);
        }
    }

    @Test
    public void testGroupStrategyFail() throws Exception {
        setup(ENTITY_GROUP_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testGroupStrategyFail(ENTITY_GROUP_LOCKING, id);
            fail("Should have failed to update!");
        } catch (Exception e) {
        } finally {
            tearDown(ENTITY_GROUP_LOCKING);
        }
    }

    @Test
    public void testReadStrategyPass() throws Exception {
        setup(ENTITY_READ_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testReadStrategyPass(ENTITY_READ_LOCKING, id);
        } finally {
            tearDown(ENTITY_READ_LOCKING);
        }
    }

    @Test
    public void testReadStrategyFail() throws Exception {
        setup(ENTITY_READ_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testReadStrategyFail(ENTITY_READ_LOCKING, id);
            fail("Should have failed to update.");
        } catch (Exception e) {
        } finally {
            tearDown(ENTITY_READ_LOCKING);
        }
    }

    @Test
    public void testModifiedStrategyPass() throws Exception {
        setup(ENTITY_MODIFIED_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testModifiedStrategyPass(ENTITY_MODIFIED_LOCKING, id);
        } finally {
            tearDown(ENTITY_MODIFIED_LOCKING);
        }
    }

    @Test
    public void testModifiedStrategyFail() throws Exception {
        setup(ENTITY_MODIFIED_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testModifiedStrategyFail(ENTITY_MODIFIED_LOCKING, id);
            fail("Should have failed to update!");
        } catch (Exception e) {
            // expected
        } finally {
            tearDown(ENTITY_MODIFIED_LOCKING);
        }
    }

    @Test
    public void testUpdateLockOnSync() throws Exception {
        setup(ENTITY_VERSION_LOCKING);
        FacadeBean facade = getFacade();
        try {
            facade.testUpdateLockOnSync(ENTITY_VERSION_LOCKING, id);
        } catch (Exception e) {
            fail("Locked fields are not updated on sync!");
        } finally {
            tearDown(ENTITY_VERSION_LOCKING);
        }
    }

    // Private

    private void setup(String jndiName) throws Exception {
        FacadeBean facade = getFacade();
        facade.createCmpEntity(jndiName, id,
                "str1", new Integer(1), new Double(1.1),
                "str2", new Integer(2), new Double(2.2));
    }

    private void tearDown(String jndiName) throws Exception {
        FacadeBean facade = getFacade();
        facade.safeRemove(jndiName, id);
    }

    private FacadeBean getFacade() throws NamingException {
        if (facade == null) {
            facade = (FacadeBean) iniCtx.lookup("java:module/FacadeBean!org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.FacadeBean");
        }
        return facade;
    }
}
