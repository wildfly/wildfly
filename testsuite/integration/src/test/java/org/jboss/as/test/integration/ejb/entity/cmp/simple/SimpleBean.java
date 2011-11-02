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
package org.jboss.as.test.integration.ejb.entity.cmp.simple;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Hashtable;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.FinderException;

public abstract class SimpleBean extends MiddleBean implements EntityBean, Bottom {
    private transient EntityContext ctx;

    public SimpleBean() {
    }

    public String ejbCreate(String id) throws CreateException {
        setId(id);
        return id;
    }

    public void ejbPostCreate(String id) {
    }

    public abstract Collection ejbSelectValueClass()
            throws FinderException;

    public Collection ejbHomeSelectValueClass() throws FinderException {
        return ejbSelectValueClass();
    }

    public abstract Collection ejbSelectDynamic(String jbossQl, Object[] args)
            throws FinderException;

    public Collection ejbHomeSelectDynamic(String jbossQl, Object[] args)
            throws FinderException {
        return ejbSelectDynamic(jbossQl, args);
    }

    //   public abstract String getId();
    public abstract void setId(String id);

    public abstract ValueClass getValueClass();

    public abstract void setValueClass(ValueClass vc);

    public abstract boolean getBooleanPrimitive();

    public abstract void setBooleanPrimitive(boolean b);

    public abstract Boolean getBooleanObject();

    public abstract void setBooleanObject(Boolean b);

    public abstract byte getBytePrimitive();

    public abstract void setBytePrimitive(byte b);

    public abstract Byte getByteObject();

    public abstract void setByteObject(Byte b);

    public abstract short getShortPrimitive();

    public abstract void setShortPrimitive(short s);

    public abstract Short getShortObject();

    public abstract void setShortObject(Short s);

    public abstract int getIntegerPrimitive();

    public abstract void setIntegerPrimitive(int i);

    public abstract Integer getIntegerObject();

    public abstract void setIntegerObject(Integer i);

    public abstract long getLongPrimitive();

    public abstract void setLongPrimitive(long l);

    public abstract Long getLongObject();

    public abstract void setLongObject(Long l);

    public abstract float getFloatPrimitive();

    public abstract void setFloatPrimitive(float f);

    public abstract Float getFloatObject();

    public abstract void setFloatObject(Float f);

    public abstract double getDoublePrimitive();

    public abstract void setDoublePrimitive(double d);

    public abstract Double getDoubleObject();

    public abstract void setDoubleObject(Double d);

    public abstract String getStringValue();

    public abstract void setStringValue(String s);

    public abstract java.util.Date getUtilDateValue();

    public abstract void setUtilDateValue(java.util.Date d);

    public void updateUtilDateValue(java.util.Date d) {
        setUtilDateValue(d);
    }

    public abstract java.sql.Date getSqlDateValue();

    public abstract void setSqlDateValue(java.sql.Date d);

    public abstract Time getTimeValue();

    public abstract void setTimeValue(Time t);

    public abstract Timestamp getTimestampValue();

    public abstract void setTimestampValue(Timestamp t);

    public abstract java.math.BigDecimal getBigDecimalValue();

    public abstract void setBigDecimalValue(java.math.BigDecimal d);

    public abstract byte[] getByteArrayValue();

    public abstract void setByteArrayValue(byte[] bytes);

    public abstract Object getObjectValue();

    public abstract void setObjectValue(Object t);

    public abstract Hashtable getHashtable();

    public abstract void setHashtable(Hashtable t);

    public void addToHashtable(String key, String value) {
        Hashtable temp = getHashtable();
        temp.put(key, value);
        setHashtable(temp);
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

    public void ejbRemove() {
    }
}
