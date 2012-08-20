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
package org.jboss.as.test.integration.ejb.entity.cmp.relationship.manyToOneUnidirectional;

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
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "mto1uni-cmp-relationship.jar");
        jar.addPackage(ABTestCase.class.getPackage());
        jar.addAsManifestResource(ABTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(ABTestCase.class.getPackage(), "jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
        AbstractCmpTest.addDeploymentAssets(jar);
        return jar;
    }

    private AHome getTableAHome() {
        try {
            return (AHome) iniCtx.lookup("java:module/A_ManyToOne_Uni_Table_EJB!org.jboss.as.test.integration.ejb.entity.cmp.relationship.manyToOneUnidirectional.AHome");
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getTableAHome: " + e.getMessage());
        }
        return null;
    }

    private BHome getTableBHome() {
        try {
            return (BHome) iniCtx.lookup("java:module/B_ManyToOne_Uni_Table_EJB!org.jboss.as.test.integration.ejb.entity.cmp.relationship.manyToOneUnidirectional.BHome");
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getTableBHome: " + e.getMessage());
        }
        return null;
    }

    private AHome getFKAHome() {
        try {
            return (AHome) iniCtx.lookup("java:module/A_ManyToOne_Uni_FK_EJB!org.jboss.as.test.integration.ejb.entity.cmp.relationship.manyToOneUnidirectional.AHome");
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getFKAHome: " + e.getMessage());
        }
        return null;
    }

    private BHome getFKBHome() {
        try {
            return (BHome) iniCtx.lookup("java:module/B_ManyToOne_Uni_FK_EJB!org.jboss.as.test.integration.ejb.entity.cmp.relationship.manyToOneUnidirectional.BHome");
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getFKBHome: " + e.getMessage());
        }
        return null;
    }

    @Test // b1j.setA(b2k.getA());
    public void test_b1jSetA_b2kGetA_Table() throws Exception {
        AHome aHome = getTableAHome();
        BHome bHome = getTableBHome();
        b1jSetA_b2kGetA(aHome, bHome);
    }

    @Test // b1j.setA(b2k.getA());
    public void test_b1jSetA_b2kGetA_FK() throws Exception {
        AHome aHome = getFKAHome();
        BHome bHome = getFKBHome();
        b1jSetA_b2kGetA(aHome, bHome);
    }

    // b1j.setA(b2k.getA());
    private void b1jSetA_b2kGetA(AHome aHome, BHome bHome) throws Exception {
        // Before change:
        A a1 = aHome.create(new Integer(1));
        A a2 = aHome.create(new Integer(2));

        B[] b1x = new B[20];
        B[] b2x = new B[30];

        for (int i = 0; i < b1x.length; i++) {
            b1x[i] = bHome.create(new Integer(10000 + i));
            b1x[i].setA(a1);
        }

        for (int i = 0; i < b2x.length; i++) {
            b2x[i] = bHome.create(new Integer(20000 + i));
            b2x[i].setA(a2);
        }

        // (a1.isIdentical(b11.getA())) && ... && (a1.isIdentical(b1n.getA()
        for (int i = 0; i < b1x.length; i++) {
            a1.isIdentical(b1x[i].getA());
        }

        // (a2.isIdentical(b21.getA())) && ... && (a2.isIdentical(b2m.getA()
        for (int i = 0; i < b2x.length; i++) {
            a2.isIdentical(b2x[i].getA());
        }

        // Change:

        // b1j.setA(b2k.getA());
        int j = b1x.length / 3;
        int k = b2x.length / 2;
        b1x[j].setA(b2x[k].getA());

        // Expected result:

        // a1.isIdentical(b11.getA())
        // a1.isIdentical(b12.getA())
        // ...
        // a2.isIdentical(b1j.getA())
        // ...
        // a1.isIdentical(b1n.getA())
        for (int i = 0; i < b1x.length; i++) {
            if (i != j) {
                assertTrue(a1.isIdentical(b1x[i].getA()));
            } else {
                assertTrue(a2.isIdentical(b1x[i].getA()));
            }
        }

        // a2.isIdentical(b21.getA())
        // a2.isIdentical(b22.getA())
        // ...
        // a2.isIdentical(b2k.getA())
        // ...
        // a2.isIdentical(b2m.getA())
        for (int i = 0; i < b2x.length; i++) {
            assertTrue(a2.isIdentical(b2x[i].getA()));
        }
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



