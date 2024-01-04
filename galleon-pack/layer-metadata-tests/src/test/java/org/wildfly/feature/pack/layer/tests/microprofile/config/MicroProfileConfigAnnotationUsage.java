/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.feature.pack.layer.tests.microprofile.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public class MicroProfileConfigAnnotationUsage {
    @ConfigProperty
    private String test;
}
