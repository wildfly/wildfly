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

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.FinderException;
import javax.naming.InitialContext;
import org.jboss.as.naming.context.NamespaceContextSelector;
import static org.junit.Assert.fail;


public abstract class LineItemBean implements EntityBean {
    transient private EntityContext ctx;

    private static long id = 0;

    public Long ejbCreate() throws CreateException {
        setId(id++);
        return null;
    }

    public void ejbPostCreate() throws CreateException {
        try {
            System.out.println("Current: " + NamespaceContextSelector.getCurrentSelector());
            ProductLocalHome ph = (ProductLocalHome) new InitialContext().lookup("java:module/ProductEJB!" + ProductLocalHome.class.getName());
            ProductLocal p = ph.create();
        } catch (CreateException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CreateException("hosed");
        }
    }

    public Long ejbCreate(Long id) throws CreateException {
        setId(id);
        return null;
    }

    public void ejbPostCreate(Long id) throws CreateException {
    }

    public Long ejbCreate(OrderLocal order) throws CreateException {
        setId(id++);
        return null;
    }

    public void ejbPostCreate(OrderLocal order) throws CreateException {
        order.getLineItems().add(ctx.getEJBLocalObject());
    }

    public Long ejbCreateWithOrderId(Long orderId) throws CreateException {
        setId(id++);
        return null;
    }

    public void ejbPostCreateWithOrderId(Long orderId) throws CreateException {
        try {
            getOrderHome().findByPrimaryKey(orderId).getLineItems().add(ctx.getEJBLocalObject());
        } catch (FinderException e) {
            throw new CreateException(e.getMessage());
        }
    }

    public void setOrderId(Long id) {
        try {
            setOrder(getOrderHome().findByPrimaryKey(id));
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }

    public Object getOrderNumber() {
        return getOrder().getOrdernumber();
    }

    public abstract Long getId();

    public abstract void setId(Long id);

    public abstract OrderLocal getOrder();

    public abstract void setOrder(OrderLocal o);

    public abstract ProductLocal getProduct();

    public abstract void setProduct(ProductLocal p);

    public abstract int getQuantity();

    public abstract void setQuantity(int q);

    public abstract boolean getShipped();

    public abstract void setShipped(boolean shipped);

    public void setEntityContext(EntityContext ctx) {
        this.ctx = ctx;
    }

    public void unsetEntityContext() {
        this.ctx = null;
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }

    public void ejbLoad() {
    }

    public void ejbStore() {
    }

    public void ejbRemove() {
    }

    public void ejbHomeResetId() {
        id = 0;
    }

    private OrderLocalHome getOrderHome() {
        try {
            return (OrderLocalHome) new InitialContext().lookup("java:module/OrderEJB!" + OrderLocalHome.class.getName());
        } catch (Exception e) {
            fail("Exception in getOrderHome: " + e.getMessage());
        }
        return null;
    }
}
