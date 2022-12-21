/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests subsystem against configurations for all supported subsystem schema versions.
 * @author Paul Ferraro
 */
@RunWith(Parameterized.class)
public class UndertowSubsystemTestCase extends AbstractUndertowSubsystemTestCase {

    // TODO Create formal enumeration of schema versions
    @Parameters
    public static Collection<Object[]> parameters() {
        return List.of(
                new Object[] { 3, 1 },
                new Object[] { 4, 0 },
                new Object[] { 5, 0 },
                new Object[] { 6, 0 },
                new Object[] { 7, 0 },
                new Object[] { 8, 0 },
                new Object[] { 9, 0 },
                new Object[] { 10, 0 },
                new Object[] { 11, 0 },
                new Object[] { 12, 0 },
                new Object[] { 13, 0 });
    }

    public UndertowSubsystemTestCase(int major, int minor) {
        super(major, minor);
    }
}
