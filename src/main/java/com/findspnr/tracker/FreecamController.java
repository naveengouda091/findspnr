package com.findspnr.tracker;

import com.findspnr.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * Client-Side Freecam Controller:
 *  - Toggled with key 'K' or command '/findspnr freecam'.
 *  - Detaches camera view and allows flying through world using WASD / Space / Shift.
 *  - Real player character body remains frozen in place.
 */
public class FreecamController {

    private static Vec3d freecamPos = Vec3d.ZERO;
    private static float freecamYaw = 0f;
    private static float freecamPitch = 0f;
    private static Vec3d originalBodyPos = Vec3d.ZERO;

    public static void toggle(MinecraftClient client) {
        ModConfig.freecamEnabled = !ModConfig.freecamEnabled;

        if (client.player != null) {
            if (ModConfig.freecamEnabled) {
                originalBodyPos = client.player.getPos();
                freecamPos = client.player.getEyePos();
                freecamYaw = client.player.getYaw();
                freecamPitch = client.player.getPitch();

                client.player.sendMessage(
                        Text.literal("§c[FindSpnr] §fFreecam §aENABLED ✔ §7(Press K to exit)"), true);
            } else {
                client.player.sendMessage(
                        Text.literal("§c[FindSpnr] §fFreecam §cDISABLED ✖"), true);
            }
        }
    }

    public static void tick(MinecraftClient client) {
        if (!ModConfig.freecamEnabled || client.player == null) return;

        double speed = client.options.sprintKey.isPressed() ? 1.2 : 0.5;

        double radYaw = Math.toRadians(freecamYaw);
        double fwdX = -Math.sin(radYaw);
        double fwdZ = Math.cos(radYaw);
        double rightX = Math.cos(radYaw);
        double rightZ = Math.sin(radYaw);

        double moveX = 0, moveY = 0, moveZ = 0;

        if (client.options.forwardKey.isPressed()) {
            moveX += fwdX * speed;
            moveZ += fwdZ * speed;
        }
        if (client.options.backKey.isPressed()) {
            moveX -= fwdX * speed;
            moveZ -= fwdZ * speed;
        }
        if (client.options.leftKey.isPressed()) {
            moveX -= rightX * speed;
            moveZ -= rightZ * speed;
        }
        if (client.options.rightKey.isPressed()) {
            moveX += rightX * speed;
            moveZ += rightZ * speed;
        }
        if (client.options.jumpKey.isPressed()) {
            moveY += speed;
        }
        if (client.options.sneakKey.isPressed()) {
            moveY -= speed;
        }

        freecamPos = freecamPos.add(moveX, moveY, moveZ);
    }

    public static Vec3d getFreecamPos() { return freecamPos; }
    public static float getFreecamYaw() { return freecamYaw; }
    public static float getFreecamPitch() { return freecamPitch; }
    public static Vec3d getOriginalBodyPos() { return originalBodyPos; }
}
