/**
 * 
 */
package org.jboss.as.model;

import java.io.Serializable;
import java.util.Arrays;

import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A DeploymentUnitKey.
 * 
 * @author Brian Stansberry
 */
public class DeploymentUnitKey  implements Serializable {
    
    private static final long serialVersionUID = 8171593872559737006L;
    private final String name;
    private final byte[] sha1Hash;
    private final int hashCode;
    
    public DeploymentUnitKey(String name, byte[] sha1Hash) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (sha1Hash == null) {
            throw new IllegalArgumentException("sha1Hash is null");
        }
        this.name = name;
        this.sha1Hash = sha1Hash;
        
        int result = 17;
        result += 31 * name.hashCode();
        result += 31 * Arrays.hashCode(sha1Hash);
        this.hashCode = result;
    }

    public String getName() {
        return name;
    }

    public byte[] getSha1Hash() {
        return sha1Hash.clone();
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
    
    long elementHash() {
        return name.hashCode() & 0xffffffffL ^ AbstractModelElement.calculateElementHashOf(sha1Hash);
    }
    
    void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        streamWriter.writeAttribute(Attribute.SHA1.getLocalName(), AbstractModelElement.bytesToHexString(sha1Hash));
    }
    
}
