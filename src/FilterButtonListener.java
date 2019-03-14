import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class FilterButtonListener implements ActionListener {

    public static final int ADD = 1;
    public static final int EDIT = 2;
    public static final int REMOVE = 3;

    private int mode;
    private JList list;
    private DefaultListModel model;
    private ProjectEditor parent;

    public FilterButtonListener(int mode, JList list, 
            DefaultListModel model, ProjectEditor parent) {
        this.mode = mode;
        this.list = list;
        this.model = model;
        this.parent = parent;
    }

    public void actionPerformed(ActionEvent e) {
        if (mode == ADD) {
            String filter = JOptionPane.showInputDialog(parent,
                "Enter filter (regular expression):");
            if (filter != null) {
                model.addElement(filter);
            }
        } else if (mode == EDIT) {
            if (list.getSelectedValue() != null) {
                String filter = JOptionPane.showInputDialog(parent,
                    "Enter filter (regular expression):", 
                    list.getSelectedValue());
                if (filter != null) {
                    model.set(list.getSelectedIndex(), filter);
                }
            }
        } else if (mode == REMOVE) {
            if (list.getSelectedValue() != null) {
                model.remove(list.getSelectedIndex());
            }
        }
        parent.refreshFileListPreview();
    }

}

