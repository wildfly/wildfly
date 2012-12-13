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
package org.jboss.as.core.model.test.util;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.ModelWriteSanitizer;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class StandardServerGroupInitializers {
    public static final ModelInitializer XML_MODEL_INITIALIZER = new ModelInitializer() {
        public void populateModel(Resource rootResource) {
            rootResource.registerChild(PathElement.pathElement(PROFILE, "test"), Resource.Factory.create());
            rootResource.registerChild(PathElement.pathElement(SOCKET_BINDING_GROUP, "test-sockets"), Resource.Factory.create());
        }
    };

    public static final ModelWriteSanitizer XML_MODEL_WRITE_SANITIZER = new ModelWriteSanitizer() {
        @Override
        public ModelNode sanitize(ModelNode model) {
            //Remove the profile and socket-binding-group removed by the initializer so the xml does not include a profile
            model.remove(PROFILE);
            model.remove(SOCKET_BINDING_GROUP);
            return model;
        }
    };

    public static LegacyKernelServicesInitializer addServerGroupInitializers(LegacyKernelServicesInitializer legacyKernelServicesInitializer) {
        legacyKernelServicesInitializer.initializerCreateModelResource(PathAddress.EMPTY_ADDRESS, PathElement.pathElement(PROFILE, "test"), null)
            .initializerCreateModelResource(PathAddress.EMPTY_ADDRESS, PathElement.pathElement(SOCKET_BINDING_GROUP, "test-sockets"), null);
        return legacyKernelServicesInitializer;
    }

}
