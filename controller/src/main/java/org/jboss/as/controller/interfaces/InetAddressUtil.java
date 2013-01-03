/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.interfaces;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

/**
 * @author Tomaz Cerar
 * @created 26.1.12 22:47
 */
public class InetAddressUtil {
    /**
     * Methods returns InetAddress for localhost
     *
     * @return InetAddress of the localhost
     * @throws UnknownHostException if localhost could not be resolved
     */
    public static InetAddress getLocalHost() throws UnknownHostException {
        InetAddress addr;
        try {
            addr = InetAddress.getLocalHost();
        } catch (ArrayIndexOutOfBoundsException e) {  //this is workaround for mac osx bug see AS7-3223 and JGRP-1404
            addr = InetAddress.getByName(null);
        }
        return addr;
    }

    public static String getLocalHostName() {
        try {
            InetAddress address = getLocalHost();
            return address != null ? address.getHostName() : null;
        } catch (UnknownHostException e) {
            throw MESSAGES.cannotDetermineDefaultName(e);
        }
    }
}
