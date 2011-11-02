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

import java.util.List;
import javax.sql.DataSource;
import org.jboss.as.cmp.bridge.EntityBridge;
import org.jboss.as.cmp.bridge.FieldBridge;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public interface JDBCAbstractEntityBridge extends EntityBridge {
    JDBCFieldBridge[] getPrimaryKeyFields();

    JDBCAbstractCMRFieldBridge[] getCMRFields();

    JDBCFieldBridge[] getTableFields();

    JDBCEntityPersistenceStore getManager();

    String getTableName();

    String getQualifiedTableName();

    DataSource getDataSource();

    boolean[] getLoadGroupMask(String eagerLoadGroupName);

    JDBCEntityMetaData getMetaData();
}
