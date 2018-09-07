/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.compat.jpa.hibernate.transformer;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.SingleColumnType;
import org.hibernate.type.Type;

public class BooleanSingleColumnType implements SingleColumnType<Boolean> {
    @Override
    public int sqlType() {
        return Types.BOOLEAN;
    }

    @Override
    public String toString(Boolean value) throws HibernateException {
        return value != null ? value.toString() : "null";
    }

    @Override
    public Boolean fromStringValue(String xml) throws HibernateException {
        return Boolean.getBoolean(xml);
    }

    private static void internalSessionImplementorUsingMethod(SessionImplementor session) {
        session.isTransactionInProgress();
    }

    @Override
    public Boolean nullSafeGet(ResultSet resultSet, String name, SessionImplementor session) throws HibernateException, SQLException {
        internalSessionImplementorUsingMethod(session);
        session.isTransactionInProgress();
        boolean result = resultSet.getBoolean(name);
        return new Boolean(result);
    }

    @Override
    public Object get(ResultSet rs, String name, SessionImplementor session) throws HibernateException, SQLException {
        return nullSafeGet(rs, name, session);
    }

    @Override
    public void set(PreparedStatement st, Boolean value, int index, SessionImplementor session) throws HibernateException, SQLException {
        nullSafeSet(st, value, index, session);
    }

    @Override
    public boolean isAssociationType() {
        return false;
    }

    @Override
    public boolean isCollectionType() {
        return false;
    }

    @Override
    public boolean isEntityType() {
        return false;
    }

    @Override
    public boolean isAnyType() {
        return false;
    }

    @Override
    public boolean isComponentType() {
        return false;
    }

    @Override
    public int getColumnSpan(Mapping mapping) throws MappingException {
        return 1;
    }

    @Override
    public int[] sqlTypes(Mapping mapping) throws MappingException {
        return new int[]{Types.BOOLEAN};
    }

    @Override
    public Size[] dictatedSizes(Mapping mapping) throws MappingException {
        return new Size[0];
    }

    @Override
    public Size[] defaultSizes(Mapping mapping) throws MappingException {
        return new Size[]{new Size(19, 2, 255, Size.LobMultiplier.NONE)};
    }

    @Override
    public Class getReturnedClass() {
        return Boolean.class;
    }

    @Override
    public boolean isSame(Object x, Object y) throws HibernateException {
        return x == y;
    }

    @Override
    public boolean isEqual(Object x, Object y) throws HibernateException {
        return x == y;
    }

    @Override
    public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) throws HibernateException {
        return x == y;
    }

    @Override
    public int getHashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public int getHashCode(Object x, SessionFactoryImplementor factory) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public int compare(Object x, Object y) {
        return ((Boolean) x).compareTo((Boolean) y);
    }

    @Override
    public boolean isDirty(Object old, Object current, SessionImplementor session) throws HibernateException {
        return false;
    }

    @Override
    public boolean isDirty(Object oldState, Object currentState, boolean[] checkable, SessionImplementor session) throws HibernateException {
        return false;
    }

    @Override
    public boolean isModified(Object dbState, Object currentState, boolean[] checkable, SessionImplementor session) throws HibernateException {
        return false;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        return nullSafeGet(rs, names[0], session);
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        return nullSafeGet(rs, name, session);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session) throws HibernateException, SQLException {
        nullSafeSet(st, value, index, session);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        internalSessionImplementorUsingMethod(session);
        if (value == null) {
            st.setNull(index, Types.BOOLEAN);
        } else {
            st.setBoolean(index, (Boolean) value);
        }
    }

    @Override
    public String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
        return BooleanSingleColumnType.class.getName();
    }

    @Override
    public String getName() {
        return BooleanSingleColumnType.class.getName();
    }

    @Override
    public Object deepCopy(Object value, SessionFactoryImplementor factory) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value, SessionImplementor session, Object owner) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, SessionImplementor session, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public void beforeAssemble(Serializable cached, SessionImplementor session) {

    }

    @Override
    public Object hydrate(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        return null;
    }

    @Override
    public Object resolve(Object value, SessionImplementor session, Object owner) throws HibernateException {
        return value;
    }

    @Override
    public Object semiResolve(Object value, SessionImplementor session, Object owner) throws HibernateException {
        return value;
    }

    @Override
    public Type getSemiResolvedType(SessionFactoryImplementor factory) {
        return this;
    }

    @Override
    public Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache) throws HibernateException {
        return original;
    }

    @Override
    public Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache, ForeignKeyDirection foreignKeyDirection) throws HibernateException {
        return replace(original, target, session, owner, copyCache);
    }

    @Override
    public boolean[] toColumnNullness(Object value, Mapping mapping) {
        return value == null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
    }
}
