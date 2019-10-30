package org.jboss.as.naming;

/**
 * @author Stuart Douglas
 */
public class ImmediateManagedReferenceFactory implements ManagedReferenceFactory {

    private final ManagedReference reference;

    public ImmediateManagedReferenceFactory(final Object value) {
        this.reference = new ImmediateManagedReference(value);
    }

    @Override
    public ManagedReference getReference() {
        return reference;
    }
}
