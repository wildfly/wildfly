/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.jpa.beanmanager;

import java.util.concurrent.atomic.AtomicInteger;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Foo {

    public AtomicInteger getCounter() {
        return counter;
    }

    AtomicInteger counter = new AtomicInteger();

    public void ping(){
        counter.incrementAndGet();
    }


}
