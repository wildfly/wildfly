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

package org.jboss.as.domain.controller.mgmt;

/**
 * @author John Bailey
 */
public interface DomainControllerProtocol {
    int DOMAIN_CONTROLLER_REQUEST = 0x08;

    int PARAM_SERVER_MANAGER_ID = 0x09;
    int PARAM_SERVER_MANAGER_HOST = 0x10;
    int PARAM_SERVER_MANAGER_PORT = 0x11;
    int REGISTER_REQUEST = 0x12;
    int PARAM_DOMAIN_MODEL = 0x13;
    int REGISTER_RESPONSE = 0x14;
    int UNREGISTER_REQUEST = 0x15;
    int UNREGISTER_RESPONSE = 0x16;
    int SYNC_FILE_REQUEST = 0x17;
    int PARAM_ROOT_ID = 0x18;
    int PARAM_ROOT_ID_FILE = 0x19;
    int PARAM_ROOT_ID_CONFIGURATION = 0x20;
    int PARAM_ROOT_ID_DEPLOYMENT = 0x21;
    int PARAM_FILE_PATH = 0x22;
    int PARAM_NUM_FILES = 0x23;
    int PARAM_FILE_SIZE = 0x24;
    int FILE_START = 0x25;
    int FILE_END = 0x26;
    int SYNC_FILE_RESPONSE = 0x27;
}
