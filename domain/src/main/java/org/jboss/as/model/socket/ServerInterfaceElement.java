/**
 *
 */
package org.jboss.as.model.socket;

/**
 * A named interface definition that is associated with a Server. Differs from
 * {@link InterfaceElement} in that it is required to specify
 * either an address specification or some
 * {@link AbstractInterfaceCriteriaElement criteria} for selecting an address at
 * runtime.
 *
 * @author Brian Stansberry
 */
public class ServerInterfaceElement extends AbstractInterfaceElement<ServerInterfaceElement> {

    private static final long serialVersionUID = 3412142474527180840L;

    public ServerInterfaceElement(final String name) {
        super(name);
    }

    /**
     * Creates a new ServerInterfaceElement from an existing
     * {@link AbstractInterfaceElement#isFullySpecified() fully specified}
     * {@link InterfaceElement}.
     *
     * @param fullySpecified the element to use as a base
     *
     * @throws NullPointerException if {@code fullySpecified} is {@code null}
     * @throws IllegalArgumentException if {@code fullySpecified.isFullySpecified()}
     *             returns {@code false}.
     */
    public ServerInterfaceElement(InterfaceElement fullySpecified) {
        super(fullySpecified);
        if (!fullySpecified.isFullySpecified()) {
            throw new IllegalArgumentException(fullySpecified + " is not fully specified");
        }
    }

    @Override
    protected Class<ServerInterfaceElement> getElementClass() {
        return ServerInterfaceElement.class;
    }

}
