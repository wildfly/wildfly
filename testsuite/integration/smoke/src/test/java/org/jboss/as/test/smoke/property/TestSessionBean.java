/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.property;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;

/**
 * @author John Bailey
 */
@Stateless
public class TestSessionBean implements TestBean {
    @Resource
    private String value;

    @Resource
    private String valueOverride;

    public String getValue() {
        return value;
    }

    public String getValueOverride() {
        return valueOverride;
    }
}
