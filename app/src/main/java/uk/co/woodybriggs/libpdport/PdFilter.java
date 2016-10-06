package uk.co.woodybriggs.libpdport;

import java.io.File;
import java.io.FilenameFilter;

public class PdFilter implements FilenameFilter {
    @Override
    public boolean accept(File dir, String filename) {
        return (filename.endsWith(".pd"));
    }
}
