/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.classinherit;

import jakarta.interceptor.Interceptors;
import jakarta.ejb.Stateless;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Stateless
@Interceptors({ TestInterceptor.class })
public class StatelessWithBeanInterceptorBean extends AbstractBaseClassWithInterceptor implements StatelessRemote {
    public String method() {
        return StatelessWithBeanInterceptorBean.class.getSimpleName() + ".method()";
    }

    public String superMethod() {
        return StatelessWithBeanInterceptorBean.class.getSimpleName() + ".superMethod()";
    }
}
