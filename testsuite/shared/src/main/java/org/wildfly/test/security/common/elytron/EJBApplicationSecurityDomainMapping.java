/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * @author Jan Martiska
 */
public class EJBApplicationSecurityDomainMapping implements ConfigurableElement {

    private final String appDomain;
    private final String elytronDomain;


    public EJBApplicationSecurityDomainMapping(String appDomain, String elytronDomain) {
        this.appDomain = appDomain;
        this.elytronDomain = elytronDomain;
    }

    @Override
    public String getName() {
        return appDomain;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        cli.sendLine("/subsystem=ejb3/application-security-domain="+ appDomain +":add(security-domain="+elytronDomain+")");
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine("/subsystem=ejb3/application-security-domain="+ appDomain +":remove");
    }
}
