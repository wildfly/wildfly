/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.controller.parsing.DomainXml;
import org.jboss.as.controller.parsing.HostXml;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.parsing.StandaloneXml;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class __ThrowawayParsingTest {

    final static String JBOSS_HOME = "/Users/kabir/sourcecontrol/jboss-as7/git/jboss-as/build/target/jboss-7.0.0.Alpha2/";

    @Test
    public void testParseStandaloneXml() {
        String fileName = JBOSS_HOME + "standalone/configuration/standalone.xml";
        File file = new File(fileName);
        StandaloneXml xml = new StandaloneXml(null);
        parseXml(file, new QName(Namespace.DOMAIN_1_0.getUriString(), "server"), xml);
    }

    @Test
    public void testParseHostXml() {
        String fileName = JBOSS_HOME + "domain/configuration/host.xml";
        File file = new File(fileName);
        HostXml xml = new HostXml(null);
        parseXml(file, new QName(Namespace.DOMAIN_1_0.getUriString(), "host"), xml);
    }

    @Test
    public void testParseDomainXml() {
        String fileName = JBOSS_HOME + "domain/configuration/domain.xml";
        File file = new File(fileName);
        DomainXml xml = new DomainXml(null);
        parseXml(file, new QName(Namespace.DOMAIN_1_0.getUriString(), "domain"), xml);
    }

    private List<ModelNode> parseXml(File file, QName rootElement, XMLElementReader<List<ModelNode>> parser){
        final XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(rootElement, parser);
        List<ModelNode> updates = new ArrayList<ModelNode>();

        try {
            //FIXME hook up with host configuration persister
            final FileInputStream fis = new FileInputStream(file);
            try {
                BufferedInputStream input = new BufferedInputStream(fis);
                XMLStreamReader streamReader = XMLInputFactory.newInstance().createXMLStreamReader(input);
                mapper.parseDocument(updates, streamReader);
                streamReader.close();
                input.close();
                fis.close();

                System.out.println("Parsed updates " + updates);


            } finally {
                StreamUtils.safeClose(fis);
            }
        } catch (Exception e) {
            //throw new ConfigurationPersistenceException("Failed to parse configuration", e);
            throw new RuntimeException(e);
        }


        return updates;
    }

}
