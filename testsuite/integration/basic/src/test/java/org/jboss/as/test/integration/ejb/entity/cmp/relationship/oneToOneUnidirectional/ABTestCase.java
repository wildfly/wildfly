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
package org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneUnidirectional;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.ejb.entity.cmp.AbstractCmpTest;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CmpTestRunner.class)
public class ABTestCase extends AbstractCmpTest {
    static org.jboss.logging.Logger log = org.jboss.logging.Logger.getLogger(ABTestCase.class);

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "1to1uni-cmp-relationship.jar");
        jar.addPackage(ABTestCase.class.getPackage());
        jar.addAsManifestResource(ABTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(ABTestCase.class.getPackage(), "jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");

        AbstractCmpTest.addDeploymentAssets(jar);
        return jar;
    }

    private AHome getTableAHome() {
        try {
            return (AHome) iniCtx.lookup("java:module/A_OneToOne_Uni_Table_EJB!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneUnidirectional.AHome");
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getTableAHome: " + e.getMessage());
        }
        return null;
    }

    private BHome getTableBHome() {
        try {
            return (BHome) iniCtx.lookup("java:module/B_OneToOne_Uni_Table_EJB!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneUnidirectional.BHome");
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getTableBHome: " + e.getMessage());
        }
        return null;
    }

    private AHome getFKAHome() {
        try {
            return (AHome) iniCtx.lookup("java:module/A_OneToOne_Uni_FK_EJB!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneUnidirectional.AHome");
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getFKAHome: " + e.getMessage());
        }
        return null;
    }

    private BHome getFKBHome() {
        try {
            return (BHome) iniCtx.lookup("java:module/B_OneToOne_Uni_FK_EJB!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneUnidirectional.BHome");
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getFKBHome: " + e.getMessage());
        }
        return null;
    }

    private A a1;
    private A a2;
    private B b1;
    private B b2;

    protected void beforeChange(AHome aHome, BHome bHome) throws Exception {
        a1 = aHome.create(new Integer(1));
        a2 = aHome.create(new Integer(2));
        b1 = bHome.create(new Integer(10));
        b2 = bHome.create(new Integer(20));
        a1.setB(b1);
        a2.setB(b2);

        assertTrue(b1.isIdentical(a1.getB()));
        assertTrue(b2.isIdentical(a2.getB()));
    }

    @Test // a1.setB(a2.getB());
    public void test_a1SetB_a2GetB_Table() throws Exception {
        AHome aHome = getTableAHome();
        BHome bHome = getTableBHome();

        beforeChange(aHome, bHome);
        a1SetB_a2GetB(aHome, bHome);
    }

    @Test // a1.setB(a2.getB());
    public void test_a1SetB_a2GetB_FK() throws Exception {
        AHome aHome = getFKAHome();
        BHome bHome = getFKBHome();
        beforeChange(aHome, bHome);
        a1SetB_a2GetB(aHome, bHome);
    }

    // a1.setB(a2.getB());
    protected void a1SetB_a2GetB(AHome aHome, BHome bHome) throws Exception {
        // Change:
        a1.setB(a2.getB());

        // Expected result:

        // b2.isIdentical(a1.getB())
        assertTrue(b2.isIdentical(a1.getB()));

        // a2.getB() == null
        assertNull(a2.getB());
    }

    public void tearDownEjb() throws Exception {
        AHome aHome;
        BHome bHome;

        aHome = getTableAHome();
        bHome = getTableBHome();
        deleteAllAsAndBs(aHome, bHome);

        aHome = getFKAHome();
        bHome = getFKBHome();
        deleteAllAsAndBs(aHome, bHome);
    }

    public void deleteAllAsAndBs(AHome aHome, BHome bHome) throws Exception {
        // delete all As
        Iterator currentAs = aHome.findAll().iterator();
        while (currentAs.hasNext()) {
            A a = (A) currentAs.next();
            a.remove();
        }

        // delete all Bs
        Iterator currentBs = bHome.findAll().iterator();
        while (currentBs.hasNext()) {
            B b = (B) currentBs.next();
            b.remove();
        }
    }
}



