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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Locale;

/**
 * Utility methods related to networking.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class NetworkUtils {

    private static final int IPV6_LEN = 8;
    private static final boolean can_bind_to_mcast_addr; // are we running on Linux ?

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

    /**
     * Formats input address. For IPV4 returns simply host address, for IPV6 formats address according to <a
     * href="http://tools.ietf.org/html/rfc5952">RFC5952</a> rules. It does not embed IPV6 address in '[', ']', since those are part of IPV6 URI literal.
     *
     * @param inet
     * @return
     */
    public static String formatAddress(InetAddress inet){
        if(inet == null){
            throw new NullPointerException();
        }
        if(inet instanceof Inet4Address){
            return inet.getHostAddress();
        } else if (inet instanceof Inet6Address){
            byte[] byteRepresentation = inet.getAddress();
            int[] hexRepresentation = new int[IPV6_LEN];

            for(int i=0;i < hexRepresentation.length;i++){
                hexRepresentation[i] = ( byteRepresentation[2*i] & 0xFF) << 8 | ( byteRepresentation[2*i+1] & 0xFF );
            }
            compactLongestZeroSequence(hexRepresentation);
            return formatAddress6(hexRepresentation);
        } else {
            return inet.getHostAddress();
        }
    }

    /**
     * Converts socket address into string literal, which has form: 'address:port'. Example:<br>
     * <ul>
     *      <li>127.0.0.1:8080</li>
     *      <li>dns.name.com:8080</li>
     *      <li>[0fe:1::20]:8080</li>
     *      <li>[::1]:8080</li>
     * </ul>
     * @param inet
     * @return
     */
    public static String formatAddress(InetSocketAddress inet){
        if(inet == null){
            throw new NullPointerException();
        }
        StringBuilder result = new StringBuilder();
        if(inet.isUnresolved()){
            result.append(inet.getHostName());
        }else{
            result.append(formatPossibleIpv6Address(formatAddress(inet.getAddress())));
        }
        result.append(":").append(inet.getPort());
        return result.toString();
    }

    /**
     * Converts IPV6 int[] representation into valid IPV6 string literal. Sequence of '-1' values are converted into '::'.
     * @param hexRepresentation
     * @return
     */
    private static String formatAddress6(int[] hexRepresentation){
       if(hexRepresentation == null){
           throw new NullPointerException();
       }
       if(hexRepresentation.length != IPV6_LEN){
           throw new IllegalArgumentException();
       }
       StringBuilder stringBuilder = new StringBuilder();
       boolean inCompressedSection = false;
       for(int i = 0;i<hexRepresentation.length;i++){
           if(hexRepresentation[i] == -1){
               if(!inCompressedSection){
                   inCompressedSection = true;
                   if(i == 0){
                       stringBuilder.append("::");
                   } else {
                       stringBuilder.append(":");
                   }
               }
           } else {
               inCompressedSection = false;
               stringBuilder.append(Integer.toHexString(hexRepresentation[i]));
               if(i+1<hexRepresentation.length){
                   stringBuilder.append(":");
               }
           }
       }
       return stringBuilder.toString();
    }

    public static boolean isBindingToMulticastDressSupported() {
        return can_bind_to_mcast_addr;
    }

    private static void compactLongestZeroSequence(int[] hexRepresentatoin){
        int bestRunStart = -1;
        int bestRunLen = -1;
        boolean inRun = false;
        int runStart = -1;
        for(int i=0;i<hexRepresentatoin.length;i++){

            if(hexRepresentatoin[i] == 0){
                if(!inRun){
                    runStart = i;
                    inRun = true;
                }
            } else {
                if(inRun){
                    inRun = false;
                    int runLen = i - runStart;
                    if(bestRunLen < 0){
                        bestRunStart = runStart;
                        bestRunLen = runLen;
                    } else {
                        if(runLen > bestRunLen){
                            bestRunStart = runStart;
                            bestRunLen = runLen;
                        }
                    }
                }
            }
        }
        if(bestRunStart >=0){
            Arrays.fill(hexRepresentatoin, bestRunStart, bestRunStart + bestRunLen, -1);
        }
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
