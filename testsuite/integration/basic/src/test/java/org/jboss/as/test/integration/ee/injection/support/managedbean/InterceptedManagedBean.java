/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.managedbean;

import jakarta.annotation.ManagedBean;

import org.jboss.as.test.integration.ee.injection.support.AroundConstructBinding;

@AroundConstructBinding
@ManagedBean("InterceptedManagedBean")
public class InterceptedManagedBean {
}
