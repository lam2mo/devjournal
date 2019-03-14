import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;

public class App {

    public static final String APP_NAME = "DevJournal";
    public static final String APP_VERSION = "1.0.0";

    public static void recordSession(String projectPath) {
        Project p = Project.readFromFile(new File(projectPath));
        Session s = new Session(p);
        s.start();
    }

    public static void viewLog(String logPath, String logAttachDir) {
        EventLog log = new EventLog(new File(logPath), new File(logAttachDir));
        EventLogViewer viewer = new EventLogViewer(log);
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(viewer, BorderLayout.CENTER);
        if (log.getProject() != null) {
            JFrame viewerDialog = new JFrame(log.getProject().getName());
            viewer.setParent(viewerDialog);
            viewer.setExitButtonLabel("Close Log");
            viewerDialog.getContentPane().add(mainPanel);
            viewerDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            viewerDialog.setSize(900,700);
            viewerDialog.setVisible(true);
            viewerDialog.setLocationRelativeTo(null);
        } else {
            System.out.println("Invalid log file: " + logPath);
        }
    }

    public static void verifyProjectAndLogDirs() {
        File projectDir = new File("projects");
        File logDir = new File("logs");
        if (!projectDir.exists()) {
            if (!projectDir.mkdirs()) {
                JOptionPane.showMessageDialog(null, 
                        "ERROR: Could not create projects subdirectory!",
                        APP_NAME, JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }
        } else if (!projectDir.isDirectory()) {
            JOptionPane.showMessageDialog(null, 
                    "ERROR: Could not create projects subdirectory; " + 
                    "there is already a file with the same name!",
                    APP_NAME, JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                JOptionPane.showMessageDialog(null, 
                        "ERROR: Could not create logs subdirectory!",
                        APP_NAME, JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }
        } else if (!logDir.isDirectory()) {
            JOptionPane.showMessageDialog(null, 
                    "ERROR: Could not create logs subdirectory; " + 
                    "there is already a file with the same name!",
                    APP_NAME, JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        verifyProjectAndLogDirs();
        if (args.length == 1) {
            if (args[0].equals("-h")) {
                System.out.println("usage: watcher");
                System.out.println("         or");
                System.out.println("       watcher <project-file>");
                System.out.println("         or");
                System.out.println("       watcher <log-file> <log-attach-dir>");
            } else if (SimpleFileFilter.getProjectFilter().accept(args[0])) {
                recordSession(args[0]);
            } else if (SimpleFileFilter.getLogFilter().accept(args[0])) {
                viewLog(args[0], args[0].substring(0,args[0].length()-4));
            }
        } else if (args.length == 2) {
            viewLog(args[0], args[1]);
        } else {
            (new StartDialog()).setVisible(true);
        }
    }

}

