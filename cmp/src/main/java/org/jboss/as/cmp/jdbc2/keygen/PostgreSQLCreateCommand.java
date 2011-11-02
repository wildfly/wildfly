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

import org.jboss.as.cmp.jdbc.SQLUtil;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityCommandMetaData;
import org.jboss.as.cmp.jdbc2.JDBCStoreManager2;

/**
 * PostgreSQL create command
 *
 * @author <a href="mailto:jep@worldleaguesports.com">Jesper Pedersen</a>
 * @version <tt>$Revision: 82920 $</tt>
 */
public class PostgreSQLCreateCommand extends AbstractCreateCommand {

    public void init(JDBCStoreManager2 manager) {
        super.init(manager);

        JDBCEntityCommandMetaData metadata = entityBridge.getMetaData().getEntityCommand();
        String sequence = metadata.getAttribute("sequence");
        if (sequence == null) {
            sequence = entityBridge.getQualifiedTableName()
                    + '_' + SQLUtil.getColumnNamesClause(pkField, new StringBuffer(20))
                    + "_seq";
        }

        pkSql = "SELECT nextval('" + sequence + "')";

        if (log.isDebugEnabled()) {
            log.debug("entity-command generate pk sql: " + pkSql);
        }
    }
}
