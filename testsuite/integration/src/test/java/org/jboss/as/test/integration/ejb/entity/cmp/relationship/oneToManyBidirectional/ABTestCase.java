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
package org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToManyBidirectional;

import java.util.Collection;
import java.util.Iterator;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.as.test.integration.ejb.entity.cmp.relationship.AbstractRelationshipTest;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CmpTestRunner.class)
public class ABTestCase extends AbstractRelationshipTest {
    static org.jboss.logging.Logger log = org.jboss.logging.Logger.getLogger(ABTestCase.class);

    private AHome getTableAHome() {
        try {
            return (AHome) iniCtx.lookup("java:module/A_OneToMany_Bi_Table_EJB!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToManyBidirectional.AHome");
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getTableAHome: " + e.getMessage());
        }
        return null;
    }

    private BHome getTableBHome() {
        try {
            return (BHome) iniCtx.lookup("java:module/B_OneToMany_Bi_Table_EJB!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToManyBidirectional.BHome");
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getTableBHome: " + e.getMessage());
        }
        return null;
    }

    private AHome getFKAHome() {
        try {
            return (AHome) iniCtx.lookup("java:module/A_OneToMany_Bi_FK_EJB!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToManyBidirectional.AHome");
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getFKAHome: " + e.getMessage());
        }
        return null;
    }

    private BHome getFKBHome() {
        try {
            return (BHome) iniCtx.lookup("java:module/B_OneToMany_Bi_FK_EJB!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToManyBidirectional.BHome");
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getFKBHome: " + e.getMessage());
        }
        return null;
    }

    @Test // a1.setB(a2.getB());
    public void test_a1SetB_a2GetB_Table() throws Exception {
        AHome aHome = getTableAHome();
        BHome bHome = getTableBHome();
        a1SetB_a2GetB(aHome, bHome);
    }

    @Test // a1.setB(a2.getB());
    public void test_a1SetB_a2GetB_FK() throws Exception {
        AHome aHome = getFKAHome();
        BHome bHome = getFKBHome();
        a1SetB_a2GetB(aHome, bHome);
    }

    // a1.setB(a2.getB());
    private void a1SetB_a2GetB(AHome aHome, BHome bHome) throws Exception {
        // Before change:
        A a1 = aHome.create(new Integer(1));
        A a2 = aHome.create(new Integer(2));

        Collection b1 = a1.getB();
        Collection b2 = a2.getB();

        B[] b1x = new B[20];
        B[] b2x = new B[30];

        for (int i = 0; i < b1x.length; i++) {
            b1x[i] = bHome.create(new Integer(10000 + i));
            b1.add(b1x[i]);
        }

        for (int i = 0; i < b2x.length; i++) {
            b2x[i] = bHome.create(new Integer(20000 + i));
            b2.add(b2x[i]);
        }

        // B b11, b12, ... , b1n; members of b1
        for (int i = 0; i < b1x.length; i++) {
            assertTrue(b1.contains(b1x[i]));
        }

        // B b21, b22, ... , b2m; members of b2
        for (int i = 0; i < b2x.length; i++) {
            assertTrue(b2.contains(b2x[i]));
        }

        // Change:
        a1.setB(a2.getB());

        // Expected result:

        // a2.getB().isEmpty()
        assertTrue(a2.getB().isEmpty());

        // b2.isEmpty()
        assertTrue(b2.isEmpty());

        // b1 == a1.getB()
        assertTrue(b1 == a1.getB());

        // b2 == a2.getB()
        assertTrue(b2 == a2.getB());

        // a1.getB().contains(b21)
        // a1.getB().contains(b22)
        // a1.getB().contains(...)
        // a1.getB().contains(b2m)
        for (int i = 0; i < b2x.length; i++) {
            assertTrue(a1.getB().contains(b2x[i]));
        }

        // b11.getA() == null
        // b12.getA() == null
        // ....getA() == null
        // b1n.getA() == null
        for (int i = 0; i < b1x.length; i++) {
            assertTrue(b1x[i].getA() == null);
        }


        // a1.isIdentical(b21.getA())
        // a1.isIdentical(b22.getA())
        // a1.isIdentical(....getA())
        // a1.isIdentical(b2m.getA()))
        for (int i = 0; i < b2x.length; i++) {
            assertTrue(a1.isIdentical(b2x[i].getA()));
        }
    }

