/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.ejb;

import jakarta.ejb.Singleton;
import jakarta.jws.WebService;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Singleton
@WebService
public class SingletonEndpoint {
    private String state;

    public String getState() {
        return state;
    }

    public String setState(String s) {
        String previous = this.state;
        this.state = s;
        return previous;
    }
}
