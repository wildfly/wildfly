/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jdr.commands;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Properties;

import org.jboss.as.jdr.util.Sanitizer;
import org.jboss.as.jdr.util.Utils;

/**
 * Add the JVM System properties to the JDR report
 *
 * @author Brad Maxwell
 */
public class SystemProperties extends JdrCommand {

    private LinkedList<Sanitizer> sanitizers = new LinkedList<Sanitizer>();

    public SystemProperties sanitizer(Sanitizer ... sanitizers) {
        for (Sanitizer s : sanitizers) {
            this.sanitizers.add(s);
        }
        return this;
    }

    @Override
    public void execute() throws Exception {
        if(!this.env.isServerRunning())
            return;

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        Properties properties = System.getProperties();

        Enumeration<?> names = properties.propertyNames();
        while(names.hasMoreElements()) {
            String name = (String) names.nextElement();
            printWriter.println(name + "=" + properties.getProperty(name));
        }
        InputStream stream = new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8));

        for (Sanitizer sanitizer : this.sanitizers) {
            stream = sanitizer.sanitize(stream);
        }

        this.env.getZip().addAsString(stream, "system-properties.txt");
        Utils.safelyClose(stream);
    }
}