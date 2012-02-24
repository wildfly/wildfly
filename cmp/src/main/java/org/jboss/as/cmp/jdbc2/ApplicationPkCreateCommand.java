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

import java.lang.reflect.Method;
import java.sql.SQLException;
import javax.ejb.CreateException;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc2.bridge.JDBCEntityBridge2;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public class ApplicationPkCreateCommand implements CreateCommand {
    private JDBCEntityBridge2 entityBridge;

    public void init(JDBCStoreManager2 manager) {
        this.entityBridge = (JDBCEntityBridge2) manager.getEntityBridge();
    }

    public Object execute(Method m, Object[] args, CmpEntityBeanContext ctx) throws CreateException {
        Object pk;
        PersistentContext pctx = (PersistentContext) ctx.getPersistenceContext();
        if (ctx.getPrimaryKey() == null) {
            pk = entityBridge.extractPrimaryKeyFromInstance(ctx);

            if (pk == null) {
                throw MESSAGES.pkIsNullForCreatedInstance();
            }

            pctx.setPk(pk);
        } else {
            // insert-after-ejb-post-create
            try {
                pctx.flush();
            } catch (SQLException e) {
                if ("23000".equals(e.getSQLState())) {
                    throw MESSAGES.uniqueKeyViolation(ctx.getPrimaryKey());
                } else {
                    throw MESSAGES.failedToCreateInstance(ctx.getPrimaryKey(), e);
                }
            }
            pk = ctx.getPrimaryKey();
        }
        return pk;
    }
}
