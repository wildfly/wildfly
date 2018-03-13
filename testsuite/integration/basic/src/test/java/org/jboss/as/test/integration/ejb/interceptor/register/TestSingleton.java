/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ejb.interceptor.register;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.InitialContext;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

@Startup
@Singleton
public class TestSingleton {

  @Resource
  private SessionContext context;

  private Logger log = Logger.getLogger(TestSingleton.class.getName());

  @EJB
  private TestRemote slsbRemote;

  private TestRemote workaroundLookupEJB() throws Exception {
    log.info("Looking up java:global/remote-interface-test/TestSLSB!reproducer.TestRemote");
    return (TestRemote) new InitialContext().lookup("java:global/remote-interface-test/TestSLSB!reproducer.TestRemote");
  }

  private String wasClientInterceptorInvoked() {
    return (String) context.getContextData().get("ClientInterceptorInvoked");
  }

  @PostConstruct
  public void test() {
    Map<String,String> results = new TreeMap<>();
    Boolean success = null;
    String clientInterceptor = null;
    try {
      log.info("Testing Remote Interface");
      slsbRemote.invoke();
      success = true;
    } catch(Throwable t) {
        success = false;
        t.printStackTrace();
    } finally {
      results.put("Remote Interface Test", String.format("%b clientInterceptorInvoked: %b %s", success, (clientInterceptor!=null), clientInterceptor));
    }

    for(Map.Entry<String,String> entry : results.entrySet()) {
      log.info(String.format("%s : %s\n", entry.getKey(), entry.getValue()));
    }
  }
}