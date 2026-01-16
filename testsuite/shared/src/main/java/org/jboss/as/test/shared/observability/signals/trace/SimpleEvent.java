/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.signals.trace;

import java.util.Map;

public record SimpleEvent(String name,
                          long timeUnixNano,
                          Map<String, String> attributes) {
}
