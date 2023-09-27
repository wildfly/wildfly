/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.marshall;

import org.infinispan.commons.dataconversion.MediaType;

/**
 * Extends Infinispan's {@link EncoderRegistry} adding the ability to unregister transcoders.
 * @author Paul Ferraro
 */
public interface EncoderRegistry extends org.infinispan.marshall.core.EncoderRegistry {

    void unregisterTranscoder(MediaType type);
}
