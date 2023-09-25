/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.authorization;

import org.jboss.ejb3.annotation.SecurityDomain;

import jakarta.ejb.Stateful;

/**
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@Stateful
@SecurityDomain("other")
public class InherritanceAnnOnlyCheckSFSB extends ParentAnnOnlyCheck implements SimpleAuthorizationRemote {
}
