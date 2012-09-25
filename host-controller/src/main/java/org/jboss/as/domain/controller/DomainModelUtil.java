/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller;

import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;


/**
 * Utilities related to the domain model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DomainModelUtil {

    /**
     * @deprecated This should be removed once the remaining resources have been converted
     */
    @Deprecated
    public static void initializeDomainRegistry(final ManagementResourceRegistration root, final ExtensibleConfigurationPersister configurationPersister,
                                                 final ContentRepository contentRepo, final HostFileRepository fileRepository, final boolean isMaster,
                                                 final LocalHostControllerInfo hostControllerInfo,
                                                 final ExtensionRegistry extensionRegistry, final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
                                                 final PathManagerService pathManager) {




    }
}
