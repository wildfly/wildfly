/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.ee.feature.pack.layer.tests.jakarta.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.data.spi.EntityDefining;

@EntityDefining
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JakartaDataAnnotationInSpiPackageUsage {
}
