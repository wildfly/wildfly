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
public class TestComparatorExternalizer implements Externalizer<TestComparator<Object>> {

    @Override
    public void writeObject(ObjectOutput output, TestComparator<Object> object) throws IOException {
    }

    @Override
    public TestComparator<Object> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return new TestComparator<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<TestComparator<Object>> getTargetClass() {
        return (Class<TestComparator<Object>>) (Class<?>) TestComparator.class;
    }
}
