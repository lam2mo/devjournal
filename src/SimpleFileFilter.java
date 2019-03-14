import java.io.*;
import java.util.*;

public class SimpleFileFilter implements FileFilter {

    public static SimpleFileFilter getProjectFilter() {
        return new SimpleFileFilter("prj");
    }

    public static SimpleFileFilter getLogFilter() {
        return new SimpleFileFilter("log");
    }

    public static SimpleFileFilter getAttachmentFilter() {
        return new SimpleFileFilter("dat");
    }

    private List<String> extensions;

    public SimpleFileFilter(String validExtensions) {
        extensions = new ArrayList<String>();
        String[] ext = validExtensions.split("\\|");
        int i;
        for (i=0; i<ext.length; i++) {
            extensions.add(ext[i]);
        }
    }

    public boolean accept(File pathname) {
        return accept(pathname.getName());
    }

    public boolean accept(String fullName) {
        int npos = fullName.lastIndexOf('.');
        boolean accepted = false;
        if (npos > 0) {
            String extension = fullName.substring(npos+1);
            if (extensions.contains(extension)) {
                accepted = true;
            }
        }
        return accepted;
    }

}

