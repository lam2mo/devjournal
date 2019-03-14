import difflib.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.text.*;
import javax.swing.*;

public class EventLog {

    private Project project;
    private long nextEventID;
    private long nextAttachID;
    private List<Event> events;

    private File logFile;
    private File logAttachDir;
    private List<EventLogListener> listeners;

    public EventLog(File logFile, File logAttachDir) {
        this(null, logFile, logAttachDir);
    }

    public EventLog(Project project, File logFile, File logAttachDir) {
        this.project = project;
        this.nextEventID = 1;
        this.nextAttachID = 1;
        this.events = new ArrayList<Event>();
        this.logFile = logFile;
        this.logAttachDir = logAttachDir;
        this.listeners = new ArrayList<EventLogListener>();
        if (logFile.exists()) {
            loadFromDisk();
        }
        if (!logAttachDir.exists()) {
            if (!logAttachDir.mkdirs()) {
                JOptionPane.showMessageDialog(null, 
                        "ERROR: Could not create log attachment subdirectory!",
                        project.getName(), JOptionPane.ERROR_MESSAGE);
                this.logAttachDir = null;
            }
        } else if (!logAttachDir.isDirectory()) {
            JOptionPane.showMessageDialog(null, 
                    "ERROR: Could not create log attachment subdirectory; " +
                    "another file already exists with the same name!",
                    project.getName(), JOptionPane.ERROR_MESSAGE);
            this.logAttachDir = null;
        }
    }

    public Project getProject() {
        return project;
    }

    public List<Event> getAllEvents() {
        return events;
    }

    public String getLogAttachDir() {
        return logAttachDir.getAbsolutePath();
    }

    public Event createEvent() {
        return createEvent(new Date());
    }

    public Event createEvent(Date timestamp) {
        Event evt = new Event(nextEventID, timestamp, this);
        nextEventID++;
        return evt;
    }

    public Event createEvent(String tag) {
        return createEvent(new Date(), tag);
    }

    public Event createEvent(Date timestamp, String tag) {
        Event evt = new Event(nextEventID, timestamp, this, tag);
        nextEventID++;
        return evt;
    }

    public Attachment createAttachment(String text) {
        Attachment att = new Attachment(nextAttachID, text, this);
        nextAttachID++;
        return att;
    }

    public Attachment createAttachment(File path) {
        Attachment att = new Attachment(nextAttachID, path, this);
        nextAttachID++;
        return att;
    }

    public void addEvent(Event evt) {
        int i;
        boolean added = false;
        for (i=0; !added && i<events.size(); i++) {
            if (evt.compareTo(events.get(i)) < 0) {
                events.add(i, evt);
                added = true;
                notifyReloadListeners();
            }
        }
        if (!added) {
            events.add(evt);
            notifyAddListeners(evt);
        }
        saveToDisk();
    }

    public void removeEvent(Event evt) {
        events.remove(evt);
        notifyRemoveListeners(evt);
    }

