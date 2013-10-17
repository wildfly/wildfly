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
public interface ManagementProtocol {
    // Headers
    byte[] SIGNATURE = {Byte.MAX_VALUE, Byte.MIN_VALUE, Byte.MAX_VALUE, Byte.MIN_VALUE};
    int VERSION_FIELD = 0x00; // The version field header
    int VERSION = 2; // The current protocol version

    byte TYPE = 0x1;
    byte TYPE_REQUEST = 0x2;
    byte TYPE_RESPONSE = 0x3;
    byte TYPE_BYE_BYE = 0x4;
    byte TYPE_PING = 0x5;
    byte TYPE_PONG = 0x6;

    byte REQUEST_ID = 0x10;
    byte BATCH_ID = 0x11;
    byte OPERATION_ID = 0x12;
    byte ONE_WAY = 0x13;
    byte REQUEST_BODY = 0x14;
    byte REQUEST_END = 0x15;

    byte RESPONSE_ID = 0x20;
    byte RESPONSE_TYPE = 0x21;
    byte RESPONSE_BODY = 0x22;
    byte RESPONSE_ERROR = 0x23;
    byte RESPONSE_END = 0x24;
}
