/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.server.controller.resources;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.version.ProductConfig;
import org.jboss.as.version.Version;
import org.jboss.dmr.ModelNode;

/**
 * Initializes the part of the model where the versions are stored
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class VersionModelInitializer {
    public static void registerRootResource(Resource rootResource, ProductConfig cfg) {
        ModelNode model = rootResource.getModel();
        model.get(ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION).set(Version.MANAGEMENT_MAJOR_VERSION);
        model.get(ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION).set(Version.MANAGEMENT_MINOR_VERSION);
        model.get(ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION).set(Version.MANAGEMENT_MICRO_VERSION);

        model.get(ModelDescriptionConstants.RELEASE_VERSION).set(Version.AS_VERSION);
        model.get(ModelDescriptionConstants.RELEASE_CODENAME).set(Version.AS_RELEASE_CODENAME);

        if (cfg != null) {
            if (cfg.getProductVersion() != null) {
                model.get(ModelDescriptionConstants.PRODUCT_VERSION).set(cfg.getProductVersion());
            }
            if (cfg.getProductName() != null) {
                model.get(ModelDescriptionConstants.PRODUCT_NAME).set(cfg.getProductName());
            }
        }
    }
}
