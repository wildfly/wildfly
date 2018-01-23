/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.util.EnumSet;

import org.junit.Assert;

/**
 * Tester for an {@link Externalizer} of an enum.
 * @author Paul Ferraro
 */
public class EnumExternalizerTester<E extends Enum<E>> extends ExternalizerTester<E> {

    private final Class<E> targetClass;

    public EnumExternalizerTester(Externalizer<E> externalizer) {
        super(externalizer, Assert::assertSame);
        this.targetClass = externalizer.getTargetClass();
    }

    public void test() throws ClassNotFoundException, IOException {
        for (E value : EnumSet.allOf(this.targetClass)) {
            this.test(value);
        }
    }
}
