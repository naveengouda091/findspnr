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

    /** Base Finder toggle (/findspnr base) – tracks Shulker Boxes & Ender Chests */
    public static boolean renderBaseFinder = false;

    /** Bastion Finder toggle (/findspnr bastion) – tracks Nether Bastion Remnants */
    public static boolean renderBastionFinder = false;

    /** Freecam toggle (Press K or /findspnr freecam) */
    public static boolean freecamEnabled = false;

    /** How many chunks around the player to scan (default 8 = 128 block radius) */
    public static int scanRadiusChunks = 8;
}
