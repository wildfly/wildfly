/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author Paul Ferraro
 */
public interface UUIDBuilder extends Supplier<UUID> {

    UUIDBuilder setMostSignificantBits(long bits);

    UUIDBuilder setLeastSignificantBits(long bits);
}
