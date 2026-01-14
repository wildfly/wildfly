/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.signals.trace;

import java.util.Map;

public record Link(String traceId,
                   String spanId,
                   String traceState,
                   Map<String, String> attributes,
                   int droppedAttributesCount,
                   int flags) {
}
