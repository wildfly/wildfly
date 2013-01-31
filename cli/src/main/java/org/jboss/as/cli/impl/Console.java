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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.CommandLineCompleter;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.settings.Settings;

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

    int getTerminalWidth();

    int getTerminalHeight();

    static final class Factory {

        public static Console getConsole(CommandContext ctx) throws CliInitializationException {
            return getConsole(ctx, null, null);
        }

        public static Console getConsole(final CommandContext ctx, InputStream is, OutputStream os) throws CliInitializationException {

            org.jboss.aesh.console.Console aeshConsole = null;
            try {
                aeshConsole = new org.jboss.aesh.console.Console();
            } catch (IOException e) {
                e.printStackTrace();
            }

            final org.jboss.aesh.console.Console finalAeshConsole = aeshConsole;
            return new Console() {

                private CommandContext cmdCtx = ctx;
                private org.jboss.aesh.console.Console console = finalAeshConsole;
                private CommandHistory history = new HistoryImpl();

                @Override
                public void addCompleter(final CommandLineCompleter completer) {
                    console.addCompletion(new Completion() {
                        @Override
                        public void complete(CompleteOperation co) {
                            int offset =  completer.complete(cmdCtx,
                                    co.getBuffer(), co.getCursor(), co.getCompletionCandidates());
                            co.setOffset(offset);
                            if(co.getCompletionCandidates().size() == 1 &&
                                    co.getCompletionCandidates().get(0).startsWith(co.getBuffer()))
                                co.doAppendSeparator(true);
                            else
                                co.doAppendSeparator(false);
                        }
                    });
                }

                @Override
                public boolean isUseHistory() {
                    return !Settings.getInstance().isHistoryDisabled();
                }

                @Override
                public void setUseHistory(boolean useHistory) {
                    Settings.getInstance().setHistoryDisabled(!useHistory);
                }

                @Override
                public CommandHistory getHistory() {
                    return history;
                }

                @Override
                public void setHistoryFile(File f) {
                    Settings.getInstance().setHistoryFile(f);
                }

                @Override
                public void clearScreen() {
                    try {
                        console.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void printColumns(Collection<String> list) {
                    String[] newList = new String[list.size()];
                    list.toArray(newList);
                    try {
                        console.pushToStdOut(
                                org.jboss.aesh.util.Parser.formatDisplayList(newList,
                                        console.getTerminalHeight(), console.getTerminalWidth()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void print(String line) {
                    try {
                        console.pushToStdOut(line);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void printNewLine() {
                    try {
                        console.pushToStdOut(Config.getLineSeparator());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public String readLine(String prompt) {
                    try {
                        return console.read(prompt).getBuffer();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                public String readLine(String prompt, Character mask) {
                    try {
                        return console.read(prompt, mask).getBuffer();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                public int getTerminalWidth() {
                    return console.getTerminalWidth();
                }

                @Override
                public int getTerminalHeight() {
                    return console.getTerminalHeight();
                }

            class HistoryImpl implements CommandHistory {

                @SuppressWarnings("unchecked")
                @Override
                public List<String> asList() {
                    return console.getHistory().getAll();
                }

                @Override
                public boolean isUseHistory() {
                    return !Settings.getInstance().isHistoryDisabled();
                }

                @Override
                public void setUseHistory(boolean useHistory) {
                    Settings.getInstance().setHistoryDisabled(!useHistory);
                }

                @Override
                public void clear() {
                    console.getHistory().clear();
                }

                @Override
                public void setMaxSize(int maxSize) {
                    Settings.getInstance().setHistorySize(maxSize);
                }

                @Override
                public int getMaxSize() {
                    return Settings.getInstance().getHistorySize();
                }
            }};
        }
    }
}
