/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.preview.persistence.cdi;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

/**
 * Associatiate Pu2Qualifier with persistence unit: pu1
 *
 * @author Scott Marlow
 */
@Qualifier
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface Pu2Qualifier {

}
