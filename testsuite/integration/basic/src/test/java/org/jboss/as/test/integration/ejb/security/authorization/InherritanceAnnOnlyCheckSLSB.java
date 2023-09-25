/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.authorization;

import org.jboss.ejb3.annotation.SecurityDomain;

import jakarta.ejb.Stateless;

/**
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@Stateless
@SecurityDomain("other")
public class InherritanceAnnOnlyCheckSLSB extends ParentAnnOnlyCheck implements SimpleAuthorizationRemote {
}
