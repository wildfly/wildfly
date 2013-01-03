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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMRFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCFieldBridge;
import org.jboss.as.cmp.context.CmpEntityBeanContext;

/**
 * This command establishes relationships for CMR fields that have
 * foreign keys mapped to primary keys.
 *
 * @author <a href="mailto:aloubyansky@hotmail.com">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCPostCreateEntityCommand {
    // Attributes ------------------------------------
    private final JDBCEntityBridge entity;
    private final JDBCCMRFieldBridge[] cmrWithFKMappedToCMP;

    // Constructors ----------------------------------
    public JDBCPostCreateEntityCommand(JDBCStoreManager manager) {
        entity = (JDBCEntityBridge) manager.getEntityBridge();
        JDBCFieldBridge[] cmrFields = entity.getCMRFields();
        List fkToCMPList = new ArrayList(4);
        for (int i = 0; i < cmrFields.length; ++i) {
            JDBCCMRFieldBridge cmrField = (JDBCCMRFieldBridge) cmrFields[i];
            JDBCCMRFieldBridge relatedCMRField = (JDBCCMRFieldBridge) cmrField.getRelatedCMRField();
            if (cmrField.hasFKFieldsMappedToCMPFields()
                    || relatedCMRField.hasFKFieldsMappedToCMPFields()) {
                fkToCMPList.add(cmrField);
            }
        }
        if (fkToCMPList.isEmpty())
            cmrWithFKMappedToCMP = null;
        else
            cmrWithFKMappedToCMP = (JDBCCMRFieldBridge[]) fkToCMPList
                    .toArray(new JDBCCMRFieldBridge[fkToCMPList.size()]);
    }

    // Public ----------------------------------------
    public Object execute(Method m, Object[] args, CmpEntityBeanContext ctx) {
        if (cmrWithFKMappedToCMP == null)
            return null;

        for (int i = 0; i < cmrWithFKMappedToCMP.length; ++i) {
            JDBCCMRFieldBridge cmrField = cmrWithFKMappedToCMP[i];
            JDBCCMRFieldBridge relatedCMRField = (JDBCCMRFieldBridge) cmrField.getRelatedCMRField();
            if (cmrField.hasFKFieldsMappedToCMPFields()) {
                Object relatedId = cmrField.getRelatedIdFromContext(ctx);
                if (relatedId != null) {
                    try {
                        if (cmrField.isForeignKeyValid(relatedId)) {
                            cmrField.createRelationLinks(ctx, relatedId);
                        } else {
                            relatedCMRField.addRelatedPKWaitingForMyPK(relatedId, ctx.getPrimaryKeyUnchecked());
                        }
                    } catch (Exception e) {
                        // no such object
                    }
                }
            } else if (relatedCMRField.hasFKFieldsMappedToCMPFields()) {
                cmrField.addRelatedPKsWaitedForMe(ctx);
            }
        }
        return null;
    }
}
