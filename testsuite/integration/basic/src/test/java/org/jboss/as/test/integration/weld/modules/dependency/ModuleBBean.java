/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules.dependency;

import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
@ApplicationScoped
public class ModuleBBean {

    public String ping() {
        return ModuleBBean.class.getSimpleName();
    }
}
