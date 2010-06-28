/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.domain;

import org.jboss.as.deployment.unit.DeploymentUnitProcessor;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface DomainController {

    void performDomainUpdate(AbstractDomainUpdate update);

    /**
     * Install a deployment unit processor at a point in the chain.  Deployment unit processors are executed in
     * increasing order by priority.  In the case of a conflict, the processors are executed in alpha order by
     * fully qualified class name.  If that conflicts, order is indeterminate.
     *
     * @param processor the processor
     * @param priority the priority
     */
    void installDeploymentUnitProcessor(DeploymentUnitProcessor processor, int priority);
}
