package com.diodeit.spendtrack.models;

public class Category {
    private String name;
    private String shortName;
    private String emoji;
    private int iconRes;

    public Category(String name, String shortName, String emoji) {
        this.name = name;
        this.shortName = shortName;
        this.emoji = emoji;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }
}