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

import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ExampleParser {

    private final File inputFile;
    private String[] subsystems;

    public ExampleParser(final File inputFile) {
        this.inputFile = inputFile;
    }

    void parse() throws IOException, XMLStreamException {
        InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
        try {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(in);
            reader.require(START_DOCUMENT, null, null);
            int type = reader.next();
            while (type != END_DOCUMENT) {
                System.out.println(formatType(type));
                if (type == START_ELEMENT) {
                    System.out.println(reader.getLocalName());
                } else if (type == CHARACTERS) {
                    if (!reader.isWhiteSpace()) {
                        System.out.println("->" + reader.getText());
                    }
                }
                type = reader.next();
            }
        } finally {
            try {
                in.close();
            } catch (Exception ignore) {
            }
        }
    }

    private void parseUsingEventReader() {

    }

    private String formatType(int type) {
        switch (type) {
        case 1: return type + "-START_ELEMENT";
        case 2: return type + "-END_ELEMENT";
        case 3: return type + "-PROCESSING_INSTRUCTION";
        case 4: return type + "-CHARACTERS";
        case 5: return type + "-COMMENT";
        case 6: return type + "-SPACE";
        case 7: return type + "-START_DOCUMENT";
        case 8: return type + "-END_DOCUMENT";
        case 9: return type + "-ENTITY_REFERENCE";
        case 10: return type + "-ATTRIBUTE";
        case 11: return type + "-DTD";
        case 12: return type + "-CDATA";
        case 13: return type + "-NAMESPACE";
        case 14: return type + "-NOTATION_DECLARATION";
        case 15: return type + "-ENTITY_DECLARATION";
        default: throw new IllegalStateException("Grr");
        }

    }
}
