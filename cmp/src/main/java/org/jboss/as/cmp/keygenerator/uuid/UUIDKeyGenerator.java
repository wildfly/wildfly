/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.keygenerator.uuid;

import java.net.InetAddress;
import java.security.SecureRandom;
import org.jboss.as.cmp.keygenerator.KeyGenerator;


/**
 * The implementation of UUID key generator
 * based on the algorithm from Floyd Marinescu's EJB Design Patterns.
 *
 * @author <a href="mailto:loubyansky@ukr.net">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public class UUIDKeyGenerator implements KeyGenerator {

    // Attributes ---------------------------------------------------

    /**
     * secure random to provide nonrepeating seed
     */
    SecureRandom seeder;

    /**
     * cached middle value
     */
    private String midValue;

    // Constructor --------------------------------------------------

    public UUIDKeyGenerator()
            throws Exception {
        // cache the middle part for UUID

        StringBuffer buffer = new StringBuffer(16);

        // construct host part of the uuid (8 hex digits)
        byte[] addr = InetAddress.getLocalHost().getAddress();
        buffer.append(toHex(toInt(addr), 8));

        // append the hash code for this object (8 hex digits)
        buffer.append(toHex(System.identityHashCode(this), 8));

        // set up midValue
        midValue = buffer.toString();

        // load up the randomizer
        seeder = new SecureRandom();
        int node = seeder.nextInt();
    }

    // KeyGenerator implementation ----------------------------------

    public Object generateKey() {
        StringBuffer buffer = new StringBuffer(32);

        // append current time as unsigned int value
        buffer.append(toHex((int) (System.currentTimeMillis() & 0xFFFFFFFF), 8));

        // append cached midValue
        buffer.append(midValue);

        // append the next random int
        buffer.append(toHex(seeder.nextInt(), 8));

        // return the result
        return buffer.toString();
    }

    // Private ------------------------------------------------------

    /**
     * Converts int value to string hex representation
     */
    private String toHex(int value, int length) {
        // hex digits
        char[] hexDigits =
                {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        StringBuffer buffer = new StringBuffer(length);
        int shift = (length - 1) << 2;
        int i = -1;
        while (++i < length) {
            buffer.append(hexDigits[(value >> shift) & 0x0000000F]);
            value <<= 4;
        }
        return buffer.toString();
    }

    /**
     * Constructs int value from byte array
     */
    private static int toInt(byte[] bytes) {
        int value = 0;
        int i = -1;
        while (++i < bytes.length) {
            value <<= 8;
            int b = bytes[i] & 0xff;
            value |= b;
        }
        return value;
    }
}
