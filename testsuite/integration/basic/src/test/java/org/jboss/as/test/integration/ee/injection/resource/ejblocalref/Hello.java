/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.ejblocalref;

import jakarta.ejb.Local;

/**
 * @author Stuart Douglas
 */
@Local
public interface Hello {

    String sayHello();
}
