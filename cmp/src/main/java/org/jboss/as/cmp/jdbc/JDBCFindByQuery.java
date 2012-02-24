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


import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCReadAheadMetaData;

/**
 * JDBCFindByQuery automatic finder used in CMP 1.x.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:rickard.oberg@telkel.com">Rickard Oberg</a>
 * @author <a href="mailto:marc.fleury@telkel.com">Marc Fleury</a>
 * @author <a href="mailto:shevlandj@kpi.com.au">Joe Shevland</a>
 * @author <a href="mailto:justin@j-m-f.demon.co.uk">Justin Forder</a>
 * @author <a href="mailto:danch@nvisia.com">danch (Dan Christopherson)</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCFindByQuery extends JDBCAbstractQueryCommand {
    // The meta-info for the field we are finding by
    private final JDBCCMPFieldBridge cmpField;

    public JDBCFindByQuery(JDBCStoreManager manager, JDBCQueryMetaData q) {

        super(manager, q);

        JDBCEntityBridge entity = (JDBCEntityBridge) manager.getEntityBridge();

        String finderName = q.getMethod().getName();

        // finder name will be like findByFieldName
        // we need to convert it to fieldName.
        String cmpFieldName = Character.toLowerCase(finderName.charAt(6)) + finderName.substring(7);

        // get the field
        cmpField = entity.getCMPFieldByName(cmpFieldName);
        if (cmpField == null) {
            throw CmpMessages.MESSAGES.noFinderForMethod(finderName);
        }

        // set the preload fields
        JDBCReadAheadMetaData readAhead = q.getReadAhead();
        if (readAhead.isOnFind()) {
            setEagerLoadGroup(readAhead.getEagerLoadGroup());
        }

        // generate the sql
        StringBuffer sql = new StringBuffer(300);
        sql.append(SQLUtil.SELECT);

        SQLUtil.getColumnNamesClause(entity.getPrimaryKeyFields(), sql);
        if (getEagerLoadGroup() != null) {
            SQLUtil.appendColumnNamesClause(entity, getEagerLoadGroup(), sql);
        }
        sql.append(SQLUtil.FROM)
                .append(entity.getQualifiedTableName())
                .append(SQLUtil.WHERE);
        SQLUtil.getWhereClause(cmpField, sql);

        setSQL(sql.toString());
        setParameterList(QueryParameter.createParameters(0, cmpField));
    }
}
