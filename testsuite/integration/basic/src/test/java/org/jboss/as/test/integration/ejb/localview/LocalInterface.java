/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.localview;

import jakarta.ejb.Local;

/**
 * An ejb local interface
 *
 * @author Stuart Douglas
 */
@Local
public interface LocalInterface {

    void localOperation();

}
