/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remove.method;

import jakarta.ejb.Stateless;

/**
 * Defines a custom "remove" method that is not associated w/ EJBOBject/EJBLocalObject.remove
 *
 * @author <a href="mailto:arubinge@redhat.com">ALR</a>
 */
@Stateless
public class StatelessRemoveBean extends AbstractRemoveBean implements RemoveStatelessRemote, RemoveStatelessLocal {
}
