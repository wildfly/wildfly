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


/**
 *  Contains interfaces to be used as test tags for additional test grouping,
 *  using org.junit.experimental.categories.Category(...).
 *  These tags do not define the main categories - those are still denoted by java packages.
 *  These groups just help to define certain tests with cross-cutting concerns like security and management.
 *  Before you define a new group, please discuss with the community.
 *
 *  This feature needs JUnit 4.7+, and Surefire/Failsafe 2.12.4+
 *
 *  Examples of categories:
    <code>
        interface AllTests;
        interface ATests extends AllTests;
        interface BTests extends AllTests;
        interface AaTests extends ATests;

        @Category(ATests.class) public void ATest();
        @Category(AaTests.class) public void AaTest();
        @Category(BTests.class) public void BTest();
    </code>

    As is apparent, interfaces inheritance can be leveraged to create a hierarchy.

    A test can belong into multiple categories:
    <code>
        @Categories({Foo.class, Bar.class})
    </code>

    The tagged tests can be run in groups using Surefire's
    <code>&lt;includedGroups></code> and <code>&lt;excludedGroups></code> options in pom.xml,
    or <code>-Dtest.group=...</code> and <code>-Dtest.exgroup=...</code> at command line.

    These parameters support expressions with operators: &amp;&amp; || !  (&amp;amp;&amp;amp; in pom.xml)
    <code>org.jboss.as.test.categories.Security &amp;&amp; !org.jboss.as.test.categories.CommonCriteria</code>

 */
package org.jboss.as.test.categories;
