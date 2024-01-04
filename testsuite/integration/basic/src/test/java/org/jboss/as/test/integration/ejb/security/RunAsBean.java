/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security;

import jakarta.annotation.security.RunAs;
import jakarta.ejb.Stateful;

/**
 * User: jpai
 */
@Stateful
@RunAs("SpiderMan")
public class RunAsBean {
}
