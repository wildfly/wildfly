/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.ejb.interceptor.serverside;

import jakarta.ejb.Stateless;

@Stateless
public class SubstituteSampleBean implements SubstituteSampleBeanRemote {
    public String getSimpleName() {
        return SubstituteSampleBean.class.getSimpleName();
    }
}
