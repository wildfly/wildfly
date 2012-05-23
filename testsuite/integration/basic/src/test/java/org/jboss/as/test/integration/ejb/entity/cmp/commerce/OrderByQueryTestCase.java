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

import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.ejb.entity.cmp.AbstractCmpTest;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CmpTestRunner.class)
public class OrderByQueryTestCase extends AbstractCmpTest {
	
    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "cmp-commerce.jar");
        jar.addPackage(OrderByQueryTestCase.class.getPackage());
        jar.addAsManifestResource(OrderByQueryTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(OrderByQueryTestCase.class.getPackage(), "jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
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

    @Test
  	@SuppressWarnings("unchecked")
    public void testSortedCollectionFinderMethod() {
        OrderHome orderHome = getOrderHome();
        try {
        	for (int i=1000; i<2000; i++) {
        		Order order = orderHome.create();
        		order.setOrderStatus("" + i);
        	}
 			Collection<Order> col = orderHome.findCollectionSortedByStatus();
    		Assert.assertEquals(1000, col.size());
    		// check that returned results are sorted
    		Integer previousValue = null;
    		for (Order order : col) {
    			Integer value = Integer.parseInt(order.getOrderStatus());
    			if (previousValue != null)
    				Assert.assertTrue("Returned entities should be sorted",
    						value > previousValue);
    			previousValue = value;
    		}
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error in testSortedCollectionFinderMethod: " + e.getMessage() + " " + e.toString());
        }
    }

    @Test
  	@SuppressWarnings("unchecked")
    public void testSortedEnumerationFinderMethod() {
        OrderHome orderHome = getOrderHome();
        try {
        	for (int i=1000; i<2000; i++) {
        		Order order = orderHome.create();
        		order.setOrderStatus("" + i);
        	}
 			Enumeration<Order> enumeration = orderHome.findEnumerationSortedByStatus();
    		Assert.assertTrue("Returned enumeration must not be empty", enumeration.hasMoreElements());
    		// check that returned results are sorted
    		Integer previousValue = null;
    		while (enumeration.hasMoreElements()) {
    			Order order = enumeration.nextElement();
    			Integer value = Integer.parseInt(order.getOrderStatus());
    			if (previousValue != null)
    				Assert.assertTrue("Returned entities should be sorted",
    						value > previousValue);
    			previousValue = value;
    		}
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error in testSortedEnumerationFinderMethod");
        }
    }

    public void setUpEjb() throws Exception {
        deleteAllOrders(getOrderHome());
    }

    public void tearDownEjb() throws Exception {
        deleteAllOrders(getOrderHome());
    }

    public void deleteAllOrders(OrderHome orderHome) throws Exception {
        Iterator orders = orderHome.findAll().iterator();
        while (orders.hasNext()) {
            Order order = (Order) orders.next();
            order.remove();
        }
    }
    
}



