package com.florlet.quickalias.core;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/**
 * <p> Handles variable substitution in commands.
 * <p> Replaces named variables ({target}) and constants ({X}, {Y}, {Z}, {ID}, {DIM}).
 * <p> Handles escaping: \{ becomes {, \\ becomes \.
 *
 * @author Florlet
 */
public class VariableResolver {

    /**
     * Resolves variables and constants in a command string.
     *
     * @param command   The raw command string.
     * @param variables Map of captured variables from the alias path.
     * @return The processed command string ready for execution.
     */
    public static String resolve(String command, Map<String, String> variables) {
        if (command == null || command.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        StringBuilder currentVar = new StringBuilder();
        boolean insideVar = false;
        boolean escape = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (escape) {
                // Handle escaped characters
                if (c == '{' || c == '\\') {
                    // Valid escape sequence: \{ -> {, \\ -> \
                    if (insideVar) currentVar.append(c);
                    else result.append(c);
                } else {
                    // Invalid escape sequence, treat as literal backslash + char
                    if (insideVar) {
                        currentVar.append('\\').append(c);
                    } else {
                        result.append('\\').append(c);
                    }
                }
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '{') {
                if (insideVar) {
                    // Nested { is not allowed/supported, treat previous { as literal
                    result.append('{').append(currentVar);
                    currentVar.setLength(0);
                }
                insideVar = true;
                continue;
            }

            if (c == '}') {
                if (insideVar) {
                    String varName = currentVar.toString();
                    String resolved = resolveSingleVariable(varName, variables);
                    result.append(resolved);
                    currentVar.setLength(0);
                    insideVar = false;
                } else {
                    result.append(c);
                }
                continue;
            }

            if (insideVar) {
                currentVar.append(c);
            } else {
                result.append(c);
            }
        }

        // Handle unclosed brace at the end or trailing escape
        if (escape) result.append('\\');
        if (insideVar) result.append('{').append(currentVar);

        return result.toString();
    }

    private static String resolveSingleVariable(String varName, Map<String, String> variables) {
        // Check constants
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        switch (varName) {
            case "X":
                return player != null ? String.format("%.1f", player.getX()) : "{X}";
            case "Y":
                return player != null ? String.format("%.1f", player.getY()) : "{Y}";
            case "Z":
                return player != null ? String.format("%.1f", player.getZ()) : "{Z}";
            case "ID":
                return player != null ? player.getGameProfile().getName() : "{ID}";
            case "DIM":
                if (player != null) {
                    return player.level().dimension().location().toString();
                }
                return "{DIM}";
        }

        // Check captured variables
        if (variables != null && variables.containsKey(varName)) {
            return variables.get(varName);
        }

        // Not found - return raw format.
        return "{" + varName + "}";
    }
}
