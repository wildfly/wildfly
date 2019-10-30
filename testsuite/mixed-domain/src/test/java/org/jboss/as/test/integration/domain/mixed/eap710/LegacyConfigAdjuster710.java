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

package org.jboss.as.test.integration.domain.mixed.eap710;

import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.mixed.LegacyConfigAdjuster;
import org.jboss.dmr.ModelNode;

/**
 * Adjusts a config produced from the EAP 7.0 or earlier domain.xml.
 *
 * @author Brian Stansberry
 */
public class LegacyConfigAdjuster710 extends LegacyConfigAdjuster {
    @Override
    protected List<ModelNode> adjustForVersion(DomainClient client, PathAddress profileAddress) throws Exception {
        List<ModelNode> result = super.adjustForVersion(client, profileAddress);

        // DO NOT INTRODUCE NEW ADJUSTMENTS HERE WITHOUT SOME SORT OF PUBLIC DISCUSSION.
        // CAREFULLY DOCUMENT IN THIS CLASS WHY ANY ADJUSTMENT IS NEEDED AND IS THE BEST APPROACH.
        // If an adjustment is needed here, that means our users will need to do the same
        // just to get an existing profile to work, and we want to minimize that.


        return result;
    }
}
