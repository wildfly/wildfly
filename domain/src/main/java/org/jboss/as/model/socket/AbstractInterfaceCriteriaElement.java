/**
 *
 */
package org.jboss.as.model.socket;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.Element;

/**
 * Base class for domain model elements that represent
 * {@link InterfaceCriteria the criteria for choosing an IP address} for a
 * {@link InterfaceElement named interface}.
 *
 * @author Brian Stansberry
 */
public abstract class AbstractInterfaceCriteriaElement<T extends AbstractInterfaceCriteriaElement<T>>
    extends AbstractModelElement<T> {

    private static final long serialVersionUID = 396313309912557378L;

    private final Element element;
    private InterfaceCriteria interfaceCriteria;

    /**
     * Creates a new AbstractInterfaceCriteriaElement by parsing an xml stream.
     * Subclasses using this constructor are responsible for calling
     * {@link #setInterfaceCriteria(InterfaceCriteria)} before returning from
     * their constructor.
     *
     * @param element the element being read
     */
    protected AbstractInterfaceCriteriaElement(final Element element) {
        if (element == null)
            throw new IllegalArgumentException("element is null");
        this.element = element;
    }

    /**
     * Creates a new AbstractInterfaceCriteriaElement by parsing an xml stream
     *
     * @param element the element being read
     * @param interfaceCriteria the criteria to use to check whether an network
     *         interface and address is acceptable for use by an interface
     */
    protected AbstractInterfaceCriteriaElement(final Element element, final InterfaceCriteria interfaceCriteria) {
        this(element);
        setInterfaceCriteria(interfaceCriteria);
    }

    /**
     * Gets the InterfaceCriteria associated with this element.
     *
     * @return the criteria. May be <code>null</code> if this method is invoked
     *                  before any subclass constructor has completed; otherwise
     *                  will not be <code>null</code>
     */
    public InterfaceCriteria getInterfaceCriteria() {
        return interfaceCriteria;
    }

    /**
     * Sets the InterfaceCriteria associated with this element.
     *
     * @param criteria the criteria. Cannot be <code>null</code>
     */
    protected final void setInterfaceCriteria(InterfaceCriteria criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("criteria is null");
        }
        this.interfaceCriteria = criteria;
    }

    /**
     * Gets the {@link Element} type this object represents.
     *
     * @return the element type. Will not be <code>null</code>
     */
    Element getElement() {
        return element;
    }
}
