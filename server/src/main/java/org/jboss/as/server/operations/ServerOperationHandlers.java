/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.operations;

import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.server.RuntimeOperationHandler;

/**
 *
 * @author Emanuel Muckenhuber
 */
public class ServerOperationHandlers {

    public static final OperationHandler SERVER_READ_RESOURCE_HANDLER = new ReadResourceHandler();
    public static final OperationHandler SERVER_READ_ATTRIBUTE_HANDLER = new ReadAttributeHandler();
    public static final OperationHandler SERVER_WRITE_ATTRIBUTE_HANDLER = new WriteAttributeHandler();

    static final String[] NO_LOCATION = new String[0];

    static class ReadResourceHandler extends GlobalOperationHandlers.ReadResourceHandler implements RuntimeOperationHandler {
        //
    }

    static class ReadAttributeHandler extends GlobalOperationHandlers.ReadAttributeHandler implements RuntimeOperationHandler {
        //
    }

    static class WriteAttributeHandler extends GlobalOperationHandlers.WriteAttributeHandler implements RuntimeOperationHandler {
        //
    }

}
