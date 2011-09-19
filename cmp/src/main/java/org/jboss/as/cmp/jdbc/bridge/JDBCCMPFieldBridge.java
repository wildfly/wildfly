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
package org.jboss.as.cmp.jdbc.bridge;


import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.jboss.as.cmp.bridge.CMPFieldBridge;
import org.jboss.as.cmp.jdbc.LockingStrategy;
import org.jboss.as.cmp.context.CmpEntityBeanContext;

/**
 * JDBCCMPFieldBridge represents one CMP field. This implementations of
 * this interface handles setting are responsible for setting statement
 * parameters and loading results for instance values and primary
 * keys.
 * <p/>
 * Life-cycle:
 * Tied to the EntityBridge.
 * <p/>
 * Multiplicity:
 * One for each entity bean cmp field.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:loubyansky@hotmail.com">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public interface JDBCCMPFieldBridge extends CMPFieldBridge {
    /**
     * The index of the field among the table fields.
     */
    int getTableIndex();

    /**
     * Returns the default field flags.
     */
    byte getDefaultFlags();

    /**
     * TODO: Get rid of it
     *
     * @param flag
     */
    void addDefaultFlag(byte flag);

    /**
     * @param ctx instance's context
     * @return field value that was locked.
     */
    Object getLockedValue(CmpEntityBeanContext ctx);

    /**
     * Optimistically locks field value.
     */
    void lockInstanceValue(CmpEntityBeanContext ctx);

    /**
     * @param lockingStrategy locking strategy assigned to the field
     */
    void setLockingStrategy(LockingStrategy lockingStrategy);

    /**
     * Gets the field of the primary key object in which the value of this
     * field is stored.
     */
    Field getPrimaryKeyField();

    /**
     * Gets the value of this field in the specified primaryKey object.
     *
     * @param primaryKey the primary key object from which this fields value
     *                   will be extracted
     * @return the value of this field in the primaryKey object
     */
    Object getPrimaryKeyValue(Object primaryKey)
            throws IllegalArgumentException;

    /**
     * @return true if the field belongs to a relation table
     */
    boolean isRelationTableField();

    /**
     * Sets the value of this field to the specified value in the
     * specified primaryKey object.
     *
     * @param primaryKey the primary key object which the value
     *                   will be inserted
     * @param value      the value for field that will be set in the pk
     * @return the updated primary key object; the actual object may
     *         change not just the value
     */
    Object setPrimaryKeyValue(Object primaryKey, Object value)
            throws IllegalArgumentException;

    /**
     * Sets the prepared statement parameters with the data from the
     * primary key.
     */
    int setPrimaryKeyParameters(PreparedStatement ps, int parameterIndex, Object primaryKey) throws IllegalArgumentException;

    /**
     * Sets the prepared statement parameters with the data from the
     * object. The object must be the type of this field.
     */
    int setArgumentParameters(PreparedStatement ps, int parameterIndex, Object arg);

    /**
     * Loads the data from result set into the primary key object.
     */
    int loadPrimaryKeyResults(ResultSet rs, int parameterIndex, Object[] pkRef) throws IllegalArgumentException;
}
