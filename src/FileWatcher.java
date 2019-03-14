import difflib.*;
import java.io.*;
import java.util.*;

public class FileWatcher extends TimerTask {

    private Map<File,Date> lastModified;
    private Map<File,List<String>> lastContents;
    private Map<File,Boolean> savedOriginal;
    private Timer timer;
    private int pollInterval;
    private File root;
    private EventLog eventLog;
    private List<String> includeFileFilters;
    private List<String> excludeFileFilters;
    private List<String> includeFolderFilters;
    private List<String> excludeFolderFilters;
    private boolean scanning;

    public FileWatcher(String path, EventLog log) {
        this(new File(path), log, 1000);
    }

    public FileWatcher(String path, EventLog log, int interval) {
        this(new File(path), log, interval);
    }

    public FileWatcher(File path, EventLog log) {
        this(path, log, 1000);
    }

    public FileWatcher(File path, EventLog log, int interval) {
        root = path;
        eventLog = log;
        pollInterval = interval;
        lastModified = new HashMap<File,Date>();
        lastContents = new HashMap<File,List<String>>();
        savedOriginal = new HashMap<File,Boolean>();
        includeFileFilters = new ArrayList<String>();
        excludeFileFilters = new ArrayList<String>();
        includeFolderFilters = new ArrayList<String>();
        excludeFolderFilters = new ArrayList<String>();
        scanning = false;
    }

    public void addIncludeFileFilter(String filter) {
        includeFileFilters.add(filter);
    }

    public void addExcludeFileFilter(String filter) {
        excludeFileFilters.add(filter);
    }

    public void addIncludeFolderFilter(String filter) {
        includeFolderFilters.add(filter);
    }

    public void addExcludeFolderFilter(String filter) {
        excludeFolderFilters.add(filter);
    }
    
