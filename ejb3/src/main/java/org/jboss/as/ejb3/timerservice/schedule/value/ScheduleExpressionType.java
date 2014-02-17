/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.timerservice.schedule.value;

/**
 * Represents the type of expression used in the values of a {@link javax.ejb.ScheduleExpression}
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public enum ScheduleExpressionType {
    /**
     * Represents a single value type.
     * <p>
     * For example:
     * <ul>
     * <li>second = "10"</li>
     * <li>month = "Jun"</li>
     * </ul>
     * <p/>
     * <p/>
     * </p>
     */
    SINGLE_VALUE,

    /**
     * Represents a wildcard "*" value.
     * <p/>
     * <p>
     * For example:
     * <ul>
     * <li>second = "*"</li>
     * </ul>
     * </p>
     */
    WILDCARD,

    /**
     * Represents a value represented as a list.
     * <p/>
     * <p>
     * For example:
     * <ul>
     * <li>second = "1, 10"</li>
     * <li>dayOfMonth = "Sun, Fri, Mon"</li>
     * <ul>
     * </p>
     */
    LIST,

    /**
     * Represents a value represented as a range.
     * <p>
     * For example:
     * <ul>
     * <li>minute = "0-15"</li>
     * <li>year = "2009-2012</li>
     * </ul>
     * </p>
     */
    RANGE,

    /**
     * Represents a value represented as an increment.
     * <p>
     * For example:
     * <ul>
     * <li>hour = "* &#47; 3"</li>
     * <li>minute = "20 &#47; 10</li>
     * </ul>
     * </p>
     */
    INCREMENT

}
