/**
 *
 */
package org.jboss.as.model;

import java.io.Serializable;

/**
 * A object that can resolve a named reference to another element.
 *
 * @param K the type of the reference
 * @param V the type of the element referenced
 *
 * @author Brian Stansberry
 */
public interface RefResolver<K, V extends AbstractModelElement<V>> extends Serializable {

    /**
     * Returns the element whose "name" matches the given name. What an element's
     * name is depends on it's type.
     *
     * @param ref the unique name of the element. Cannot be <code>null</code>
     *
     * @return the element, or <code>null</code> if no element with the given name is known
     *
     * @throws IllegalArgumentException if <code>ref</code> is <code>null</code>
     */
    V resolveRef(K ref);
}
