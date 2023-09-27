/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.inheritance;

/**
 * @author Stuart Douglas
 */
public interface LocalSynchronizations extends Synchronizations {

    void destroy();
}
