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

package org.jboss.as.cmp.component.interceptors;

import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.component.CmpEntityBeanComponentInstance;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.bridge.CMRMessage;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMRFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.logging.Logger;

/**
 * @author John Bailey
 */
public class CmpEntityBeanJdbcRelationshipInterceptor implements Interceptor {
    private static final Logger log = Logger.getLogger(CmpEntityBeanJdbcRelationshipInterceptor.class);
    private static final CmpEntityBeanJdbcRelationshipInterceptor INSTANCE = new CmpEntityBeanJdbcRelationshipInterceptor();

    public Object processInvocation(InterceptorContext context) throws Exception {
        CMRMessage relationshipMessage = context.getPrivateData(CMRMessage.class);
        if (relationshipMessage == null) {
            // Not a relationship message. Invoke down the chain
            return context.proceed();
        }
        final CmpEntityBeanComponentInstance instance = (CmpEntityBeanComponentInstance) context.getPrivateData(ComponentInstance.class);
        final CmpEntityBeanContext ctx = instance.getEjbContext();

        // We are going to work with the context a lot
        JDBCCMRFieldBridge cmrField = (JDBCCMRFieldBridge) context.getParameters()[0];

        if (CMRMessage.GET_RELATED_ID == relationshipMessage) {
            // call getRelateId
            if (log.isTraceEnabled()) {
                log.trace("Getting related id: field=" + cmrField.getFieldName() + " id=" + ctx.getPrimaryKey());
            }
            return cmrField.getRelatedId(ctx);

        } else if (CMRMessage.ADD_RELATION == relationshipMessage) {
            // call addRelation
            Object relatedId = context.getParameters()[1];
            if (log.isTraceEnabled()) {
                log.trace("Add relation: field=" + cmrField.getFieldName() +
                        " id=" + ctx.getPrimaryKey() +
                        " relatedId=" + relatedId);
            }

            cmrField.addRelation(ctx, relatedId);

            return null;

        } else if (CMRMessage.REMOVE_RELATION == relationshipMessage) {
            // call removeRelation
            Object relatedId = context.getParameters()[1];
            if (log.isTraceEnabled()) {
                log.trace("Remove relation: field=" + cmrField.getFieldName() +
                        " id=" + ctx.getPrimaryKey() +
                        " relatedId=" + relatedId);
            }

            cmrField.removeRelation(ctx, relatedId);

            return null;
        } else if (CMRMessage.SCHEDULE_FOR_CASCADE_DELETE == relationshipMessage) {
            JDBCEntityBridge entity = (JDBCEntityBridge) cmrField.getEntity();
            entity.scheduleForCascadeDelete(ctx);
            return null;
        } else if (CMRMessage.SCHEDULE_FOR_BATCH_CASCADE_DELETE == relationshipMessage) {
            JDBCEntityBridge entity = (JDBCEntityBridge) cmrField.getEntity();
            entity.scheduleForBatchCascadeDelete(ctx);
            return null;
        } else {
            // this should not be possible we are using a type safe enum
            throw CmpMessages.MESSAGES.unknownCmpRelationshipMessage(relationshipMessage);
        }
    }

    public static InterceptorFactory FACTORY = new InterceptorFactory() {
        public Interceptor create(InterceptorFactoryContext context) {
            return INSTANCE;
        }
    };
}
