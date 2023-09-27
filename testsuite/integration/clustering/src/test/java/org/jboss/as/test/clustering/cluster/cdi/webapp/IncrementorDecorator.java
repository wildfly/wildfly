/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.cdi.webapp;

import java.io.Serializable;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

import org.jboss.as.test.clustering.cluster.ejb.stateful.bean.Incrementor;

/**
 * Test that Weld's {@link Decorator} impl serializes and deserializes on a remote note.
 *
 * @author Radoslav Husar
 */
@Decorator
@Priority(Interceptor.Priority.APPLICATION)
public class IncrementorDecorator implements Incrementor, Serializable {
    private static final long serialVersionUID = 7389752076482339566L;

    @Inject
    @Delegate
    private Incrementor delegate;

    @Override
    public int increment() {
        return delegate.increment();
    }
}
