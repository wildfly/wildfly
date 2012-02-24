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

package org.jboss.as.cmp;

import java.sql.SQLException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface CmpLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    CmpLogger ROOT_LOGGER = Logger.getMessageLogger(CmpLogger.class, CmpLogger.class.getPackage().getName());


    @LogMessage(level = ERROR)
    @Message(id = 18883, value = "Sql Error")
    void sqlError(@Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id = 18884, value = "Failed to stop entity bridge.")
    void failedToStopEntityBridge(@Cause Exception e);


    @LogMessage(level = ERROR)
    @Message(id = 18885, value = "Failed to update table")
    void failedToUpdateTable(@Cause SQLException e);

    @LogMessage(level = ERROR)
    @Message(id = 18886, value = "Failed to rollback")
    void failedToRollback(@Cause SystemException e1);

    @LogMessage(level = ERROR)
    @Message(id = 18887, value = "Failed to commit")
    void failedToCommit(@Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id = 18888, value = "CMR table structure is incorrect for %s")
    void incorrectCmrTableStructure(String qualifiedTableName);

    @LogMessage(level = ERROR)
    @Message(id = 18889, value = "Could not suspend current transaction before drop table. '%s' will not be dropped.")
    void couldNotSuspendTxBeforeDrop(String qualifiedTableName, @Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id = 18890, value = "Could not reattach original transaction after drop table")
    void couldNotReattachAfterDrop();

    @LogMessage(level = WARN)
    @Message(id = 18891, value = "Exception while trying to rollback tx: %s")
    void exceptionRollingBackTx(Transaction tx, @Cause Exception e);

    @LogMessage(level = WARN)
    @Message(id = 18892, value = "Exception altering table")
    void exceptionAlterTable(@Cause Exception e);

    @LogMessage(level = WARN)
    @Message(id = 18893, value = "PK field %s was found more than once in class hierarchy of %s. Will use the one from %s")
    void pkFoundMoreThanOnceInHierarchy(String fieldName, String entityPkClass, String pkClass);

    @LogMessage(level = INFO)
    @Message(id = 18894, value = "Dropped table %s successfully")
    void droppedTable(String tableName);
}
