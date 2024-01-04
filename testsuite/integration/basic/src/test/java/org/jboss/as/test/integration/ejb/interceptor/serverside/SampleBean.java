/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.serverside;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

/**
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
@Stateless
@Local
public class SampleBean {

    public String getSimpleName() {
        return SampleBean.class.getSimpleName();
    }
}
