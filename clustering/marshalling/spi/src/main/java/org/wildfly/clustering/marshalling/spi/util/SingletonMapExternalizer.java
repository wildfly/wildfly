/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Map;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * @author Paul Ferraro
 */
public class SingletonMapExternalizer implements Externalizer<Map<Object, Object>> {

    @Override
    public void writeObject(ObjectOutput output, Map<Object, Object> map) throws IOException {
        Map.Entry<Object, Object> entry = map.entrySet().iterator().next();
        output.writeObject(entry.getKey());
        output.writeObject(entry.getValue());
    }

    @Override
    public Map<Object, Object> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return Collections.singletonMap(input.readObject(), input.readObject());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Map<Object, Object>> getTargetClass() {
        return (Class<Map<Object, Object>>) Collections.singletonMap(null, null).getClass();
    }
}
