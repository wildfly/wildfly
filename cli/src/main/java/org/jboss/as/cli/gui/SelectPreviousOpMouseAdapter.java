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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class SelectPreviousOpMouseAdapter extends MouseAdapter implements ClipboardOwner {
    private JTextPane output;
    private JTextField cmdText;
    private DoOperationActionListener opListener;
    private Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    public SelectPreviousOpMouseAdapter(JTextPane output, JTextField cmdText, DoOperationActionListener opListener) {
        this.output = output;
        this.cmdText = cmdText;
        this.opListener = opListener;
    }

    @Override
    public void mouseClicked(MouseEvent me) {
        if (me.getClickCount() < 2) return;

        int pos = output.viewToModel(me.getPoint());

        try {
            int rowStart = Utilities.getRowStart(output, pos);
            int rowEnd = Utilities.getRowEnd(output, pos);
            String line = output.getDocument().getText(rowStart, rowEnd - rowStart);
            if (opListener.getCmdHistory().contains(line)) {
                output.select(rowStart, rowEnd);
                cmdText.setText(line);
                systemClipboard.setContents(new StringSelection(line), this);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

    }

    public void lostOwnership(Clipboard clpbrd, Transferable t) {
        // do nothing
    }

}
