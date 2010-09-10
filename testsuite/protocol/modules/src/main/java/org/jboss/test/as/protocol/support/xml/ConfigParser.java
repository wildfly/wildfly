/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.test.as.protocol.support.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.xml.stream.XMLInputFactory;

import org.jboss.as.model.Domain;
import org.jboss.as.model.Host;
import org.jboss.as.model.ParseResult;
import org.jboss.as.server.manager.StandardElementReaderRegistrar;
import org.jboss.staxmapper.XMLMapper;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ConfigParser {
    static StandardElementReaderRegistrar extensionRegistrar = StandardElementReaderRegistrar.Factory.getRegistrar();

    public static Host parseHost(File cfgDir) throws Exception {
        XMLMapper mapper = XMLMapper.Factory.create();
        extensionRegistrar.registerStandardHostReaders(mapper);
        return parseXml(cfgDir, "host.xml", mapper, Host.class);
    }

    public static Domain parseDomain(File cfgDir) throws Exception {
        XMLMapper mapper = XMLMapper.Factory.create();
        extensionRegistrar.registerStandardDomainReaders(mapper);
        return parseXml(cfgDir, "domain.xml", mapper, Domain.class);
    }

    private static <T> T parseXml(File cfgDir, String name, XMLMapper mapper, Class<T> type) throws Exception {
        File file = new File(cfgDir, name);
        if (!file.exists())
            throw new IllegalStateException("File " + file.getAbsolutePath() + " does not exist.");

        try {
            ParseResult<T> parseResult = new ParseResult<T>();
            mapper.parseDocument(parseResult, XMLInputFactory.newInstance().createXMLStreamReader(new BufferedReader(new FileReader(file))));
            return parseResult.getResult();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Caught exception during processing of " + name, e);
        }
    }

}
