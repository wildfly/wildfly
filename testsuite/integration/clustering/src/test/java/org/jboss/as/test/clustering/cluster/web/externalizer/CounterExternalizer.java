/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.externalizer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * {@link Externalizer} for {@link Counter}.
 * @author Paul Ferraro
 */
public class CounterExternalizer implements Externalizer<Counter> {

    @Override
    public void writeObject(ObjectOutput output, Counter object) throws IOException {
        output.writeInt(object.getValue());
    }

    @Override
    public Counter readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return new Counter(input.readInt());
    }

    @Override
    public Class<Counter> getTargetClass() {
        return Counter.class;
    }
}
