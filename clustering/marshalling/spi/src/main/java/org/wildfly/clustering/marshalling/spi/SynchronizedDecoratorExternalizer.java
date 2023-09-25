/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectOutput;
import java.util.function.UnaryOperator;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A decorator marshaller that writes the decorated object while holding its monitor lock.
 * e.g. to enable iteration over a decorated collection without the risk of a ConcurrentModificationException.
 * @author Paul Ferraro
 */
public class SynchronizedDecoratorExternalizer<T> extends DecoratorExternalizer<T> {

    /**
     * Constructs a decorator externalizer.
     * @param decoratedClass the generalized type of the decorated object
     * @param decorator the decoration function
     * @param sample a sample object used to determine the type of the decorated object
     */
    public SynchronizedDecoratorExternalizer(Class<T> decoratedClass, UnaryOperator<T> decorator, T sample) {
        super(decoratedClass, decorator, sample);
    }

    @Override
    public void writeObject(ObjectOutput output, T value) throws IOException {
        T decorated = WildFlySecurityManager.doUnchecked(value, this);
        synchronized (decorated) {
            output.writeObject(decorated);
        }
    }
}
