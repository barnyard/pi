package com.bt.pi.demo.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.bt.pi.core.entity.PiEntityBase;

public class Hutch extends PiEntityBase {
    public static final String SCHEME = "hutch";
    public static final String URL = SCHEME + ":bunnies";
    private Map<String, ArrayList<Bunny>> bunnies;

    public Hutch() {
        bunnies = new HashMap<String, ArrayList<Bunny>>();
    }

    public Map<String, ArrayList<Bunny>> getBunnies() {
        return bunnies;
    }

    public void setBunnies(Map<String, ArrayList<Bunny>> bunnies) {
        this.bunnies = bunnies;
    }

    public void addABunny(String owner, Bunny b) {
        if (!bunnies.containsKey(owner)) {
            ArrayList<Bunny> bs = new ArrayList<Bunny>();
            bs.add(b);
            this.bunnies.put(owner, bs);
        } else
            this.bunnies.get(owner).add(b);
    }

    public ArrayList<Bunny> adoptABunny(String oldOwner, String newOwner) {
        ArrayList<Bunny> oldBs = bunnies.remove(oldOwner);
        for (Bunny b : oldBs) {
            addABunny(newOwner, b);
        }
        return bunnies.get(newOwner);
    }

    @JsonIgnore
    public int getNumberOfBunnies() {
        return bunnies.values().size();
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return URL;
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }
}
