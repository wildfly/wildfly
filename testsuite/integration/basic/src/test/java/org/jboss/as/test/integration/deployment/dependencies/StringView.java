/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.dependencies;

import jakarta.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface StringView {

    String getString();

}
