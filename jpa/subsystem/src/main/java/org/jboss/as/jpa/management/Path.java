/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.management;

import java.util.ArrayList;

import org.jboss.as.controller.PathElement;
import org.jipijapa.management.spi.PathAddress;

/**
 * Path to a statistic value
 *
 * @author Scott Marlow
 */
public class Path implements PathAddress {
    private final ArrayList<PathElement> pathElements = new ArrayList<>();

    public Path(org.jboss.as.controller.PathAddress pathAddress) {

        for (int looper = 0; looper < pathAddress.size(); looper++) {
            pathElements.add(pathAddress.getElement(looper));
        }
    }

    public static Path path(org.jboss.as.controller.PathAddress pathAddress) {
        return new Path(pathAddress);
    }
    @Override
    public int size() {
        return pathElements.size();
    }

    @Override
    public String getValue(String name) {
        for ( PathElement pathElement : pathElements) {
            if (pathElement.getKey().equals(name)) {
                return pathElement.getValue();
            }
        }
        return null;
    }

    @Override
    public String getValue(int index) {
        return pathElements.get(index).getValue();
    }
}
