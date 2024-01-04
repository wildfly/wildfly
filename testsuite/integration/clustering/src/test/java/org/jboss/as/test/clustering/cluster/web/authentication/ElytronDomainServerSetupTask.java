/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.authentication;

import java.io.File;

import org.wildfly.test.security.common.elytron.ElytronDomainSetup;

public class ElytronDomainServerSetupTask extends ElytronDomainSetup {

    public ElytronDomainServerSetupTask(String securityDomain) {
        super(new File(FormAuthenticationWebFailoverTestCase.class.getResource("users.properties").getFile()).getAbsolutePath(),
                new File(FormAuthenticationWebFailoverTestCase.class.getResource("roles.properties").getFile()).getAbsolutePath(),
                securityDomain);
    }
}