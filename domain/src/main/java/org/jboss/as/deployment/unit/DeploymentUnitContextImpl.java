/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment.unit;

import org.jboss.as.deployment.SimpleAttachable;
import org.jboss.as.deployment.item.DeploymentItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation for DeploymentUnitContext.
 *  
 * @author John E. Bailey
 */
public class DeploymentUnitContextImpl extends SimpleAttachable implements DeploymentUnitContext {
    private final String name;
    private final List<DeploymentItem> deploymentItems = new ArrayList<DeploymentItem>();

    public DeploymentUnitContextImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addDeploymentItem(DeploymentItem item) {
        deploymentItems.add(item);
    }

    public List<DeploymentItem> getDeploymentItems() {
        return new ArrayList<DeploymentItem>(deploymentItems);        
    }
}
