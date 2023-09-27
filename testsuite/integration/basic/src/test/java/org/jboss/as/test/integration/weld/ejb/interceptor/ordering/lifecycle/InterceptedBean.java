/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.ordering.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Stateful;
import jakarta.interceptor.Interceptors;

@Stateful
@Interceptors(LegacyInterceptor.class)
@CdiIntercepted
public class InterceptedBean extends Superclass {

    @PostConstruct
    void postConstruct() {
        ActionSequence.addAction(InterceptedBean.class.getSimpleName());
    }

    @PreDestroy
    void preDestroy() {
        ActionSequence.addAction(InterceptedBean.class.getSimpleName());
    }
}
