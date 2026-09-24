package dev.aldi.sayuti.editor.manage;

import static pro.sketchware.utility.FileUtil.formatFileSize;
import static pro.sketchware.utility.FileUtil.getFileSize;
import static pro.sketchware.utility.FileUtil.readFile;

import java.io.File;

public class LocalLibrary {
    private final String size;
    private final String info;
    private String name;
    private boolean isSelected;

    private LocalLibrary(String name, String size, String info) {
        this.name = name;
        this.size = size;
        this.info = info;
    }

    public static LocalLibrary fromFile(File file) {
        if (file.getPath().endsWith(".info")) {
            return new LocalLibrary(file.getName(), formatFileSize(getFileSize(file), true), readFile(file.getPath()));
        } else {
            return new LocalLibrary(file.getName(), formatFileSize(getFileSize(file), false), file.getPath());
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo() {return info;}

    public String getSize() {
        return size;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
