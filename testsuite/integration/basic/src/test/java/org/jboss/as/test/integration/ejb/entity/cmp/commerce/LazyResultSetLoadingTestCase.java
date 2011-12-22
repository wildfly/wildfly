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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.ejb.entity.cmp.AbstractCmpTest;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81036 $</tt>
 */
@RunWith(CmpTestRunner.class)
public class LazyResultSetLoadingTestCase extends AbstractCmpTest {
    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "cmp-commerce.jar");
        jar.addPackage(LazyResultSetLoadingTestCase.class.getPackage());
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

    private LineItemHome getLineItemHome() {
        try {
            return (LineItemHome) iniCtx.lookup("java:module/LineItemEJB!org.jboss.as.test.integration.ejb.entity.cmp.commerce.LineItemHome");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception in getLineItemHome: " + e.getMessage());
        }
        return null;
    }

    public void setUpEjb() throws Exception {
        OrderHome oh = getOrderHome();
        Order o = oh.create(new Long(1));

        LineItemHome lih = getLineItemHome();
        LineItem li = lih.create(new Long(11));
        o.getLineItems().add(li);

        li = lih.create(new Long(22));
        o.getLineItems().add(li);

        li = lih.create(new Long(33));
        o.getLineItems().add(li);
    }

    public void tearDownEjb() throws Exception {
        getOrderHome().remove(new Long(1));
    }

    @Test
    public void testLazyResultSetLoading() throws Exception {
        final OrderHome oh = getOrderHome();

        // empty result
        Collection col = oh.selectLazy("select object(o) from Address o where o.state='CA'", null);
        assertTrue("Expected empty collection but got " + col.size(), col.isEmpty());

        // collection of results
        col = oh.selectLazy("select object(o) from LineItem o", null);
        assertTrue("Expected 3 line items but got " + col.size(), 3 == col.size());

        Iterator i = col.iterator();
        LineItem removed = (LineItem) i.next();
        i.remove();
        assertTrue("Expected 2 line items but got " + col.size(), 2 == col.size());

        Collection firstPassCol = new ArrayList(2);
        while (i.hasNext()) {
            firstPassCol.add(i.next());
        }

        Collection secondPassCol = new ArrayList(3);
        i = col.iterator();
        while (i.hasNext()) {
            final LineItem li = (LineItem) i.next();
            assertTrue(firstPassCol.contains(li));
            secondPassCol.add(li);
        }
        assertTrue("Expected 2 line items but got " + secondPassCol.size(), secondPassCol.size() == 2);
        secondPassCol.add(removed);
        assertTrue("Expected 3 line items but got " + secondPassCol.size(), secondPassCol.size() == 3);

        // limit & offset
        col = oh.selectLazy("select object(o) from LineItem o offset 1 limit 2", null);
        assertTrue("Expected 2 line items but got " + col.size(), col.size() == 2);
        int count = 0;
        for (i = col.iterator(); i.hasNext(); ) {
            i.next();
            ++count;
        }
        assertTrue("Expected 2 but got " + count, count == 2);
    }
}
