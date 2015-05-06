/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jpa.hibernate4;


import org.hibernate.jpa.boot.archive.spi.ArchiveException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * JipiJapa message range is 20200-20299
 * note: keep duplicate messages in sync between different sub-projects that use the same messages
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Scott Marlow
 */
@MessageLogger(projectCode = "JIPI")
public interface JpaLogger extends BasicLogger {

    /**
     * A logger with the category {@code org.jboss.jpa}.
     */
    JpaLogger JPA_LOGGER = Logger.getMessageLogger(JpaLogger.class, "org.jipijapa");

    /**
     * Could not open VFS stream
     *
     * @param cause the cause of the error.
     * @param name of VFS file
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 20250, value = "Unable to open VirtualFile-based InputStream %s")
    ArchiveException cannotOpenVFSStream(@Cause Throwable cause, String name);

    /**
     * URI format is incorrect, which results in a syntax error
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 20251, value = "URI syntax error")
    IllegalArgumentException uriSyntaxException(@Cause Throwable cause);

}
