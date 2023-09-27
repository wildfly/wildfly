/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.authentication;

import jakarta.ejb.Stateless;

import org.jboss.as.test.integration.ejb.security.Entry;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Concrete implementation to allow deployment of bean.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
@SecurityDomain("ejb3-tests")
public class EntryBean extends org.jboss.as.test.integration.ejb.security.base.EntryBean implements Entry {
}
