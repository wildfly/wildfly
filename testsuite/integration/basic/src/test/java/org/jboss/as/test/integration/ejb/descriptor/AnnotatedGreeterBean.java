/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.descriptor;

import jakarta.ejb.Stateless;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless(name = "AnnotatedGreeter")
public class AnnotatedGreeterBean {
    public String greet(String name) {
        return "Hi " + name;
    }
}
