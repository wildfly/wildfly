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
package org.jboss.as.cmp.jdbc2;

import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.jdbc2.bridge.JDBCEntityBridge2;
import org.jboss.as.cmp.jdbc2.bridge.JDBCCMPFieldBridge2;
import org.jboss.as.cmp.jdbc.metadata.JDBCJBossQLQueryMetaData;
import org.jboss.as.cmp.jdbc.QLCompiler;
import org.jboss.as.cmp.jdbc.EJBQLToSQL92Compiler;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public class JBossQLQueryCommand
        extends AbstractQueryCommand {
    public JBossQLQueryCommand(JDBCEntityBridge2 entity, JDBCJBossQLQueryMetaData metadata) {
        this.entity = entity;

        JDBCStoreManager2 manager = (JDBCStoreManager2) entity.getManager();
        QLCompiler compiler = new EJBQLToSQL92Compiler(manager.getCatalog());

        try {
            compiler.compileJBossQL(
                    metadata.getJBossQL(),
                    metadata.getMethod().getReturnType(),
                    metadata.getMethod().getParameterTypes(),
                    metadata);
        } catch (Throwable t) {
            throw CmpMessages.MESSAGES.errorCompilingJbossQlStatementRuntime(metadata.getJBossQL(), t);
        }

        sql = compiler.getSQL();

        setOffsetParam(compiler.getOffsetParam());
        setOffsetValue(compiler.getOffsetValue());
        setLimitParam(compiler.getLimitParam());
        setLimitValue(compiler.getLimitValue());

        log = Logger.getLogger(getClass().getName() + "." + entity.getEntityName() + "#" + metadata.getMethod().getName());
        log.debug("sql: " + sql);

        setParameters(compiler.getInputParameters());
        setResultType(metadata.getMethod().getReturnType());

        if (!compiler.isSelectEntity()) {
            if (compiler.isSelectField()) {
                setFieldReader((JDBCCMPFieldBridge2) compiler.getSelectField());
            } else {
                setFunctionReader(compiler.getSelectFunction());
            }
        } else {
            setEntityReader((JDBCEntityBridge2) compiler.getSelectEntity(), compiler.isSelectDistinct());
        }
    }
}
