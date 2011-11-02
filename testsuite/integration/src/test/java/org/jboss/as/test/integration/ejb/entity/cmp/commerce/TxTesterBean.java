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

import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
public class TxTesterBean implements TxTester{
    private SessionContext ctx;
    @EJB
    private OrderHome orderHome;
    @EJB
    private LineItemHome lineItemHome;

//    public void ejbCreate() throws CreateException {
//        try {
//            InitialContext jndiContext = new InitialContext();
//
//            orderHome = (OrderHome) jndiContext.lookup("java:module/OrderEJB!org.jboss.as.test.integration.ejb.entity.cmp.commerce.OrderHome");
//            lineItemHome = (LineItemHome) jndiContext.lookup("java:module/LineItemEJB!org.jboss.as.test.integration.ejb.entity.cmp.commerce.LineItemHome");
//        } catch (Exception e) {
//            throw new CreateException("Error getting OrderHome and " +
//                    "LineItemHome: " + e.getMessage());
//        }
//    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean accessCMRCollectionWithoutTx() {
        Order o;
        LineItem l1;
        LineItem l2;

        // create something to work with
        try {
            o = orderHome.create();
            l1 = lineItemHome.create();
            l2 = lineItemHome.create();
        } catch (CreateException ex) {
            throw new EJBException(ex);
        }

        // this should work
        l1.setOrder(o);


        // this should throw an IllegalStateException
        Collection c = o.getLineItems();
        try {
            c.add(l2);
        } catch (IllegalStateException ex) {
            return true;
        }
        return false;
    }

    public void setSessionContext(SessionContext ctx) {
        this.ctx = ctx;
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }

    public void ejbRemove() {
    }
}
