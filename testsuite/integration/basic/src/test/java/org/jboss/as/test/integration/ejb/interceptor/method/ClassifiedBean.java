/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.method;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author Stuart Douglas
 */
@Stateless
@Interceptors(SecretInterceptor.class)
public class ClassifiedBean extends AbstractClassifiedBean {

    public String secretMethod() {
        return "Secret";
    }

    @Interceptors(TopSecretInterceptor.class)
    public String topSecretMethod() {
        return "TopSecret";
    }

    public String overloadedMethod(Integer i) {
        return "ArgInt:" + i.toString();
    }

    public String overloadedMethod(String str) {
        return "ArgStr:" + str;
    }

    public String methodWithArrayArgument(String[] array)  {
        return "Array:" + array;
    }

    public String overridedMethod(String a) {
        return "Str " + a;
    }
}
