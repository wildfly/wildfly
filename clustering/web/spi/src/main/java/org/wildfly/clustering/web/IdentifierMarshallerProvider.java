/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web;

import java.nio.ByteBuffer;

import org.wildfly.clustering.marshalling.spi.Marshaller;

/**
 * @author Paul Ferraro
 */
public interface IdentifierMarshallerProvider {
    Marshaller<String, ByteBuffer> getMarshaller();
}
