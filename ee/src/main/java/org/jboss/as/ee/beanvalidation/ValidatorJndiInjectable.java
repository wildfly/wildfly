package org.jboss.as.ee.beanvalidation;

import javax.validation.ValidatorFactory;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.msc.value.ImmediateValue;

/**
* @author Stuart Douglas
*/
final class ValidatorJndiInjectable implements ManagedReferenceFactory {
    private final ValidatorFactory factory;

    public ValidatorJndiInjectable(ValidatorFactory factory) {
        this.factory = factory;
    }

    @Override
    public ManagedReference getReference() {
        return new ValueManagedReference(new ImmediateValue<Object>(factory.getValidator()));
    }
}
