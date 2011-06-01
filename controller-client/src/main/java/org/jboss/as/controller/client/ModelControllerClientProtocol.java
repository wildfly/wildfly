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
package org.jboss.as.controller.client;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public interface ModelControllerClientProtocol {
    byte EXECUTE_ASYNCHRONOUS_REQUEST = 0x45;
    byte EXECUTE_SYNCHRONOUS_REQUEST = 0x47;
    byte CANCEL_ASYNCHRONOUS_OPERATION_REQUEST = 0x49;

    byte PARAM_OPERATION = 0x60;
    byte PARAM_RESULT_HANDLER = 0x61;
    byte PARAM_LOCATION = 0x62;
    byte PARAM_HANDLE_RESULT_FRAGMENT = 0x63;
    byte PARAM_HANDLE_RESULT_COMPLETE = 0x64;
    byte PARAM_HANDLE_CANCELLATION = 0x65;
    byte PARAM_REQUEST_ID = 0x66;
    byte PARAM_HANDLE_RESULT_FAILED = 0x67;

    byte PARAM_INPUT_STREAM = 0x68;
    byte PARAM_REQUEST_END = 0x69;
}
