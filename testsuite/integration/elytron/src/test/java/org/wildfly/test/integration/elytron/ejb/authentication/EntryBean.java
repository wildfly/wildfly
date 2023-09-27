/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.ejb.authentication;

import jakarta.ejb.Stateless;

import org.wildfly.test.integration.elytron.ejb.Entry;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Concrete implementation to allow deployment of bean.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
@SecurityDomain("elytron-tests")
public class EntryBean extends org.wildfly.test.integration.elytron.ejb.base.EntryBean implements Entry {
}
