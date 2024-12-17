/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.jboss.externalizer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jboss.marshalling.Externalizer;

/**
 * Adapts a {@link org.wildfly.clustering.marshalling.Externalizer} to a JBoss Marshalling {@link org.jboss.marshalling.Externalizer}.
 * @author Paul Ferraro
 */
@Deprecated
public class ExternalizerAdapter implements Externalizer {
    private static final long serialVersionUID = 1714120446322944436L;

    private final org.wildfly.clustering.marshalling.Externalizer<Object> externalizer;

    @SuppressWarnings("unchecked")
    public ExternalizerAdapter(org.wildfly.clustering.marshalling.Externalizer<?> externalizer) {
        this.externalizer = (org.wildfly.clustering.marshalling.Externalizer<Object>) externalizer;
    }

    @Override
    public void writeExternal(Object subject, ObjectOutput output) throws IOException {
        this.externalizer.writeObject(output, subject);
    }

    @Override
    public Object createExternal(Class<?> subjectType, ObjectInput input) throws IOException, ClassNotFoundException {
        return this.externalizer.readObject(input);
    }
}
