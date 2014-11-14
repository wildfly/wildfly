/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.repository;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class ContentReference {

    private final String contentIdentifier;
    private final String hexHash;

    public ContentReference(String contentIdentifier, String hexHash) {
        this.contentIdentifier = contentIdentifier;
        if (hexHash == null) {
            this.hexHash = "";
        } else {
            this.hexHash = hexHash;
        }
    }

    public ContentReference(String contentIdentifier, byte[] hash) {
        this(contentIdentifier, hash, System.currentTimeMillis());
    }

    public ContentReference(String contentIdentifier, byte[] hash, long timestamp) {
        this.contentIdentifier = contentIdentifier;
        if (hash == null || hash.length == 0) {
            this.hexHash = "";
        } else {
            this.hexHash = HashUtil.bytesToHexString(hash);
        }
    }

    public String getContentIdentifier() {
        return contentIdentifier;
    }

    public String getHexHash() {
        return hexHash;
    }

    public byte[] getHash() {
        if(hexHash.isEmpty()) {
            return new byte[0];
        }
        return HashUtil.hexStringToByteArray(hexHash);
    }

    @Override
    public int hashCode() {
        int hashCode = 7;
        hashCode = 43 * hashCode + (this.contentIdentifier != null ? this.contentIdentifier.hashCode() : 0);
        hashCode = 43 * hashCode + (this.hexHash != null ? this.hexHash.hashCode() : 0);
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ContentReference other = (ContentReference) obj;
        if (! ((this.contentIdentifier == other.contentIdentifier)
                || (this.contentIdentifier != null && this.contentIdentifier.equals(other.contentIdentifier)))) {
            return false;
        }
        if (! ((this.hexHash == other.hexHash) || (this.hexHash != null && this.hexHash.equals(other.hexHash)))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ContentReference{" + "contentIdentifier=" + contentIdentifier + ", hexHash=" + hexHash + '}';
    }

}
