/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.client;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * The operation attachments. This interface extends {@code Closeable}
 * which can be used to close all associated input streams with
 * this attachment.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface OperationAttachments extends Closeable {

    /**
     * Flag indicating whether the streams should be automatically closed
     * once the operation completed.
     *
     * @return {@code true} if the streams are going to be closed, false otherwise
     */
    boolean isAutoCloseStreams();

    /**
     * Input streams associated with the operation
     *
     * @return the streams. If there are none an empty list is returned
     */
    List<InputStream> getInputStreams();

    OperationAttachments EMPTY = new OperationAttachments() {
        @Override
        public boolean isAutoCloseStreams() {
            return false;
        }

        @Override
        public List<InputStream> getInputStreams() {
            return Collections.emptyList();
        }

        @Override
        public void close() {
            //
        }
    };

}
