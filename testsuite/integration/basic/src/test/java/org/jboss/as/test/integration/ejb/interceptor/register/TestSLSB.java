/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.register;

import jakarta.ejb.Stateless;

@Stateless
public class TestSLSB implements TestRemote {

  public String invoke(String s) {
    return s;
  }

}
