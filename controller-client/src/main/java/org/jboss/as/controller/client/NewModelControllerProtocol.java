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

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public interface NewModelControllerProtocol {

    byte EXECUTE_REQUEST = 0x45;
    byte HANDLE_REPORT_REQUEST = 0x46;
    byte OPERATION_FAILED_REQUEST = 0x47;
    byte OPERATION_COMPLETED_REQUEST = 0x48;
    byte OPERATION_PREPARED_REQUEST = 0x49;
    byte COMMIT_TRANSACTION_REQUEST = 0x50;
    byte ROLLBACK_TRANSACTION_REQUEST = 0x51;
    byte GET_INPUTSTREAM_REQUEST = 0x52;


    byte PARAM_END = 0x60;
    byte PARAM_OPERATION = 0x61;
    byte PARAM_EXECUTION_ID = 0x62;
    byte PARAM_MESSAGE_SEVERITY = 0x63;
    byte PARAM_MESSAGE = 0x64;
    byte PARAM_RESPONSE = 0x65;
    byte PARAM_INPUTSTREAMS_LENGTH = 0x66;
    byte PARAM_INPUTSTREAM_INDEX = 0x67;
    byte PARAM_INPUTSTREAM_LENGTH = 0x68;
    byte PARAM_INPUTSTREAM_CONTENTS = 0x69;
}
