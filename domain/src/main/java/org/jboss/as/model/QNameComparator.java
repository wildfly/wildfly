package org.jboss.as.model;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Comparator;

import javax.xml.namespace.QName;

/**
 * Compares two {@link QName}s.
 * 
 * @author Brian Stansberry
 */
public class QNameComparator implements Comparator<QName>, Serializable {

    private static final long serialVersionUID = -4438373076800825294L;
    
    private static final QNameComparator INSTANCE = new QNameComparator();
    
    public static QNameComparator getInstance() {
        return INSTANCE;
    }
    
    /** Prevent external instantiation */
    private QNameComparator() {
        // 
    }
    
    /**
     * {@inheritDoc}
     * 
     * Compares two {@link QName}s, first by comparing their
     * {@link QName#getNamespaceURI() namespaces} then if necessary by comparing 
     * their {@link QName#getLocalPart() local parts} and finally if necessary
     * by comparing their {@link QName#getPrefix() prefixes}.
     */
    @Override
    public int compare(QName o1, QName o2) {
        int result = o1.getNamespaceURI().compareTo(o2.getNamespaceURI());
        if (result == 0) {
            result = o1.getLocalPart().compareTo(o2.getLocalPart());
        }
        if (result == 0) {
            result = o1.getPrefix().compareTo(o2.getPrefix());
        }
        return result;
    }
    
    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }

}
