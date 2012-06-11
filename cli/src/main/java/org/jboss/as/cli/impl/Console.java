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
package org.jboss.as.cli.impl;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import jline.Completor;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.protocol.StreamUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public interface Console {

    void addCompleter(CommandLineCompleter completer);

    boolean isUseHistory();

    void setUseHistory(boolean useHistory);

    CommandHistory getHistory();

    void setHistoryFile(File f);

    void clearScreen();

    void printColumns(Collection<String> list);

    void print(String line);

    void printNewLine();

    String readLine(String prompt);

    String readLine(String prompt, Character mask);

    static final class Factory {

        public static Console getConsole(CommandContext ctx) throws CliInitializationException {
            return getConsole(ctx, null, null);
        }

        public static Console getConsole(final CommandContext ctx, InputStream is, OutputStream os) throws CliInitializationException {

            final String bindingsName;
            final String osName = SecurityActions.getSystemProperty("os.name").toLowerCase(Locale.ENGLISH);
            if(osName.indexOf("windows") >= 0) {
                bindingsName = "keybindings/jline-windows-bindings.properties";
            } else if(osName.startsWith("mac")) {
                bindingsName = "keybindings/jline-mac-bindings.properties";
            } else {
                bindingsName = "keybindings/jline-default-bindings.properties";
            }

            if(is == null) {
                is = new FileInputStream(FileDescriptor.in);
            }
            if(os == null) {
                os = System.out;
            }
            final Writer writer;
            try {
                String encoding = SecurityActions.getSystemProperty("jline.WindowsTerminal.output.encoding");
                if(encoding == null) {
                    encoding = SecurityActions.getSystemProperty("file.encoding");
                }
                writer = new PrintWriter(new OutputStreamWriter(os, encoding));
            } catch (UnsupportedEncodingException e) {
                throw new CliInitializationException("Failed to initialize console writer.", e);
            }

            ClassLoader cl = SecurityActions.getClassLoader(Factory.class);
            InputStream bindingsIs = cl.getResourceAsStream(bindingsName);
            final jline.ConsoleReader jlineConsole;
            if(bindingsIs == null) {
                System.err.println("Failed to locate key bindings for OS '" + osName +"': " + bindingsName);
                try {
                    jlineConsole = new jline.ConsoleReader(is, writer);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to initialize console reader", e);
                }
            } else {
                try {
                    jlineConsole = new jline.ConsoleReader(is, writer, bindingsIs);
                } catch(Exception e) {
                    throw new IllegalStateException("Failed to initialize console reader", e);
                } finally {
                    StreamUtils.safeClose(bindingsIs);
                }
            }

            return new Console() {

                private CommandContext cmdCtx = ctx;
                private jline.ConsoleReader console = jlineConsole;
                private CommandHistory history = new HistoryImpl();

                @Override
                public void addCompleter(final CommandLineCompleter completer) {
                    console.addCompletor(new Completor(){
                        @SuppressWarnings({ "rawtypes", "unchecked" })
                        @Override
                        public int complete(String buffer, int cursor, List candidates) {
                            return completer.complete(cmdCtx, buffer, cursor, candidates);
                        }});
                }

                @Override
                public boolean isUseHistory() {
                    return jlineConsole.getUseHistory();
                }

                @Override
                public void setUseHistory(boolean useHistory) {
                    jlineConsole.setUseHistory(useHistory);
                }

                @Override
                public CommandHistory getHistory() {
                    return history;
                }

                @Override
                public void setHistoryFile(File f) {
                    try {
                        console.getHistory().setHistoryFile(f);
                    } catch (IOException e) {
                        System.err.println("Failed to setup the history file: " + f.getAbsolutePath());
                        e.printStackTrace();
                    }
                }

                @Override
                public void clearScreen() {
                    try {
                        console.setDefaultPrompt("");// it has to be reset apparently
                                                     // because otherwise it'll be printed
                                                     // twice
                        console.clearScreen();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void printColumns(Collection<String> list) {
                    try {
                        console.printColumns(list);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void print(String line) {
                    try {
                        console.printString(line);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void printNewLine() {
                    try {
                        console.printNewline();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public String readLine(String prompt) {
                    try {
                        return console.readLine(prompt);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                public String readLine(String prompt, Character mask) {
                    try {
                        return console.readLine(prompt, mask);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }

            class HistoryImpl implements CommandHistory {

                @SuppressWarnings("unchecked")
                @Override
                public List<String> asList() {
                    return console.getHistory().getHistoryList();
                }

                @Override
                public boolean isUseHistory() {
                    return console.getUseHistory();
                }

                @Override
                public void setUseHistory(boolean useHistory) {
                    console.setUseHistory(useHistory);
                }

                @Override
                public void clear() {
                    console.getHistory().clear();
                }

                @Override
                public void setMaxSize(int maxSize) {
                    console.getHistory().setMaxSize(maxSize);
                }

                @Override
                public int getMaxSize() {
                    return console.getHistory().getMaxSize();
                }
            }};
        }
    }
}
