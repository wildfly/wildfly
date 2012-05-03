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
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.FinderException;

public abstract class AddressBean implements EntityBean {
    transient private EntityContext ctx;

    private static long id = 0;

    public Long ejbCreate() throws CreateException {
        setId(id++);
        return null;
    }

    public void ejbPostCreate() {
    }

    public Long ejbCreate(Long id) throws CreateException {
        setId(id);
        return null;
    }

    public void ejbPostCreate(Long id) {
    }

    public abstract Collection ejbSelectAddresses(String street) throws FinderException;

    public abstract Collection ejbSelectAddresses(int zip) throws FinderException;

    public Collection ejbHomeSelectAddresses(String street) throws FinderException {
        return ejbSelectAddresses(street);
    }

    public abstract Long getId();

    public abstract void setId(Long id);

    public abstract String getStreet();

    public abstract void setStreet(String street);

    public abstract String getCity();

    public abstract void setCity(String city);

    public abstract String getState();

    public abstract void setState(String state);

    public abstract int getZip();

    public abstract void setZip(int zip);

    public abstract int getZipPlus4();

    public abstract void setZipPlus4(int zipPlus4);

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
