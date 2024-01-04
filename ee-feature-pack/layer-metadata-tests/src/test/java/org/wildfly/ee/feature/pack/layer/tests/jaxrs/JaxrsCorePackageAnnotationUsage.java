/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.jaxrs;


import jakarta.ws.rs.core.Context;

public class JaxrsCorePackageAnnotationUsage {
    @Context
    String s;
}
