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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.jboss.as.cmp.bridge.FieldBridge;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.JDBCType;


/**
 * @author <a href="mailto:loubyansky@ua.fm">Alex Loubyansky and others</a>
 */
public interface JDBCFieldBridge extends FieldBridge {
    /**
     * Gets the JDBC type of this field.
     */
    JDBCType getJDBCType();

    /**
     * Is this field a member of the primary key.
     *
     * @return true if this field is a member of the primary key
     */
    boolean isPrimaryKeyMember();

    /**
     * Is this field read only.
     *
     * @return true if this field is read only
     */
    boolean isReadOnly();

    /**
     * Has current data read timed out?
     */
    boolean isReadTimedOut(CmpEntityBeanContext ctx);

    /**
     * Has the data been loaded?
     */
    boolean isLoaded(CmpEntityBeanContext ctx);

    /**
     * Set CMPFieldValue to Java default value (i.e., 0 or null).
     */
    void initInstance(CmpEntityBeanContext ctx);

    /**
     * Resets any persistence data maintained in the context.
     */
    void resetPersistenceContext(CmpEntityBeanContext ctx);

    /**
     * Sets the prepared statement parameters with the data from the
     * instance associated with the context.
     */
    int setInstanceParameters(PreparedStatement ps, int parameterIndex, CmpEntityBeanContext ctx);

    /**
     * Gets the internal value of this field without user level checks.
     *
     * @param ctx the context for which this field's value should be fetched
     * @return the value of this field
     */
    Object getInstanceValue(CmpEntityBeanContext ctx);

    /**
     * Sets the internal value of this field without user level checks.
     *
     * @param ctx   the context for which this field's value should be set
     * @param value the new value of this field
     */
    void setInstanceValue(CmpEntityBeanContext ctx, Object value);

    /**
     * Loads the data from result set into the instance associated with
     * the specified context.
     */
    int loadInstanceResults(ResultSet rs, int parameterIndex, CmpEntityBeanContext ctx);

    /**
     * Loads the value of this cmp field from result set into argument reference.
     */
    int loadArgumentResults(ResultSet rs, int parameterIndex, Object[] argumentRef);

    /**
     * Has the value of this field changes since the last time clean was called.
     */
    boolean isDirty(CmpEntityBeanContext ctx);

    /**
     * Mark this field as clean.
     */
    void setClean(CmpEntityBeanContext ctx);

    boolean isCMPField();

    JDBCEntityPersistenceStore getManager();

    Object getPrimaryKeyValue(Object arg);
}
