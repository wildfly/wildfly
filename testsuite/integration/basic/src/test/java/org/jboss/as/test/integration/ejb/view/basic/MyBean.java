/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.view.basic;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

/**
 * User: jpai
 */
@Local
@Stateless
public class MyBean implements MyInterface {
}
