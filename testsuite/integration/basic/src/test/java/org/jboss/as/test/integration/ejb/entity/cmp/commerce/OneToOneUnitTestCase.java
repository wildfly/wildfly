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
public class OneToOneUnitTestCase extends AbstractCmpTest {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "cmp-commerce.jar");
        jar.addPackage(OneToOneUnitTestCase.class.getPackage());
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

    private AddressHome getAddressHome() {
        try {
            return (AddressHome) iniCtx.lookup("java:module/AddressEJB!org.jboss.as.test.integration.ejb.entity.cmp.commerce.AddressHome");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception in getAddressHome: " + e.getMessage());
        }
        return null;
    }

    private Order a1;
    private Order a2;

    private Address b1;
    private Address b2;

    public void setUpEjb() throws Exception {
        OrderHome orderHome = getOrderHome();
        AddressHome addressHome = getAddressHome();

        // clean out the db
        deleteAllOrders(orderHome);
        deleteAllAddresses(addressHome);
    }

    private void beforeChange(OrderHome orderHome, AddressHome addressHome) throws Exception {

        // Before change:
        a1 = orderHome.create();
        a2 = orderHome.create();

        b1 = addressHome.create();
        a1.setShippingAddress(b1);

        b2 = addressHome.create();
        a2.setShippingAddress(b2);

        assertTrue(a1.getShippingAddress().isIdentical(b1));
        assertTrue(a2.getShippingAddress().isIdentical(b2));
    }

    @Test // a1.setB(a2.getB());
    public void test_a1setB_a2getB() throws Exception {
        // setup the before change part of the test
        beforeChange(getOrderHome(), getAddressHome());

        // Change:
        // a1.setB(a2.getB());
        a1.setShippingAddress(a2.getShippingAddress());

        // Expected result:
        // (b2.isIdentical(a1.getB())) && (a2.getB() == null)
        assertTrue(b2.isIdentical(a1.getShippingAddress()));
        assertTrue(a2.getShippingAddress() == null);
    }

    public void deleteAllOrders(OrderHome orderHome) throws Exception {
        // delete all Orders
        Iterator currentOrders = orderHome.findAll().iterator();
        while (currentOrders.hasNext()) {
            Order o = (Order) currentOrders.next();
            o.remove();
        }
    }

    public void deleteAllAddresses(AddressHome addressHome) throws Exception {
        // delete all Addresses
        Iterator currentAddresses = addressHome.findAll().iterator();
        while (currentAddresses.hasNext()) {
            Address a = (Address) currentAddresses.next();
            a.remove();
        }
    }
}



