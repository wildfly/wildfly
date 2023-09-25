/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.reflect;

public interface TriFunction<P1, P2, P3, R> {
    R apply(P1 p1, P2 p2, P3 p3);
}