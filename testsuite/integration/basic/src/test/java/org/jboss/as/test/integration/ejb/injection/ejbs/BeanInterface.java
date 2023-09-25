/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejbs;

import jakarta.ejb.Local;

/**
 * @author Stuart Douglas
 */
@Local
public interface BeanInterface {

    String name();

}
