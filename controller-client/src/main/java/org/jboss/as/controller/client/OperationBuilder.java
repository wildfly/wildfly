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

import java.io.InputStream;

import org.jboss.dmr.ModelNode;

/**
 * The operation builder.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface OperationBuilder {

    OperationBuilder addInputStream(InputStream in);

    int getInputStreamCount();

    Operation build();

    public static class Factory{
        /**
         * Create an operation builder.
         *
         * @param operation the operation
         * @return the operation
         */
        public static OperationBuilder create(ModelNode operation) {
            if (operation == null) {
                throw new IllegalArgumentException("Null operation");
            }
            return new OperationImpl(operation);
        }

        /**
         * Create an operation builder based on existing attachments and operation.
         *
         * @param attachments the operation attachments
         * @param operation the operation
         * @return the operation builder
         */
        public static OperationBuilder copy(OperationAttachments attachments, ModelNode operation) {
            if (operation == null) {
                throw new IllegalArgumentException("Null operation");
            }
            OperationImpl ctx = new OperationImpl(operation);
            for (InputStream in : attachments.getInputStreams()) {
                ctx.addInputStream(in);
            }
            return ctx;
        }

    }
}
