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
package org.jboss.as.test.integration.ejb.remote.contextdata;

import java.util.HashSet;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;


/**
 * Client side JBoss interceptor.
 */
public class ClientInterceptor implements EJBClientInterceptor {

  private static final String DATA = "client data";

  /**
   * Creates a new ClientInterceptor object.
   */
  public ClientInterceptor() {
  }

  @Override
  public void handleInvocation(EJBClientInvocationContext context) throws Exception {
    HashSet<String> keys = new HashSet<>();
    keys.add("data1");
    keys.add("data2");
    context.getContextData().put("jboss.returned.keys", keys); //TODO: replace with call to org.jboss.ejb.client.EJBClientInvocationContext#addReturnedContextDataKey
    context.getContextData().put("clientData", DATA);
    // Must make this call
    context.sendRequest();
  }

  @Override
  public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
    context.getResult(); //in case there was an exception
    //we just ignore the result, and replace it with the context data
    return "DATA:" + context.getContextData().get("data1") + ":" + context.getContextData().get("data2");
  }

}
