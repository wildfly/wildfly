/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules.alias;

import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 * @author <a href="mailto:bspyrkos@redhat.com">Bartosz Spyrko-Smietanko</a>
 */
@ApplicationScoped
public class ModuleBean {

    public String test() {
        return "test";
    }
}
