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

package org.jboss.as.threads;

import java.math.BigDecimal;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * This module is using message IDs in the range 12400-12499. This file is using the subset 12400-12449 for
 * logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
interface ThreadsLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    ThreadsLogger ROOT_LOGGER = Logger.getMessageLogger(ThreadsLogger.class, ThreadsLogger.class.getPackage().getName());

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 12400, value = "The '%s' attribute is no longer supported. The value [%f] of the '%s' attribute " +
                    "is being combined with the value [%f] of the '%s' attribute and the current processor count [%d] " +
                    "to derive a new value of [%d] for '%s'.")
    void perCpuNotSupported(Attribute perCpuAttr, BigDecimal count, Attribute countAttr, BigDecimal perCpu, Attribute perCpuAgain,
                            int processors, int fullCount, Attribute countAttrAgain);
}
