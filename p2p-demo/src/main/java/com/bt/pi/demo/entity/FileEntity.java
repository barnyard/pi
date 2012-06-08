package com.bt.pi.demo.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import com.bt.pi.core.entity.PiEntityBase;

public class FileEntity extends PiEntityBase {
    private final static String SCHEME = "FileEntity";
    @JsonProperty
    private String filename;
    @JsonProperty
    private String contents;

    public FileEntity() {
    }

    public FileEntity(String filename, String contents) {
        this.filename = filename;
        this.contents = contents;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return SCHEME + ":" + filename;
    }

    @JsonIgnore
    public String getFilename() {
        return filename;
    }

    @JsonIgnore
    public String getContents() {
        return contents;
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }
}