    @Test // b2m.setA(b1n.getA());
    public void test_b2mSetA_b1nGetA_Table() throws Exception {
        AHome aHome = getTableAHome();
        BHome bHome = getTableBHome();
        b2mSetA_b1nGetA(aHome, bHome);
    }

    @Test // b2m.setA(b1n.getA());
    public void test_b2mSetA_b1nGetA_FK() throws Exception {
        AHome aHome = getFKAHome();
        BHome bHome = getFKBHome();
        b2mSetA_b1nGetA(aHome, bHome);
    }

    // b2m.setA(b1n.getA());
    public void b2mSetA_b1nGetA(AHome aHome, BHome bHome) throws Exception {
        // Before change:
        A a1 = aHome.create(new Integer(1));
        A a2 = aHome.create(new Integer(2));

        Collection b1 = a1.getB();
        Collection b2 = a2.getB();

        B[] b1x = new B[20];
        B[] b2x = new B[30];

        for (int i = 0; i < b1x.length; i++) {
            b1x[i] = bHome.create(new Integer(10000 + i));
            b1.add(b1x[i]);
        }

        for (int i = 0; i < b2x.length; i++) {
            b2x[i] = bHome.create(new Integer(20000 + i));
            b2.add(b2x[i]);
        }

        // B b11, b12, ... , b1n; members of b1
        for (int i = 0; i < b1x.length; i++) {
            assertTrue(b1.contains(b1x[i]));
        }

        // B b21, b22, ... , b2m; members of b2
        for (int i = 0; i < b2x.length; i++) {
            assertTrue(b2.contains(b2x[i]));
        }

        // Change:

        // b2m.setA(b1n.getA());
        b2x[b2x.length - 1].setA(b1x[b1x.length - 1].getA());

        // Expected result:

        // b1.contains(b11)
        // b1.contains(b12)
        // b1.contains(...)
        // b1.contains(b1n)
        for (int i = 0; i < b1x.length; i++) {
            assertTrue(b1.contains(b1x[i]));
        }

        // b1.contains(b2m)
        assertTrue(b1.contains(b2x[b2x.length - 1]));

        // b2.contains(b21)
        // b2.contains(b22)
        // b2.contains(...)
        // b2.contains(b2m_1)
        for (int i = 0; i < b2x.length - 1; i++) {
            assertTrue(b2.contains(b2x[i]));
        }

        // a1.isIdentical(b11.getA())
        // a1.isIdentical(b12.getA())
        // a1.isIdentical(....getA())
        // a1.isIdentical(b1n.getA())
        for (int i = 0; i < b1x.length; i++) {
            assertTrue(a1.isIdentical(b1x[i].getA()));
        }

        // a2.isIdentical(b21.getA())
        // a2.isIdentical(b22.getA())
        // a2.isIdentical(....getA())
        // a2.isIdentical(b2m_1.getA())
        for (int i = 0; i < b2x.length - 1; i++) {
            assertTrue(a2.isIdentical(b2x[i].getA()));
        }

        // a1.isIdentical(b2m.getA())
        assertTrue(a1.isIdentical(b2x[b2x.length - 1].getA()));
    }

    @Test // a1.getB().add(b2m);
    public void test_a1GetB_addB2m_Table() throws Exception {
        AHome aHome = getTableAHome();
        BHome bHome = getTableBHome();
        a1GetB_addB2m(aHome, bHome);
    }

    @Test // a1.getB().add(b2m);
    public void test_a1GetB_addB2m_FK() throws Exception {
        AHome aHome = getFKAHome();
        BHome bHome = getFKBHome();
        a1GetB_addB2m(aHome, bHome);
    }

