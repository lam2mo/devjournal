import java.io.*;
import java.text.*;
import java.util.*;

public class Event implements Comparable, Serializable {

    static final long serialVersionUID = 3748688294296L;

    private long eventID;           // unique identifier
    private Date timestamp;         // time of event (used for ordering)

    private Set<String> tags;                       // boolean tags
    private Map<String, String> properties;         // O(1) data items
    private Map<String, Attachment> attachments;    // O(n) data items (stored in files)
    private List<Event> subEvents;                  // merged events

    private EventLog log;
    private Event parent;

    public Event(long id, Date timestamp, EventLog log) {
        this.eventID = id;
        this.timestamp = (Date)timestamp.clone();
        this.tags = new HashSet<String>();
        this.properties = new HashMap<String, String>();
        this.attachments = new HashMap<String, Attachment>();
        this.subEvents = new ArrayList<Event>();
        this.log = log;
        this.parent = null;
    }

    public Event(long id, Date timestamp, EventLog log, String tag) {
        this(id, timestamp, log);
        addTag(tag);
    }
   
    // {{{ data accessors

    public long getID() {
        return eventID;
    }

    public Date getTimestamp() {
        return (Date)timestamp.clone();
    }

    public void addTag(String tag) {
        if (!hasTag(tag)) {
            tags.add(tag);
        }
    }

    public Set<String> getTags() {
        return tags;
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public void setAttachment(String key, Attachment value) {
        attachments.put(key, value);
    }

    public boolean hasAttachment(String key) {
        return attachments.containsKey(key);
    }

    public Attachment getAttachment(String key) {
        return attachments.get(key);
    }

    public int getAttachmentCount() {
        return attachments.size();
    }

    public String getFormattedTimestamp() {
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(timestamp);
    }

    public String getDetailPreview() {
        String details = getDetails(), desc = "";
        if (details != null) {
            int pos = details.indexOf('\n');
            if (pos >=0) {
                desc += "  " + details.substring(0,pos) + " ...";
            } else if (details.length() < 100) {
                desc += "  " + details;
            }
        }
        return desc;
    }

    public String getDetails() {
        String details = null;
        if (hasProperty("details")) {
            details = getProperty("details");
        } else if (hasAttachment("details")) {
            details = getAttachment("details").getTextData();
        }
        return details;
    }

    public boolean isAggregate() {
        return hasTag("aggregate");
    }

    public void addSubEvent(Event evt) {
        subEvents.add(evt);
    }

    public List<Event> getSubEvents() {
        return subEvents;
    }

    public Event getSubEvent(int i) {
        return subEvents.get(i);
    }

    public void setEventLog(EventLog log) {
        this.log = log;
        for (Attachment att : attachments.values()) {
            att.setEventLog(log);
        }
        for (Event e : subEvents) {
            e.setEventLog(log);
        }
    }

    public EventLog getEventLog() {
        return log;
    }

    public void setParent(Event parent) {
        this.parent = parent;
    }

    public Event getParent() {
        return parent;
    }

    // }}}

    public int hashCode() {
        return timestamp.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof Event) {
            return timestamp.equals(((Event)o).getTimestamp());
        } else {
            throw new ClassCastException();
        }
    }

    public int compareTo(Object o) {
        if (o instanceof Event) {
            return timestamp.compareTo(((Event)o).getTimestamp());
        } else {
            throw new ClassCastException();
        }
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        out.writeLong(eventID);
        out.writeObject(timestamp);
        out.writeInt(tags.size());
        for (String t : tags) {
            out.writeUTF(t);
        }
        out.writeInt(properties.size());
        for (String key : properties.keySet()) {
            out.writeUTF(key);
            out.writeUTF(properties.get(key));
        }
        out.writeInt(attachments.size());
        for (String key : attachments.keySet()) {
            out.writeUTF(key);
            out.writeObject(attachments.get(key));
        }
        out.writeInt(subEvents.size());
        for (Event e : subEvents) {
            out.writeObject(e);
        }
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        String key, value;
        Attachment att;
        Event evt;
        int num, i;
        tags = new HashSet<String>();
        properties = new HashMap<String, String>();
        attachments = new HashMap<String, Attachment>();
        subEvents = new ArrayList<Event>();
        eventID = in.readLong();
        timestamp = (Date)in.readObject();
        num = in.readInt();
        for (i=0; i<num; i++) {
            tags.add(in.readUTF());
        }
        num = in.readInt();
        for (i=0; i<num; i++) {
            key = in.readUTF();
            value = in.readUTF();
            properties.put(key, value);
        }
        num = in.readInt();
        for (i=0; i<num; i++) {
            key = in.readUTF();
            att = (Attachment)in.readObject();
            attachments.put(key, att);
        }
        num = in.readInt();
        for (i=0; i<num; i++) {
            evt = (Event)in.readObject();
            evt.parent = this;
            subEvents.add(evt);
        }
        log = null;
        parent = null;
    }

    private void readObjectNoData() 
            throws ObjectStreamException {
        eventID = 0;
        timestamp = new Date();
        tags = new HashSet<String>();
        properties = new HashMap<String, String>();
        attachments = new HashMap<String, Attachment>();
        subEvents = new ArrayList<Event>();
        log = null;
        parent = null;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("======================================================================\n");
        buffer.append("EVENT " + eventID + " - " + timestamp.toString() + ": ");
        for (String tag : tags) {
            buffer.append("[");
            buffer.append(tag);
            buffer.append("] ");
        }
        buffer.append("\n");
        for (String key : properties.keySet()) {
            buffer.append("property: ");
            buffer.append(key);
            buffer.append("=\"");
            buffer.append(properties.get(key));
            buffer.append("\"\n");
        }
        for (String key : attachments.keySet()) {
            buffer.append("attachment: ");
            buffer.append(key);
            buffer.append("  ");
            buffer.append(attachments.get(key).toString());
            buffer.append("\n");
        }
        return buffer.toString();
    }

}

