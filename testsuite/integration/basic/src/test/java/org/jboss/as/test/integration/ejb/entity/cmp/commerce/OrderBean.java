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
import java.util.Set;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.FinderException;

public abstract class OrderBean implements EntityBean {
    transient private EntityContext ctx;

    private static long id = 0;

    public Long ejbCreate() throws CreateException {
        setOrdernumber(id++);
        return null;
    }

    public void ejbPostCreate() {
    }

    public Long ejbCreate(Long id) throws CreateException {
        setOrdernumber(id);
        return null;
    }

    public void ejbPostCreate(Long id) {
    }

    public abstract Long getOrdernumber();

    public abstract void setOrdernumber(Long ordernumber);

    public abstract Card getCreditCard();

    public abstract void setCreditCard(Card card);

    public abstract String getOrderStatus();

    public abstract void setOrderStatus(String orderStatus);

    public abstract Customer getCustomer();

    public abstract void setCustomer(Customer c);

    public abstract Collection getLineItems();

    public abstract void setLineItems(Collection lineItems);

    public abstract Address getShippingAddress();

    public abstract void setShippingAddress(Address shippingAddress);

    public abstract Address getBillingAddress();

    public abstract void setBillingAddress(Address billingAddress);

    public abstract Set ejbSelectOrdersShippedToCA() throws FinderException;

    public abstract Set ejbSelectOrdersShippedToCA2() throws FinderException;

    public abstract Collection ejbSelectOrderShipToStates()
            throws FinderException;

    public abstract Collection ejbSelectOrderShipToStates2()
            throws FinderException;

    public abstract Set ejbSelectAddressesInCA() throws FinderException;

    public abstract Set ejbSelectAddressesInCA2() throws FinderException;

    public Set getOrdersShippedToCA() throws FinderException {
        return ejbSelectOrdersShippedToCA();
    }

    public Set getOrdersShippedToCA2() throws FinderException {
        return ejbSelectOrdersShippedToCA2();
    }

    public Collection getStatesShipedTo() throws FinderException {
        return ejbSelectOrderShipToStates();
    }

    public Collection getStatesShipedTo2() throws FinderException {
        return ejbSelectOrderShipToStates2();
    }

    public Set getAddressesInCA() throws FinderException {
        return ejbSelectAddressesInCA();
    }

    public Set getAddressesInCA2() throws FinderException {
        return ejbSelectAddressesInCA2();
    }

    public Set ejbHomeGetStuff(String jbossQl, Object[] arguments)
            throws FinderException {
        return ejbSelectGeneric(jbossQl, arguments);
    }

    public Set ejbHomeSelectLazy(String jbossQl, Object[] arguments)
            throws FinderException {
        return ejbSelectLazy(jbossQl, arguments);
    }

    public abstract Set ejbSelectGeneric(String jbossQl, Object[] arguments)
            throws FinderException;

    public abstract Set ejbSelectLazy(String jbossQl, Object[] arguments)
            throws FinderException;

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
}
