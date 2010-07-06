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

package org.jboss.as.deployment.item;

import java.io.Serializable;
import org.jboss.msc.service.BatchBuilder;

/**
 * A general deployment item which can deploy itself, either at deploy time or server startup.  Deployment items
 * must be serializable in order to preserve state when the server is shut down.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface DeploymentItem extends Serializable {

    /**
     * Install this item into the batch builder.
     *
     * @param builder the batch builder into which the item should be added
     *
     * @return the runtime state
     */
    void install(BatchBuilder builder);

    /**
     * Get a human-readable description of this deployment item.  The string should state what type of deployment item
     * it is, along with any identifier such as a name.  An example would be {@code "servlet 'MyServlet'"}.
     *
     * @return the string representation
     */
    String toString();
}
