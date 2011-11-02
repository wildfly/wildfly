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

import java.util.List;
import org.jboss.as.cmp.ejbql.SelectFunction;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaData;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public interface QLCompiler {
    void compileEJBQL(
            String ejbql,
            Class returnType,
            Class[] parameterTypes,
            JDBCQueryMetaData metadata
    ) throws Exception;

    void compileJBossQL(
            String ejbql,
            Class returnType,
            Class[] parameterTypes,
            JDBCQueryMetaData metadata
    )
            throws Exception;

    String getSQL();

    int getOffsetValue();

    int getOffsetParam();

    int getLimitValue();

    int getLimitParam();

    boolean isSelectEntity();

    JDBCAbstractEntityBridge getSelectEntity();

    boolean isSelectField();

    JDBCFieldBridge getSelectField();

    SelectFunction getSelectFunction();

    JDBCEntityPersistenceStore getStoreManager();

    List getInputParameters();

    List getLeftJoinCMRList();

    boolean isSelectDistinct();
}
