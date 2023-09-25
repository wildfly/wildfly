/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.mockprovider.skipquerydetach;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TestQuery
 *
 * @author Scott Marlow
 */
public class TestQuery implements InvocationHandler {

    private static final List<String> invocations = Collections.synchronizedList(new ArrayList<String>());

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        invocations.add(method.getName());
        return null;
    }
}
