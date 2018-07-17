/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.web.security.bufferFreed;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Log handler for test BufferFreedTestCase. Checks the severity of errors in the logs and privides callback into BufferFreedTestCase.
 *
 * @author Daniel Cihak
 */
public class MyHandler extends ExtHandler {

    private static Callback callback = null;

    public interface Callback {
        void warn(LogRecord logRecord);
        void error(LogRecord logRecord);
    }

    public static void setCallback(Callback newCallback) {
        callback = newCallback;
    }

    @Override
    protected void doPublish(ExtLogRecord record) {
        if(callback != null) {
            if(record.getLevel().intValue() >= Level.SEVERE.intValue()) {
                callback.error(record);
            } else if(record.getLevel().intValue() >= Level.WARNING.intValue()) {
                callback.warn(record);
            }
        }
    }
}
