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

import java.util.Collection;
import java.util.Set;
import javax.ejb.EJBLocalObject;
import javax.ejb.FinderException;

public interface OrderLocal extends EJBLocalObject {
    Long getOrdernumber();

    Card getCreditCard();

    void setCreditCard(Card card);

    String getOrderStatus();

    void setOrderStatus(String orderStatus);

    Address getShippingAddress();

    void setShippingAddress(Address address);

    Address getBillingAddress();

    void setBillingAddress(Address address);

    Collection getLineItems();

    void setLineItems(Collection lineItems);

    Set getOrdersShippedToCA() throws FinderException;

    Set getOrdersShippedToCA2() throws FinderException;

    Collection getStatesShipedTo() throws FinderException;

    Collection getStatesShipedTo2() throws FinderException;

    Set getAddressesInCA() throws FinderException;

    Set getAddressesInCA2() throws FinderException;
}