    private List<String> readFile(File path) {
        List<String> lines = new ArrayList<String>();
        String line = "";
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            while ((line = in.readLine()) != null) {
                lines.add(line);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private String mergeLines(List<String> lines) {
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        for (String s : lines) {
            if (!first) {
                buffer.append("\n");
            } else {
                first = false;
            }
            buffer.append(s);
        }
        return buffer.toString();
    }

    public void start() {
        if (root != null) {
            scan(true);
            if (timer == null) {
                timer = new Timer();
            } else {
                timer.cancel();
            }
            timer.schedule(this, pollInterval, pollInterval);
        }
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }
    
    public void run() {
        if (!scanning) {
            //timer.cancel();
            scan(false);
            //timer.schedule(this, pollInterval);
        }
    }

    public static boolean includeString(String str, List<String> included, 
            List<String>excluded) {
        boolean include = true;
        if (included.size() > 0) {
            include = false;
            for (String f : included) {
                if (str.matches(f)) {
                    include = true;
                    break;
                }
            }
        }
        if (include) {
            for (String f : excluded) {
                if (str.matches(f)) {
                    include = false;
                }
            }
        }
        return include;
    }

    private void rscan(File path, boolean initialize) {
        /*
         *if (initialize) {
         *   System.out.println("scanning " + path.getAbsolutePath() + "  [" + path.getName() + "]");
         *}
         */
        /*
         *if (path.isHidden() || path.getName().startsWith(".")) {    // ignore dot and hidden files
         *    if (initialize) {
         *        System.out.println("hidden: " + path.getName());
         *    }
         *    return;
         *}
         */
        if (!path.exists()) {
            return;
        }
        if (path != root) {
            if (path.isDirectory()) {
                if (!includeString(path.getName(), includeFolderFilters, 
                        excludeFolderFilters)) {
                    return;
                }
            } else {
                if (!includeString(path.getName(), includeFileFilters, 
                        excludeFileFilters)) {
                    return;
                }
            }
        }
        /*
         *if (initialize) {
         *    if (path.isDirectory()) {
         *        System.out.println("ENTER FOLDER: " + path.getAbsolutePath());
         *    } else {
         *        System.out.println("SCANNING: " + path.getAbsolutePath());
         *    }
         *}
         */
        Date lm = new Date(path.lastModified());
        if (initialize) {
            lastModified.put(path, lm);
            if (!path.isDirectory()) {
                lastContents.put(path, readFile(path));
                savedOriginal.put(path, Boolean.FALSE);
            }
        } else {
            if (!lastModified.containsKey(path)) {
                lastModified.put(path, lm);
                Event evt = eventLog.createEvent(lm, "create");
                evt.setProperty("path", 
                        eventLog.getRelativePath(path.getAbsolutePath()));
                if (!path.isDirectory()) {
                    List<String> current = readFile(path);
                    lastContents.put(path, current);
                    evt.setAttachment("snapshot", 
                            eventLog.createAttachment(mergeLines(current)));
                }
                eventLog.addEvent(evt);
            } else if (!path.isDirectory()) {   // ignore directory modifications
                if (lm.after(lastModified.get(path))) {
                    List<String> previous = lastContents.get(path);
                    List<String> current = readFile(path);
                    Patch patch = DiffUtils.diff(previous, current);
                    Event evt = eventLog.createEvent(lm, "modify");
                    evt.setProperty("path", 
                            eventLog.getRelativePath(path.getAbsolutePath()));
                    // only add original if this is the first "modify"
                    // event for this file in this session
                    if (!savedOriginal.containsKey(path) || 
                            !savedOriginal.get(path).booleanValue()) {
                        evt.setAttachment("original",
                                eventLog.createAttachment(mergeLines(previous)));
                        savedOriginal.put(path, Boolean.TRUE);
                    }
                    evt.setAttachment("snapshot",
                            eventLog.createAttachment(mergeLines(current)));
                    try {
                        List<String> udiff = DiffUtils.generateUnifiedDiff(
                                path.getAbsolutePath(), path.getAbsolutePath(), 
                                previous, patch, 3);
                        evt.setAttachment("diff", 
                                eventLog.createAttachment(mergeLines(udiff)));
                    } catch (IndexOutOfBoundsException ex) {
                        StringBuffer text = new StringBuffer();
                        for (Delta d : patch.getDeltas()) {
                            text.append("<<<<<<<<<<\n");
                            text.append(d.getOriginal().toString());
                            text.append("\n<<<<<<<<<<\n");
                            text.append(">>>>>>>>>>\n");
                            text.append(d.getRevised().toString());
                            text.append("\n>>>>>>>>>>\n");
                        }
                        text.append(patch.getDeltas().size() + " total delta(s)");
                        evt.setAttachment("diff",
                                eventLog.createAttachment("Could not create unified diff:\n" +
                                    text.toString()));
                    }
                    eventLog.addEvent(evt);
                    lastModified.put(path, lm);
                    lastContents.put(path, current);
                } // else unmodified
            }
        }
        int i;
        File[] files = path.listFiles();
        if (files != null) {
            for (i=0; i<files.length; i++) {
                rscan(files[i], initialize);
            }
        }
    }

    private void scan(boolean initialize) {
        Date now = new Date();
        //System.out.println("begin scan: " + root.getAbsolutePath() + 
                //"  [" + now.toString() + "]");
        scanning = true;
        if (initialize) {
            lastModified.clear();
            lastContents.clear();
        }
        rscan(root, initialize);
        if (initialize) {
            StringBuffer files = new StringBuffer();
            int fileCount = 0;
            for (File f : lastModified.keySet()) {
                if (!f.isDirectory()) {
                    if (fileCount > 0) {
                        files.append("\n");
                    }
                    files.append(f.getAbsolutePath());
                    fileCount++;
                }
            }
            StringBuffer text = new StringBuffer();
            text.append("Initialized with ");
            text.append(lastModified.size()-fileCount);
            text.append(" folder(s) containing ");
            text.append(fileCount);
            text.append(" file(s):\n");
            text.append(files.toString());
            Event evt = eventLog.createEvent(now, "status");
            evt.setProperty("path", root.getName());
            if (text.length() < 4096) {
                evt.setProperty("details", text.toString());
            } else {
                evt.setAttachment("details", eventLog.createAttachment(text.toString()));
            }
            eventLog.addEvent(evt);
        } else {
            Set<File> toRemove = new HashSet<File>();
            for (File path : lastModified.keySet()) {
                if (!path.exists()) {
                    Event evt = eventLog.createEvent(now, "delete");
                    evt.setProperty("path", eventLog.getRelativePath(path.getAbsolutePath()));
                    eventLog.addEvent(evt);
                    toRemove.add(path);
                }
            }
            for (File path : toRemove) {
                lastModified.remove(path);
                if (lastContents.containsKey(path)) {
                    lastContents.remove(path);
                }
            }
        }
        scanning = false;
        //System.out.println("end scan\n");
    }

}

