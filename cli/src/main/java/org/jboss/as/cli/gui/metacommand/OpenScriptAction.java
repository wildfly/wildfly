/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.cli.gui.metacommand;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.gui.CliGuiContext;
import org.jboss.as.protocol.StreamUtils;

/**
 * Action for the open script menu selection.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class OpenScriptAction extends AbstractAction {
    // make this static so that it always retains the last directory chosen
    private static JFileChooser fileChooser = new JFileChooser(new File("."));

    private CliGuiContext cliGuiCtx;

    public OpenScriptAction(CliGuiContext cliGuiCtx) {
        super("Open Script");
        this.cliGuiCtx = cliGuiCtx;
    }

    public void actionPerformed(ActionEvent e) {
        int returnVal = fileChooser.showOpenDialog(cliGuiCtx.getMainPanel());
        if (returnVal != JFileChooser.APPROVE_OPTION) return;

        PrintStream originalOut = System.out;
        ByteArrayOutputStream scriptOut = new ByteArrayOutputStream();
        PrintStream printOut = new PrintStream(scriptOut);

        try {
            String filePath = fileChooser.getSelectedFile().getCanonicalPath();
            if ((filePath == null) || (filePath.trim().equals(""))) return;


            System.setOut(printOut);

            doScript(getCommandLines(filePath), scriptOut);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            System.setOut(originalOut);
            System.out.println("from print stream:");
            System.out.println(scriptOut.toString());
        }
    }

    private List<String> getCommandLines(String filePath) {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine();
            while (line != null) {
                lines.add(line.trim());
                line = reader.readLine();
            }
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to process file '" + filePath + "'", e);
        } finally {
            StreamUtils.safeClose(reader);
        }

        return lines;
    }

    private void doScript(List<String> commands, ByteArrayOutputStream out) throws IOException {

        for (String command : commands) {
            try {
                if (!command.startsWith("/")) System.out.println(command);
                cliGuiCtx.getCommmandContext().handle(command);
            } catch (CommandLineException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
