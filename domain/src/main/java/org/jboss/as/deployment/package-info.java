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

/**
 * The JBossAS domain deployment subsystem classes.
 * <p/>
 * Deployment of a unit (typically an archive) has a number of distinct phases.
 * <ol>
 * <li>Descriptor Validation.  This phase runs on the domain controller to perform a first-pass validation of all
 * descriptors and other meta-information in the deployment unit.</li>
 * <li>Domain Deployment.  This phase runs on the domain controller to install the validated deployment unit into the
 * domain model itself, and coordinate the distribution of the domain update to the relevant servers, using the deployment
 * plan to coordinate distribution appropriately.</li>
 * <li>Descriptor Parsing.  In this phase, a deployer chain is executed on each server over a deployment unit to parse any descriptors
 * add them to the {@link DeploymentUnitContext}.</li>
 * <li>Processing.  In this phase, the deployer chain is executed over the deployment unit context to process descriptors
 * and produce {@link DeploymentItem}s.</li>
 * <li>Installation.  In this phase, the deployment items are executed to install the actual services.</li>
 * </ol>
 */
package org.jboss.as.deployment;
