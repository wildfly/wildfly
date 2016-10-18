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
package org.jboss.as.test.shared;

/**
 * Adjusts default timeouts according to the system property.
 *
 * All tests influenced by machine slowness should employ this util.
 *
 * @author Ondrej Zizka
 * @author Radoslav Husar
 */
public class TimeoutUtil {

    public static final String FACTOR_SYS_PROP = "ts.timeout.factor";
    private static int factor;

    static {
        factor = Integer.getInteger(FACTOR_SYS_PROP, 100);
    }

    /**
     * Adjusts timeout for operations.
     *
     * @return given timeout adjusted by ratio from system property "ts.timeout.factor"
     */
    public static int adjust(int amount) {
        return amount * factor / 100;
    }

    /**
     * Get timeout factor to multiply by.
     *
     * @return double factor value
     */
    public static double getFactor() {
        return factor / 100;
    }

    /**
     * Get raw timeout factor.
     *
     * @return value of parsed system property "ts.timeout.factor"
     */
    public static int getRawFactor() {
        return factor;
    }
}