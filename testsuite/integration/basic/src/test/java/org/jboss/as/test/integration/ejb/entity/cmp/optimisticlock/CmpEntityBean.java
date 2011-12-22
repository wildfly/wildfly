/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock;

import javax.ejb.EntityBean;
import javax.ejb.CreateException;
import javax.ejb.RemoveException;
import javax.ejb.EntityContext;

/**
 * @author <a href="mailto:aloubyansky@hotmail.com">Alex Loubyansky</a>
 * @ejb.bean name="CmpEntity"
 * local-jndi-name="local/CmpEntityBean"
 * view-type="local"
 * type="CMP"
 * reentrant="false"
 * cmp-version="2.x"
 * primkey-field="id"
 * @jboss.create-table "true"
 * @jboss.remove-table "false"
 * @ejb.finder signature="CmpEntityLocal findById(java.lang.Integer id)"
 * query="select object(o) from CmpEntity o where o.id=?1"
 */
public abstract class CmpEntityBean
        implements EntityBean {
    // Attributes ----------------------------------------
    private EntityContext ctx;

    // CMP accessors -------------------------------------

    /**
     * @ejb.pk-field
     * @ejb.persistent-field
     * @ejb.interface-method
     */
    public abstract Integer getId();

    public abstract void setId(Integer id);

    /**
     * @ejb.persistent-field
     * @ejb.interface-method
     */
    public abstract String getStringGroup1();

    /**
     * @ejb.interface-method
     */
    public abstract void setStringGroup1(String stringField);

    /**
     * @ejb.persistent-field
     * @ejb.interface-method
     */
    public abstract Integer getIntegerGroup1();

    /**
     * @ejb.interface-method
     */
    public abstract void setIntegerGroup1(Integer value);

    /**
     * @ejb.persistent-field
     * @ejb.interface-method
     */
    public abstract Double getDoubleGroup1();

    /**
     * @ejb.interface-method
     */
    public abstract void setDoubleGroup1(Double value);

    /**
     * @ejb.persistent-field
     * @ejb.interface-method
     */
    public abstract String getStringGroup2();

    /**
     * @ejb.interface-method
     */
    public abstract void setStringGroup2(String stringField);

    /**
     * @ejb.persistent-field
     * @ejb.interface-method
     */
    public abstract Integer getIntegerGroup2();

    /**
     * @ejb.interface-method
     */
    public abstract void setIntegerGroup2(Integer value);

    /**
     * @ejb.persistent-field
     * @ejb.interface-method
     */
    public abstract Double getDoubleGroup2();

    /**
     * @ejb.interface-method
     */
    public abstract void setDoubleGroup2(Double value);

    /**
     * @ejb.persistent-field
     * @ejb.interface-method
     */
    public abstract Long getVersionField();

    /**
     * @ejb.interface-method
     */
    public abstract void setVersionField(Long value);

    // EntityBean implementation -------------------------

    /**
     * @ejb.create-method
     */
    public Integer ejbCreate(Integer id,
                             String stringGroup1,
                             Integer integerGroup1,
                             Double doubleGroup1,
                             String stringGroup2,
                             Integer integerGroup2,
                             Double doubleGroup2)
            throws CreateException {
        setId(id);
        setStringGroup1(stringGroup1);
        setIntegerGroup1(integerGroup1);
        setDoubleGroup1(doubleGroup1);
        setStringGroup2(stringGroup2);
        setIntegerGroup2(integerGroup2);
        setDoubleGroup2(doubleGroup2);
        return null;
    }

    public void ejbPostCreate(Integer id,
                              String stringGroup1,
                              Integer integerGroup1,
                              Double doubleGroup1,
                              String stringGroup2,
                              Integer integerGroup2,
                              Double doubleGroup2) {
    }

    public void ejbRemove() throws RemoveException {
    }

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
}
