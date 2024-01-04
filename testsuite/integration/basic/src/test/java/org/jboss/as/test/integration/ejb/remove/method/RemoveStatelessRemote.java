/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remove.method;

import jakarta.ejb.Remote;

/**
 * Remote implementation of "Remove"
 *
 * @author <a href="mailto:arubinge@redhat.com">ALR</a>
 */
@Remote
public interface RemoveStatelessRemote extends Remove {
}
