import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class Session extends TimerTask implements WindowListener {

    private Date startTime;
    private Project project;    // never used?
    private boolean active;

    private EventLog log;
    private java.util.List<FileWatcher> fileWatchers;

    private JFrame sessionDialog;

    public Session(Project project) {
        this.startTime = new Date();
        this.project = project;
        this.active = false;

        // create logfile with name based on start time
        DateFormat dfm = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
        String logTag = "logs" + File.separator + project.getName() + "-" + dfm.format(startTime);
        log = new EventLog(project, new File(logTag + ".log"), new File(logTag));

        // initialize file watchers
        fileWatchers = new ArrayList<FileWatcher>();
        for (String s : project.getRoots()) {
            FileWatcher fw = new FileWatcher(s, log, project.getPollInterval());
            for (String f : project.getIncludeFileFilters()) {
                fw.addIncludeFileFilter(f);
            }
            for (String f : project.getExcludeFileFilters()) {
                fw.addExcludeFileFilter(f);
            }
            for (String f : project.getIncludeFolderFilters()) {
                fw.addIncludeFolderFilter(f);
            }
            for (String f : project.getExcludeFolderFilters()) {
                fw.addExcludeFolderFilter(f);
            }
            fileWatchers.add(fw);
        }

        // build graphical interface
        EventLogViewer viewer = new EventLogViewer(log);
        CommentWatcher cwatch = new CommentWatcher(log);
        JSplitPane mainPanel = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, true, viewer, cwatch);
        mainPanel.setDividerLocation(550);
        dfm = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        sessionDialog = new JFrame("Recording Session: " + project.getName() + 
                " - " + dfm.format(startTime));
        viewer.setParent(sessionDialog);
        viewer.setExitButtonLabel("End Session");
        sessionDialog.getContentPane().add(mainPanel);
        sessionDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        sessionDialog.addWindowListener(this);
        sessionDialog.setSize(900,800);
        sessionDialog.setLocationRelativeTo(null);
    }

    public Project getProject() {
        return project;
    }

    public void start() {
        sessionDialog.setVisible(true);
        sessionDialog.invalidate();
        sessionDialog.repaint();
        (new java.util.Timer()).schedule(this, 100);
    }

    public void run() {
        if (!active) {
            Event evt = log.createEvent(startTime, "status");
            evt.setProperty("details", "Started session");
            log.addEvent(evt);
            for (FileWatcher fw : fileWatchers) {
                fw.start();
            }
            active = true;
        }
    }

    public void stop() {
        if (active) {
            active = false;
            for (FileWatcher fw : fileWatchers) {
                fw.cancel();
            }
            Event evt = log.createEvent("status");
            evt.setProperty("details", "Stopped session");
            log.addEvent(evt);

            String desc = JOptionPane.showInputDialog(sessionDialog,
                    "Enter a brief summary of this session's activities (optional):");
            if (desc != null && desc.trim().length() > 0) {
                Event devt = log.createEvent("summary");
                devt.setProperty("details", desc);
                devt.addTag("comment");
                log.addEvent(devt);
            }

            log.close();
            sessionDialog.dispose();
        }
    }

    public boolean isActive() {
        return active;
    }

    public void windowClosing(WindowEvent e) {
        stop();
    }

    public void windowClosed(WindowEvent e) {
        stop();
    }

    public void windowActivated(WindowEvent e) { }
    public void windowDeactivated(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) { }
    public void windowIconified(WindowEvent e) { }
    public void windowOpened(WindowEvent e) { }

}

