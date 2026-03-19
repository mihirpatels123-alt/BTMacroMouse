package com.btmacromouse;

public class MacroButton {
    private String name;
    private int targetX;  // relative X from top-left after homing
    private int targetY;  // relative Y from top-left after homing
    private int color;    // button color (ARGB)

    public MacroButton(String name, int targetX, int targetY, int color) {
        this.name = name;
        this.targetX = targetX;
        this.targetY = targetY;
        this.color = color;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getTargetX() { return targetX; }
    public void setTargetX(int targetX) { this.targetX = targetX; }

    public int getTargetY() { return targetY; }
    public void setTargetY(int targetY) { this.targetY = targetY; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
}
