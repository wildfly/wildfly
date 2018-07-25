/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.web.deployment;

import org.wildfly.clustering.web.infinispan.routing.RankedRoutingConfiguration;
import org.wildfly.extension.clustering.web.RankedAffinityResourceDefinition;

/**
 * @author Paul Ferraro
 */
public class MutableRankedRoutingConfiguration implements RankedRoutingConfiguration {

    private String delimter = RankedAffinityResourceDefinition.Attribute.DELIMITER.getDefinition().getDefaultValue().asString();
    private int maxRoutes = RankedAffinityResourceDefinition.Attribute.MAX_ROUTES.getDefinition().getDefaultValue().asInt();

    @Override
    public String getDelimiter() {
        return this.delimter;
    }

    public void setDelimiter(String delimiter) {
        this.delimter = delimiter;
    }

    @Override
    public int getMaxRoutes() {
        return this.maxRoutes;
    }

    public void setMaxRoutes(int maxRoutes) {
        this.maxRoutes = maxRoutes;
    }
}
