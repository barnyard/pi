/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.common.conf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

@Component
public class XenConfigurationParser {
    private static final String SPACE = " ";
    private static final String SINGLE_QUOTE = "'";

    private Map<String, String> map;

    public XenConfigurationParser() {
        map = new HashMap<String, String>();
    }

    public void init(String configurationFilePath) throws IOException {
        String configurationFileContents = FileUtils.readFileToString(new File(configurationFilePath));
        parse(configurationFileContents);
    }

    private void parse(String s) {
        int sCount = 0;
        int eCount = 0;
        int startIndex = -1;
        int endIndex = -1;
        startIndex = s.indexOf('(');
        if (startIndex == -1)
            return;

        int nextIndex = startIndex;
        sCount++;
        while (sCount != eCount) {
            nextIndex = getNextIndex(s, nextIndex + 1);
            if (nextIndex > 0) {
                if (s.charAt(nextIndex) == '(')
                    sCount++;
                else {
                    eCount++;
                    endIndex = nextIndex;
                }
            }
        }
        addKVP(s.substring(startIndex + 1, endIndex));
        parse(s.substring(endIndex + 1));
    }

    private int getNextIndex(String s, int startIndex) {
        int open = s.indexOf('(', startIndex);
        int close = s.indexOf(')', startIndex);
        if (open == -1 && close == -1)
            return -1;
        if (open == -1)
            return close;
        if (close == -1)
            return open;
        return Math.min(open, close);
    }

    private void addKVP(String string) {
        String keyValuePair = string.trim();
        if (keyValuePair.startsWith("(") && keyValuePair.endsWith(")")) {
            parse(keyValuePair.substring(1, keyValuePair.length() - 2));
            return;
        }

        String[] kvp = keyValuePair.split(SPACE);
        if (kvp.length == 2) {
            map.put(kvp[0].replace(SINGLE_QUOTE, ""), kvp[1].replace(SINGLE_QUOTE, ""));
            return;
        }

        int counter = 0;
        String key = kvp[0];
        StringBuilder keyBuilder = new StringBuilder();
        if (kvp[0].startsWith(SINGLE_QUOTE)) {
            for (; counter < kvp.length; counter++) {
                if (counter > 0)
                    keyBuilder.append(SPACE);
                if (kvp[counter].endsWith(SINGLE_QUOTE)) {
                    keyBuilder.append(kvp[counter].substring(0, kvp[counter].length() - 1));
                    break;
                }
                keyBuilder.append(kvp[counter]);
            }
            key = keyBuilder.toString();
        }

        StringBuilder valueBuilder = new StringBuilder();
        for (int i = counter + 1; i < kvp.length; i++) {
            if (i > counter + 1)
                valueBuilder.append(SPACE);
            valueBuilder.append(kvp[i].replace(SINGLE_QUOTE, ""));
        }
        map.put(key, valueBuilder.toString());
    }

    public int getIntValue(String key) {
        return Integer.parseInt(getValue(key));
    }

    public long getLongValue(String key) {
        return Long.parseLong(getValue(key));
    }

    public String getValue(String key) {
        return map.get(key);
    }

    public Map<String, String> getMap() {
        return map;
    }
}
