/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.jdbc2.keygen;

import org.jboss.as.cmp.jdbc.metadata.JDBCEntityCommandMetaData;
import org.jboss.as.cmp.jdbc2.JDBCStoreManager2;

/**
 * Create command for Hypersonic that generated keys using an IDENTITY column.
 *
 * @author <a href="mailto:jep@worldleaguesports.com">Jesper Pedersen</a>
 * @version $Revision: 82920 $
 */
public class HsqldbCreateCommand extends AbstractCreateCommand {

    public void init(JDBCStoreManager2 manager) {
        super.init(manager);

        JDBCEntityCommandMetaData metadata = entityBridge.getMetaData().getEntityCommand();
        pkSql = metadata.getAttribute("pk-sql");
        if (pkSql == null) {
            pkSql = "CALL IDENTITY()";
        }

        if (log.isDebugEnabled()) {
            log.debug("entity-command generate pk sql: " + pkSql);
        }
    }
}