    public void mergeEvents(List<Event> eventsToMerge) {
        // special handling to build the new aggregate event 
        // and remove the old ones
        int i;

        if (eventsToMerge.size() < 2) {
            return;
        }
        Collections.sort(eventsToMerge);

        // set up merged "modify" event hash
        Map<String,Event> originalModify = new HashMap<String,Event>();
        Map<String,Event> currentModify = new HashMap<String,Event>();

        // aggregate's event's attributes
        Date newTime = eventsToMerge.get(eventsToMerge.size()-1).getTimestamp();
        StringBuffer newDetails = new StringBuffer();
        String details, path = null, temp;
        Event evt; Attachment att;
        SortedSet<String> allTags = new TreeSet<String>();

        for (i=0; i<eventsToMerge.size(); i++) {
            evt = eventsToMerge.get(i);
            //System.out.println("merging: " + evt.getID());

            if (evt.hasTag("modify") && !evt.hasTag("aggregate") &&
                    evt.hasProperty("path") && evt.hasAttachment("snapshot")) {
                temp = evt.getProperty("path");
                if (!originalModify.containsKey(temp)) {
                    originalModify.put(temp, evt);
                }
                currentModify.put(temp, evt);
            } else {
                details = evt.getDetails();
                if (details == null) {
                    att = evt.getAttachment("diff");
                    if (att != null) {
                        details = att.getTextData();
                    } else {
                        att = evt.getAttachment("snapshot");
                        if (att != null) {
                            details = att.getTextData();
                        }
                    }
                }
                if (details != null) {
                    if (i > 0) {
                        newDetails.append("\n\n");
                    }
                    newDetails.append(details);
                }
            }

            allTags.addAll(evt.getTags());
            if (i==0) {
                if (evt.hasProperty("path")) {
                    path = evt.getProperty("path");
                }
            } else if (path != null && evt.hasProperty("path") && 
                    !evt.getProperty("path").equals(path)) {
                path = null;
            }
        }

        // create merged modification diffs
        for (Map.Entry<String,Event> entry : originalModify.entrySet()) {
            String p = entry.getKey();
            Event orig = entry.getValue();
            String original = orig.getAttachment("snapshot").getTextData();
            if (orig.hasAttachment("original")) {
                // diff from original version if available
                original = orig.getAttachment("original").getTextData();
            }
            String current = currentModify.get(p).getAttachment("snapshot").getTextData();
            java.util.List<String> origList = Arrays.asList(original.split("\n"));
            java.util.List<String> currList = Arrays.asList(current.split("\n"));
            Patch patch = DiffUtils.diff(origList, currList);
            java.util.List<String> udiff = DiffUtils.generateUnifiedDiff(p, p, origList, patch, 3);
            if (newDetails.length() > 0) {
                newDetails.append("\n\n");
            }
            if (!orig.hasAttachment("original") && orig.hasAttachment("diff")) {
                // if we don't have the original, add the first diff
                newDetails.append(orig.getAttachment("diff").getTextData());
                newDetails.append("\n\n");
            }
            boolean first = true;
            for (String s : udiff) {
                if (!first) {
                    newDetails.append("\n");
                } else {
                    first = false;
                }
                newDetails.append(s);
            }
        }

        // create aggregate event
        Event newEvent = createEvent(newTime, "aggregate");
        for (String t : allTags) {
            newEvent.addTag(t);
        }
        if (path != null) {
            newEvent.setProperty("path", path);
        }
        newEvent.setAttachment("details", 
                createAttachment(newDetails.toString()));
        for (i=0; i<eventsToMerge.size(); i++) {
            newEvent.addSubEvent(eventsToMerge.get(i));
            removeEvent(eventsToMerge.get(i));
        }
        addEvent(newEvent);
    }

    public void splitEvent(Event eventToSplit) {
        if (eventToSplit.hasTag("aggregate")) {
            removeEvent(eventToSplit);
            for (Event evt : eventToSplit.getSubEvents()) {
                addEvent(evt);
            }
        }
    }

    public String getRelativePath(String fullPath) {
        // TODO: search project roots and find the parent of this pathname,
        // then strip it
        return fullPath;
    }

    public String getAbsolutePath(String fileName) {
        // TODO: search project roots and build the full path
        return fileName;
    }

    public Attachment getLastSnapshot(String path) {
        // TODO: search the events to find snapshot for this path, then return
        // the latest one
        return null;
    }

    public void loadFromDisk() {
        Event evt;
        int num, i;
        try {
            ObjectInputStream in = new ObjectInputStream(
                    new BufferedInputStream(
                    new GZIPInputStream(
                    new FileInputStream(logFile))));
            project = (Project)in.readObject();
            nextEventID = in.readLong();
            nextAttachID = in.readLong();
            num = in.readInt();
            for (i=0; i<num; i++) {
                evt = (Event)in.readObject();
                evt.setEventLog(this);
                events.add(evt);
            }
            in.close();
        } catch (IOException e) { }
          catch (ClassNotFoundException e) { }
    }

    public void saveToDisk() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(
                    new GZIPOutputStream(
                    new FileOutputStream(logFile))));
            out.writeObject(project);
            out.writeLong(nextEventID);
            out.writeLong(nextAttachID);
            out.writeInt(events.size());
            for (Event e : events) {
                out.writeObject(e);
            }
            out.close();
        } catch (IOException e) { }
    }

    public void addEventLogListener(EventLogListener ell) {
        listeners.add(ell);
    }

    public void notifyAddListeners(Event evt) {
        for (EventLogListener l : listeners) {
            l.eventAdded(evt);
        }
    }

    public void notifyRemoveListeners(Event evt) {
        for (EventLogListener l : listeners) {
            l.eventRemoved(evt);
        }
    }

    public void notifyReloadListeners() {
        for (EventLogListener l : listeners) {
            l.reloadEvents();
        }
    }

    public void close() {
        saveToDisk();
    }

}

