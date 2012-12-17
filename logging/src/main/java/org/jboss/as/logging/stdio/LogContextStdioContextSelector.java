/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging.stdio;

import org.jboss.as.logging.CommonAttributes;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.Logger.AttachmentKey;
import org.jboss.stdio.LoggingOutputStream;
import org.jboss.stdio.NullInputStream;
import org.jboss.stdio.StdioContext;
import org.jboss.stdio.StdioContextSelector;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LogContextStdioContextSelector implements StdioContextSelector {

    public static final AttachmentKey<StdioContext> STDIO_CONTEXT_ATTACHMENT_KEY = new AttachmentKey<StdioContext>();

    public LogContextStdioContextSelector(final StdioContext defaultContext) {
        // Register the STDIO context on the default log context
        LogContext.getLogContext().getLogger(CommonAttributes.ROOT_LOGGER_NAME).attachIfAbsent(STDIO_CONTEXT_ATTACHMENT_KEY, defaultContext);
    }

    @Override
    public StdioContext getStdioContext() {
        final LogContext logContext = LogContext.getLogContext();
        final Logger root = logContext.getLogger(CommonAttributes.ROOT_LOGGER_NAME);
        StdioContext stdioContext = root.getAttachment(STDIO_CONTEXT_ATTACHMENT_KEY);
        if (stdioContext == null) {
            stdioContext = StdioContext.create(
                    new NullInputStream(),
                    new LoggingOutputStream(logContext.getLogger("stdout"), Level.INFO),
                    new LoggingOutputStream(logContext.getLogger("stderr"), Level.ERROR)
            );
            final StdioContext appearing = root.attachIfAbsent(STDIO_CONTEXT_ATTACHMENT_KEY, stdioContext);
            if (appearing != null) {
                stdioContext = appearing;
            }
        }
        return stdioContext;
    }
}
