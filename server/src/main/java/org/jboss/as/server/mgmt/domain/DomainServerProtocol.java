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
    int SERVER_TO_HOST_CONTROLLER_OPERATION = Byte.MAX_VALUE; // TODO: Correct
    int REGISTER_REQUEST = 0x00;
    int PARAM_SERVER_NAME = 0x01;


//    int SERVER_MODEL_UPDATES_REQUEST = 0x10;
//    int PARAM_ALLOW_ROLLBACK = 0x28;
//    int PARAM_SERVER_MODEL_UPDATE_COUNT = 0x29;
//    int PARAM_SERVER_MODEL_UPDATE = 0x30;
//    int PARAM_SERVER_MODEL_UPDATE_RESPONSE_COUNT = 0x31;
//    int PARAM_SERVER_MODEL_UPDATE_RESPONSE = 0x32;
//    int SERVER_MODEL_UPDATES_RESPONSE = 0x15;

    int GET_SERVER_MODEL_REQUEST = 0x20;
    int RETURN_SERVER_MODEL = 0x21;
    int GET_SERVER_MODEL_RESPONSE = 0x22;

}
