/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.noncomponent;

import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.transaction.UserTransaction;

/**
 * This class is an EE component, and it's resource injection should be able to be looked up.
 * In the ee11 source tree this is an {@code @ManagedBean}, a type that is not available in EE 11.
 *
 * @author Stuart Douglas
 */
@Singleton
public class ComponentResourceInjection {

    @Resource
    private UserTransaction userTransaction;

}
