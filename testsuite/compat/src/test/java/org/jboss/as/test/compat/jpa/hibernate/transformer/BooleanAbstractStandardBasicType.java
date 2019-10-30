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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.descriptor.java.BooleanTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

public class BooleanAbstractStandardBasicType extends AbstractStandardBasicType<Boolean> {

    public BooleanAbstractStandardBasicType() {
        this( org.hibernate.type.descriptor.sql.BooleanTypeDescriptor.INSTANCE, BooleanTypeDescriptor.INSTANCE );
    }

    public BooleanAbstractStandardBasicType(SqlTypeDescriptor sqlTypeDescriptor, JavaTypeDescriptor<Boolean> javaTypeDescriptor) {
        super( sqlTypeDescriptor, javaTypeDescriptor);
    }

    private static void internalSessionImplementorUsingMethod(SessionImplementor session) {
        session.isTransactionInProgress();
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session) throws HibernateException, SQLException {
        internalSessionImplementorUsingMethod(session);
        if (value == null) {
            st.setNull( index, Types.BOOLEAN );
        }
        else {
            st.setBoolean( index, (Boolean) value );
        }
    }

    @Override
    public Object get(ResultSet rs, String name, SessionImplementor session) throws HibernateException, SQLException {
        return super.get(rs, name, session);
    }

    @Override
    public void set(PreparedStatement st, Boolean value, int index, SessionImplementor session) throws HibernateException, SQLException {
        super.set(st, value, index, session);
    }

    @Override
    public String getName() {
        return BooleanAbstractStandardBasicType.class.getName();
    }
}
