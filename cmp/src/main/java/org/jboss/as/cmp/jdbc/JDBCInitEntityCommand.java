/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cmp.jdbc;

import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;

/**
 * CMPStoreManager JDBCActivateEntityCommand
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class JDBCInitEntityCommand {
    private final JDBCEntityBridge entity;

    public JDBCInitEntityCommand(JDBCStoreManager manager) {
        entity = (JDBCEntityBridge) manager.getEntityBridge();
    }

    /**
     * Called before ejbCreate. In the JDBCStoreManager we need to
     * initialize the persistence context. The persistence context is where
     * where bean data is stored. If CMP 1.x, original values are store
     * and for CMP 2.x actual values are stored int the context. Then we
     * initialize the data. In CMP 1.x fields are reset to Java defaults, and
     * in CMP 2.x current value in persistence store are initialized.
     * <p/>
     * Note: persistence context is also initialized in activate.
     */
    public void execute(CmpEntityBeanContext ctx) {
        entity.initPersistenceContext(ctx);
        entity.initInstance(ctx);
    }
}
