/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.iiop.openjdk;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This class contains mapping from IIOP subsystem constants to orb specific properties.
 * </p>
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

import com.sun.corba.se.impl.orbutil.ORBConstants;

public interface PropertiesMap {

    Map<String, String> PROPS_MAP = Collections.unmodifiableMap(new HashMap<String, String>() {
        {
            put(Constants.ORB_PERSISTENT_SERVER_ID, ORBConstants.ORB_SERVER_ID_PROPERTY);
            put(Constants.ORB_GIOP_VERSION, ORBConstants.GIOP_VERSION);
            put(Constants.TCP_HIGH_WATER_MARK, ORBConstants.HIGH_WATER_MARK_PROPERTY);
            put(Constants.TCP_NUMBER_TO_RECLAIM, ORBConstants.NUMBER_TO_RECLAIM_PROPERTY);
        }
    });
}
