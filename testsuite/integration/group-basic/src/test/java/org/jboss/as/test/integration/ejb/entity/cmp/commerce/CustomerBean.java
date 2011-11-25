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
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.FinderException;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

public abstract class CustomerBean implements EntityBean {
    transient private EntityContext ctx;

    private static long id = 0;

    public Long ejbCreate() throws CreateException {
        setId(id++);
        return null;
    }

    public void ejbPostCreate() {
    }

    public abstract Long getId();

    public abstract void setId(Long id);

    public abstract String getName();

    public abstract void setName(String name);

    public User getUser() {
        return userLocalToUser(getUserLocal());
    }

    public void setUser(User user) {
        setUserLocal(userToUserLocal(user));
    }

    public abstract UserLocal getUserLocal();

    public abstract void setUserLocal(UserLocal user);

    public abstract Collection getOrders();

    public abstract void setOrders(Collection c);

    public abstract Collection getAddresses();

    public abstract void setAddresses(Collection c);

    public abstract Collection ejbSelectAddressesInCAForCustomer(Long id)
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

    protected User userLocalToUser(UserLocal userLocal) {
        if (userLocal == null) {
            return null;
        }
        UserHome userHome = getUserHome();
        try {
            return userHome.findByPrimaryKey((String) userLocal.getPrimaryKey());
        } catch (Exception e) {
            throw new EJBException("Error converting user local into user", e);
        }
    }

    protected UserLocal userToUserLocal(User user) {
        if (user == null) {
            return null;
        }
        UserLocalHome userLocalHome = getUserLocalHome();
        try {
            return userLocalHome.findByPrimaryKey((String) user.getPrimaryKey());
        } catch (Exception e) {
            throw new EJBException("Error converting user into user local", e);
        }
    }


    private UserHome getUserHome() {
        try {
            InitialContext jndiContext = new InitialContext();

            Object ref = jndiContext.lookup("java:comp/env/ejb/User");

            // remote interfaces have to be narrowed
            return (UserHome) PortableRemoteObject.narrow(ref, UserHome.class);
        } catch (Exception e) {
            throw new EJBException("Exception in getUserHome: ", e);
        }
    }

    private UserLocalHome getUserLocalHome() {
        try {
            InitialContext jndiContext = new InitialContext();

            return (UserLocalHome)
                    jndiContext.lookup("java:comp/env/ejb/UserLocal");
        } catch (Exception e) {
            throw new EJBException("Exception in getUserLocalHome: ", e);
        }
    }

}
