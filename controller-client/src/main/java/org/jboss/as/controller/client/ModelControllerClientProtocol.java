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
    int EXECUTE_ASYNCHRONOUS_REQUEST = 0x45;
    int EXECUTE_ASYNCHRONOUS_RESPONSE = 0x46;
    int EXECUTE_SYNCHRONOUS_REQUEST = 0x47;
    int EXECUTE_SYNCHRONOUS_RESPONSE = 0x48;
    int CANCEL_ASYNCHRONOUS_OPERATION_REQUEST = 0x49;
    int CANCEL_ASYNCHRONOUS_OPERATION_RESPONSE = 0x50;

    int PARAM_OPERATION = 0x60;
    int PARAM_RESULT_HANDLER = 0x61;
    int PARAM_LOCATION = 0x52;
    int PARAM_HANDLE_RESULT_FRAGMENT = 0x63;
    int PARAM_HANDLE_RESULT_COMPLETE = 0x64;
    int PARAM_HANDLE_CANCELLATION = 0x65;
    int PARAM_REQUEST_ID = 0x66;
    int PARAM_HANDLE_RESULT_FAILED = 0x67;

}
