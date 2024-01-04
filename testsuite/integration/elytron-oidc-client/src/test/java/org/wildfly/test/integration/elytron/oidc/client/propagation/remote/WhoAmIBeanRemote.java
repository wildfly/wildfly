/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.oidc.client.propagation.remote;

import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.wildfly.test.integration.elytron.oidc.client.propagation.base.WhoAmIBean;

/**
 * Concrete implementation to allow deployment of bean.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@Stateless
@SecurityDomain("ejb-domain")
public class WhoAmIBeanRemote extends WhoAmIBean implements WhoAmIRemote {
}
