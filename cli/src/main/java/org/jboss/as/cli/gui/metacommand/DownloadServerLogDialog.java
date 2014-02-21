/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.gui.CliGuiContext;
import org.jboss.dmr.ModelNode;

/**
 * Dialog to choose destination file and download log.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class DownloadServerLogDialog extends JDialog implements ActionListener {
    // make this static so that it always retains the last directory chosen
    private static JFileChooser fileChooser = new JFileChooser(new File("."));

    private CliGuiContext cliGuiCtx;
    private String fileName;

    // we'll use this if we decide to show a progress indicator
    // might not be perfectly accurate
    private Long fileSize;

    private JPanel inputPanel = new JPanel(new GridBagLayout());
    private JTextField pathField = new JTextField(40);

    public DownloadServerLogDialog(CliGuiContext cliGuiCtx, String fileName, Long fileSize) {
        super(cliGuiCtx.getMainWindow(), "Download " + fileName, Dialog.ModalityType.APPLICATION_MODAL);
        this.cliGuiCtx = cliGuiCtx;
        this.fileName = fileName;
        this.fileSize = fileSize;
        fileChooser.setSelectedFile(new File(fileName));
        setPathField();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10));

        contentPane.add(makeInputPanel(), BorderLayout.CENTER);

        contentPane.add(makeButtonPanel(), BorderLayout.SOUTH);
        pack();
        setResizable(false);
    }

    private void setPathField() {
        try {
            pathField.setText(fileChooser.getSelectedFile().getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JPanel makeInputPanel() {
        GridBagConstraints gbConst = new GridBagConstraints();
        gbConst.anchor = GridBagConstraints.WEST;
        gbConst.insets = new Insets(5, 5, 5, 5);

        JLabel pathLabel = new JLabel("Download To:");
        gbConst.gridwidth = 1;
        inputPanel.add(pathLabel, gbConst);

        addStrut();
        inputPanel.add(pathField, gbConst);

        addStrut();
        JButton browse = new JButton("Browse ...");
        browse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                int returnVal = fileChooser.showOpenDialog(DownloadServerLogDialog.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    setPathField();
                }
            }
        });
        gbConst.gridwidth = GridBagConstraints.REMAINDER;
        inputPanel.add(browse, gbConst);

        return inputPanel;
    }

    private void addStrut() {
        inputPanel.add(Box.createHorizontalStrut(5));
    }

    private JPanel makeButtonPanel() {
        JPanel buttonPanel = new JPanel();

        JButton ok = new JButton("OK");
        ok.addActionListener(this);
        ok.setMnemonic(KeyEvent.VK_ENTER);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                DownloadServerLogDialog.this.dispose();
            }
        });

        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        return buttonPanel;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String path = pathField.getText();
        if (path.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "A file path must be selected.", "Empty File Path", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File selectedFile = new File(path);
        if (selectedFile.exists()) {
            this.setVisible(false);
            int option = JOptionPane.showConfirmDialog(cliGuiCtx.getMainWindow(), "Overwrite " + path, "Overwrite?", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.NO_OPTION) {
                this.setVisible(true);
                return;
            }
        }

        this.dispose();
        cliGuiCtx.getMainWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        PrintStream out = null;
        try {
            out = new PrintStream(new BufferedOutputStream(new FileOutputStream(selectedFile)));
            int linesToRead = 5000;
            int skip = 0;
            List<ModelNode> dataLines = null;
            long bytesRead = 0; // we'll use this if we decide to show a progress indicator
            int lineSepLength = System.getProperty("line.separator").length();

            do {
                String command = "/subsystem=logging/:read-log-file(name=" + fileName + ",lines=" + linesToRead + ",skip=" + skip + ",tail=false)";
                ModelNode result = cliGuiCtx.getExecutor().doCommand(command);
                if (result.get("outcome").asString().equals("failed")) {
                    String error = "Failure at server: " + result.get("failure-description").toString();
                    JOptionPane.showMessageDialog(cliGuiCtx.getMainWindow(), error, "Download Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                dataLines = result.get("result").asList();
                for (ModelNode line : dataLines) {
                    String strLine = line.asString();
                    bytesRead += strLine.length() + lineSepLength;
                    out.println(strLine);
                }
                skip += linesToRead;
            } while (dataLines.size() == linesToRead);
        } catch (IOException | CommandFormatException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (out != null) out.close();
            cliGuiCtx.getMainWindow().setCursor(Cursor.getDefaultCursor());
        }

        JOptionPane.showMessageDialog(cliGuiCtx.getMainWindow(), "Download complete.");
    }
}
