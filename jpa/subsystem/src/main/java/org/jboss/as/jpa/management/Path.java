/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
