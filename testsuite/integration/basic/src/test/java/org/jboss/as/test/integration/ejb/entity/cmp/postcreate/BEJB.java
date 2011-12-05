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

package org.jboss.as.test.integration.ejb.entity.cmp.postcreate;


import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

public abstract class BEJB implements EntityBean {

    private EntityContext context = null;

    // ===========================================================
    // getters and setters for cmp fields

    public abstract String getId();

    public abstract void setId(String v);

    public abstract String getName();

    public abstract void setName(String v);

    public abstract int getValue();

    public abstract void setValue(int v);

    // ===========================================================
    // getters and setters for relationship fields

    // 1x1
    public abstract ALocal getA();

    public abstract void setA(ALocal v);

    public boolean isA() {
        return getA() != null;
    }

    public ADVC getAInfo() {
        try {
            if (isA()) {
                ALocal a = getA();
                ADVC aDVC = new ADVC(a.getId(),
                        a.getName(),
                        a.getValue());
                return aDVC;
            } else
                return null;
        } catch (Exception e) {
            throw new EJBException("Exception occurred: " + e);
        }
    }

    // ===========================================================
    // EJB Specification Required Methods

    public String ejbCreate(String id, String name, int value)
            throws CreateException {
        try {
            setId(id);
            setName(name);
            setValue(value);
        } catch (Exception e) {
            throw new CreateException("Exception occurred: " + e);
        }
        return null;
    }

    public void ejbPostCreate(String id, String name, int value)
            throws CreateException {
    }

    public void setEntityContext(EntityContext c) {
        context = c;
    }

    public void unsetEntityContext() {
    }

    public void ejbRemove() throws RemoveException {
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }

    public void ejbLoad() {
    }

    public void ejbStore() {
    }
}
