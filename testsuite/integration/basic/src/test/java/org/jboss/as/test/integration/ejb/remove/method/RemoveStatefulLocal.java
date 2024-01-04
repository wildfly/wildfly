/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remove.method;

import jakarta.ejb.Local;

/**
 * Local implementation of "Remove"
 *
 * @author <a href="mailto:arubinge@redhat.com">ALR</a>
 */
@Local
public interface RemoveStatefulLocal extends Remove {
}
