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

package org.jboss.as.controller.remote;

import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.dmr.ModelNode;

/**
* @author Emanuel Muckenhuber
*/
public class TransactionalOperationImpl implements TransactionalProtocolClient.Operation {

    private final ModelNode operation;
    private final OperationMessageHandler messageHandler;
    private final OperationAttachments attachments;

    protected TransactionalOperationImpl(final ModelNode operation, final OperationMessageHandler messageHandler, final OperationAttachments attachments) {
        this.operation = operation;
        this.messageHandler = messageHandler;
        this.attachments = attachments;
    }

    @Override
    public ModelNode getOperation() {
        return operation;
    }

    @Override
    public OperationMessageHandler getMessageHandler() {
        return messageHandler;
    }

    @Override
    public OperationAttachments getAttachments() {
        return attachments;
    }

}
