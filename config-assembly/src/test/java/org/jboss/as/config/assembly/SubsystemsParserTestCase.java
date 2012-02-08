/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.config.assembly;

import java.io.File;
import java.net.URL;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SubsystemsParserTestCase {

    @Test
    public void testParseTemplate() throws Exception {
        URL url = this.getClass().getResource("subsystems.xml");
        SubsystemsParser templateParser = new SubsystemsParser(new File(url.toURI()));
        templateParser.parse();
        Map<String, SubsystemConfig[]> config = templateParser.getSubsystemConfigs();
        Assert.assertEquals(3, config.size());
        SubsystemConfig[] defaultConfig = config.get("default");
        Assert.assertNotNull(defaultConfig);
        Assert.assertEquals(2, defaultConfig.length);
        Assert.assertEquals("simple-with-text-and-comments.xml", defaultConfig[0].getSubsystemFile());
        Assert.assertNull(defaultConfig[0].getSupplement());
        Assert.assertEquals("empty-with-attributes.xml", defaultConfig[1].getSubsystemFile());
        Assert.assertNull(defaultConfig[1].getSupplement());

        SubsystemConfig[] haConfig = config.get("ha");
        Assert.assertNotNull(haConfig);
        Assert.assertEquals(2, haConfig.length);
        Assert.assertEquals("empty.xml", haConfig[0].getSubsystemFile());
        Assert.assertNull(haConfig[0].getSupplement());
        Assert.assertEquals("ha-only.xml", haConfig[1].getSubsystemFile());
        Assert.assertEquals("hah!", haConfig[1].getSupplement());

        SubsystemConfig[] unnamed = config.get("");
        Assert.assertNotNull(unnamed);
        Assert.assertEquals(2, unnamed.length);
        Assert.assertEquals("simple-with-text-and-comments.xml", unnamed[0].getSubsystemFile());
        Assert.assertNull(unnamed[0].getSupplement());
        Assert.assertEquals("empty-with-attributes.xml", unnamed[1].getSubsystemFile());
        Assert.assertNull(unnamed[1].getSupplement());
    }

}
