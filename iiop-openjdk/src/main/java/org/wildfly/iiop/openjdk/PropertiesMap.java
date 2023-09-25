/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
