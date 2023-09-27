/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming;


/**
 * A {@link ManagedReferenceFactory} which properly supports {@link javax.naming.Context} list operations, and JNDI View lookups.
 *
 * @author Eduardo Martins
 *
 */
public interface ContextListAndJndiViewManagedReferenceFactory extends ContextListManagedReferenceFactory,
        JndiViewManagedReferenceFactory {

}
