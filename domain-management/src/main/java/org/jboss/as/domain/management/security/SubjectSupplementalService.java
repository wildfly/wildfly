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

package org.jboss.as.domain.management.security;

import java.util.Map;

/**
 * Interface to be implemented by services supplying SubjectSupplemental implementations.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface SubjectSupplementalService {

    /**
     * Obtain a SubjectSupplemental instance to load role information.
     *
     * The service can decide if it will return a single shared SubjectSupplemental or a new one for each call to this method.
     *
     * The shared state is authentication request specific but this is the only time it will be provided, for
     * SubjectSupplemental making use of this then state specific instances should be returned.
     *
     * @param sharedState - The state to be shared between the authentication side of the call and the authorization side.
     * @return A SubjectSupplemental instance.
     */
    SubjectSupplemental getSubjectSupplemental(final Map<String, Object> sharedState);

}
