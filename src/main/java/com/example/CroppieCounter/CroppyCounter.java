package com.example.croppie;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

public class CroppieCounter implements ModInitializer, HudRenderCallback {
    private static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.croppiecounter.toggle",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_C,
        "category.croppiecounter"
    ));

    private static boolean showCounter = true;
    private static int hudX = 10;
    private static int hudY = 10;
    private static boolean dragging = false;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;
    private static int totalCroppies = 0;

    @Override
    public void onInitialize() {
        // ---- toggle display with the C key ----
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.wasPressed()) {
                showCounter = !showCounter;
                MinecraftClient ci = MinecraftClient.getInstance();
                if (ci.player != null) {
                    ci.player.sendMessage(
                        Text.literal("Croppie Counter: " + (showCounter ? "Enabled" : "Disabled")),
                        false
                    );
                }
            }
        });

        // ---- update croppie count every second ----
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (showCounter && client.player != null) {
                updateCroppieCount(client);
            }
        });

        // ---- mouse handling for dragging the HUD ----
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.player == null || !showCounter) return;

            int mouseX = client.mouse.getX();
            int mouseY = client.mouse.getY();

            String text = "Croppies: " + totalCroppies;
            int textWidth = client.textRenderer.getWidth(text);
            int textHeight = client.textRenderer.fontHeight;

            boolean isHovering = mouseX >= hudX - 2 && mouseX <= hudX + textWidth + 2 &&
                                mouseY >= hudY - 2 && mouseY <= hudY + textHeight + 2;

            if (client.mouse.isLeftPressed()) {
                if (isHovering && !dragging) {
                    dragging = true;
                    dragOffsetX = mouseX - hudX;
                    dragOffsetY = mouseY - hudY;
                }
            } else {
                dragging = false;
            }

            if (dragging && client.mouse.isLeftPressed()) {
                hudX = Math.max(0, Math.min(client.mouse.getX() - dragOffsetX,
                                          client.getWindow().getScaledWidth() - textWidth - 4));
                hudY = Math.max(0, Math.min(client.mouse.getY() - dragOffsetY,
                                          client.getWindow().getScaledHeight() - textHeight - 4));
            }
        });

        // ---- register HUD renderer ----
        HudRenderCallback.EVENT.register(this);
    }

    private void updateCroppieCount(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null) {
            totalCroppies = 0;
            return;
        }

        int count = 0;

        // main inventory
        for (ItemStack stack : player.getInventory().main) {
            if (!stack:
                if (!stack.isEmpty() && isCroppie(stack)) {
                    count += stack.getCount();
                }
        }

        // off‑hand
        ItemStack off = player.getInventory().offHand.get(0);
        if (!off.isEmpty() && isCroppie(off)) {
            count += off.getCount();
        }

        totalCroppies = count;
    }

    /** true only for a player_head whose display name is exactly "Croppie :3" */
    private boolean isCroppie(ItemStack stack) {
        if (stack.isOf(Items.PLAYER_HEAD)) {
            Text name = stack.getName();
            return name.getString().equals("Croppie :3");   // case‑sensitive
        }
        return false;
    }

    @Override
    public void onHudRender(MatrixStack matrices, float tickDelta) {
        if (!showCounter) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        String text = "Croppies: " + totalCroppies;
        TextRenderer tr = client.textRenderer;
        int textWidth = tr.getWidth(text);
        int textHeight = tr.fontHeight;

        int bgColor = 0x60000000;   // semi‑transparent black
        int textColor = 0xFFFFFFFF; // white

        int x1 = hudX - 2;
        int y1 = hudY - 2;
        int x2 = hudX + textWidth + 2;
        int y2 = hudY + textHeight + 2;

        DrawableHelper.fill(matrices, x1, y1, x2, y2, bgColor);
        tr.draw(matrices, text, hudX, hudY, textColor);
    }
}