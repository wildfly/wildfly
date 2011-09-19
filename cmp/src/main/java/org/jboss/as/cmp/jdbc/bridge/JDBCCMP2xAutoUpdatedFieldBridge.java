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
import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldMetaData;

import org.jboss.as.cmp.context.CmpEntityBeanContext;

/**
 * The base class for all automatically updated fields such as audit and version.
 *
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public abstract class JDBCCMP2xAutoUpdatedFieldBridge extends JDBCCMP2xFieldBridge {
    // Constructors

    public JDBCCMP2xAutoUpdatedFieldBridge(JDBCStoreManager manager,
                                           JDBCCMPFieldMetaData metadata) {
        super(manager, metadata);
        defaultFlags |= JDBCEntityBridge.ADD_TO_SET_ON_UPDATE;
    }

    public JDBCCMP2xAutoUpdatedFieldBridge(JDBCCMP2xFieldBridge cmpField) {
        super(
                (JDBCStoreManager) cmpField.getManager(),
                cmpField.getFieldName(),
                cmpField.getFieldType(),
                cmpField.getJDBCType(),
                cmpField.isReadOnly(),               // should always be false?
                cmpField.getReadTimeOut(),
                cmpField.getPrimaryKeyClass(),
                cmpField.getPrimaryKeyField(),
                cmpField,
                null,                                // it should not be a foreign key
                cmpField.getColumnName()
        );
        defaultFlags |= JDBCEntityBridge.ADD_TO_SET_ON_UPDATE; // it should be redundant
        cmpField.addDefaultFlag(JDBCEntityBridge.ADD_TO_SET_ON_UPDATE);
    }

    public void initInstance(CmpEntityBeanContext ctx) {
        setFirstVersion(ctx);
    }

    public int setInstanceParameters(PreparedStatement ps,
                                     int parameterIndex,
                                     CmpEntityBeanContext ctx) {
        Object value;
        if (ctx.isValid()) {
            // update
            // generate new value unless it is already provided by the user
            value = isDirty(ctx) ? getInstanceValue(ctx) : updateVersion(ctx);
        } else {
            // create
            value = getInstanceValue(ctx);
        }
        return setArgumentParameters(ps, parameterIndex, value);
    }

    public abstract void setFirstVersion(CmpEntityBeanContext ctx);

    public abstract Object updateVersion(CmpEntityBeanContext ctx);
}
