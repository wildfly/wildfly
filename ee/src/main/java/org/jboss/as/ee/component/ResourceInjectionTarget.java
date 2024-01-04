/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

/**
 * Represents an object that has a descriptor based JNDI environment.
 *
 * @author Stuart Douglas
 */
public interface ResourceInjectionTarget {

    void addResourceInjection(final ResourceInjectionConfiguration injection);

}
