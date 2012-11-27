/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.core.model.test;

import org.jboss.as.controller.registry.Resource;

/**
 * Fast track to initialize model with resources required by the operations required.
 * When testing the xml variety things added here might be added back to the marshalled
 * xml, causing problems when comparing the original and marshalled xml. Use {@link ModelWriteSanitizer}
 * to remove things added here from the model.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface ModelInitializer {

    void populateModel(Resource rootResource);
}
