package com.bt.pi.demo.entity;

import com.bt.pi.core.entity.PiEntityBase;

public class Bunny extends PiEntityBase {
    public static final String[] BUNNY_NAMES = new String[] { "Nauman", "Josh", "Linda", "Rags", "Adrian", "Dan", "Marc", "Juan", "Uros", "Bob" };
    public static final String[] BUNNY_COLORS = new String[] { "black", "red", "green", "yellow", "blue", "magenta", "cyan", "white" };
    private static final String SCHEME = "bunny";

    private static final String BUNNY = "               (`.         ,-,\n" + "               `\\ `.    ,;' \n" + "                \\`. \\ ,'/ .'\n" + "          __     `.\\ Y /.'\n" + "       .-'  ''--.._` ` (\n" + "     .'            /   `\n"
            + "    ,           ` '   Q '\n" + "    ,         ,   `._    \\\n" + "    |         '     `-.;_'\n" + "    `  ;    `  ` --,.._;\n" + "    `    ,   )   .'\n" + "     `._ ,  '   /_\n" + "        ; ,''-,;' ``-\n"
            + "         ``-..__\\``--` \n";

    private int color;
    private String name;
    private String lastFed;

    public Bunny() {
        this(null, 0);
    }

    public Bunny(String aName, int aColor) {
        color = aColor;
        name = aName;
        lastFed = null;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int aColor) {
        this.color = aColor;
    }

    public String getName() {
        return name;
    }

    public void setName(String aName) {
        this.name = aName;
    }

    public String getLastFed() {
        return lastFed;
    }

    public void setLastFed(String wasLastFed) {
        this.lastFed = wasLastFed;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return SCHEME + ":" + name + "-" + color;
    }

    @Override
    public String toString() {
        int ansiColor = 30 + color;
        String lastFedTime = lastFed == null ? "a long long long time ago. " : lastFed;
        return name + " the " + BUNNY_COLORS[color] + " bunny\n" + "\033[" + ansiColor + "m" + BUNNY + "\033[39m" + " was last fed " + lastFedTime;
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }
}
