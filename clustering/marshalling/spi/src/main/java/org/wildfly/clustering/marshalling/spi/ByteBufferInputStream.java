/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

/**
 * @author Paul Ferraro
 */
public class ByteBufferInputStream extends ByteArrayInputStream {

    public ByteBufferInputStream(ByteBuffer buffer) {
        super(buffer.array(), buffer.arrayOffset(), buffer.limit() - buffer.arrayOffset());
    }
}
