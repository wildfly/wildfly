/*
 * Copyright (c) 2020. Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.subsystem.FilterSpecClassResolverFilter.DEFAULT_FILTER_SPEC;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class FilterSpecClassResolverFilterUnitTestCase {

    private static final String[] DEFAULT_SPECS = DEFAULT_FILTER_SPEC.split(";");

    @Test
    public void testBlackListSanity() {
        // It's fine to change this list if we change what we want.
        // This test is just a guard against unintended modification
        final String sanityFilterSpec =
                "!org.apache.commons.collections.functors.InvokerTransformer;"
                        + "!org.apache.commons.collections.functors.InstantiateTransformer;"
                        + "!org.apache.commons.collections4.functors.InvokerTransformer;"
                        + "!org.apache.commons.collections4.functors.InstantiateTransformer;"
                        + "!org.codehaus.groovy.runtime.ConvertedClosure;"
                        + "!org.codehaus.groovy.runtime.MethodClosure;"
                        + "!org.springframework.beans.factory.ObjectFactory;"
                        + "!com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;"
                        + "!org.apache.xalan.xsltc.trax.TemplatesImpl";

        Set<String> defaultSpec = new HashSet<>(Arrays.asList(DEFAULT_SPECS));
        for (String spec : sanityFilterSpec.split(";")) {
            Assert.assertTrue(spec, defaultSpec.contains(spec));
        }
    }

    @Test
    public void testDefaultBlacklist() {
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter();
        for (String spec : DEFAULT_SPECS) {
            boolean negate = spec.startsWith("!");
            if (negate) {
                Assert.assertFalse(spec, filter.apply(spec.substring(1)));
            } else {
                Assert.assertTrue(spec, filter.apply(spec));
            }
        }
    }

    @Test
    public void testDefaultBlacklistDisabled() {
        String currentFlag = System.getProperty("jboss.ejb.unmarshalling.filter.disabled");
        try {
            System.setProperty("jboss.ejb.unmarshalling.filter.disabled", "true");
            FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter();
            for (String spec : DEFAULT_SPECS) {
                spec = spec.startsWith("!") ? spec.substring(1) : spec;
                Assert.assertTrue(spec, filter.apply(spec));
            }
        } finally {
            if (currentFlag == null) {
                System.clearProperty("jboss.ejb.unmarshalling.filter.disabled");
            } else {
                System.setProperty("jboss.ejb.unmarshalling.filter.disabled", currentFlag);
            }
        }
    }

    @Test
    public void testBasicWhitelist() {
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter("java.lang.String");
        Assert.assertTrue(filter.apply("java.lang.String"));
        Assert.assertFalse(filter.apply("java.lang.Integer"));
    }

    @Test
    public void testWhitelistAllowsEjbClient() {
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter("java.lang.String");
        Assert.assertTrue(filter.apply("org.jboss.ejb.client.SessionId"));
        Assert.assertTrue(filter.apply("org.jboss.ejb.client.Foo"));
        Assert.assertFalse(filter.apply("org.jboss.ejb.client.foo.Bar"));
    }

    // It's fine to remove this test if the jboss.experimental.ejb.unmarshalling.filter.spec logic is removed
    @Test
    public void testExperimentalConfig() {
        String currentFlag = System.getProperty("jboss.experimental.ejb.unmarshalling.filter.spec");
        try {
            System.setProperty("jboss.experimental.ejb.unmarshalling.filter.spec", "java.lang.String;java.lang.Boolean");
            FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter();
            Assert.assertTrue(filter.apply("java.lang.String"));
            Assert.assertTrue(filter.apply("java.lang.Boolean"));
            Assert.assertFalse(filter.apply("java.lang.Integer"));
        } finally {
            if (currentFlag == null) {
                System.clearProperty("jboss.experimental.ejb.unmarshalling.filter.spec");
            } else {
                System.setProperty("jboss.experimental.ejb.unmarshalling.filter.spec", currentFlag);
            }
        }
    }

    @Test
    public void testPackageWhitelist() {
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter("foo.bar.*");
        Assert.assertTrue(filter.apply("foo.bar.FooBar"));
        Assert.assertTrue(filter.apply("foo.bar.Baz"));
        Assert.assertFalse(filter.apply("foo.bar.baz.FooBar"));
        Assert.assertFalse(filter.apply("foo.barFly"));
        Assert.assertFalse(filter.apply("foo.baz.bar.FooBar"));
    }

    @Test
    public void testPackageBlacklist() {
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter("!foo.bar.*");
        Assert.assertFalse(filter.apply("foo.bar.FooBar"));
        Assert.assertFalse(filter.apply("foo.bar.Baz"));
        Assert.assertTrue(filter.apply("foo.bar.baz.FooBar"));
        Assert.assertTrue(filter.apply("foo.barFly"));
        Assert.assertTrue(filter.apply("foo.baz.bar.FooBar"));
    }

    @Test
    public void testPackageHierarchyWhitelist() {
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter("foo.bar.**");
        Assert.assertTrue(filter.apply("foo.bar.FooBar"));
        Assert.assertTrue(filter.apply("foo.bar.Baz"));
        Assert.assertTrue(filter.apply("foo.bar.baz.FooBar"));
        Assert.assertFalse(filter.apply("foo.barFly"));
        Assert.assertFalse(filter.apply("foo.baz.bar.FooBar"));
    }

    @Test
    public void testPackageHierarchyBlacklist() {
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter("!foo.bar.**");
        Assert.assertFalse(filter.apply("foo.bar.FooBar"));
        Assert.assertFalse(filter.apply("foo.bar.Baz"));
        Assert.assertFalse(filter.apply("foo.bar.baz.FooBar"));
        Assert.assertTrue(filter.apply("foo.barFly"));
        Assert.assertTrue(filter.apply("foo.baz.bar.FooBar"));
    }

    @Test
    public void testEndsWithWhitelist() {
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter("foo.bar*");
        Assert.assertTrue(filter.apply("foo.bar.FooBar"));
        Assert.assertTrue(filter.apply("foo.bar.Baz"));
        Assert.assertTrue(filter.apply("foo.bar.baz.FooBar"));
        Assert.assertTrue(filter.apply("foo.barFly"));
        Assert.assertFalse(filter.apply("foo.baz.bar.FooBar"));
        Assert.assertFalse(filter.apply("foo.bazFly"));
    }

    @Test
    public void testEndsWithBlacklist() {
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter("!foo.bar*");
        Assert.assertFalse(filter.apply("foo.bar.FooBar"));
        Assert.assertFalse(filter.apply("foo.bar.Baz"));
        Assert.assertFalse(filter.apply("foo.bar.baz.FooBar"));
        Assert.assertFalse(filter.apply("foo.barFly"));
        Assert.assertTrue(filter.apply("foo.baz.bar.FooBar"));
        Assert.assertTrue(filter.apply("foo.bazFly"));
    }

    @Test
    public void testGlobalWhitelist() {
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter("*");

        for (String spec : DEFAULT_SPECS) {
            spec = spec.startsWith("!") ? spec.substring(1) : spec;
            Assert.assertTrue(spec, filter.apply(spec));
        }
        Assert.assertTrue(filter.apply("java.lang.String"));
        Assert.assertTrue(filter.apply(""));
        Assert.assertTrue(filter.apply("*")); // not really legal input but why not :)
    }

    @Test
    public void testEmptySpec() {
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter("");

        for (String spec : DEFAULT_SPECS) {
            spec = spec.startsWith("!") ? spec.substring(1) : spec;
            Assert.assertTrue(spec, filter.apply(spec));
        }
        Assert.assertTrue(filter.apply("java.lang.String"));
        Assert.assertTrue(filter.apply(""));
        Assert.assertTrue(filter.apply("*")); // not really legal input but why not :)
    }

    @Test
    public void testGlobalBlacklist() {
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter("!*");

        for (String spec : DEFAULT_SPECS) {
            spec = spec.startsWith("!") ? spec.substring(1) : spec;
            Assert.assertFalse(spec, filter.apply(spec));
        }
        Assert.assertFalse(filter.apply("java.lang.String"));
        Assert.assertFalse(filter.apply(""));
        Assert.assertFalse(filter.apply("*")); // not really legal input but why not :)
    }

    @Test
    public void testWhiteListExceptions() {
        exceptionTest("!foo.bar.Baz;foo.bar.*");
        exceptionTest("foo.bar.*;!foo.bar.Baz");
        exceptionTest("!foo.bar.Baz;foo.bar.**");
        exceptionTest("foo.bar.**;!foo.bar.Baz");
        exceptionTest("!foo.bar.Baz;foo.bar*");
        exceptionTest("foo.bar*;!foo.bar.Baz");
        exceptionTest("!foo.bar.Baz;foo.bar.FooBar");
        exceptionTest("foo.bar.FooBar;!foo.bar.Baz");
        // Negation overrules even exact whitelisting
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter("!foo.bar.Baz;foo.bar.Baz");
        Assert.assertFalse(filter.apply("foo.bar.Baz"));
    }

    private void exceptionTest(String filterSpec) {
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter(filterSpec);
        Assert.assertFalse(filter.apply("foo.bar.Baz"));
        Assert.assertTrue(filter.apply("foo.bar.FooBar"));
    }

    @Test
    public void testNullInput() {
        FilterSpecClassResolverFilter filter = new FilterSpecClassResolverFilter();
        try {
            filter.apply(null);
            Assert.fail("Null input accepted");
        } catch (IllegalArgumentException good) {
            // good
        }
    }

    @Test
    public void testIllegalSpecs() {
        illegalSpecTest("foo.***");
        illegalSpecTest("foo.*.**");
        illegalSpecTest("foo.*.bar.**");
        illegalSpecTest("foo.**.**");
        illegalSpecTest("*foo.Bar");
        illegalSpecTest("*foo.*");
        illegalSpecTest("*foo.**");
        illegalSpecTest("*foo*");
        illegalSpecTest("foo.Bar;bar*.Baz");
        illegalSpecTest("a=2");
        illegalSpecTest("foo.*;a=2;bar.baz.**");
    }

    private void illegalSpecTest(String spec) {
        try {
            new FilterSpecClassResolverFilter(spec);
            Assert.fail("Illegal spec should have been rejected: " + spec);
        } catch (IllegalArgumentException good) {
            // good
        }
    }
}
