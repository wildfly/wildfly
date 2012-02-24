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
package org.jboss.as.cmp.jdbc2;

import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.jdbc2.bridge.JDBCEntityBridge2;
import org.jboss.as.cmp.jdbc2.bridge.JDBCCMRFieldBridge2;
import org.jboss.as.cmp.jdbc2.schema.EntityTable;
import org.jboss.as.cmp.jdbc2.schema.Cache;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractCMRFieldBridge;

import javax.ejb.DuplicateKeyException;
import java.sql.SQLException;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public class PersistentContext {
    private final EntityTable.Row row;
    private final JDBCCMRFieldBridge2.FieldState[] cmrStates;

    public PersistentContext(JDBCEntityBridge2 entity, EntityTable.Row row) {
        this.row = row;

        JDBCAbstractCMRFieldBridge[] cmrFields = entity.getCMRFields();
        if (cmrFields != null) {
            cmrStates = new JDBCCMRFieldBridge2.FieldState[cmrFields.length];
        } else {
            cmrStates = null;
        }
    }

    public Object getFieldValue(int rowIndex) {
        return row.getFieldValue(rowIndex);
    }

    public void setFieldValue(int rowIndex, Object value) {
        row.setFieldValue(rowIndex, value);
    }

    public void setPk(Object pk) throws DuplicateKeyException {
        if (pk == null) {
            throw CmpMessages.MESSAGES.cannotSetNullPk();
        }

        row.insert(pk);
    }

    public boolean isDirty() {
        return row.isDirty();
    }

    public void setDirty() {
        row.setDirty();
    }

    public void setDirtyRelations() {
        row.setDirtyRelations();
    }

    public void remove() {
        row.delete();
    }

    public JDBCCMRFieldBridge2.FieldState getCMRState(int cmrIndex) {
        return cmrStates[cmrIndex];
    }

    public void setCMRState(int cmrIndex, JDBCCMRFieldBridge2.FieldState state) {
        cmrStates[cmrIndex] = state;
    }

    public void loadCachedRelations(int cmrIndex, Cache.CacheLoader loader) {
        row.loadCachedRelations(cmrIndex, loader);
    }

    public void cacheRelations(int cmrIndex, Cache.CacheLoader loader) {
        row.cacheRelations(cmrIndex, loader);
    }

    public void flush() throws SQLException, DuplicateKeyException {
        row.flush();
    }

    public void nullForeignKey(EntityTable.ForeignKeyConstraint constraint) {
        row.nullForeignKey(constraint);
    }

    public void nonNullForeignKey(EntityTable.ForeignKeyConstraint constraint) {
        row.nonNullForeignKey(constraint);
    }
}
