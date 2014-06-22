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

import org.jboss.as.controller.logging.ControllerLogger;

/**
 * @author Tomaz Cerar
 * @created 26.1.12 22:47
 */
public class InetAddressUtil {

    private static final int MAX_GROUP_LENGTH = 4;
    private static final int IPV6_LEN = 8;

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
            return address != null ? canonize(address.getHostName()) : null;
        } catch (UnknownHostException e) {
            throw ControllerLogger.ROOT_LOGGER.cannotDetermineDefaultName(e);
        }
    }

    /**
     * <p>Convert IPv6 adress into RFC 5952 form.
     * E.g. 2001:db8:0:1:0:0:0:1 -> 2001:db8:0:1::1</p>
     *
     * <p>Method is null safe, and if IPv4 address or host name is passed to the
     * method it is returned wihout any processing.</p>
     *
     * <p>Method also supports IPv4 in IPv6 (e.g. 0:0:0:0:0:ffff:192.0.2.1 ->
     * ::ffff:192.0.2.1), and zone ID (e.g. fe80:0:0:0:f0f0:c0c0:1919:1234%4
     * -> fe80::f0f0:c0c0:1919:1234%4).</p>
     *
     * @param ipv6Address String representing valid IPv6 address.
     * @return String representing IPv6 in canonical form.
     * @throws IllegalArgumentException if IPv6 format is unacceptable.
     */
    public static String canonize(String ipv6Address) throws IllegalArgumentException {

        if (ipv6Address == null) {
            return null;
        }

        // Definitely not an IPv6, return untouched input.
        if (!mayBeIPv6Address(ipv6Address)) {
            return ipv6Address;
        }

        // Length without zone ID (%zone) or IPv4 address
        int ipv6AddressLength = ipv6Address.length();
        if (isIPv4AddressInIPv6(ipv6Address)) {
            // IPv4 in IPv6
            // e.g. 0:0:0:0:0:FFFF:127.0.0.1
            int lastColonPos = ipv6Address.lastIndexOf(":");
            int lastColonsPos = ipv6Address.lastIndexOf("::");
            if (lastColonsPos >= 0 && lastColonPos == lastColonsPos + 1) {
                // IPv6 part ends with two consecutive colons, last colon is part of IPv6 format.
                // e.g. ::127.0.0.1
                ipv6AddressLength = lastColonPos + 1;
            } else {
                // IPv6 part ends with only one colon, last colon is not part of IPv6 format.
                // e.g. ::FFFF:127.0.0.1
                ipv6AddressLength = lastColonPos;
            }
        } else if (ipv6Address.contains(":") && ipv6Address.contains("%")) {
            // Zone ID
            // e.g. fe80:0:0:0:f0f0:c0c0:1919:1234%4
            ipv6AddressLength = ipv6Address.lastIndexOf("%");
        }

        StringBuilder result = new StringBuilder();
        char [][] groups = new char[IPV6_LEN][MAX_GROUP_LENGTH];
        int groupCounter = 0;
        int charInGroupCounter = 0;

        // Index of the current zeroGroup, -1 means not found.
        int zeroGroupIndex = -1;
        int zeroGroupLength = 0;

        // maximum length zero group, if there is more then one, then first one
        int maxZeroGroupIndex = -1;
        int maxZeroGroupLength = 0;

        boolean isZero = true;
        boolean groupStart = true;

        /*
         *  Two consecutive colons, initial expansion.
         *  e.g. 2001:db8:0:0:1::1 -> 2001:db8:0:0:1:0:0:1
         */
        StringBuilder expanded = new StringBuilder(ipv6Address);
        int colonsPos = ipv6Address.indexOf("::");
        int length = ipv6AddressLength;
        int change = 0;

        if (colonsPos >= 0 && colonsPos < ipv6AddressLength - 2) {
            int colonCounter = 0;
            for (int i = 0; i < ipv6AddressLength; i++) {
                if (ipv6Address.charAt(i) == ':') {
                    colonCounter++;
                }
            }

            if (colonsPos == 0) {
                expanded.insert(0, "0");
                change = change + 1;
            }

            for (int i = 0; i < IPV6_LEN - colonCounter; i++) {
                expanded.insert(colonsPos + 1, "0:");
                change = change + 2;
            }


            if (colonsPos == ipv6AddressLength - 2) {
                expanded.setCharAt(colonsPos + change + 1, '0');
            } else {
                expanded.deleteCharAt(colonsPos + change + 1);
                change = change - 1;
            }
            length = length + change;
        }


        // Processing one char at the time
        for (int charCounter = 0; charCounter < length; charCounter++) {
            char c = expanded.charAt(charCounter);
            if (c >= 'A' && c <= 'F') {
                c = (char) (c + 32);
            }
            if (c != ':') {
                groups[groupCounter][charInGroupCounter] = c;
                if (!(groupStart && c == '0')) {
                    ++charInGroupCounter;
                    groupStart = false;
                }
                if (c != '0') {
                    isZero = false;
                }
            }
            if (c == ':' || charCounter == (length - 1)) {
                // We reached end of current group
                if (isZero) {
                    ++zeroGroupLength;
                    if (zeroGroupIndex == -1) {
                        zeroGroupIndex = groupCounter;
                    }
                }

                if (!isZero || charCounter == (length - 1)) {
                    // We reached end of zero group
                    if (zeroGroupLength > maxZeroGroupLength) {
                        maxZeroGroupLength = zeroGroupLength;
                        maxZeroGroupIndex = zeroGroupIndex;
                    }
                    zeroGroupLength = 0;
                    zeroGroupIndex = -1;
                }
                ++groupCounter;
                charInGroupCounter = 0;
                isZero = true;
                groupStart = true;
            }
        }

        int numberOfGroups = groupCounter;

        // Output results
        for (groupCounter = 0; groupCounter < numberOfGroups; groupCounter++) {
            if (maxZeroGroupLength <= 1 || groupCounter < maxZeroGroupIndex
                    || groupCounter >= maxZeroGroupIndex + maxZeroGroupLength) {
                for (int j = 0; j < MAX_GROUP_LENGTH; j++) {
                    if (groups[groupCounter][j] != 0) {
                        result.append(groups[groupCounter][j]);
                    }
                }
                if (groupCounter < (numberOfGroups - 1)
                        && (groupCounter != maxZeroGroupIndex - 1
                                || maxZeroGroupLength <= 1)) {
                    result.append(':');
                }
            } else if (groupCounter == maxZeroGroupIndex) {
                result.append("::");
            }
        }

        // Solve problem with three colons in IPv4 in IPv6 format
        // e.g. 0:0:0:0:0:0:127.0.0.1 -> :::127.0.0.1 -> ::127.0.0.1
        int resultLength = result.length();
        if (result.charAt(resultLength - 1) == ':' && ipv6AddressLength < ipv6Address.length()
                && ipv6Address.charAt(ipv6AddressLength) == ':') {
            result.delete(resultLength - 1, resultLength);
        }

        /*
         * Append IPv4 from IPv4-in-IPv6 format or Zone ID
         */
        for (int i = ipv6AddressLength; i < ipv6Address.length(); i++) {
            result.append(ipv6Address.charAt(i));
        }

        return result.toString();
    }

    /**
     * Heuristic check if string might be an IPv6 address.
     *
     * @param input Any string or null
     * @return true, if input string contains only hex digits and at least two colons, before '.' or '%' character.
     */
    private static boolean mayBeIPv6Address(String input) {
        if (input == null) {
            return false;
        }

        boolean result = false;
        int colonsCounter = 0;
        int length = input.length();
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if (c == '.' || c == '%') {
                // IPv4 in IPv6 or Zone ID detected, end of checking.
                break;
            }
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F') || c == ':')) {
                return false;
            } else if (c == ':') {
                colonsCounter++;
            }
        }
        if (colonsCounter >= 2) {
            result = true;
        }
        return result;
    }
    /**
     * Check if it is an IPv4 in IPv6 format.
     * e.g. 0:0:0:0:0:FFFF:127.0.0.1
     *
     * @param ipv6Address the address
     * @return true, if input string is an IPv4 address in IPv6 format.
     */
    private static boolean isIPv4AddressInIPv6(String ipv6Address) {
        return (ipv6Address.contains(":") && ipv6Address.contains("."));
    }
}
