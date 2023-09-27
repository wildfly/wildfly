/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.batch;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * This is here as just a hack to be able to set the security domain of a deployment.
 * See https://issues.jboss.org/browse/JBEAP-8702 discussion for more context.
 * @author Jan Martiska
 */
@Stateless
@SecurityDomain("BatchDomain")
@LocalBean
public class SecurityDomainSettingEJB {
}
