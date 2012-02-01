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
package org.jboss.as.host.controller.mgmt;

import org.jboss.as.controller.client.impl.ModelControllerProtocol;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public interface DomainControllerProtocol extends ModelControllerProtocol {
    byte REGISTER_HOST_CONTROLLER_REQUEST = 0x51;
    byte UNREGISTER_HOST_CONTROLLER_REQUEST = 0x53;
    byte GET_FILE_REQUEST = 0x55;
    byte IS_ACTIVE_REQUEST = 0x57;
    byte COMPLETE_HOST_CONTROLLER_REGISTRATION = 0x58;

    byte PARAM_HOST_ID = 0x20;
    byte PARAM_OK = 0x21;
    byte PARAM_ERROR = 0x22;
    byte PARAM_ROOT_ID = 0x24;
    byte PARAM_FILE_PATH = 0x25;
    byte PARAM_ROOT_ID_FILE = 0x26;
    byte PARAM_ROOT_ID_CONFIGURATION = 0x27;
    byte PARAM_ROOT_ID_DEPLOYMENT = 0x28;
    byte PARAM_NUM_FILES = 0x29;
    byte FILE_START = 0x30;
    byte PARAM_FILE_SIZE = 0x31;
    byte FILE_END = 0x32;
}
