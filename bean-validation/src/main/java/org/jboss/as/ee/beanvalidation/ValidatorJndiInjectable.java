/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.beanvalidation;

import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.JndiViewManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;

/**
* @author Stuart Douglas
* @author Eduardo Martins
*/
final class ValidatorJndiInjectable implements ContextListAndJndiViewManagedReferenceFactory {
    private final ValidatorFactory factory;

    public ValidatorJndiInjectable(ValidatorFactory factory) {
        this.factory = factory;
    }

    @Override
    public ManagedReference getReference() {
        return new ImmediateManagedReference(factory.getValidator());
    }

    @Override
    public String getInstanceClassName() {
        // the default and safe value. A more appropriate value, for instance using the getReference() result, may be provided extending the method
        return Validator.class.getName();
    }

    @Override
    public String getJndiViewInstanceValue() {
        // the default and safe value. A more appropriate value, for instance using the getReference() result, may be provided extending the method
        return JndiViewManagedReferenceFactory.DEFAULT_JNDI_VIEW_INSTANCE_VALUE;
    }
}
