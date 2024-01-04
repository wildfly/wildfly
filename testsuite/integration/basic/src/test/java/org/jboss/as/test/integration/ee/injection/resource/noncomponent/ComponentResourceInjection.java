/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.noncomponent;

import jakarta.annotation.ManagedBean;
import jakarta.annotation.Resource;
import jakarta.transaction.UserTransaction;

/**
 * This class is a managed bean, and it's resource injection should be able to be looked up
 *
 * @author Stuart Douglas
 */
@ManagedBean
public class ComponentResourceInjection {

    @Resource
    private UserTransaction userTransaction;

}
