/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.test.integration.weld.ejb.interceptor.interceptorsorderwithexclusions;

import jakarta.ejb.Stateless;
import jakarta.interceptor.ExcludeClassInterceptors;
import jakarta.interceptor.Interceptors;

/**
 * @author Marius Bogoevici
 */
@Stateless
@Counted
@Interceptors(EjbInterceptor.class)
public class SimpleProcessor implements Processor {

    static int count;

    @Secured
    @Interceptors(EjbInterceptor2.class)
    public int add(int x, int y) {
        count = Counter.next();
        return x + y;
    }

    @Secured
    @ExcludeClassInterceptors
    public int subtract(int x, int y) {
        count = Counter.next();
        return x - y;
    }

}
