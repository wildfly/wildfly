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
package org.jboss.as.test.integration.ejb.remote.entity.cmp.commerce;

import static org.junit.Assert.fail;

import javax.ejb.EJBHome;
import javax.ejb.ObjectNotFoundException;
import java.util.Iterator;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class CascadeDeleteTestCase {

    private static final String APP_NAME = "cmp-commerce";
    private static final String MODULE_NAME = "ejb";

    @Deployment
    public static Archive<?> deploy() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(CascadeDeleteTestCase.class.getPackage());
        jar.addAsManifestResource("ejb/remote/entity/cmp/commerce/ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource("ejb/remote/entity/cmp/commerce/jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
        ear.addAsModule(jar);
        return ear;
    }

    private OrderHome getOrderHome() {
        return getHome(OrderHome.class, "OrderEJB");
    }

    private ProductCategoryHome getProductCategoryHome() {
        return getHome(ProductCategoryHome.class, "ProductCategoryEJB");
    }

    private ProductCategoryHome getProductCategoryBatchDeleteHome() {
        return getHome(ProductCategoryHome.class, "ProductCategoryBatchDeleteEJB");
    }

    private ProductCategoryTypeHome getProductCategoryTypeHome() {
        return getHome(ProductCategoryTypeHome.class, "ProductCategoryTypeEJB");
    }

    private ProductCategoryTypeHome getProductCategoryTypeBatchDeleteHome() {
        return getHome(ProductCategoryTypeHome.class, "ProductCategoryTypeBatchDeleteEJB");
    }

    private LineItemHome getLineItemHome() {
        return getHome(LineItemHome.class, "LineItemEJB");
    }

    private AddressHome getAddressHome() {
        return getHome(AddressHome.class, "AddressEJB");
    }

    private <T extends EJBHome> T getHome(final Class<T> homeClass, final String beanName) {
        final EJBHomeLocator<T> locator = new EJBHomeLocator<T>(homeClass, APP_NAME, MODULE_NAME, beanName, "");
        return EJBClient.createProxy(locator);
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
        order.setShippingAddressId(ship.getId());

        Long billId = new Long(88888);
        Address bill = addressHome.create(billId);
        bill.setState("CA");
        order.setBillingAddressId(bill.getId());

        // lineItemId and shipId are the same to check for
        // weird cascade delete problems
        Long lineItemId = shipId;
        LineItem lineItem = lineItemHome.create(lineItemId);
        lineItem.setOrderId(order.getOrdernumber());

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

        tearDownEjb();
    }

    @Test
    public void testCategory_Type() throws Exception {
        ProductCategoryHome ch = getProductCategoryHome();

        ProductCategory parent = ch.create();
        CompositeId parentId = parent.getPK();

        ProductCategory child = ch.create();
        child.setParentId(parent.getPK());
        CompositeId childId = child.getPK();

        ProductCategory grandChild = ch.create();
        grandChild.setParentId(parent.getPK());
        CompositeId grandChildId = grandChild.getPK();

        ProductCategoryTypeHome th = getProductCategoryTypeHome();
        ProductCategoryType type = th.create();
        parent.setTypeId(type.getId());
        child.setTypeId(type.getId());
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
        tearDownEjb();
    }

    @Test
    public void testCategory_Type_BatchCascadeDelete() throws Exception {
        ProductCategoryHome ch = getProductCategoryBatchDeleteHome();

        ProductCategory parent = ch.create();
        CompositeId parentId = parent.getPK();

        ProductCategory child = ch.create();
        child.setParentId(parent.getPK());
        CompositeId childId = child.getPK();

        ProductCategory grandChild = ch.create();
        grandChild.setParentId(parent.getPK());
        CompositeId grandChildId = grandChild.getPK();

        ProductCategoryTypeHome th = getProductCategoryTypeBatchDeleteHome();
        ProductCategoryType type = th.create();
        System.out.println(type.getId());
        System.out.println(type.getPrimaryKey());
        parent.setTypeIdBatch(type.getId());
        child.setTypeIdBatch(type.getId());
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
        tearDownEjb();
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
