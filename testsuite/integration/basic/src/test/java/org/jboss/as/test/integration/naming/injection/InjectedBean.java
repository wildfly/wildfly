/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.injection;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;

/**
 * @author Eduardo Martins
 */
@Stateless
public class InjectedBean implements Injected {

    @Resource(lookup=Binder.LINK_NAME)
    private String resource;

    public String getInjectedResource() {
        return resource;
    }

}
