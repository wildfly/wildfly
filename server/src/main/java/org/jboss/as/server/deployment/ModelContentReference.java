/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.server.deployment;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.repository.ContentReference;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;

/**
 * ContentReference built from the Management Model.
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class ModelContentReference {

    public static final ContentReference fromDeploymentName(String name, String hash) {
        return new ContentReference(PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT, name)).toCLIStyleString(),
                hash);
    }

    public static final ContentReference fromDeploymentName(String name, byte[] hash) {
        return fromDeploymentName(name, HashUtil.bytesToHexString(hash));
    }

    public static final ContentReference fromModelAddress(PathAddress address, byte[] hash) {
        return new ContentReference(address.toCLIStyleString(), hash);
    }
}
