/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.feature.pack.layer.tests.microprofile.openapi;

import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

@SecurityRequirement(name = "s")
public class OpenApiAnnotationFromSecurityPackageUsage {
}
