/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * {@link Formatter} implementation for binary keys.
 * @author Paul Ferraro
 */
public class BinaryFormatter<K> implements Formatter<K> {

    private final Class<K> targetClass;
    private final Serializer<K> serializer;

    public BinaryFormatter(Class<K> targetClass, Serializer<K> serializer) {
        this.targetClass = targetClass;
        this.serializer = serializer;
    }

    @Override
    public Class<K> getTargetClass() {
        return this.targetClass;
    }

    @Override
    public K parse(String value) {
        byte[] bytes = Base64.getDecoder().decode(value);
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return this.serializer.read(input);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String format(K key) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            this.serializer.write(output, key);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
}
