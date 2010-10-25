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
    int PARAM_SERVER_MODEL_UPDATE_COUNT = 0x42;
    int PARAM_SERVER_MODEL_UPDATE = 0x43;
    int UPDATE_SERVER_MODEL_RESPONSE = 0x44;
}
