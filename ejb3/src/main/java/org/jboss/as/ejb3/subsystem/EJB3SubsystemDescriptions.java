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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

import java.util.Locale;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * User: jpai
 */
public class EJB3SubsystemDescriptions {

    public static final DescriptionProvider STRICT_MAX_BEAN_INSTANCE_POOL = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            // TODO: Use Locale
            final ModelNode description = new ModelNode();
            // setup the description for the strict-max-bean-instance-pool address
            description.get(DESCRIPTION).set("A bean instance pool with a strict upper limit");

            return description;
        }
    };

}
