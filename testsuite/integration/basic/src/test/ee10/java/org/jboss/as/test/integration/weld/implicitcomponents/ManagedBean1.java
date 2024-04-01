/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.implicitcomponents;

import jakarta.annotation.ManagedBean;
import jakarta.inject.Inject;

/**
 * @author Stuart Douglas
 */
@ManagedBean
public class ManagedBean1 {

    @Inject
    private ManagedBean2 bean;

    public String getMessage() {
        return bean.getMessage();
    }

}
