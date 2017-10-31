/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.domain.mixed.eap620;

import org.jboss.as.test.integration.domain.mixed.MixedDeploymentOverlayTestCase;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.domain.mixed.Version.AsVersion;
import org.junit.BeforeClass;

/**
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
@Version(AsVersion.EAP_6_4_0)
public class MixedDomainDeploymentOverlay620TestCase extends MixedDeploymentOverlayTestCase {
    @BeforeClass
    public static void beforeClass() {
        MixedDomainOverlay620TestSuite.initializeDomain();
        MixedDeploymentOverlayTestCase.setupDomain();
    }

    @Override
    protected String getUnknowOperationErrorCode() {
        return "JBAS014884";
    }

    @Override
    protected boolean isUndertowSupported() {
        return false;
    }
}
