package com.bt.pi.demo.entity;

import com.bt.pi.core.entity.PiEntityBase;

public class WordEntity extends PiEntityBase {

    private static final String SCHEME = "word";
    private String word;

    public WordEntity() {
        this.word = null;
    }

    public WordEntity(String word) {
        this.word = word;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }

    @Override
    public String getUrl() {
        return SCHEME + ":" + word;
    }

}
