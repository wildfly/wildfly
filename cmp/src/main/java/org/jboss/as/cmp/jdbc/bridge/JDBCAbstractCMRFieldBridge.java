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

import org.jboss.as.cmp.CmpMessages;
import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.bridge.CMRFieldBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationshipRoleMetaData;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public abstract class JDBCAbstractCMRFieldBridge implements JDBCFieldBridge, CMRFieldBridge {
    public abstract JDBCRelationshipRoleMetaData getMetaData();

    public abstract JDBCFieldBridge[] getForeignKeyFields();

    public abstract JDBCFieldBridge[] getTableKeyFields();

    public abstract boolean hasForeignKey();

    public abstract JDBCAbstractCMRFieldBridge getRelatedCMRField();

    public abstract JDBCAbstractEntityBridge getEntity();

    public abstract String getQualifiedTableName();

    public abstract String getTableName();

    public Object getPrimaryKeyValue(Object o) {
        throw MESSAGES.getPrimaryKeyValueNotSupported();
    }
}
