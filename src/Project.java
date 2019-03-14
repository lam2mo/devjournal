import java.io.*;
import java.util.*;

public class Project implements Serializable {

    static final long serialVersionUID = 62679518368205L;

    private String name;
    private List<String> roots;
    private int pollInterval;
    private List<String> includeFileFilters;
    private List<String> excludeFileFilters;
    private List<String> includeFolderFilters;
    private List<String> excludeFolderFilters;

    // {{{ text config I/O

    public static Project readFromFile(File config) {
        String line = "", key, value;
        Project p = new Project();
        try {
            BufferedReader in = new BufferedReader(new FileReader(config));
            while ((line = in.readLine()) != null) {
                int npos = line.indexOf('=');
                if (npos > 0) {
                    key = line.substring(0,npos).trim();
                    value = line.substring(npos+1).trim();
                    //System.out.println("key: \"" + key + "\" value: \"" + value + "\"");
                    if (key.equals("name")) {
                        p.name = value;
                    } else if (key.equals("root")) {
                        p.roots.add(value);
                    } else if (key.equals("interval")) {
                        p.pollInterval = Integer.parseInt(value);
                    } else if (key.equals("include-file-filter")) {
                        p.includeFileFilters.add(value);
                    } else if (key.equals("exclude-file-filter")) {
                        p.excludeFileFilters.add(value);
                    } else if (key.equals("include-folder-filter")) {
                        p.includeFolderFilters.add(value);
                    } else if (key.equals("exclude-folder-filter")) {
                        p.excludeFolderFilters.add(value);
                    }
                }
            }
            in.close();
        } catch (IOException e) {
            p = null;
            e.printStackTrace();
        }
        return p;
    }

    public static void saveToFile(File config, Project p) {
        try {
            PrintStream out = new PrintStream(config);
            out.println("name=" + p.name);
            for (String f : p.roots) {
                out.println("root=" + f);
            }
            out.println("interval=" + p.pollInterval);
            for (String f : p.includeFileFilters) {
                out.println("include-file-filter=" + f);
            }
            for (String f : p.excludeFileFilters) {
                out.println("exclude-file-filter=" + f);
            }
            for (String f : p.includeFolderFilters) {
                out.println("include-folder-filter=" + f);
            }
            for (String f : p.excludeFolderFilters) {
                out.println("exclude-folder-filter=" + f);
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // }}}

    public Project() {
        this("");
    }

    public Project(String name) {
        this.name = name;
        this.roots = new ArrayList<String>();
        this.pollInterval = 5000;
        this.includeFileFilters = new ArrayList<String>();
        this.excludeFileFilters = new ArrayList<String>();
        this.includeFolderFilters = new ArrayList<String>();
        this.excludeFolderFilters = new ArrayList<String>();
    }

    // {{{ data accessors

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRoots() {
        return roots;
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int interval) {
        pollInterval = interval;
    }

    public List<String> getIncludeFileFilters() {
        return includeFileFilters;
    }

    public List<String> getExcludeFileFilters() {
        return excludeFileFilters;
    }

    public List<String> getIncludeFolderFilters() {
        return includeFolderFilters;
    }

    public List<String> getExcludeFolderFilters() {
        return excludeFolderFilters;
    }

    // }}}

    // {{{ binary I/O

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        out.writeUTF(name);
        out.writeInt(roots.size());
        for (String f : roots) {
            out.writeUTF(f);
        }
        out.writeInt(pollInterval);
        out.writeInt(includeFileFilters.size());
        for (String f : includeFileFilters) {
            out.writeUTF(f);
        }
        out.writeInt(excludeFileFilters.size());
        for (String f : excludeFileFilters) {
            out.writeUTF(f);
        }
        out.writeInt(includeFolderFilters.size());
        for (String f : includeFolderFilters) {
            out.writeUTF(f);
        }
        out.writeInt(excludeFolderFilters.size());
        for (String f : excludeFolderFilters) {
            out.writeUTF(f);
        }
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        int num, i;
        roots = new ArrayList<String>();
        includeFileFilters = new ArrayList<String>();
        excludeFileFilters = new ArrayList<String>();
        includeFolderFilters = new ArrayList<String>();
        excludeFolderFilters = new ArrayList<String>();
        name = in.readUTF();
        num = in.readInt();
        for (i=0; i<num; i++) {
            roots.add(in.readUTF());
        }
        pollInterval = in.readInt();
        num = in.readInt();
        for (i=0; i<num; i++) {
            includeFileFilters.add(in.readUTF());
        }
        num = in.readInt();
        for (i=0; i<num; i++) {
            excludeFileFilters.add(in.readUTF());
        }
        num = in.readInt();
        for (i=0; i<num; i++) {
            includeFolderFilters.add(in.readUTF());
        }
        num = in.readInt();
        for (i=0; i<num; i++) {
            excludeFolderFilters.add(in.readUTF());
        }
    }

    private void readObjectNoData() 
            throws ObjectStreamException {
        name = "";
        pollInterval = 5000;
        roots = new ArrayList<String>();
        includeFileFilters = new ArrayList<String>();
        excludeFileFilters = new ArrayList<String>();
        includeFolderFilters = new ArrayList<String>();
        excludeFolderFilters = new ArrayList<String>();
    }
    
    // }}}

}

