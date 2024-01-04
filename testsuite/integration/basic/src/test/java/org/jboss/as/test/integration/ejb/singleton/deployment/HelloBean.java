/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.singleton.deployment;

import jakarta.ejb.Stateless;

/**
 * @author Bartosz Spyrko-Smietanko
 */
@Stateless
public class HelloBean implements HelloRemote {

    public String hello() {
        return "hello";
    }
}
