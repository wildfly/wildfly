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
package org.jboss.as.controller.client;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.jboss.dmr.ModelNode;

/**
 * Encapsulates a detyped operation request passed in to the model controller, along with
 * any attachments associated with the request.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface Operation extends OperationAttachments {

    /**
     * The detyped operation to execute
     *
     * @return the operation
     */
    ModelNode getOperation();

    /**
     * Clones this operation.
     */
    @Deprecated
    Operation clone();

    /**
     * Clones this operation, but overrides the raw operation node
     */
    @Deprecated
    Operation clone(ModelNode operation);

    /** Factory methods for creating {@code Operation}s */
    class Factory {
        /**
         * Create a simple operation with no stream attachments.
         *
         * @param operation the DMR operation. Cannot be {@code null}
         *
         * @return the operation. Will not be {@code null}
         */
        public static Operation create(final ModelNode operation) {
            return create(operation, Collections.<InputStream>emptyList());
        }
        /**
         * Create a simple operation with stream attachments. The streams will not
         * be {@link OperationAttachments#isAutoCloseStreams() automatically closed}
         * when operation execution is completed.
         *
         * @param operation the DMR operation. Cannot be {@code null}
         * @param attachments the stream attachments. Cannot be {@code null}
         *
         * @return the operation. Will not be {@code null}
         */
        public static Operation create(final ModelNode operation, final List<InputStream> attachments) {
            return new OperationImpl(operation, attachments);
        }

        /**
         * Create an operation using the given streams and be {@link OperationAttachments#isAutoCloseStreams() auto-close streams}
         * setting.
         *
         * @param operation the DMR operation. Cannot be {@code null}
         * @param attachments  the stream attachments. Cannot be {@code null}
         * @param autoCloseStreams {@code true} if the attached streams should be automatically closed when
         *                         operation execution completes
         *
         * @return the operation. Will not be {@code null}
         */
        public static Operation create(final ModelNode operation, final List<InputStream> attachments, final boolean autoCloseStreams) {
            return new OperationImpl(operation, attachments, autoCloseStreams);
        }
    }
}
