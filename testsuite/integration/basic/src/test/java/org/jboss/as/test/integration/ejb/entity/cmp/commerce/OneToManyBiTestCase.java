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
package org.jboss.as.test.integration.ejb.entity.cmp.commerce;

import java.util.Collection;
import java.util.Iterator;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.ejb.entity.cmp.AbstractCmpTest;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CmpTestRunner.class)
public class OneToManyBiTestCase extends AbstractCmpTest {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "cmp-commerce.jar");
        jar.addPackage(OneToManyBiTestCase.class.getPackage());
        jar.addAsManifestResource("ejb/entity/cmp/commerce/ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource("ejb/entity/cmp/commerce/jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
        AbstractCmpTest.addDeploymentAssets(jar);
        return jar;
    }

    private OrderHome getOrderHome() {
        try {
            return (OrderHome) iniCtx.lookup("java:module/OrderEJB!org.jboss.as.test.integration.ejb.entity.cmp.commerce.OrderHome");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception in getOrder: " + e.getMessage());
        }
        return null;
    }

    private LineItemHome getLineItemHome() {
        try {
            return (LineItemHome) iniCtx.lookup("java:module/LineItemEJB!org.jboss.as.test.integration.ejb.entity.cmp.commerce.LineItemHome");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception in getLineItemHome: " + e.getMessage());
        }
        return null;
    }

    private Order a1;
    private Order a2;

    private Collection b1;
    private Collection b2;

    private LineItem[] b1x = new LineItem[20];
    private LineItem[] b2x = new LineItem[30];

    public void setUpEjb() throws Exception {
        OrderHome orderHome = getOrderHome();
        LineItemHome lineItemHome = getLineItemHome();

        // clean out the db
        deleteAllOrders(orderHome);
        deleteAllLineItems(lineItemHome);
    }

    private void beforeChange(OrderHome orderHome, LineItemHome lineItemHome)
            throws Exception {

        // Before change:
        a1 = orderHome.create();
        a2 = orderHome.create();

        b1 = a1.getLineItems();
        b2 = a2.getLineItems();

        for (int i = 0; i < b1x.length; i++) {
            b1x[i] = lineItemHome.create();
            b1.add(b1x[i]);
        }

        for (int i = 0; i < b2x.length; i++) {
            b2x[i] = lineItemHome.create();
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
    }

    @Test // a1.setB(a2.getB());
    public void test_a1SetB_a2GetB() throws Exception{
        // setup the before change part of the test
        beforeChange(getOrderHome(), getLineItemHome());

        // Change:
        a1.setLineItems(a2.getLineItems());

        // Expected result:

        // a2.getB().isEmpty()
        assertTrue(a2.getLineItems().isEmpty());

        // b2.isEmpty()
        assertTrue(b2.isEmpty());

        // b1 == a1.getB()
        assertTrue(b1 == a1.getLineItems());

        // b2 == a2.getB()
        assertTrue(b2 == a2.getLineItems());

        // a1.getB().contains(b21)
        // a1.getB().contains(b22)
        // a1.getB().contains(...)
        // a1.getB().contains(b2m)
        for (int i = 0; i < b2x.length; i++) {
            assertTrue(a1.getLineItems().contains(b2x[i]));
        }

        // b11.getA() == null
        // b12.getA() == null
        // ....getA() == null
        // b1n.getA() == null
        for (int i = 0; i < b1x.length; i++) {
            assertTrue(b1x[i].getOrder() == null);
        }


        // a1.isIdentical(b21.getA())
        // a1.isIdentical(b22.getA())
        // a1.isIdentical(....getA())
        // a1.isIdentical(b2m.getA()))
        for (int i = 0; i < b2x.length; i++) {
            assertTrue(a1.isIdentical(b2x[i].getOrder()));
        }
    }

    @Test // b2m.setA(b1n.getA());
    public void test_b2mSetA_b1nGetA() throws Exception {
        // setup the before change part of the test
        beforeChange(getOrderHome(), getLineItemHome());

        // Change:

        // b2m.setA(b1n.getA());
        b2x[b2x.length - 1].setOrder(b1x[b1x.length - 1].getOrder());

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
            assertTrue(a1.isIdentical(b1x[i].getOrder()));
        }

        // a2.isIdentical(b21.getA())
        // a2.isIdentical(b22.getA())
        // a2.isIdentical(....getA())
        // a2.isIdentical(b2m_1.getA())
        for (int i = 0; i < b2x.length - 1; i++) {
            assertTrue(a2.isIdentical(b2x[i].getOrder()));
        }

        // a1.isIdentical(b2m.getA())
        assertTrue(a1.isIdentical(b2x[b2x.length - 1].getOrder()));
    }

    @Test // a1.getB().add(b2m);
    public void test_a1GetB_addB2m() throws Exception {
        // setup the before change part of the test
        beforeChange(getOrderHome(), getLineItemHome());

        // Change:

        // a1.getB().add(b2m);
        a1.getLineItems().add(b2x[b2x.length - 1]);

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
            assertTrue(a1.isIdentical(b1x[i].getOrder()));
        }

        // a2.isIdentical(b21.getA())
        // a2.isIdentical(b22.getA())
        // a2.isIdentical(....getA())
        // a2.isIdentical(b2m_1.getA())
        for (int i = 0; i < b2x.length - 1; i++) {
            assertTrue(a2.isIdentical(b2x[i].getOrder()));
        }

        // a1.isIdentical(b2m.getA())
        assertTrue(a1.isIdentical(b2x[b2x.length - 1].getOrder()));
    }

    @Test // a1.getB().remove(b1n);
    public void test_a1GetB_removeB1n() throws Exception {
        // setup the before change part of the test
        beforeChange(getOrderHome(), getLineItemHome());

        // Change:

        // a1.getB().remove(b1n);
        a1.getLineItems().remove(b1x[b1x.length - 1]);

        // Expected result:

        // b1n.getA() == null
        assertTrue(b1x[b1x.length - 1].getOrder() == null);

        // b1 == a1.getB()
        assertTrue(b1 == a1.getLineItems());

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
        OrderHome orderHome = getOrderHome();
        LineItemHome lineItemHome = getLineItemHome();

        // clean out the db
        deleteAllOrders(orderHome);
        deleteAllLineItems(lineItemHome);
    }

    public void deleteAllOrders(OrderHome orderHome) throws Exception {
        // delete all Orders
        Iterator currentOrders = orderHome.findAll().iterator();
        while (currentOrders.hasNext()) {
            Order o = (Order) currentOrders.next();
            o.remove();
        }
    }

    public void deleteAllLineItems(LineItemHome lineItemHome) throws Exception {
        // delete all LineItems
        Iterator currentLineItems = lineItemHome.findAll().iterator();
        while (currentLineItems.hasNext()) {
            LineItem l = (LineItem) currentLineItems.next();
            l.remove();
        }
    }
}



