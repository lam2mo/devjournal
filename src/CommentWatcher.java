import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.util.*;

public class CommentWatcher extends JPanel implements ActionListener {

    private EventLog eventLog;
    private JTextArea commentArea;
    private JButton addFileButton;
    private JButton pasteButton;
    private JButton consoleButton;
    private JButton commentButton;
    private Action pasteAction;

    public CommentWatcher(EventLog log) {
        eventLog = log;
        commentArea = new JTextArea(8,70);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        commentArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane commentPane = new JScrollPane(commentArea);
        pasteAction = findAction(commentArea.getActions(), DefaultEditorKit.pasteAction);
        addFileButton = new JButton("Add File");
        addFileButton.addActionListener(this);
        pasteButton = new JButton("Paste text from Clipboard");
        pasteButton.addActionListener(this);
        consoleButton = new JButton("Add as Console Dump");
        consoleButton.addActionListener(this);
        commentButton = new JButton("Add as Comment");
        commentButton.addActionListener(this);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addFileButton);
        buttonPanel.add(pasteButton);
        buttonPanel.add(consoleButton);
        buttonPanel.add(commentButton);
        setLayout(new BorderLayout());
        JLabel label = new JLabel("Enter comment:");
        label.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
        add(label, BorderLayout.NORTH);
        add(commentPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    }

    private Action findAction(Action actions[], String key) {
        Hashtable commands = new Hashtable();
        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            commands.put(action.getValue(Action.NAME), action);
        }
        return (Action) commands.get(key);
    }

    public void addTextEvent(String tag) {
        Event evt = eventLog.createEvent("text");
        if (tag != null) {
            evt.addTag(tag);
        }
        String text = commentArea.getText();
        if (text.length() < 4096) {
            evt.setProperty("details", text);
        } else {
            evt.setAttachment("details", eventLog.createAttachment(text));
        }
        eventLog.addEvent(evt);
    }
    
    public void addFile() {
        JFileChooser chooser = new JFileChooser();
        int rval = chooser.showOpenDialog(this);
        if (rval == JFileChooser.APPROVE_OPTION) {
            Attachment att = eventLog.createAttachment(chooser.getSelectedFile());
            if (att.hasProperty("mimetype") && att.getProperty("mimetype").startsWith("image")) {
                Event evt = eventLog.createEvent("file");
                att.setProperty("path", chooser.getSelectedFile().getAbsolutePath());
                evt.setProperty("path", chooser.getSelectedFile().getAbsolutePath());
                evt.setAttachment("image", att);
                eventLog.addEvent(evt);
            } else if (att.hasProperty("mimetype") && att.getProperty("mimetype").startsWith("text")) {
                Event evt = eventLog.createEvent("file");
                att.setProperty("path", chooser.getSelectedFile().getAbsolutePath());
                evt.setProperty("path", chooser.getSelectedFile().getAbsolutePath());
                evt.setAttachment("snapshot", att);
                eventLog.addEvent(evt);
            } else {
                JOptionPane.showMessageDialog(this, "ERROR: File type not supported.", 
                        "Add File", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == commentButton) {
            addTextEvent("comment");
            commentArea.setText("");
            commentArea.requestFocus();
        } else if (e.getSource() == consoleButton) {
            addTextEvent("console");
            commentArea.setText("");
            commentArea.requestFocus();
        } else if (e.getSource() == pasteButton) {
            commentArea.requestFocus();
            pasteAction.actionPerformed(new ActionEvent(commentArea, 0, DefaultEditorKit.pasteAction));
        } else if (e.getSource() == addFileButton) {
            addFile();
        }
    }

}

