package com.bt.pi.app.common.entities;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.scope.NodeScope;

@Backupable
@EntityScope(scope = NodeScope.REGION)
public class ImageIndex extends PiEntityBase {
    public static final String URL = ResourceSchemes.IMAGE_INDEX + ":" + "images";
    private Set<String> set;

    public ImageIndex() {
        this.set = new HashSet<String>();
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return URL;
    }

    public Set<String> getImages() {
        return this.set;
    }

    // method to migrate from previous version where images where stored in a map
    public void setMap(Map<String, Image> map) {
        this.set.addAll(map.keySet());
    }

    @Override
    public String getUriScheme() {
        return ResourceSchemes.IMAGE_INDEX.toString();
    }
}
