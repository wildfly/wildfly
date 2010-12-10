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

package org.jboss.as.model;

import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DomainExtensionRemove extends AbstractDomainModelUpdate<Void> {

    private static final long serialVersionUID = 3718982114819320314L;

    private final String moduleName;

    public DomainExtensionRemove(final String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    protected void applyUpdate(final DomainModel element) throws UpdateFailedException {
        if (!element.removeExtension(moduleName)) {
            throw new UpdateFailedException("Extension " + moduleName + " was not configured");
        }
    }

    @Override
    public DomainExtensionAdd getCompensatingUpdate(final DomainModel original) {
        if (original.getExtensions().contains(moduleName))
            return new DomainExtensionAdd(moduleName);
        return null;
    }

    @Override
    public AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return null;
    }

    @Override
    public List<String> getAffectedServers(DomainModel domainModel, HostModel hostModel) throws UpdateFailedException {
        return Collections.emptyList();
    }
}
