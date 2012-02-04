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

package org.jboss.as.server.mgmt.domain;

/**
 * @author John Bailey
 */
public interface DomainServerProtocol {

    byte REGISTER_REQUEST = 0x00;
    byte SERVER_STARTED_REQUEST = 0x02;
    byte SERVER_RECONNECT_REQUEST = 0x03;

//    byte GET_SERVER_MODEL_REQUEST = 0x20;
//    byte RETURN_SERVER_MODEL = 0x21;
//    byte GET_SERVER_MODEL_RESPONSE = 0x22;

    byte PARAM_SERVER_NAME = 0x01;
    byte GET_FILE_REQUEST = 0x24;
    byte PARAM_FILE_PATH = 0x25;
    byte PARAM_ROOT_ID_FILE = 0x26;
    byte PARAM_ROOT_ID_CONFIGURATION = 0x27;
    byte PARAM_ROOT_ID_DEPLOYMENT = 0x28;
    byte PARAM_NUM_FILES = 0x29;
    byte FILE_START = 0x30;
    byte PARAM_FILE_SIZE = 0x31;
    byte FILE_END = 0x32;
    byte PARAM_ROOT_ID = 0x33;
}