    // a1.getB().add(b2m);
    private void a1GetB_addB2m(AHome aHome, BHome bHome) throws Exception {
        // Before change:
        A a1 = aHome.create(new Integer(1));
        A a2 = aHome.create(new Integer(2));

        Collection b1 = a1.getB();
        Collection b2 = a2.getB();

        B[] b1x = new B[20];
        B[] b2x = new B[30];

        for (int i = 0; i < b1x.length; i++) {
            b1x[i] = bHome.create(new Integer(10000 + i));
            b1.add(b1x[i]);
        }

        for (int i = 0; i < b2x.length; i++) {
            b2x[i] = bHome.create(new Integer(20000 + i));
            b2.add(b2x[i]);
        }

        // B b11, b12, ... , b1n; members of b1
        for (int i = 0; i < b1x.length; i++) {
            assertTrue(b1.contains(b1x[i]));
        }

        // B b21, b22, ... , b2m; members of b2
        for (int i = 0; i < b2x.length; i++) {
            assertTrue(b2.contains(b2x[i]));
        }

        // Change:

        // a1.getB().add(b2m);
        a1.getB().add(b2x[b2x.length - 1]);

        // Expected result:

        // b1.contains(b11)
        // b1.contains(b12)
        // b1.contains(...)
        // b1.contains(b1n)
        for (int i = 0; i < b1x.length; i++) {
            assertTrue(b1.contains(b1x[i]));
        }

        // b1.contains(b2m)
        assertTrue(b1.contains(b2x[b2x.length - 1]));

        // b2.contains(b21)
        // b2.contains(b22)
        // b2.contains(...)
        // b2.contains(b2m_1)
        for (int i = 0; i < b2x.length - 1; i++) {
            assertTrue(b2.contains(b2x[i]));
        }

        // a1.isIdentical(b11.getA())
        // a1.isIdentical(b12.getA())
        // a1.isIdentical(....getA())
        // a1.isIdentical(b1n.getA())
        for (int i = 0; i < b1x.length; i++) {
            assertTrue(a1.isIdentical(b1x[i].getA()));
        }

        // a2.isIdentical(b21.getA())
        // a2.isIdentical(b22.getA())
        // a2.isIdentical(....getA())
        // a2.isIdentical(b2m_1.getA())
        for (int i = 0; i < b2x.length - 1; i++) {
            assertTrue(a2.isIdentical(b2x[i].getA()));
        }

        // a1.isIdentical(b2m.getA())
        assertTrue(a1.isIdentical(b2x[b2x.length - 1].getA()));
    }

    @Test // a1.getB().remove(b1n);
    public void test_a1GetB_removeB1n_Table() throws Exception {
        AHome aHome = getTableAHome();
        BHome bHome = getTableBHome();
        a1GetB_removeB1n(aHome, bHome);
    }

    @Test // a1.getB().remove(b1n);
    public void test_a1GetB_removeB1n_FK() throws Exception {
        AHome aHome = getFKAHome();
        BHome bHome = getFKBHome();
        a1GetB_removeB1n(aHome, bHome);
    }

    // a1.getB().remove(b1n);
    private void a1GetB_removeB1n(AHome aHome, BHome bHome) throws Exception {
        // Before change:
        A a1 = aHome.create(new Integer(1));
        A a2 = aHome.create(new Integer(2));

        Collection b1 = a1.getB();
        Collection b2 = a2.getB();

        B[] b1x = new B[20];
        B[] b2x = new B[30];

        for (int i = 0; i < b1x.length; i++) {
            b1x[i] = bHome.create(new Integer(10000 + i));
            b1.add(b1x[i]);
        }

        for (int i = 0; i < b2x.length; i++) {
            b2x[i] = bHome.create(new Integer(20000 + i));
            b2.add(b2x[i]);
        }

        // B b11, b12, ... , b1n; members of b1
        for (int i = 0; i < b1x.length; i++) {
            assertTrue(b1.contains(b1x[i]));
        }

        // B b21, b22, ... , b2m; members of b2
        for (int i = 0; i < b2x.length; i++) {
            assertTrue(b2.contains(b2x[i]));
        }

        // Change:

        // a1.getB().remove(b1n);
        a1.getB().remove(b1x[b1x.length - 1]);

        // Expected result:

        // b1n.getA() == null
        assertTrue(b1x[b1x.length - 1].getA() == null);

        // b1 == a1.getB()
        assertTrue(b1 == a1.getB());

        // b1.contains(b11)
        // b1.contains(b12)
        // b1.contains(...)
        // b1.contains(b1n_1)
        for (int i = 0; i < b1x.length - 1; i++) {
            assertTrue(b1.contains(b1x[i]));
        }

        // !(b1.contains(b1n))
        assertTrue(!(b1.contains(b1x[b1x.length - 1])));
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



