/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
