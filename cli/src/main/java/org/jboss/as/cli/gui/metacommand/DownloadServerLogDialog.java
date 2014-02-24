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
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.gui.CliGuiContext;
import org.jboss.dmr.ModelNode;

/**
 * Dialog to choose destination file and download log.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class DownloadServerLogDialog extends JDialog implements ActionListener, PropertyChangeListener {
    // make these static so that they always retains the last value chosen
    private static JFileChooser fileChooser = new JFileChooser(new File("."));
    private static JCheckBox viewInLogViewer = new JCheckBox("View in default log viewer");
    static {
        viewInLogViewer.setSelected(true);
    }

    private CliGuiContext cliGuiCtx;
    private String fileName;
    private Long fileSize;
    private JPanel inputPanel = new JPanel(new GridBagLayout());
    private JTextField pathField = new JTextField(40);

    private ProgressMonitor progressMonitor;
    private DownloadLogTask downloadTask;

    public DownloadServerLogDialog(CliGuiContext cliGuiCtx, String fileName, Long fileSize) {
        super(cliGuiCtx.getMainWindow(), "Download " + fileName, Dialog.ModalityType.APPLICATION_MODAL);
        this.cliGuiCtx = cliGuiCtx;
        this.fileName = fileName;
        this.fileSize = fileSize;

        fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), fileName));
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
            throw new RuntimeException(e);
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

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            JLabel emptyLabel = new JLabel("");
            gbConst.gridwidth = 1;
            inputPanel.add(emptyLabel, gbConst);
            addStrut();
            gbConst.gridwidth = GridBagConstraints.REMAINDER;
            inputPanel.add(viewInLogViewer, gbConst);
        }

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

        progressMonitor = new ProgressMonitor(cliGuiCtx.getMainWindow(), "Downloading " + fileName, "", 0, 100);
        progressMonitor.setProgress(0);
        downloadTask = new DownloadLogTask(selectedFile);
        downloadTask.addPropertyChangeListener(this);
        downloadTask.execute();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())){
            int percentRead = (Integer) evt.getNewValue();
            progressMonitor.setProgress(percentRead);
        }

        if ("bytesRead".equals(evt.getPropertyName())) {
            progressMonitor.setNote(evt.getNewValue() + " of " + fileSize + " bytes received.");
        }

        if (progressMonitor.isCanceled()) {
            downloadTask.cancel(false);
        }
    }

    class DownloadLogTask extends SwingWorker<Void, Void> {
        private File selectedFile;

        public DownloadLogTask(File selectedFile) {
            this.selectedFile = selectedFile;
        }

        @Override
        public Void doInBackground() {
            PrintStream out = null;
            try {
                out = new PrintStream(new BufferedOutputStream(new FileOutputStream(selectedFile)));
                int linesToRead = 5000;
                int skip = 0;
                List<ModelNode> dataLines = null;
                long bytesRead = 0;
                long bytesReadOldValue = 0;
                int lineSepLength = System.getProperty("line.separator").length();

                do {
                    String command = "/subsystem=logging/:read-log-file(name=" + fileName + ",lines=" + linesToRead + ",skip=" + skip + ",tail=false)";
                    ModelNode result = cliGuiCtx.getExecutor().doCommand(command);
                    if (result.get("outcome").asString().equals("failed")) {
                        cancel(false);
                        String error = "Failure at server: " + result.get("failure-description").toString();
                        JOptionPane.showMessageDialog(cliGuiCtx.getMainWindow(), error, "Download Failed", JOptionPane.ERROR_MESSAGE);
                        return null;
                    }

                    dataLines = result.get("result").asList();
                    for (ModelNode line : dataLines) {
                        String strLine = line.asString();
                        bytesRead += strLine.length() + lineSepLength;
                        out.println(strLine);
                    }
                    skip += linesToRead;

                    setProgress(Math.min(Math.round(((float)bytesRead / (float)fileSize)*100), 100));
                    firePropertyChange("bytesRead", bytesReadOldValue, bytesRead);
                    bytesReadOldValue = bytesRead;
                } while ((dataLines.size() == linesToRead) && !isCancelled());
            } catch (IOException | CommandFormatException ex) {
                throw new RuntimeException(ex);
            } finally {
                if (out != null) {
                    out.close();
                }

                if (isCancelled()) {
                    selectedFile.delete();
                }
            }

            return null;
        }

        @Override
        public void done() {
            String message = "Download " + fileName + " ";
            if (isCancelled()) {
                JOptionPane.showMessageDialog(cliGuiCtx.getMainWindow(), message + "cancelled.", message + "cancelled.", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!viewInLogViewer.isSelected()) {
                JOptionPane.showMessageDialog(cliGuiCtx.getMainWindow(), message + "complete.");
                return;
            }

            try {
                Desktop.getDesktop().open(selectedFile);
            } catch (IOException ioe) {
                // try to open in file manager for destination directory
                try {
                    Desktop.getDesktop().open(fileChooser.getCurrentDirectory());
                } catch (IOException ioe2) {
                    JOptionPane.showMessageDialog(cliGuiCtx.getMainWindow(), "Download success.  No registered application to view " + fileName, "Can't view file.", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
