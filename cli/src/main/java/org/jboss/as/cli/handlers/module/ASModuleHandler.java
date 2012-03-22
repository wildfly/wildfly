/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.handlers.module;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;
import org.jboss.as.cli.handlers.DefaultFilenameTabCompleter;
import org.jboss.as.cli.handlers.FilenameTabCompleter;
import org.jboss.as.cli.handlers.WindowsFilenameTabCompleter;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class ASModuleHandler extends CommandHandlerWithHelp {

    private static final String MODULE_PATH = "module.path";

    private static final String PATH_SEPARATOR = ","; // to make same command work on different systems

    private final ArgumentWithValue name = new ArgumentWithValue(this, "--name");
    private final ArgumentWithValue mainClass = new ArgumentWithValue(this, "--main-class");
    private final ArgumentWithValue resources;
    private final ArgumentWithValue dependencies;
    private final ArgumentWithValue props;

    public ASModuleHandler(CommandContext ctx) {
        super("module", false);

        final FilenameTabCompleter pathCompleter = Util.isWindows() ? new WindowsFilenameTabCompleter(ctx) : new DefaultFilenameTabCompleter(ctx);
        resources = new ArgumentWithValue(this, new CommandLineCompleter(){
            @Override
            public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {
                final int lastSeparator = buffer.lastIndexOf(PATH_SEPARATOR);
                if(lastSeparator >= 0) {
                    return lastSeparator + 1 + pathCompleter.complete(ctx, buffer.substring(lastSeparator + 1), cursor, candidates);
                }
                return pathCompleter.complete(ctx, buffer, cursor, candidates);
            }}, "--resources") {
            @Override
            public String getValue(ParsedCommandLine args) {
                String value = super.getValue(args);
                if(value != null) {
                    if(value.length() >= 0 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                        value = value.substring(1, value.length() - 1);
                    }
                    value = pathCompleter.translatePath(value);
                }
                return value;
            }
        };

        dependencies = new ArgumentWithValue(this, "--dependencies");
        props = new ArgumentWithValue(this, "--properties");
    }

    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {

        final ParsedCommandLine parsedCmd = ctx.getParsedCommandLine();
        final String moduleName = name.getValue(parsedCmd, true);

        final String modulesDir = SecurityActions.getSystemProperty(MODULE_PATH);
        if(modulesDir == null) {
            throw new CommandLineException(MODULE_PATH + " system property is not available.");
        }

        final String resourcePaths = resources.getValue(parsedCmd, true);
        final String[] resourceArr = resourcePaths.split(PATH_SEPARATOR);
        File[] resourceFiles = new File[resourceArr.length];
        for(int i = 0; i < resourceArr.length; ++i) {
            final File f = new File(resourceArr[i]);
            if(!f.exists()) {
                throw new CommandLineException("Failed to locate " + f.getAbsolutePath());
            }
            resourceFiles[i] = f;
        }

        final String modulePath = modulesDir + File.separatorChar + moduleName.replace('.', File.separatorChar);
        final File moduleDir = new File(modulePath);
        if(moduleDir.exists()) {
            throw new CommandLineException("Module " + moduleName + " already exists.");
        }
        if(!moduleDir.mkdirs()) {
            throw new CommandLineException("Failed to create directory " + moduleDir.getAbsolutePath());
        }

        final ModuleConfigImpl config = new ModuleConfigImpl(moduleName);

        for(File f : resourceFiles) {
            final File target = new File(moduleDir, f.getName());
            config.addResource(new ResourceRoot(target.getName()));
            try {
                copy(f, target);
            } catch (IOException e) {
                throw new CommandLineException("Failed to copy " + f.getAbsolutePath() + " to " + target.getAbsolutePath());
            }
        }

        final String dependenciesStr = dependencies.getValue(parsedCmd);
        if(dependenciesStr != null) {
            final String[] depsArr = dependenciesStr.split(",+");
            for(String dep : depsArr) {
                // TODO validate dependencies
                config.addDependency(new ModuleDependency(dep));
            }
        }

        final String propsStr = props.getValue(parsedCmd);
        if(propsStr != null) {
            final String[] pairs = propsStr.split(",");
            for (String pair : pairs) {
                int equals = pair.indexOf('=');
                if (equals == -1) {
                    throw new CommandFormatException("Property '" + pair + "' in '" + propsStr + "' is missing the equals sign.");
                }
                final String propName = pair.substring(0, equals);
                if (propName.isEmpty()) {
                    throw new CommandFormatException("Property name is missing for '" + pair + "' in '" + propsStr + "'");
                }
                config.setProperty(propName, pair.substring(equals + 1));
            }
        }

        final String mainCls = mainClass.getValue(parsedCmd);
        if(mainCls != null) {
            config.setMainClass(mainCls);
        }

        FileWriter moduleWriter = null;
        final File moduleFile = new File(moduleDir, "module.xml");
        try {
            moduleWriter = new FileWriter(moduleFile);
            XMLExtendedStreamWriter xmlWriter = create(XMLOutputFactory.newFactory().createXMLStreamWriter(moduleWriter));
            config.writeContent(xmlWriter, null);
            xmlWriter.flush();
        } catch (IOException e) {
            throw new CommandLineException("Failed to create file " + moduleFile.getAbsolutePath(), e);
        } catch (XMLStreamException e) {
            throw new CommandLineException("Failed to write to " + moduleFile.getAbsolutePath(), e);
        } finally {
            if(moduleWriter != null) {
                try {
                    moduleWriter.close();
                } catch (IOException e) {}
            }
        }
    }

    public static XMLExtendedStreamWriter create(XMLStreamWriter writer) throws CommandLineException {
        try {
            // Use reflection to access package protected class FormattingXMLStreamWriter
            // TODO: at some point the staxmapper API could be enhanced to make this unnecessary
            Class<?> clazz = Class.forName("org.jboss.staxmapper.FormattingXMLStreamWriter");
            Constructor<?> ctr = clazz.getConstructor( XMLStreamWriter.class );
            ctr.setAccessible(true);
            return (XMLExtendedStreamWriter)ctr.newInstance(new Object[]{writer});
        } catch (Exception e) {
            throw new CommandLineException("Failed to create xml stream writer.", e);
        }
    }

    public static void copy(final File source, final File target) throws IOException {
         final byte[] buff = new byte[8192];
         final BufferedInputStream in = new BufferedInputStream(new FileInputStream(source));
         final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target));
         int read;
         try {
            while ((read = in.read(buff)) != -1) {
               out.write(buff, 0, read);
            }
         } finally {
            out.flush();
            in.close();
            out.close();
         }
      }
}
