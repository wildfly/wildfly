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
package org.jboss.as.cli.gui.component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class HelpButton extends JButton {

    private JScrollPane helpScroller;

    public HelpButton(String helpFile) {
        super("Help");
        try {
            readHelpFile(helpFile);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(HelpButton.this, "Unable to read " + helpFile);
            return;
        }

        addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JOptionPane helpPane = new JOptionPane(helpScroller, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION);
                JDialog dialog = helpPane.createDialog(HelpButton.this, "Help");
                dialog.setResizable(true);
                dialog.setModal(false);
                dialog.setSize(dialog.getHeight(), helpScroller.getWidth() + 10);
                dialog.setVisible(true);
            }

        });
    }

    private void readHelpFile(String helpFile) throws IOException {
        InputStream in = getClass().getResourceAsStream("/help/" + helpFile);
        JEditorPane helpText = new JEditorPane();
        helpText.read(in, null);
        helpText.setEditable(false);
        helpScroller = new JScrollPane(helpText);
    }
}
