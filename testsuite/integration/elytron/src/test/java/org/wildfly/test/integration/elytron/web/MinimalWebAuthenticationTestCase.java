/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.web;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.ServletElytronDomainSetup;

/**
 * Test case to test authentication to web applications using programatic authentication.
 *
 * This test uses an application security-domain mapping to a security domain instead of an authentication factory.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({ WebAuthenticationTestCaseBase.ElytronDomainSetupOverride.class, ServletElytronDomainSetup.class })
public class MinimalWebAuthenticationTestCase extends WebAuthenticationTestCaseBase {

    @Override
    protected String getWebXmlName() {
        return "minimal-web.xml";
    }

    public static class ElytronServletSetupOverride extends ServletElytronDomainSetup {

        @Override
        protected boolean useAuthenticationFactory() {
            return false;
        }

    }

}
