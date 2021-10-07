/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import org.junit.Assert;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractCircularReferenceTestCase {

    private final MarshallingTesterFactory factory;

    public AbstractCircularReferenceTestCase(MarshallingTesterFactory factory) {
        this.factory = factory;
    }

    @org.junit.Test
    public void test() throws IOException {
        Person parent = Person.create("parent");
        Person self = Person.create("self");
        parent.addChild(self);
        parent.addChild(Person.create("sibling"));
        self.addChild(Person.create("son"));
        self.addChild(Person.create("daughter"));

        Tester<Person> tester = this.factory.createTester();
        tester.test(self, (expected, actual) -> {
            Assert.assertEquals(expected, actual);
            Assert.assertEquals(expected.getParent(), actual.getParent());
            Assert.assertEquals(expected.getChildren(), actual.getChildren());
            // Validate referential integrity
            for (Person child : actual.getParent().getChildren()) {
                Assert.assertSame(actual.getParent(), child.getParent());
            }
            for (Person child : actual.getChildren()) {
                Assert.assertSame(actual, child.getParent());
            }
        });
    }
}
