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

import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.JDBCTypeFactory;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldMetaData;

import org.jboss.as.cmp.context.CmpEntityBeanContext;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 */
public class JDBCTimestampVersionFieldBridge extends JDBCCMP2xVersionFieldBridge {
    public JDBCTimestampVersionFieldBridge(JDBCStoreManager manager,
                                           JDBCCMPFieldMetaData metadata) {
        super(manager, metadata);
        checkDirtyAfterGet = false;
        stateFactory = JDBCTypeFactory.EQUALS;
    }

    public JDBCTimestampVersionFieldBridge(JDBCCMP2xFieldBridge cmpField) {
        super(cmpField);
        checkDirtyAfterGet = false;
        stateFactory = JDBCTypeFactory.EQUALS;
    }

    public void setFirstVersion(CmpEntityBeanContext ctx) {
        Object version = new java.util.Date();
        setInstanceValue(ctx, version);
    }

    public Object updateVersion(CmpEntityBeanContext ctx) {
        Object next = new java.util.Date();
        setInstanceValue(ctx, next);
        return next;
    }
}
