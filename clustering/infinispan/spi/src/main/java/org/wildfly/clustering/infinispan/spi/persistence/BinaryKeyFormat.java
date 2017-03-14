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
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;

import org.wildfly.common.function.ExceptionBiConsumer;
import org.wildfly.common.function.ExceptionFunction;

/**
 * {@link KeyFormat} implementation for binary keys.
 * @author Paul Ferraro
 */
public class BinaryKeyFormat<K> implements KeyFormat<K> {

    private final Class<K> targetClass;
    private final ExceptionFunction<DataInput, K, IOException> reader;
    private final ExceptionBiConsumer<DataOutput, K, IOException> writer;

    public BinaryKeyFormat(Class<K> targetClass, ExceptionFunction<DataInput, K, IOException> reader, ExceptionBiConsumer<DataOutput, K, IOException> writer) {
        this.targetClass = targetClass;
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public Class<K> getTargetClass() {
        return this.targetClass;
    }

    @Override
    public K parse(String value) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(value)))) {
            return this.reader.apply(input);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String format(K key) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            this.writer.accept(output, key);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
}
