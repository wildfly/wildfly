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
