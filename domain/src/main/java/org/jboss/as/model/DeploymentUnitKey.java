/**
 * 
 */
package org.jboss.as.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * An identifier for a deployment unit suitable for use as a map key.
 * 
 * @author Brian Stansberry
 */
public class DeploymentUnitKey  implements Serializable {
    
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
    
}
