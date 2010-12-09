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

/**
 * The JBossAS domain deployment subsystem classes.
 * <p/>
 * Deployment of a unit (typically an archive) has a number of distinct phases.
 * <ol>
 *   <li><b>Phase 0: Domain Controller.</b>  This phase runs on the domain controller to perform a first-pass validation of all
 *   descriptors and other meta-information in the deployment unit. <ol>
 *     <li><b>Deployment Type Identification.</b>  The type of deployment is identified, so that the proper validation sequence
 *     and deployer chain can be selected.</li>
 *     <li><b>Descriptor Validation.</b>  All descriptors in the deployment are initially validated.  This typically will entail a
 *     simple XSD validation, plus a limited degree of structural validation.</li>
 *     <li><b>Domain Deployment.</b>  This phase runs on the domain controller to install the validated deployment unit into the
 *     domain model itself, and coordinate the distribution of the domain update to the relevant servers, using the deployment
 *     plan to coordinate distribution appropriately.</li>
 *   </ol></li>
 *   <li><b>Phase 1: Server Deployment Preprocessing.</b>  This phase runs on the individual server to prepare a deployment
 *   unit for execution.  After this phase, the "parent" service for the deployment item and the service corresponding to
 *   the virtual file mount are available.<ol>
 *     <li><b>Deployment Type Check.</b>  If the deployment type corresponds to a subsystem that is not present in the
 *     current profile, log a message and do no further processing.</li>
 *     <li><b>Deployment Mount.</b>  Mount the deployment unit into the Virtual File System.</li>
 *   </ol></li>
 *   <li><b>Phase 2: Server Deployment Unit Processing.</b>  This phase runs on the individual server to process the
 *   deployment unit into actual deployment items.  No service batch is active during this phase.<ol>
 *     <li><b>Deployment Processing.</b>  In this phase, the chain of {@link DeploymentUnitProcessor}s which is associated
 *     with the deployment unit type are executed over the deployment unit to parse any descriptors, add them to the
 *     {@link DeploymentUnit}, transform them, and/or convert them into {@link DeploymentItem}s.  These {@code DeploymentItem}s
 *     represent the deployed state of the deployment unit.</li>
 *   </ol></li>
 *   <li><b>Phase 3: Installation.</b>  In this phase the deployment items are actually executed and added to a service
 *   batch, causing their corresponding services to be resolved and started up.</li>
 * </ol>
 * <p/>
 * The deployment items corresponding to a deployment unit are started, stopped, and removed by way of a unit-wide
 * dependency service.
 */

package org.jboss.as.server.deployment;
