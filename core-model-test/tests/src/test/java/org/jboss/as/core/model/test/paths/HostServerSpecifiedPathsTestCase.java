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
package org.jboss.as.core.model.test.paths;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.TestModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class HostServerSpecifiedPathsTestCase extends AbstractSpecifiedPathsTestCase {

    public HostServerSpecifiedPathsTestCase() {
        super(TestModelType.HOST);
    }

    @Override
    protected String getXmlResource() {
        return "host.xml";
    }

    @Override
    protected PathAddress getPathsParent() {
        return PathAddress.pathAddress(PathElement.pathElement(SERVER_CONFIG, "server-one"));
    }

    protected ModelInitializer createEmptyModelInitalizer() {
        return new ModelInitializer() {
            @Override
            public void populateModel(Resource rootResource) {
                rootResource.registerChild(getPathsParent().getLastElement(), Resource.Factory.create());
            }
        };
    }
}
