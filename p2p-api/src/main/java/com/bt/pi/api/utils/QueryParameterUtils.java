/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

/**
 * special processing for mangling the AWS REST params into a format suitable for FreeMarker templates
 */
@Component
public class QueryParameterUtils {
    private static final Log LOG = LogFactory.getLog(QueryParameterUtils.class);
    private static final String ACTION = "Action";
    private static final String VERSION = "Version";
    private static final String INSTANCE_ID = "InstanceId";
    private static final String VOLUME_ID = "VolumeId";
    private static final String PUBLIC_IP = "PublicIp";
    private static final String ZONE_NAME = "ZoneName";
    private static final String DOT = ".";
    private static final String PLACEMENT_AVAILABILITY_ZONE = "Placement.AvailabilityZone";
    private static final String MONITORING_ENABLED = "Monitoring.Enabled";
    private static final String EXECUTABLE_BY = "ExecutableBy";
    private static final String OWNER = "Owner";
    private static final String IMAGE_ID = "ImageId";
    private static final String KEY_NAME = "KeyName";
    private static final String SNAPSHOT_ID = "SnapshotId";
    private static final String REGION = "Region";
    private static final String GROUP_NAME = "GroupName";

    public QueryParameterUtils() {
    }

    public Map<String, Object> sanitiseParameters(Map<String, String> parameters) {
        Map<String, Object> result = new HashMap<String, Object>();

        urlDecodeParameters(parameters);

        result.putAll(parameters);

        processVersion(result);
        processSecurityGroups(result);
        processPlacementAvailabilityZone(result);
        processBlockDeviceMapping(result);
        processListOf(result, INSTANCE_ID);
        processListOf(result, VOLUME_ID);
        processListOf(result, PUBLIC_IP);
        processListOf(result, ZONE_NAME);
        processListOf(result, EXECUTABLE_BY);
        processListOf(result, OWNER);
        processListOf(result, IMAGE_ID);
        processListOf(result, KEY_NAME);
        processListOf(result, REGION);
        processListOf(result, GROUP_NAME);
        processSnapshots(result);
        processListOf(result, SNAPSHOT_ID);
        processMonitoringEnabled(result);

        return result;
    }

    // TODO: this is in place due to Tim Kay AWS script which doesn't send SnapshotId.x in array format for the query
    // API
    private void processSnapshots(Map<String, Object> result) {
        if (result.containsKey(SNAPSHOT_ID) && result.get(ACTION).equals("DescribeSnapshots")) {
            result.put(SNAPSHOT_ID + ".1", result.get(SNAPSHOT_ID));
            result.remove(SNAPSHOT_ID);
        }
    }

    private void urlDecodeParameters(Map<String, String> parameters) {
        for (Entry<String, String> entry : parameters.entrySet()) {
            try {
                entry.setValue(URLDecoder.decode(entry.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                LOG.error(String.format("error URL decoding value: %s", entry.getValue()), e);
            }
        }
    }

    private void processMonitoringEnabled(Map<String, Object> result) {
        if (!result.containsKey(MONITORING_ENABLED))
            return;
        String value = (String) result.remove(MONITORING_ENABLED);
        result.put("Monitoring", newMap("Enabled", value));
    }

    private void processVersion(Map<String, Object> result) {
        if (!result.containsKey(VERSION))
            return;
        String version = (String) result.get(VERSION);
        int i = Integer.parseInt(version.replaceAll("-", ""));
        result.put("VersionInteger", i);
    }

    private void processListOf(Map<String, Object> result, String match) {
        SortedSet<Key> sortedNames = getSortedNamesStartingWith(match + DOT, result);
        if (sortedNames.size() < 1)
            return;
        List<String> list = new ArrayList<String>();
        for (Key key : sortedNames) {
            if (key.getIndex() > -1) {
                String value = (String) result.remove(key.getName());
                list.add(value);
            }
        }
        result.put(match, list);
    }

    private void processPlacementAvailabilityZone(Map<String, Object> result) {
        if (result.containsKey(PLACEMENT_AVAILABILITY_ZONE)) {
            String value = (String) result.remove(PLACEMENT_AVAILABILITY_ZONE);
            result.put("Placement", newMap("AvailabilityZone", value));
        }
    }

    private void processBlockDeviceMapping(Map<String, Object> result) {
        SortedSet<Key> names = getSortedNamesStartingWith("BlockDeviceMapping.", result);
        if (names.size() < 1)
            return;
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        String lastPrefix = "";
        Map<String, String> m = null;
        for (Key key : names) {
            String name = key.getName();
            String prefix = name.substring(0, name.lastIndexOf(DOT));
            String suffix = name.substring(name.lastIndexOf(DOT) + 1);
            String value = (String) result.remove(name);
            if (!prefix.equals(lastPrefix)) {
                if (m != null)
                    list.add(m);
                m = new HashMap<String, String>();
            }
            if (null == m)
                m = new HashMap<String, String>();
            m.put(suffix, value);
            lastPrefix = prefix;
        }
        if (m != null)
            list.add(m);

        result.put("BlockDeviceMapping", list);
    }

    private void processSecurityGroups(Map<String, Object> result) {
        SortedSet<Key> names = getSortedNamesStartingWith("SecurityGroup.", result);
        if (names.size() < 1)
            return;
        List<String> list = new ArrayList<String>();
        for (Key name : names) {
            String value = (String) result.remove(name.getName());
            list.add(value);
        }
        result.put("SecurityGroup", list);
    }

    private SortedSet<Key> getSortedNamesStartingWith(String prefix, Map<String, Object> parameters) {
        SortedSet<String> names = new TreeSet<String>();
        for (String name : parameters.keySet())
            if (name.startsWith(prefix))
                names.add(name);

        return sortNames(names);
    }

    private SortedSet<Key> sortNames(SortedSet<String> names) {
        SortedSet<Key> result = new TreeSet<Key>();
        for (String name : names) {
            int index = getIndexFromName(name);
            result.add(new Key(index, name));
        }
        return result;
    }

    private int getIndexFromName(String name) {
        int dotIndex = name.indexOf(DOT);
        if (dotIndex < 0)
            return -1;
        int secondDotIndex = name.indexOf(DOT, dotIndex + 1);

        String s;
        if (secondDotIndex > -1) {
            s = name.substring(dotIndex + 1, secondDotIndex);
        } else {
            s = name.substring(dotIndex + 1);
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private Map<String, Object> newMap(String key, Object value) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(key, value);
        return result;
    }

    static class Key implements Comparable<Key> {
        private Integer index;
        private String name;

        public Key(Integer i, String n) {
            this.index = i;
            this.name = n;
        }

        public int getIndex() {
            return this.index;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public int compareTo(Key other) {
            if (0 == index.compareTo(other.index))
                return name.compareTo(other.name);
            else
                return index.compareTo(other.index);
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((index == null) ? 0 : index.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (index == null) {
                if (other.index != null)
                    return false;
            } else if (!index.equals(other.index))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }
    }
}
