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

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

public abstract class AEJB implements EntityBean {

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

    // 1xmany
    public abstract Collection getB();

    public abstract void setB(Collection v);


    public boolean isB() {
        return getB().isEmpty() != true;
    }

    public boolean setCmrFieldToNull() {
        try {
            setB(null);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean setCmrFieldToWrongType(Vector v) {
        boolean pass = true;
        try {
            setB(v);
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            pass = false;
        }
        return pass;
    }

    public Collection getBInfo() {
        try {
            Vector v = new Vector();
            if (isB()) {
                Collection bcol = getB();
                Iterator iterator = bcol.iterator();
                while (iterator.hasNext()) {
                    BLocal b = (BLocal) iterator.next();
                    BDVC bDVC = new BDVC(b.getId(),
                            b.getName(),
                            b.getValue());
                    v.add(bDVC);
                }
            }
            return v;
        } catch (Exception e) {
            return null;
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

    public void ejbPostCreate(String id, String name, int value) throws CreateException {
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
