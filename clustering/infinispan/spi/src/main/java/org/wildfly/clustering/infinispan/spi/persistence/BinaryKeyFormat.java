/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.infinispan.spi.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;

import org.wildfly.clustering.marshalling.spi.Serializer;

/**
 * {@link KeyFormat} implementation for binary keys.
 * @author Paul Ferraro
 */
public class BinaryKeyFormat<K> implements KeyFormat<K> {

    private final Class<K> targetClass;
    private final Serializer<K> serializer;

    public BinaryKeyFormat(Class<K> targetClass, Serializer<K> serializer) {
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
