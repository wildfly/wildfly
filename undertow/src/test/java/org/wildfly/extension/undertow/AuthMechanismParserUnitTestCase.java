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

package org.wildfly.extension.undertow;

import io.undertow.servlet.api.AuthMethodConfig;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.undertow.deployment.AuthMethodParser;

import java.util.Collections;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class AuthMechanismParserUnitTestCase {

    @Test
    public void testAuthMechanismParsing() {
        List<AuthMethodConfig> res = AuthMethodParser.parse("BASIC", Collections.<String, String>emptyMap());
        Assert.assertEquals(1, res.size());
        Assert.assertEquals(0, res.get(0).getProperties().size());
        Assert.assertEquals("BASIC", res.get(0).getName());
        res = AuthMethodParser.parse("BASIC?silent=true", Collections.<String, String>emptyMap());
        Assert.assertEquals(1, res.size());
        Assert.assertEquals(1, res.get(0).getProperties().size());
        Assert.assertEquals("BASIC", res.get(0).getName());
        Assert.assertEquals("true", res.get(0).getProperties().get("silent"));
        res = AuthMethodParser.parse("BASIC?silent=true,FORM", Collections.<String, String>emptyMap());
        Assert.assertEquals(2, res.size());
        Assert.assertEquals(1, res.get(0).getProperties().size());
        Assert.assertEquals("BASIC", res.get(0).getName());
        Assert.assertEquals("true", res.get(0).getProperties().get("silent"));
        Assert.assertEquals(0, res.get(1).getProperties().size());
        Assert.assertEquals("FORM", res.get(1).getName());
        res = AuthMethodParser.parse("BASIC?silent=true,FORM,", Collections.<String, String>emptyMap());
        Assert.assertEquals(2, res.size());
        Assert.assertEquals(1, res.get(0).getProperties().size());
        Assert.assertEquals("BASIC", res.get(0).getName());
        Assert.assertEquals("true", res.get(0).getProperties().get("silent"));
        Assert.assertEquals(0, res.get(1).getProperties().size());
        Assert.assertEquals("FORM", res.get(1).getName());
        res = AuthMethodParser.parse("BASIC?silent=true,FORM?,", Collections.<String, String>emptyMap());
        Assert.assertEquals(2, res.size());
        Assert.assertEquals(1, res.get(0).getProperties().size());
        Assert.assertEquals("BASIC", res.get(0).getName());
        Assert.assertEquals("true", res.get(0).getProperties().get("silent"));
        Assert.assertEquals(0, res.get(1).getProperties().size());
        Assert.assertEquals("FORM", res.get(1).getName());
        res = AuthMethodParser.parse("BASIC?silent=true,FORM?a=b+c&d=e%20f,", Collections.<String, String>emptyMap());
        Assert.assertEquals(2, res.size());
        Assert.assertEquals(1, res.get(0).getProperties().size());
        Assert.assertEquals("BASIC", res.get(0).getName());
        Assert.assertEquals("true", res.get(0).getProperties().get("silent"));
        Assert.assertEquals(2, res.get(1).getProperties().size());
        Assert.assertEquals("FORM", res.get(1).getName());
        Assert.assertEquals("b c", res.get(1).getProperties().get("a"));
        Assert.assertEquals("e f", res.get(1).getProperties().get("d"));
    }
}
