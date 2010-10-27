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

package org.jboss.as.protocol.mgmt;

/**
 * Headers used in the management protocol between the Domain Controller and
 * the Server Manager.
 *
 * @author John Bailey
 */
public interface ServerManagerProtocol {
    int SERVER_MANAGER_REQUEST = 0x25;
    int PARAM_DOMAIN_MODEL = 0x13;
    int UPDATE_FULL_DOMAIN_REQUEST = 0x26;
    int UPDATE_FULL_DOMAIN_RESPONSE = 0x27;
    int UPDATE_DOMAIN_MODEL_REQUEST = 0x28;
    int PARAM_DOMAIN_MODEL_UPDATE_COUNT = 0x29;
    int PARAM_DOMAIN_MODEL_UPDATE = 0x30;
    int PARAM_MODEL_UPDATE_RESPONSE_COUNT = 0x31;
    int PARAM_MODEL_UPDATE_RESPONSE = 0x32;
    int UPDATE_DOMAIN_MODEL_RESPONSE = 0x33;
    int UPDATE_HOST_MODEL_REQUEST = 0x34;
    int PARAM_HOST_MODEL_UPDATE_COUNT = 0x35;
    int PARAM_HOST_MODEL_UPDATE = 0x36;
    int UPDATE_HOST_MODEL_RESPONSE = 0x37;
    int IS_ACTIVE_REQUEST = 0x38;
    int IS_ACTIVE_RESPONSE = 0x39;
    int UPDATE_SERVER_MODEL_REQUEST = 0x40;
    int PARAM_SERVER_NAME = 0x41;
    int PARAM_ALLOW_ROLLBACK = 0x42;
    int PARAM_SERVER_MODEL_UPDATE_COUNT = 0x43;
    int PARAM_SERVER_MODEL_UPDATE = 0x44;
    int UPDATE_SERVER_MODEL_RESPONSE = 0x45;
    int GET_HOST_MODEL_REQUEST = 0x46;
    int GET_HOST_MODEL_RESPONSE = 0x47;
    int GET_SERVER_MODEL_REQUEST = 0x48;
    int RETURN_SERVER_MODEL = 0x49;
    int GET_SERVER_MODEL_RESPONSE = 0x50;
    int GET_SERVER_LIST_REQUEST = 0x51;
    int RETURN_SERVER_COUNT = 0x52;
    int RETURN_SERVER_NAME = 0x53;
    int RETURN_SERVER_GROUP_NAME = 0x54;
    int RETURN_SERVER_STATUS = 0x55;
    int GET_SERVER_LIST_RESPONSE = 0x57;
    int START_SERVER_REQUEST = 0x58;
    int START_SERVER_RESPONSE = 0x59;
    int STOP_SERVER_REQUEST = 0x60;
    int PARAM_GRACEFUL_TIMEOUT = 0x61;
    int STOP_SERVER_RESPONSE = 0x62;
    int RESTART_SERVER_REQUEST = 0x63;
    int RESTART_SERVER_RESPONSE = 0x64;
}
