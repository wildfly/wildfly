/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.parsing;

import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;

/**
 * Callback an {@link ProfileParsingCompletionHandler} can register to, upon completion of normal parsing of a profile, manipulate the list
 * of parsed boot operations associated with a profile.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ProfileParsingCompletionHandler {

    /**
     * Callback indicating normal parsing of a profile is completed.
     *
     * @param profileBootOperations the boot operations added by subsystems in the profile, keyed by the URI of the
     *                              xml namespace used for the subsystem
     * @param otherBootOperations other operations registered in the boot prior to parsing of the profile
     */
    void handleProfileParsingCompletion(final Map<String, List<ModelNode>> profileBootOperations, List<ModelNode> otherBootOperations);
}
