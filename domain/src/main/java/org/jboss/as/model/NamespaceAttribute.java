/**
 * 
 */
package org.jboss.as.model;

import java.io.Serializable;


/**
 * Encapsulates a namespace declaration.
 * 
 * @author Brian Stansberry
 */
public class NamespaceAttribute implements Serializable {

    private static final long serialVersionUID = 5356431350251733240L;
    
    private final String prefix;
    private final String uri;
    
    public NamespaceAttribute(String prefix, String uri) {
        this.prefix = prefix;
        
        if (uri == null) {
            throw new IllegalArgumentException("uri is null");
        }
        this.uri = uri;
        
    }
    public String getNamespaceURI() {
        return uri;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isDefaultNamespaceDeclaration() {
        return prefix == null;
    } 

}
