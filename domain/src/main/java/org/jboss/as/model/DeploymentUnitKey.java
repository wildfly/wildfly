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

package org.jboss.as.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * An identifier for a deployment unit suitable for use as a map key.
 *
 * @author Brian Stansberry
 */
public class DeploymentUnitKey implements Serializable, Comparable<DeploymentUnitKey> {

    private static final long serialVersionUID = 8171593872559737006L;
    private final String name;
    private final byte[] sha1Hash;
    private final int hashCode;

    /**
     * Creates a new DeploymentUnitKey
     *
     * @param name the deployment's name
     * @param sha1Hash an sha1 hash of the deployment content
     */
    public DeploymentUnitKey(String name, byte[] sha1Hash) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (sha1Hash == null) {
            throw new IllegalArgumentException("sha1Hash is null");
        }
        this.name = name;
        this.sha1Hash = sha1Hash;

        // We assume a hashcode will be wanted, so calculate and cache
        int result = 17;
        result += 31 * name.hashCode();
        result += 31 * Arrays.hashCode(sha1Hash);
        this.hashCode = result;
    }

    /**
     * Gets the name of the deployment.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets a defensive copy of the sha1 hash of the deployment.
     *
     * @return the hash
     */
    public byte[] getSha1Hash() {
        return sha1Hash.clone();
    }

    /**
     * Gets the sha1 hash of the deployment as a hex string.
     *
     * @return the hash
     */
    public String getSha1HashAsHexString() {
        return AbstractModelElement.bytesToHexString(sha1Hash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof DeploymentUnitKey) {
            DeploymentUnitKey other = (DeploymentUnitKey) obj;
            return name.equals(other.name) && Arrays.equals(sha1Hash, sha1Hash);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Computes a hash of the name and sha1 hash. Exposed as a convenience
     * since {@link #getSha1Hash()} returns a defensive copy of the byte[].
     *
     * @return a hash of the name and the sha1 hash
     */
    long elementHash() {
        return name.hashCode() & 0xffffffffL ^ AbstractModelElement.calculateElementHashOf(sha1Hash);
    }

    @Override
    public int compareTo(DeploymentUnitKey o) {

        if (sha1Hash.length < o.sha1Hash.length) {
            return -1;
        }
        else if (sha1Hash.length > o.sha1Hash.length) {
            return 1;
        }

        for (int i = 0; i < sha1Hash.length; i++) {
            if (sha1Hash[i] < o.sha1Hash[i]) {
                return -1;
            }
            else if (sha1Hash[i] > o.sha1Hash[i]) {
                return 1;
            }
        }
        return name.compareTo(o.name);
    }

}
