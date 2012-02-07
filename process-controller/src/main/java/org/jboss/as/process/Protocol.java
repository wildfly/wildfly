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

package org.jboss.as.process;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Protocol {

    private Protocol() {
    }

    // inbound messages
    public static final int AUTH = 0xEE;

    // inbound messages (SM only)
    public static final int ADD_PROCESS = 0x10;
    public static final int START_PROCESS = 0x11;
    public static final int STOP_PROCESS = 0x12;
    public static final int REMOVE_PROCESS = 0x13;
    public static final int SEND_STDIN = 0x14;
    public static final int REQUEST_PROCESS_INVENTORY = 0x15;
    public static final int RECONNECT_PROCESS = 0x16;
    public static final int SHUTDOWN = 0x17;

    // outbound messages

    // outbound messages (SM only)
    public static final int PROCESS_ADDED = 0x10;
    public static final int PROCESS_STARTED = 0x11;
    public static final int PROCESS_STOPPED = 0x12;
    public static final int PROCESS_REMOVED = 0x13;
    public static final int PROCESS_INVENTORY = 0x14;
    public static final int PROCESS_RECONNECTED = 0x15;
    public static final int OPERATION_FAILED = 0x16;
}
