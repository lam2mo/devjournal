import difflib.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.text.*;
import javax.imageio.*;

public class Attachment implements Serializable {

    static final long serialVersionUID = 81362176063423L;

    private static String getMimeType(File path) {
        String type = "text/plain";
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        type = fileNameMap.getContentTypeFor(
                "file://" + path.getAbsolutePath());
        return type;
    }

    private static List<String> splitLines(String text) {
        return Arrays.asList(text.split("\n"));
    }

    private static String mergeLines(List<String> lines) {
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

    private long attachmentID;      // unique identifier
    private int size;               // size (in bytes) of attachment data
    private Map<String, String> properties;     // path, mimetype, etc.

    private EventLog log;
    private Event event;

    public Attachment(long id, String text, EventLog log) {
        this(id, text.length());
        setProperty("mimetype", "text/plain");
        this.log = log;
        saveToLogDir(text.getBytes());
    }

    public Attachment(long id, RenderedImage img, EventLog log) {
        this(id, img.getWidth()*img.getHeight());
        setProperty("mimetype", "image/png");
        this.log = log;
        saveToLogDir(img);
    }

    public Attachment(long id, File path, EventLog log) {
        this(id, (int)path.length());
        String mimetype = getMimeType(path);
        setProperty("path", path.getAbsolutePath());
        setProperty("mimetype", mimetype);
        this.log = log;
        if (log != null) {
            if (mimetype.startsWith("image")) {
                BufferedImage img = null;
                try {
                    InputStream input = new FileInputStream(path);
                    img = ImageIO.read(input);
                    input.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
                if (img != null) {
                    saveToLogDir(img);
                }
            } else {
                byte[] data = null;
                try {
                    FileInputStream input = new FileInputStream(path);
                    data = new byte[size];
                    try {
                        input.read(data, 0, size);
                    } catch (IOException ex) {
                        System.err.println(ex.getMessage());
                    } finally {
                        input.close();
                    }
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
                if (data != null) {
                    saveToLogDir(data);
                }
            }
        }
    }

    public Attachment(long id, int size) {
        this.attachmentID = id;
        this.size = size;
        this.properties = new HashMap<String, String>();
        this.log = null;
        this.event = null;
    }

    // {{{ data accessors

    public int getSize() {
        return size;
    }

    public boolean canDiff() {
        return (hasProperty("mimetype") && getProperty("mimetype").startsWith("text"));
    }

    public void setEventLog(EventLog log) {
        this.log = log;
    }

    public EventLog getEventLog() {
        return log;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
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

    // }}}

    public String getLogDirPath() {
        String path = "attach" + attachmentID + ".dat";
        if (log != null && log.getLogAttachDir() != null) {
            path = log.getLogAttachDir() + File.separator + path;
        } else {
            path = null;
        }
        return path;
    }

    public void saveToLogDir(byte[] data) {
        try {
            OutputStream output =
                    new BufferedOutputStream(
                    new GZIPOutputStream(
                    new FileOutputStream(getLogDirPath())));
            output.write(data);
            output.flush();
            output.close();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void saveToLogDir(RenderedImage img) {
        if (log != null) {
            try {
                OutputStream output =
                        new BufferedOutputStream(
                        new GZIPOutputStream(
                        new FileOutputStream(getLogDirPath())));
                ImageIO.write(img, "png", output);
                output.close();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }
        // uncompressed version:
        //ImageIO.write(img, "png", new File(getLogDirPath()));
    }

    public byte[] getByteData() {
        byte[] data = null;
        int nbytes;
        if (log != null) {
            try {
                InputStream input =
                        new BufferedInputStream(
                        new GZIPInputStream(
                        new FileInputStream(getLogDirPath())));
                data = new byte[size];
                nbytes = input.read(data, 0, size);
                if (nbytes < size) {
                    System.out.println(
                            "ERROR: Attachment was shorter than expected: " +
                            getLogDirPath());
                }
                input.close();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }
        return data;
    }

    public String getTextData() {
        byte[] data = getByteData();
        String text = new String(data);
        return text;
    }

    public BufferedImage getImageData() {
        BufferedImage img = null;
        if (log != null) {
            try {
                InputStream input =
                        new BufferedInputStream(
                        new GZIPInputStream(
                        new FileInputStream(getLogDirPath())));
                img = ImageIO.read(input);
                input.close();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }
        return img;
        // uncompressed version:
        //return ImageIO.read(new File(getLogDirPath()));
    }

    public String diffFrom(Attachment prev) {
        String diff = null;
        if (canDiff()) {
            List<String> previous = splitLines(prev.getTextData());
            List<String> current = splitLines(getTextData());
            Patch patch = DiffUtils.diff(previous, current);
            String path = log.getRelativePath(
                    hasProperty("path") ? getProperty("path") : "CURRENT");
            String prevPath = log.getRelativePath(
                    prev.hasProperty("path") ? prev.getProperty("path") : "PREVIOUS");
            List<String> udiff = DiffUtils.generateUnifiedDiff(
                    prevPath, path, previous, patch, 3);
            diff = mergeLines(udiff);
        }
        return diff;
    }

    public String applyDiff(String diff) {
        String result = null;
        if (canDiff()) {
            result = getTextData();
            // TODO: implement this!
        }
        return result;
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        out.writeLong(attachmentID);
        out.writeInt(size);
        out.writeInt(properties.size());
        for (String key : properties.keySet()) {
            out.writeUTF(key);
            out.writeUTF(properties.get(key));
        }
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        int num, i;
        String key, value;
        attachmentID = in.readLong();
        size = in.readInt();
        properties = new HashMap<String, String>();
        num = in.readInt();
        for (i=0; i<num; i++) {
            key = in.readUTF();
            value = in.readUTF();
            properties.put(key, value);
        }
        log = null;
        event = null;
    }

    private void readObjectNoData() 
            throws ObjectStreamException {
        attachmentID = 0;
        size = 0;
        properties = new HashMap<String, String>();
        log = null;
        event = null;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(" id=");
        buffer.append(attachmentID);
        buffer.append(" size=");
        buffer.append(size);
        for (String key : properties.keySet()) {
            buffer.append("\n   property: ");
            buffer.append(key);
            buffer.append("=\"");
            buffer.append(properties.get(key));
            buffer.append("\"");
        }
        return buffer.toString();
    }

}

