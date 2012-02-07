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

import javax.ejb.ObjectNotFoundException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.ejb.entity.cmp.AbstractCmpTest;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;

@RunWith(CmpTestRunner.class)
public class CommerceCascadeDeleteTestCase extends AbstractCmpTest {
    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "cmp-commerce.jar");
        jar.addPackage(CommerceCascadeDeleteTestCase.class.getPackage());
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
            fail("Exception in getOrderHome: " + e.getMessage());
        }
        return null;
    }

    private ProductCategoryHome getProductCategoryHome() {
        try {
            return (ProductCategoryHome) iniCtx.lookup("java:module/ProductCategoryEJB!org.jboss.as.test.integration.ejb.entity.cmp.commerce.ProductCategoryHome");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception in getProductCategoryHome: " + e.getMessage());
        }
        return null;
    }

    private ProductCategoryHome getProductCategoryBatchDeleteHome() {
        try {
            return (ProductCategoryHome) iniCtx.lookup("java:module/ProductCategoryBatchDeleteEJB!org.jboss.as.test.integration.ejb.entity.cmp.commerce.ProductCategoryHome");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception in getProductCategoryBatchDeleteHome: " + e.getMessage());
        }
        return null;
    }

    private ProductCategoryTypeHome getProductCategoryTypeHome() {
        try {
            return (ProductCategoryTypeHome) iniCtx.lookup("java:module/ProductCategoryTypeEJB!org.jboss.as.test.integration.ejb.entity.cmp.commerce.ProductCategoryTypeHome");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception in getProductCategoryTypeHome: " + e.getMessage());
        }
        return null;
    }

    private ProductCategoryTypeHome getProductCategoryTypeBatchDeleteHome() {
        try {
            return (ProductCategoryTypeHome) iniCtx.lookup("java:module/ProductCategoryTypeBatchDeleteEJB!org.jboss.as.test.integration.ejb.entity.cmp.commerce.ProductCategoryTypeHome");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception in getProductCategoryTypeBatchDeleteHome: " + e.getMessage());
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

    private AddressHome getAddressHome() {
        try {
            return (AddressHome) iniCtx.lookup("java:module/AddressEJB!org.jboss.as.test.integration.ejb.entity.cmp.commerce.AddressHome");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception in getAddressHome: " + e.getMessage());
        }
        return null;
    }

    @Test
    public void testCascadeDelete() throws Exception {
        OrderHome orderHome = getOrderHome();
        AddressHome addressHome = getAddressHome();
        LineItemHome lineItemHome = getLineItemHome();

        Order order = orderHome.create();
        Long orderNumber = order.getOrdernumber();

        Long shipId = new Long(99999);
        Address ship = addressHome.create(shipId);
        ship.setState("CA");
        order.setShippingAddress(ship);

        Long billId = new Long(88888);
        Address bill = addressHome.create(billId);
        bill.setState("CA");
        order.setBillingAddress(bill);

        // lineItemId and shipId are the same to check for
        // weird cascade delete problems
        Long lineItemId = shipId;
        LineItem lineItem = lineItemHome.create(lineItemId);
        lineItem.setOrder(order);

        order.remove();

        try {
            orderHome.findByPrimaryKey(orderNumber);
            fail("Order should have been deleted");
        } catch (ObjectNotFoundException e) {
            // expected
        }

        try {
            addressHome.findByPrimaryKey(billId);
            fail("Billing address should have been deleted");
        } catch (ObjectNotFoundException e) {
            // expected
        }

        try {
            lineItemHome.findByPrimaryKey(lineItemId);
            fail("Line item should have been deleted");
        } catch (ObjectNotFoundException e) {
            // expected
        }

        try {
            addressHome.findByPrimaryKey(shipId);
            fail("Shipping address should have been deleted");
        } catch (ObjectNotFoundException e) {
            // expected
        }
    }

    @Test
    public void testCategory_Type() throws Exception {
        ProductCategoryHome ch = getProductCategoryHome();

        ProductCategory parent = ch.create();
        CompositeId parentId = parent.getPK();

        ProductCategory child = ch.create();
        child.setParent(parent);
        CompositeId childId = child.getPK();

        ProductCategory grandChild = ch.create();
        grandChild.setParent(parent);
        CompositeId grandChildId = grandChild.getPK();

        ProductCategoryTypeHome th = getProductCategoryTypeHome();
        ProductCategoryType type = th.create();
        parent.setType(type);
        child.setType(type);
        Long typeId = type.getId();

        type.remove();

        try {
            ch.findByPrimaryKey(parentId);
            fail("ProductCategory should have beed deleted.");
        } catch (ObjectNotFoundException e) {
            // expected
        }

        try {
            ch.findByPrimaryKey(childId);
            fail("ProductCategory should have beed deleted.");
        } catch (ObjectNotFoundException e) {
            // expected
        }

        try {
            ch.findByPrimaryKey(grandChildId);
            fail("ProductCategory should have beed deleted.");
        } catch (ObjectNotFoundException e) {
            // expected
        }

        try {
            th.findByPrimaryKey(typeId);
            fail("ProductCategoryType should have beed deleted.");
        } catch (ObjectNotFoundException e) {
            // expected
        }
    }

    @Test
    public void testCategory_Type_BatchCascadeDelete() throws Exception {
        ProductCategoryHome ch = getProductCategoryBatchDeleteHome();

        ProductCategory parent = ch.create();
        CompositeId parentId = parent.getPK();

        ProductCategory child = ch.create();
        child.setParent(parent);
        CompositeId childId = child.getPK();

        ProductCategory grandChild = ch.create();
        grandChild.setParent(parent);
        CompositeId grandChildId = grandChild.getPK();

        ProductCategoryTypeHome th = getProductCategoryTypeBatchDeleteHome();
        ProductCategoryType type = th.create();
        parent.setType(type);
        child.setType(type);
        Long typeId = type.getId();

        type.remove();

        try {
            ch.findByPrimaryKey(parentId);
            fail("ProductCategory should have beed deleted.");
        } catch (ObjectNotFoundException e) {
            // expected
        }

        try {
            ch.findByPrimaryKey(childId);
            fail("ProductCategory should have beed deleted.");
        } catch (ObjectNotFoundException e) {
            // expected
        }

        try {
            ch.findByPrimaryKey(grandChildId);
            fail("ProductCategory should have beed deleted.");
        } catch (ObjectNotFoundException e) {
            // expected
        }

        try {
            th.findByPrimaryKey(typeId);
            fail("ProductCategoryType should have beed deleted.");
        } catch (ObjectNotFoundException e) {
            // expected
        }
    }

    public void tearDownEjb() throws Exception {
        deleteAllOrders(getOrderHome());
        deleteAllLineItems(getLineItemHome());
        deleteAllAddresses(getAddressHome());
        deleteAllCategories(getProductCategoryHome());
    }

    public void deleteAllCategories(ProductCategoryHome catHome) throws Exception {
        Iterator cats = catHome.findAll().iterator();
        while (cats.hasNext()) {
            ProductCategory cat = (ProductCategory) cats.next();
            cat.remove();
        }
        catHome.resetId();
    }

    public void deleteAllOrders(OrderHome orderHome) throws Exception {
        Iterator orders = orderHome.findAll().iterator();
        while (orders.hasNext()) {
            Order order = (Order) orders.next();
            order.remove();
        }
        orderHome.resetId();
    }

    public void deleteAllLineItems(LineItemHome lineItemHome) throws Exception {
        Iterator lineItems = lineItemHome.findAll().iterator();
        while (lineItems.hasNext()) {
            LineItem lineItem = (LineItem) lineItems.next();
            lineItem.remove();
        }
        lineItemHome.resetId();
    }

    public void deleteAllAddresses(AddressHome addressHome) throws Exception {
        Iterator addresses = addressHome.findAll().iterator();
        while (addresses.hasNext()) {
            Address address = (Address) addresses.next();
            address.remove();
        }
        addressHome.resetId();
    }
}
