/*
Copyright 2016 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.service;

import org.jboss.msc.value.Value;

import java.lang.reflect.Method;

/**
 * Resolves the method on an injected bean using {@code MethodFinder}.
 */
public class InjectedBeanMethodValue implements Value<Method> {
    private final Value<?> targetBeanValue;
    private final MethodFinder methodFinder;

    interface MethodFinder {
        Method find(final Class<?> clazz);
    }

    public InjectedBeanMethodValue(Value<?> targetBeanValue, MethodFinder methodFinder) {
        this.methodFinder = methodFinder;
        this.targetBeanValue = targetBeanValue;
    }

    @Override
    public Method getValue() throws IllegalStateException, IllegalArgumentException {
        return methodFinder.find(targetBeanValue.getValue().getClass());
    }

}
