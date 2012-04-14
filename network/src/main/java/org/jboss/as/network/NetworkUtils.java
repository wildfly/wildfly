/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.network;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

/**
 * Utility methods related to networking.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class NetworkUtils {

    private  static final boolean can_bind_to_mcast_addr; // are we running on Linux ?

    static {
        can_bind_to_mcast_addr = checkForLinux() || checkForSolaris() || checkForHp();
    }

    public static String formatPossibleIpv6Address(String address) {
        if (address == null) {
            return address;
        }
        if (!address.contains(":")) {
            return address;
        }
        if (address.startsWith("[") && address.endsWith("]")) {
            return address;
        }
        return "[" + address + "]";
    }

    public static boolean isBindingToMulticastDressSupported() {
        return can_bind_to_mcast_addr;
    }


    private static boolean checkForLinux() {
        return checkForPresence("os.name", "linux");
    }

    private static boolean checkForHp() {
        return checkForPresence("os.name", "hp");
    }

    private static boolean checkForSolaris() {
        return checkForPresence("os.name", "sun");
    }

    private static boolean checkForWindows() {
        return checkForPresence("os.name", "win");
    }

    public static boolean checkForMac() {
        return checkForPresence("os.name", "mac");
    }

    private static boolean checkForPresence(final String key, final String value) {

        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

            @Override
            public Boolean run() {
                try {
                    String tmp = System.getProperty(key);
                    return tmp != null && tmp.trim().toLowerCase(Locale.ENGLISH).startsWith(value);
                } catch (Throwable t) {
                    return false;
                }
            }
        });
    }

    // No instantiation
    private NetworkUtils() {

    }
}
