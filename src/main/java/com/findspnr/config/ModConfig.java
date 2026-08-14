package com.findspnr.config;

/**
 * Simple in-memory config for FindSpnr.
 * All fields are public-static so they are accessible from anywhere without DI.
 */
public class ModConfig {

    /** Master toggle – press G (default) to flip */
    public static boolean enabled = true;

    /** Draw 3-D bounding-box outlines in the world */
    public static boolean renderWorldESP = true;

    /** Draw the HUD radar in the top-right corner */
    public static boolean renderHUDRadar = true;

    /** How many chunks around the player to scan (default 5 = 80 block radius) */
    public static int scanRadiusChunks = 5;
}
