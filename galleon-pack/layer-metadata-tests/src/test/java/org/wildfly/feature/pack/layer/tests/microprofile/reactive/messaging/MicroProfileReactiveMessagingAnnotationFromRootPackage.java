/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.feature.pack.layer.tests.microprofile.reactive.messaging;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

public class MicroProfileReactiveMessagingAnnotationFromRootPackage {
    @Outgoing("n")
    public void test() {

    }
}
