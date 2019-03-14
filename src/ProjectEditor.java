import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.util.*;

public class ProjectEditor extends JDialog implements ActionListener {

    public static final Font STANDARD_FONT = new Font("Monospaced", Font.PLAIN, 11);

    private File projectFile;
    private Project project;

    private String oldName;
    private JTextField nameField;

    private JList rootList;
    private DefaultListModel rootModel;
    private JTextField pollIntervalField;

    private DefaultListModel includeFileFilterModel;
    private DefaultListModel excludeFileFilterModel;
    private DefaultListModel includeFolderFilterModel;
    private DefaultListModel excludeFolderFilterModel;

    private java.util.List<String> roots;
    private java.util.List<String> includeFileFilters;
    private java.util.List<String> excludeFileFilters;
    private java.util.List<String> includeFolderFilters;
    private java.util.List<String> excludeFolderFilters;
    private java.util.List<String> files;

    private JLabel filePreviewLabel;
    private JList filePreviewList;

    private JButton okButton;
    private JButton cancelButton;

    public ProjectEditor(Frame parent, File path) {
        super(parent, true);
        projectFile = path;
        project = Project.readFromFile(path);
        oldName = project.getName();

        nameField = new JTextField(project.getName(), 15);
        nameField.setFont(STANDARD_FONT);
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.LINE_AXIS));
        namePanel.setBorder(BorderFactory.createEmptyBorder(10,10,5,10));
        namePanel.add(new JLabel("Project name: "));
        namePanel.add(nameField);

        namePanel.add(Box.createRigidArea(new Dimension(20, 0)));
        pollIntervalField = new JTextField(
                (Integer.valueOf(project.getPollInterval())).toString(), 8);
        pollIntervalField.setFont(STANDARD_FONT);
        namePanel.add(new JLabel("Poll interval (in milliseconds): "));
        namePanel.add(pollIntervalField);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(namePanel, BorderLayout.NORTH);
        topPanel.add(buildRootPanel(), BorderLayout.CENTER);

        JPanel filePreviewPanel = new JPanel();
        filePreviewLabel = new JLabel("0 files included in project:");
        filePreviewList = new JList();
        filePreviewPanel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        filePreviewPanel.setLayout(new BorderLayout());
        filePreviewPanel.add(filePreviewLabel, BorderLayout.NORTH);
        filePreviewPanel.add(new JScrollPane(filePreviewList), BorderLayout.CENTER);

        okButton = new JButton("OK");
        okButton.addActionListener(this);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        JPanel bottomButtonPanel = new JPanel();
        bottomButtonPanel.add(okButton);
        bottomButtonPanel.add(cancelButton);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(filePreviewPanel, BorderLayout.CENTER);
        bottomPanel.add(bottomButtonPanel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(buildMainFilterPanel(), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle("Editing Project: " + project.getName());
        setSize(700,700);
        setLocationRelativeTo(null);

        refreshFileListPreview();
    }

    public JPanel buildRootPanel() {
        rootModel = new DefaultListModel();
        for (String s : project.getRoots()) {
            rootModel.addElement(s);
        }
        rootList = new JList(rootModel);
        rootList.setVisibleRowCount(6);
        rootList.setFont(STANDARD_FONT);
        
        JButton addRoot = new JButton("Add");
        addRoot.setAlignmentX(Component.CENTER_ALIGNMENT);
        addRoot.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int rval = chooser.showOpenDialog(
                    ((JComponent)e.getSource()).getTopLevelAncestor());
                if (rval == JFileChooser.APPROVE_OPTION) {
                    rootModel.addElement(chooser.getSelectedFile().getAbsolutePath());
                    refreshFileListPreview();
                }
            }
        });
        JButton addCustomRoot = new JButton("Add Custom");
        addCustomRoot.setAlignmentX(Component.CENTER_ALIGNMENT);
        addCustomRoot.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String path = JOptionPane.showInputDialog(
                    ((JComponent)e.getSource()).getTopLevelAncestor(),
                    "Enter a path:");
                if (path != null) {
                    rootModel.addElement(path);
                    refreshFileListPreview();
                }
            }
        });
        JButton removeRoot = new JButton("Remove");
        removeRoot.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeRoot.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (rootList.getSelectedValue() != null) {
                    rootModel.remove(rootList.getSelectedIndex());
                    refreshFileListPreview();
                }
            }
        });
        JPanel rootButtonPanel = new JPanel();
        rootButtonPanel.setLayout(new BoxLayout(rootButtonPanel, BoxLayout.PAGE_AXIS));
        rootButtonPanel.add(addRoot);
        rootButtonPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        rootButtonPanel.add(addCustomRoot);
        rootButtonPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        rootButtonPanel.add(removeRoot);
        rootButtonPanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));

        JPanel rootPanel = new JPanel();
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        rootPanel.setLayout(new BorderLayout());
        rootPanel.add(new JLabel("Root folders:"), BorderLayout.NORTH);
        rootPanel.add(new JScrollPane(rootList), BorderLayout.CENTER);
        rootPanel.add(rootButtonPanel, BorderLayout.EAST);

        return rootPanel;
    }

    public JPanel buildFilterPanel(String label, DefaultListModel model, 
            java.util.List<String> filters) {

        for (String s : filters) {
            model.addElement(s);
        }
        JList filterList = new JList(model);
        filterList.setFont(STANDARD_FONT);
        
        JButton addFilter = new JButton("Add");
        addFilter.setAlignmentX(Component.CENTER_ALIGNMENT);
        addFilter.addActionListener(new FilterButtonListener(
                    FilterButtonListener.ADD, filterList, model, this));
        JButton editFilter = new JButton("Edit");
        editFilter.setAlignmentX(Component.CENTER_ALIGNMENT);
        editFilter.addActionListener(new FilterButtonListener(
                    FilterButtonListener.EDIT, filterList, model, this));
        JButton removeFilter = new JButton("Remove");
        removeFilter.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeFilter.addActionListener(new FilterButtonListener(
                    FilterButtonListener.REMOVE, filterList, model, this));
        JPanel filterButtons = new JPanel();
        filterButtons.setLayout(new BoxLayout(
                    filterButtons, BoxLayout.PAGE_AXIS));
        filterButtons.add(addFilter);
        filterButtons.add(Box.createRigidArea(new Dimension(0, 2)));
        filterButtons.add(editFilter);
        filterButtons.add(Box.createRigidArea(new Dimension(0, 2)));
        filterButtons.add(removeFilter);
        filterButtons.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));

        JPanel filterPanel = new JPanel();
        filterPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        filterPanel.setLayout(new BorderLayout());
        filterPanel.add(new JLabel(label), BorderLayout.NORTH);
        filterPanel.add(new JScrollPane(filterList), BorderLayout.CENTER);
        filterPanel.add(filterButtons, BorderLayout.EAST);

        return filterPanel;
    }

    public JPanel buildMainFilterPanel() {

        includeFileFilterModel = new DefaultListModel();
        excludeFileFilterModel = new DefaultListModel();
        includeFolderFilterModel = new DefaultListModel();
        excludeFolderFilterModel = new DefaultListModel();

        JPanel mainFilterPanel = new JPanel();
        mainFilterPanel.setLayout(new GridLayout(2,2));
        mainFilterPanel.add(buildFilterPanel("Include file filters:", 
                    includeFileFilterModel, project.getIncludeFileFilters()));
        mainFilterPanel.add(buildFilterPanel("Include folder filters:", 
                    includeFolderFilterModel, project.getIncludeFolderFilters()));
        mainFilterPanel.add(buildFilterPanel("Exclude file filters:", 
                    excludeFileFilterModel, project.getExcludeFileFilters()));
        mainFilterPanel.add(buildFilterPanel("Exclude folder filters:", 
                    excludeFolderFilterModel, project.getExcludeFolderFilters()));
        return mainFilterPanel;
    }

    public void saveProject() {
        int i;

        if (!projectFile.delete()) {
            // ignore failure to delete
        }

        File newFile = new File("projects" + File.separator + nameField.getText() + ".prj");
        try {
            if (newFile.createNewFile()) {
                projectFile = newFile;
                project.setName(nameField.getText());
            } else {
                JOptionPane.showMessageDialog(this, 
                       "ERROR: Invalid or pre-existing project name. Reverting back to old name.", 
                       "Edit Project", JOptionPane.ERROR_MESSAGE);
                project.setName(oldName);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, 
                   "ERROR: Cannot create new project file. Reverting back to old name and file.", 
                   "Edit Project", JOptionPane.ERROR_MESSAGE);
            project.setName(oldName);
        }

        java.util.List<String> roots = project.getRoots();
        roots.clear();
        for (i=0; i<rootModel.size(); i++) {
            Object o = rootModel.elementAt(i);
            roots.add(o.toString());
        }
        try {
            project.setPollInterval(Integer.parseInt(pollIntervalField.getText()));
        } catch (NumberFormatException ex) { }

        java.util.List<String> includeFileFilters = project.getIncludeFileFilters();
        includeFileFilters.clear();
        for (i=0; i<includeFileFilterModel.size(); i++) {
            Object o = includeFileFilterModel.elementAt(i);
            includeFileFilters.add(o.toString());
        }

        java.util.List<String> includeFolderFilters = project.getIncludeFolderFilters();
        includeFolderFilters.clear();
        for (i=0; i<includeFolderFilterModel.size(); i++) {
            Object o = includeFolderFilterModel.elementAt(i);
            includeFolderFilters.add(o.toString());
        }

        java.util.List<String> excludeFileFilters = project.getExcludeFileFilters();
        excludeFileFilters.clear();
        for (i=0; i<excludeFileFilterModel.size(); i++) {
            Object o = excludeFileFilterModel.elementAt(i);
            excludeFileFilters.add(o.toString());
        }

        java.util.List<String> excludeFolderFilters = project.getExcludeFolderFilters();
        excludeFolderFilters.clear();
        for (i=0; i<excludeFolderFilterModel.size(); i++) {
            Object o = excludeFolderFilterModel.elementAt(i);
            excludeFolderFilters.add(o.toString());
        }
        Project.saveToFile(projectFile, project);
    }

    public java.util.List<String> modelToStringList(DefaultListModel model) {
        java.util.List<String> list = new ArrayList<String>();
        int i;
        for (i=0; i<model.size(); i++) {
            list.add(model.elementAt(i).toString());
        }
        return list;
    }

    public void refreshFileListPreview() {
        roots = modelToStringList(rootModel);
        includeFileFilters = modelToStringList(includeFileFilterModel);
        excludeFileFilters = modelToStringList(excludeFileFilterModel);
        includeFolderFilters = modelToStringList(includeFolderFilterModel);
        excludeFolderFilters = modelToStringList(excludeFolderFilterModel);
        files = new ArrayList<String>();
        int i;
        File path;
        for (String root : roots) {
            path = new File(root);
            scanFile(path, path);
        }
        filePreviewLabel.setText(files.size() + " file(s) included in project:");
        filePreviewList.setListData(files.toArray());
    }

    private void scanFile(File path, File root) {
        if (!path.exists()) {
            return;
        }
        if (path != root) {
            if (path.isDirectory()) {
                if (!FileWatcher.includeString(path.getName(), 
                        includeFolderFilters, excludeFolderFilters)) {
                    return;
                }
            } else {
                if (!FileWatcher.includeString(path.getName(), 
                        includeFileFilters, excludeFileFilters)) {
                    return;
                }
            }
        }
        if (!path.isDirectory()) {
            files.add(path.getAbsolutePath());
        }
        int i;
        File[] files = path.listFiles();
        if (files != null) {
            for (i=0; i<files.length; i++) {
                scanFile(files[i], root);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            saveProject();
            dispose();
        } else if (e.getSource() == cancelButton) {
            dispose();
        }
    }

}

