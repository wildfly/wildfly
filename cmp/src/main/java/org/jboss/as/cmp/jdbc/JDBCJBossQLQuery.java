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

import org.jboss.as.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCJBossQLQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCReadAheadMetaData;

/**
 * This class generates a query from JBoss-QL.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCJBossQLQuery extends JDBCAbstractQueryCommand {

    public JDBCJBossQLQuery(JDBCStoreManager manager, JDBCQueryMetaData q) {
        super(manager, q);

        JDBCJBossQLQueryMetaData metadata = (JDBCJBossQLQueryMetaData) q;
        if (getLog().isDebugEnabled()) {
            getLog().debug("JBossQL: " + metadata.getJBossQL());
        }

        QLCompiler compiler = JDBCQueryManager.getInstance(metadata.getQLCompilerClass(), manager.getCatalog());

        try {
            compiler.compileJBossQL(
                    metadata.getJBossQL(),
                    metadata.getMethod().getReturnType(),
                    metadata.getMethod().getParameterTypes(),
                    metadata);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Error compiling JBossQL " +
                    "statement '" + metadata.getJBossQL() + "'", t);
        }

        setSQL(compiler.getSQL());
        setOffsetParam(compiler.getOffsetParam());
        setOffsetValue(compiler.getOffsetValue());
        setLimitParam(compiler.getLimitParam());
        setLimitValue(compiler.getLimitValue());

        // set select object
        if (compiler.isSelectEntity()) {
            JDBCEntityBridge selectEntity = (JDBCEntityBridge) compiler.getSelectEntity();

            // set the select entity
            setSelectEntity(selectEntity);

            // set the preload fields
            JDBCReadAheadMetaData readahead = metadata.getReadAhead();
            if (readahead.isOnFind()) {
                setEagerLoadGroup(readahead.getEagerLoadGroup());
                setOnFindCMRList(compiler.getLeftJoinCMRList());

                // exclude non-searchable columns if distinct is used
                if (compiler.isSelectDistinct()) {
                    boolean[] mask = getEagerLoadMask();
                    JDBCCMPFieldBridge[] tableFields = (JDBCCMPFieldBridge[]) selectEntity.getTableFields();
                    for (int i = 0; i < tableFields.length; ++i) {
                        if (mask[i] && !tableFields[i].getJDBCType().isSearchable()) {
                            mask[i] = false;
                        }
                    }
                }
            }
        } else if (compiler.isSelectField()) {
            setSelectField((JDBCCMPFieldBridge) compiler.getSelectField());
        } else {
            setSelectFunction(compiler.getSelectFunction(), (JDBCStoreManager) compiler.getStoreManager());
        }

        // get the parameter order
        setParameterList(compiler.getInputParameters());
    }
}
