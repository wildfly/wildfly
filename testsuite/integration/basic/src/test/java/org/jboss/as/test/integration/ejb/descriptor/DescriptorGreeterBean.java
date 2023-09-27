/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.descriptor;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
// do not annotate
//@Stateless(name = "Greeter")
public class DescriptorGreeterBean {
    public String greet(String name) {
        return "Hi " + name;
    }
}
