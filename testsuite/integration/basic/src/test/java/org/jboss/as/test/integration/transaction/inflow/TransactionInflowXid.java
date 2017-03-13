/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.transaction.inflow;

import java.net.Inet4Address;
import java.util.Arrays;
import java.util.Random;

import javax.transaction.xa.Xid;

/**
 * Test {@link Xid} implementation.
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
class TransactionInflowXid implements Xid {
    private static byte[] localIP = null;
    private static int txnUniqueID = 0;

    public int formatId;
    public byte[] gtrid;
    public byte[] bqual;

    public byte[] getGlobalTransactionId() {
        return gtrid;
    }

    public byte[] getBranchQualifier() {
        return bqual;
    }

    public int getFormatId() {
        return formatId;
    }

    private TransactionInflowXid(int formatId, byte[] gtrid, byte[] bqual) {
        this.formatId = formatId;
        this.gtrid = gtrid;
        this.bqual = bqual;
    }

    public String toString() {
        int hexVal;
        StringBuffer sb = new StringBuffer(512);
        sb.append("formatId=" + formatId);
        sb.append(" gtrid(" + gtrid.length + ")={0x");
        for (int i = 0; i < gtrid.length; i++) {
            hexVal = gtrid[i] & 0xFF;
            if (hexVal < 0x10)
                sb.append("0" + Integer.toHexString(gtrid[i] & 0xFF));
            else
                sb.append(Integer.toHexString(gtrid[i] & 0xFF));
        }
        sb.append("} bqual(" + bqual.length + ")={0x");
        for (int i = 0; i < bqual.length; i++) {
            hexVal = bqual[i] & 0xFF;
            if (hexVal < 0x10)
                sb.append("0" + Integer.toHexString(bqual[i] & 0xFF));
            else
                sb.append(Integer.toHexString(bqual[i] & 0xFF));
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Returns a globally unique transaction id.
     *
     * Xid "number" is based on provided tid argument,
     * inet local host address, static counter of generated ids
     * and a random int number.
     *
     * Xid format is static value 4660.
     */
    static Xid getUniqueXid(int tid) {
        Random rnd = new Random(System.currentTimeMillis());
        txnUniqueID++;
        int txnUID = txnUniqueID;
        int tidID = tid;
        int randID = rnd.nextInt();

        return getXid(txnUID, tidID, randID);
    }

    /**
     * Returns a transaction id which is based on tid
     * calculated the same all the time.
     *
     * Variables which are part of the calculation
     * are inet local host address.
     *
     * Xid format is static value 4660.
     */
    static Xid getStableXid(int tid) {
        int txnUID = 0;
        int tidID = tid;
        int answerToEverythingID = 42;

        return getXid(txnUID, tidID, answerToEverythingID);
    }

    private static Xid getXid(int txnUID, int tidID, int quaziRandID) {
        byte[] gtrid = new byte[64];
        byte[] bqual = new byte[64];

        if (null == localIP) {
            try {
                localIP = Inet4Address.getLocalHost().getAddress();
            } catch (Exception ex) {
                localIP = new byte[] { 0x01, 0x02, 0x03, 0x04 };
            }
        }

        // global transaction qualifier
        System.arraycopy(localIP, 0, gtrid, 0, 4);
        // branch transaction qualifier
        System.arraycopy(localIP, 0, bqual, 0, 4);

        for (int i = 0; i <= 3; i++) {
            gtrid[i + 4] = (byte) (txnUID % 0x100);
            bqual[i + 4] = (byte) (txnUID % 0x100);
            txnUID >>= 8;
            gtrid[i + 8] = (byte) (tidID % 0x100);
            bqual[i + 8] = (byte) (tidID % 0x100);
            tidID >>= 8;
            gtrid[i + 12] = (byte) (quaziRandID % 0x100);
            bqual[i + 12] = (byte) (quaziRandID % 0x100);
            quaziRandID >>= 8;
        }
        return new TransactionInflowXid(0x1234, gtrid, bqual);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(bqual);
        result = prime * result + formatId;
        result = prime * result + Arrays.hashCode(gtrid);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TransactionInflowXid other = (TransactionInflowXid) obj;
        if (!Arrays.equals(bqual, other.bqual))
            return false;
        if (formatId != other.formatId)
            return false;
        if (!Arrays.equals(gtrid, other.gtrid))
            return false;
        return true;
    }
}
