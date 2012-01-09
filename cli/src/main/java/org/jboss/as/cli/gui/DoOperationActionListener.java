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
package org.jboss.as.cli.gui;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import org.jboss.dmr.ModelNode;

/**
 * This class executes whatever command is on the command line.
 * It displays the result in the Output tab and sets "Output" to
 * be the currently selected tab.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class DoOperationActionListener extends AbstractAction {

    private CommandExecutor executor;

    private JTextField cmdText;
    private JTextComponent output;
    private JTabbedPane tabs;

    public DoOperationActionListener(JTextComponent output, JTabbedPane tabs) {
        this.executor = GuiMain.getExecutor();
        this.cmdText = GuiMain.getCommandText();
        this.output = output;
        this.tabs = tabs;
    }

    public void actionPerformed(ActionEvent ae) {
        String command = cmdText.getText();
        try {
            ModelNode result = executor.doCommand(command);
            postOutput(command, result.toString());
        } catch (Exception e) {
            try {
                postOutput(command, e.getMessage());
            } catch (BadLocationException ble) {
                ble.printStackTrace();
            }
        } finally {
            tabs.setSelectedIndex(1); // set to Output tab to view the output
        }
    }

    private void postOutput(String command, String response) throws BadLocationException {
        processOutput(response + "\n\n", null);
        processBoldOutput(command + "\n");
    }

    private void processBoldOutput(String text) throws BadLocationException {
        SimpleAttributeSet attribs = new SimpleAttributeSet();
        StyleConstants.setBold(attribs, true);
        processOutput(text, attribs);
    }


    private void processOutput(String text, AttributeSet attribs) throws BadLocationException {
        Document doc = output.getDocument();
        doc.insertString(0, text, attribs);
        output.setCaretPosition(0);
    }

}
