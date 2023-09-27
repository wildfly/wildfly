/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.cdi;

import jakarta.inject.Inject;

public class CdiInjectClass {
    @Inject
    String s;
}
