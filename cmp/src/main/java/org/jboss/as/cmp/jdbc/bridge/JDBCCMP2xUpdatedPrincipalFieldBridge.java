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
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldMetaData;
import org.jboss.as.cmp.context.CmpEntityBeanContext;

/**
 * Audit updated principal field.
 *
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 */
public class JDBCCMP2xUpdatedPrincipalFieldBridge extends JDBCCMP2xAutoUpdatedFieldBridge {
    public JDBCCMP2xUpdatedPrincipalFieldBridge(JDBCStoreManager manager,
                                                JDBCCMPFieldMetaData metadata) {
        super(manager, metadata);
    }

    public JDBCCMP2xUpdatedPrincipalFieldBridge(JDBCCMP2xFieldBridge cmpField) {
        super(cmpField);
    }

    public void setFirstVersion(CmpEntityBeanContext ctx) {
        setInstanceValue(ctx, ctx.getCallerPrincipal().getName());
    }

    public Object updateVersion(CmpEntityBeanContext ctx) {
        Object value = ctx.getCallerPrincipal().getName();
        setInstanceValue(ctx, value);
        return value;
    }
}
