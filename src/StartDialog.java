import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.util.*;

public class StartDialog extends JFrame 
    implements ActionListener, MouseListener, WindowListener {

    private File projectDir;
    private SortedSet projects;
    private JList projectList;
    private JButton newProjectButton;
    private JButton editProjectButton;
    private JButton deleteProjectButton;
    private JButton startSessionButton;

    private File logDir;
    private SortedSet logs;
    private JList logList;
    private JButton openLogButton;
    private JButton deleteLogButton;
    private JButton reloadLogsButton;
    private JButton exitButton;

    LogListRefresher refresher;

    public static class LogListEntry implements Comparable {
        private String root;
        private String desc;
        private File logFile;
        private File attachPath;
        public LogListEntry(File logFile) {
            this.root = logFile.getName().substring(0,
                    logFile.getName().lastIndexOf("."));
            this.logFile = logFile;
            this.attachPath = new File("logs" + File.separator + root);
            this.desc = root;
            EventLog log = new EventLog(logFile, attachPath);
            for (Event evt : log.getAllEvents()) {
                if (evt.hasTag("summary")) {
                    this.desc += " " + evt.getDetails();
                }
            }
        }
        public File getLogFile() {
            return logFile;
        }
        public File getAttachPath() {
            return attachPath;
        }
        public String getRoot() {
            return root;
        }
        public int hashCode() {
            return logFile.hashCode();
        }
        public int compareTo(Object o) {
            if (o instanceof LogListEntry) {
                return logFile.getAbsolutePath().compareTo(
                        ((LogListEntry)o).getLogFile().getAbsolutePath());
            } else {
                return 1;
            }
        }
        public boolean equals(Object o) {
            if (o instanceof LogListEntry) {
                return logFile.getAbsolutePath().equals(
                        ((LogListEntry)o).getLogFile().getAbsolutePath());
            } else {
                return false;
            }
        }
        public String toString() {
            return desc;
        }
    }

    public static class LogListRefresher extends TimerTask {
        private StartDialog dialog;
        private java.util.Timer timer;
        public LogListRefresher(StartDialog dialog) {
            this.dialog = dialog;
            timer = new java.util.Timer();
            timer.schedule(this, 5000, 5000);
        }
        public void run() {
            dialog.refreshLogList();
        }
        /*
         *public boolean cancel() {
         *    timer.cancel();
         *    return true;
         *}
         */
    }

    public StartDialog() {
        projectDir = new File("projects");
        projects = new TreeSet();
        projectList = new JList();
        projectList.addMouseListener(this);
        projectList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        refreshProjectList();

        JPanel projectButtons = new JPanel();
        startSessionButton = new JButton("Start Session");
        startSessionButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startSessionButton.addActionListener(this);
        newProjectButton = new JButton("New");
        newProjectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        newProjectButton.addActionListener(this);
        editProjectButton = new JButton("Edit");
        editProjectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        editProjectButton.addActionListener(this);
        deleteProjectButton = new JButton("Delete");
        deleteProjectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        deleteProjectButton.addActionListener(this);
        projectButtons.setLayout(new BoxLayout(projectButtons, BoxLayout.PAGE_AXIS));
        projectButtons.add(startSessionButton);
        projectButtons.add(Box.createRigidArea(new Dimension(0, 10)));
        projectButtons.add(newProjectButton);
        projectButtons.add(Box.createRigidArea(new Dimension(0, 2)));
        projectButtons.add(editProjectButton);
        projectButtons.add(Box.createRigidArea(new Dimension(0, 2)));
        projectButtons.add(deleteProjectButton);
        projectButtons.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));

        JPanel projectPanel = new JPanel();
        projectPanel.setLayout(new BorderLayout());
        projectPanel.add(new JLabel("Projects:"), BorderLayout.NORTH);
        projectPanel.add(new JScrollPane(projectList), BorderLayout.CENTER);
        projectPanel.add(projectButtons, BorderLayout.EAST);
        projectPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        logDir = new File("logs");
        logs = new TreeSet();
        logList = new JList();
        logList.addMouseListener(this);
        refreshLogList();

        JPanel logButtons = new JPanel();
        openLogButton = new JButton("View");
        openLogButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        openLogButton.addActionListener(this);
        deleteLogButton = new JButton("Delete");
        deleteLogButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        deleteLogButton.addActionListener(this);
        reloadLogsButton = new JButton("Refresh List");
        reloadLogsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        reloadLogsButton.addActionListener(this);
        exitButton = new JButton("Exit");
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.addActionListener(this);
        logButtons.setLayout(new BoxLayout(logButtons, BoxLayout.PAGE_AXIS));
        logButtons.add(openLogButton);
        logButtons.add(Box.createRigidArea(new Dimension(0, 2)));
        logButtons.add(deleteLogButton);
        //logButtons.add(Box.createRigidArea(new Dimension(0, 2)));
        //logButtons.add(reloadLogsButton);
        logButtons.add(Box.createVerticalGlue());
        logButtons.add(exitButton);
        logButtons.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));

        JPanel logPanel = new JPanel();
        logPanel.setLayout(new BorderLayout());
        logPanel.add(new JLabel("Logs:"), BorderLayout.NORTH);
        logPanel.add(new JScrollPane(logList), BorderLayout.CENTER);
        logPanel.add(logButtons, BorderLayout.EAST);
        logPanel.setBorder(BorderFactory.createEmptyBorder(5,10,10,10));
        //logPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        //JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
                //true, projectPanel, logPanel);
        //mainSplit.setDividerLocation(0.5);
        setLayout(new BorderLayout());
        //add(mainSplit, BorderLayout.CENTER);
        add(projectPanel, BorderLayout.NORTH);
        add(logPanel, BorderLayout.CENTER);
        addWindowListener(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle(App.APP_NAME + " " + App.APP_VERSION);
        setSize(600,500);
        setLocationRelativeTo(null);

        refresher = new LogListRefresher(this);
    }

    public void refreshProjectList() {
        int i;
        String fname;
        File[] projectFiles = 
            projectDir.listFiles(SimpleFileFilter.getProjectFilter());
        Arrays.sort(projectFiles);
        projects.clear();
        for (i=0; i<projectFiles.length; i++) {
            fname = projectFiles[i].getName();
            fname = fname.substring(0,fname.lastIndexOf("."));
            projects.add(fname);
        }
        projectList.setListData(projects.toArray());
    }


    public void refreshLogList() {
        int i, j;
        String fname;
        Object[] oldSelection = logList.getSelectedValues();
        java.util.List<Integer> newSelection = new ArrayList<Integer>();
        File[] logFiles = 
            logDir.listFiles(SimpleFileFilter.getLogFilter());
        Arrays.sort(logFiles);
        logs.clear();
        for (i=0; i<logFiles.length; i++) {
            LogListEntry lle = new LogListEntry(logFiles[i]);
            logs.add(lle);
            for (j=0; j<oldSelection.length; j++) {
               if (lle.equals(oldSelection[j])) {
                   newSelection.add(Integer.valueOf(i));
               }
            }
        }
        logList.setListData(logs.toArray());
        int[] newIdx = new int[newSelection.size()];
        i=0;
        for (Integer idx : newSelection) {
           newIdx[i++] = idx;
        }
        logList.setSelectedIndices(newIdx);
    }

    public void run() {
        refreshLogList();
    }

    public void newProject() {
        String name = JOptionPane.showInputDialog(this,
                "Enter a name for the new project:", "New Project");
        if (name == null) {
            return;
        }
        File projectFile = new File("projects" + File.separator + name + ".prj");
        try {
            if (projectFile.createNewFile()) {
                Project.saveToFile(projectFile, new Project(name));
                ProjectEditor pedit = new ProjectEditor(this, projectFile);
                pedit.setVisible(true);
                refreshProjectList();
            } else {
                JOptionPane.showMessageDialog(this, 
                       "ERROR: Invalid or pre-existing project name.", "New Project",
                       JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, 
                   "ERROR: Cannot create project file.", "New Project",
                   JOptionPane.ERROR_MESSAGE);
        }
    }

    public void editProject() {
        if (projectList.getSelectedValue() != null) {
            ProjectEditor pedit = new ProjectEditor(this, new File("projects" + 
                    File.separator + projectList.getSelectedValue().toString() + ".prj"));
            pedit.setVisible(true);
            refreshProjectList();
        }
    }

    public void deleteProject() {
        if (projectList.getSelectedValue() != null) {
            String name = projectList.getSelectedValue().toString();
            int rval = JOptionPane.showConfirmDialog(this, 
                    "Are you sure you want to delete the \"" + name + "\" project?",
                    "Delete Project", JOptionPane.YES_NO_OPTION);
            if (rval == JOptionPane.YES_OPTION) {
                File projectFile = new File("projects" + File.separator + name + ".prj");
                if (!projectFile.delete()) {
                    JOptionPane.showMessageDialog(this, 
                            "ERROR: Could not delete project file!",
                            "Delete Project", JOptionPane.ERROR_MESSAGE);
                }
                refreshProjectList();
            }
        }
    }

    public void startSession() {
        if (projectList.getSelectedValue() != null) {
            App.recordSession("projects" + File.separator + 
                    projectList.getSelectedValue().toString() + ".prj");
        }
    }

    public void openLog() {
        int i;
        if (logList.getSelectedValue() != null) {
            Object[] logs = logList.getSelectedValues();
            for (i=0; i<logs.length; i++) {
                LogListEntry lle = (LogListEntry)logs[i];
                App.viewLog(lle.getLogFile().getAbsolutePath(),
                        lle.getAttachPath().getAbsolutePath());
            }
        }
    }

    public void deleteLog() {
        int i, j;
        boolean confirmed = false;
        String name;
        if (logList.getSelectedValue() != null) {
            Object[] logs = logList.getSelectedValues();
            if (logs != null && logs.length == 1) {
                name = logs[0].toString();
                int rval = JOptionPane.showConfirmDialog(this, 
                        "Are you sure you want to delete the \"" + name + "\" log?",
                        "Delete Log", JOptionPane.YES_NO_OPTION);
                if (rval == JOptionPane.YES_OPTION) {
                    confirmed = true;
                }
            } else if (logs != null && logs.length > 1) {
                int rval = JOptionPane.showConfirmDialog(this, 
                        "Are you sure you want to delete the " + logs.length + " selected logs?",
                        "Delete Log", JOptionPane.YES_NO_OPTION);
                if (rval == JOptionPane.YES_OPTION) {
                    confirmed = true;
                }
            }
            if (confirmed) {
                for (i=0; i<logs.length; i++) {
                    File logFile = ((LogListEntry)logs[i]).getLogFile();
                    File logDir = ((LogListEntry)logs[i]).getAttachPath();
                    File[] attachments = logDir.listFiles();
                    boolean deleteSucceeded = true;
                    for (j=0; j<attachments.length; j++) {
                        deleteSucceeded = deleteSucceeded && attachments[j].delete();
                    }
                    deleteSucceeded = deleteSucceeded && logFile.delete();
                    deleteSucceeded = deleteSucceeded && logDir.delete();
                    if (!deleteSucceeded) {
                        JOptionPane.showMessageDialog(this, 
                                "ERROR: Could not delete log files!",
                                "Delete Log", JOptionPane.ERROR_MESSAGE);
                    }
                }
                refreshLogList();
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == newProjectButton) {
            newProject();
        } else if (e.getSource() == editProjectButton) {
            editProject();
        } else if (e.getSource() == deleteProjectButton) {
            deleteProject();
        } else if (e.getSource() == startSessionButton) {
            startSession();
        } else if (e.getSource() == openLogButton) {
            openLog();
        } else if (e.getSource() == deleteLogButton) {
            deleteLog();
        } else if (e.getSource() == reloadLogsButton) {
            refreshLogList();
        } else if (e.getSource() == exitButton) {
            refresher.cancel();
            dispose();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            if (e.getSource() == projectList) {
                startSession();
            } else if (e.getSource() == logList) {
                openLog();
            }
        }
    }

    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }

    public void windowClosing(WindowEvent e) {
        refresher.cancel();
    }

    public void windowClosed(WindowEvent e) {
        refresher.cancel();
        System.exit(0);
    }

    public void windowActivated(WindowEvent e) { }
    public void windowDeactivated(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) { }
    public void windowIconified(WindowEvent e) { }
    public void windowOpened(WindowEvent e) { }
}

