/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.inheritance;

import jakarta.ejb.Local;

/**
 * @author Stuart Douglas
 */
@Local
public interface ChildInterface extends ParentInterface {

}
