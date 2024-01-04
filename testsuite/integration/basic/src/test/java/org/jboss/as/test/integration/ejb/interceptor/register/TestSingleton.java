/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.register;

import jakarta.ejb.EJB;
import jakarta.ejb.NoSuchEJBException;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Startup
@Singleton
public class TestSingleton {

  @EJB
  private TestRemote slsbRemote;

  public String test(String echo) throws NoSuchEJBException {
    return slsbRemote.invoke(echo);
  }
}
