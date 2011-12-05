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
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;
import javax.naming.InitialContext;

/**
 * @author John Bailey
 */
public abstract class BeanEJB implements EntityBean {

    private static final int NO_RELATION_SET = 0;
    private static final int NULL_RELATION_SET = 1;
    private static final int RELATION_SET = 2;

    // JNDI Names for A and B Local Home Interface
    private static final String ALocal = "java:comp/env/ejb/AEJBLocal";
    private static final String BLocal = "java:comp/env/ejb/BEJBLocal";

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
    public abstract ALocal getA1();

    public abstract void setA1(ALocal v);

    // 1x1
    public abstract ALocal getA2();

    public abstract void setA2(ALocal v);

    // 1x1
    public abstract BLocal getB1();

    public abstract void setB1(BLocal v);

    // 1x1
    public abstract BLocal getB2();

    public abstract void setB2(BLocal v);

    // 1x1
    public abstract BLocal getB3();

    public abstract void setB3(BLocal v);

    // 1x1
    public abstract BLocal getB4();

    public abstract void setB4(BLocal v);

    private boolean nullTest() {
        ALocal aOne = getA1();
        BLocal bOne = getB1();

        ALocal a1 = bOne.getA();
        Collection b1 = aOne.getB();

        if (a1 == null && b1.isEmpty())
            return true;
        else
            return false;
    }

    public boolean test0() {
        return nullTest();
    }

    private ALocal createALocal(String id, String name, int value) throws Exception {
        ALocalHome aLocalHome = (ALocalHome) new InitialContext().lookup(ALocal);
        ALocal aLocal = aLocalHome.create(id, name, value);
        return aLocal;
    }

    private BLocal createBLocal(String id, String name, int value) throws Exception {
        BLocalHome bLocalHome = (BLocalHome) new InitialContext().lookup(BLocal);
        BLocal bLocal = bLocalHome.create(id, name, value);
        return bLocal;
    }


    // ===========================================================
    // EJB Specification Required Methods

    public String ejbCreate(String id, String name, int value,
                            ADVC aOne, BDVC bOne, int flag)
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

    public void ejbPostCreate(String id, String name, int value, ADVC aOne, BDVC bOne, int flag) throws CreateException {
        try {
            ALocal a1 = createALocal(aOne.getId(), aOne.getName(), aOne.getValue());
            setA1(a1);

            BLocal b1 = createBLocal(bOne.getId(), bOne.getName(), bOne.getValue());
            setB1(b1);

            switch (flag) {
                case NO_RELATION_SET:
                    break;
                case NULL_RELATION_SET:
                    b1.setA(null);
                    break;
                case RELATION_SET:
                    Collection c = a1.getB();
                    c.add(b1);
                    break;
            }

        } catch (Exception e) {
            throw new CreateException("Exception occurred: " + e);
        }
    }

    public void setEntityContext(EntityContext c) {
        context = c;
    }

    public void unsetEntityContext() {
    }

    public void ejbRemove() throws RemoveException {
        if (getA1() != null) {
            getA1().remove();
        }
        if (getA2() != null) {
            getA2().remove();
        }
        if (getB1() != null) {
            getB1().remove();
        }
        if (getB2() != null) {
            getB2().remove();
        }
        if (getB3() != null) {
            getB3().remove();
        }
        if (getB4() != null) {
            getB4().remove();
        }
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
