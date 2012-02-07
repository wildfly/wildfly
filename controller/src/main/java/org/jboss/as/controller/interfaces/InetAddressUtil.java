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
