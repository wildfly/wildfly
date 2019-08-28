/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxrs;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="rsigal@redhat.com>Ron Sigal</a>
 */
public class JaxrsSubsystem20TestCase extends AbstractSubsystemBaseTest {

    public JaxrsSubsystem20TestCase() {
        super(JaxrsExtension.SUBSYSTEM_NAME, new JaxrsExtension());
    }

    protected void standardSubsystemTest(final String configId) throws Exception {
       standardSubsystemTest(configId, true);
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return
              "<subsystem xmlns=\"urn:jboss:domain:jaxrs:2.0\">\n" +
              "    <context-parameters>\n" +
              "        <context-parameter name=\"jaxrs.2.0.request.matching\" value=\"true\"/>\n" +
              "        <context-parameter name=\"resteasy.add.charset\" value=\"false\"/>\n" +
              "        <context-parameter name=\"resteasy.buffer.exception.entity\" value=\"false\"/>\n" +
              "        <context-parameter name=\"resteasy.disable.html.sanitizer\" value=\"true\"/>\n" +
              "        <context-parameter name=\"resteasy.disable.providers\" value=\"a.b,cc.dd.ee\"/>\n" +
              "        <context-parameter name=\"resteasy.document.expand.entity.references\" value=\"true\"/>\n" +
              "        <context-parameter name=\"resteasy.document.secure.disableDTDs\" value=\"false\"/>\n" +
              "        <context-parameter name=\"resteasy.document.secure.processing.feature\" value=\"false\"/>\n" +
              "        <context-parameter name=\"resteasy.enable.providers\" value=\"f,i.j\"/>\n" +
              "        <context-parameter name=\"resteasy.gzip.max.input\" value=\"777\"/>\n" +
              "        <context-parameter name=\"resteasy.jndi.resources\" value=\"k.l,mm.nnn\"/>\n" +
              "        <context-parameter name=\"resteasy.language.mappings\" value=\"o:4444,pp:5555,q:6666\"/>\n" +
              "        <context-parameter name=\"resteasy.media.type.mappings\" value=\"r:77777\"/>\n" +
              "        <context-parameter name=\"resteasy.media.type.param.mapping\" value=\"tu\"/>\n" +
              "        <context-parameter name=\"resteasy.providers\" value=\"s,t.t\"/>\n" +
              "        <context-parameter name=\"resteasy.resources\" value=\"u.u,v,w\"/>\n" +
              "        <context-parameter name=\"resteasy.rfc7232preconditions\" value=\"true\"/>\n" +
              "        <context-parameter name=\"resteasy.role.based.security\" value=\"true\"/>\n" +
              "        <context-parameter name=\"resteasy.secure.random.max.use\" value=\"2222\"/>\n" +
              "        <context-parameter name=\"resteasy.use.builtin.providers\" value=\"false\"/>\n" +
              "        <context-parameter name=\"resteasy.use.container.form.params\" value=\"true\"/>\n" +
              "        <context-parameter name=\"resteasy.wider.request.matching\" value=\"true\"/>\n" +
              "    </context-parameters>\n" +
              "</subsystem>";
    }

    @Override
    protected String getSubsystemXml(String configId) throws IOException {
       return "<subsystem xmlns=\"urn:jboss:domain:jaxrs:2.0\"/>";
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-jaxrs_2_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[]{
                "/subsystem-templates/jaxrs_2_0.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }

    //no point in testing 1.0.0 (current) --> 1.0.0 (all previous) for transformers
}
