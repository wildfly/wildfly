/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.ordering;

import java.io.Serializable;
import java.util.List;

import javax.ejb.Stateful;
import javax.enterprise.context.ApplicationScoped;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;

@Stateful
@ApplicationScoped
@CdiIntercepted
@Interceptors(LegacyInterceptor.class)
public class InterceptedBean implements Serializable {

    private static final long serialVersionUID = -4444919869290540443L;

    public void ping(List<String> list) {
        list.add("InterceptedBean");
    }

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        Object[] parameters = ctx.getParameters();
        @SuppressWarnings("unchecked")
        List<String> sequence = (List<String>) parameters[0];
        sequence.add("TargetClassInterceptor");
        return ctx.proceed();
    }
}
