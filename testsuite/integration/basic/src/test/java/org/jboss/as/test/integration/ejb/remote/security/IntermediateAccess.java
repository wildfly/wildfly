/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.security;

/**
 * An extension to {@link SecurityInformation} that allows a username and password to be specified for calling a subsequent
 * bean.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface IntermediateAccess extends SecurityInformation {

    String getPrincipalName(final String userName, final String password);

}
