/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.kohsuke.MetaInfServices;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(Externalizer.class)
public class TestInvocationHandlerExternalizer implements Externalizer<TestInvocationHandler> {

    @Override
    public void writeObject(ObjectOutput output, TestInvocationHandler handler) throws IOException {
        output.writeObject(handler.getValue());
    }

    @Override
    public TestInvocationHandler readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return new TestInvocationHandler(input.readObject());
    }

    @Override
    public Class<TestInvocationHandler> getTargetClass() {
        return TestInvocationHandler.class;
    }
}
