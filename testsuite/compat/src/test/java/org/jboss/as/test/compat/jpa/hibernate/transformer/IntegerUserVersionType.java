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

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserVersionType;

public class IntegerUserVersionType implements UserVersionType {
    @Override
    public Object seed(SessionImplementor sessionImplementor) {
        return 0;
    }

    @Override
    public Object next(Object o, SessionImplementor sessionImplementor) {
        return (Integer) o + 1;
    }

    @Override
    public int compare(Object o1, Object o2) {
        return ((Integer)o1).compareTo((Integer)o2);
    }

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.INTEGER};
    }

    @Override
    public Class returnedClass() {
        return Integer.class;
    }

    @Override
    public boolean equals(Object o, Object o1) throws HibernateException {
        return o == o1;
    }

    @Override
    public int hashCode(Object o) throws HibernateException {
        return o.hashCode();
    }

    private static void internalSessionImplementorUsingMethod(SessionImplementor session) {
        session.isTransactionInProgress();
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] strings, SessionImplementor sessionImplementor, Object owner) throws HibernateException, SQLException {
        internalSessionImplementorUsingMethod(sessionImplementor);
        sessionImplementor.isTransactionInProgress();
        int result = resultSet.getInt(strings[0]);
        return new Integer(result);
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SessionImplementor sessionImplementor) throws HibernateException, SQLException {
        internalSessionImplementorUsingMethod(sessionImplementor);
        if (value == null) {
            preparedStatement.setNull( index, Types.INTEGER );
        }
        else {
            preparedStatement.setInt( index, (Integer) value );
        }
    }

    @Override
    public Object deepCopy(Object o) throws HibernateException {
        return o;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object o) throws HibernateException {
        return (Serializable) o;
    }

    @Override
    public Object assemble(Serializable serializable, Object o) throws HibernateException {
        return o;
    }

    @Override
    public Object replace(Object o, Object o1, Object o2) throws HibernateException {
        return o;
    }
}
