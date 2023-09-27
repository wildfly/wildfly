/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.byreference;

/**
 * @author Jaikiran Pai
 */
public interface RemoteInterface {

    void modifyFirstElementOfArray(final String[] array, final String newValue);

}
