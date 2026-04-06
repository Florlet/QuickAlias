package com.florlet.quickalias.core;

import com.florlet.quickalias.config.AliasNode;
import com.florlet.quickalias.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.*;

/**
 * Handles chat input parsing using Greedy Matching and Chain Execution.
 *
 * @author Florlet
 */
public class InputHandler {
    /**
     * Execution lock to prevent duplicate command parsing/execution
     * when simulating chat input for client-side mod compatibility.
     */
    public static boolean isExecutingAlias = false;

    /**
     * Intercepts chat input to check for alias matching.
     *
     * @param originalMessage The raw message typed by the user.
     * @return true if the message was handled (executed as alias), false otherwise.
     */
    public static boolean handleChatInput(String originalMessage) {
        if (isExecutingAlias) {
            return false;
        }

        if (!originalMessage.startsWith("/")) return false;

        String raw = originalMessage.substring(1).trim();
        if (raw.isEmpty()) return false;

        // Tokenize input
        String[] tokens = raw.split("\\s+");
        if (tokens.length == 0) return false;

        List<AliasNode> currentScope = ConfigManager.getInstance().getConfig().aliases;
        Map<String, String> capturedVariables = new HashMap<>();

        // This list holds the current command lines being built.
        List<String> accumulatedCommands = new ArrayList<>();

        int tokenIndex = 0;
        boolean matchedAny = false;

        // Traverse the tree greedily
        while (tokenIndex < tokens.length) {
            String token = tokens[tokenIndex];
            AliasNode matchedNode = null;

            // Try Exact Match
            for (AliasNode node : currentScope) {
                if (!node.isVariable() && !node.isEndNode() && node.getName().equalsIgnoreCase(token)) {
                    matchedNode = node;
                    break;
                }
            }

            // Try Variable Match (if no exact match)
            if (matchedNode == null) {
                for (AliasNode node : currentScope) {
                    if (node.isVariable()) {
                        matchedNode = node;
                        // Capture variable
                        capturedVariables.put(node.getVariableName(), token);
                        break;
                    }
                }
            }

            // If found a match
            if (matchedNode != null) {
                matchedAny = true;

                // Concatenate commands from this node to the chain
                mergeCommands(accumulatedCommands, matchedNode.getCommands());

                // Advance
                currentScope = matchedNode.getChildren();
                tokenIndex++;
            } else {
                // No match at this level, stop traversal
                break;
            }
        }

        if (!matchedAny) return false;

        // Check for {END} node logic if no tokens remain
        if (tokenIndex >= tokens.length) {
            for (AliasNode node : currentScope) {
                if (node.isEndNode()) {
                    mergeCommands(accumulatedCommands, node.getCommands());
                    break;
                }
            }
        }

        // Remaining input (if path ended but tokens remain)
        String remainingInput = "";
        if (tokenIndex < tokens.length) {
            remainingInput = String.join(" ", Arrays.copyOfRange(tokens, tokenIndex, tokens.length));
        }

        try {
            isExecutingAlias = true;
            executeChain(accumulatedCommands, capturedVariables, remainingInput);
        } finally {
            isExecutingAlias = false;
        }

        return true;
    }

    /**
     * Merges new command segments into the existing accumulator.
     * Logic:
     * - If accumulator is empty, it becomes the new segments.
     * - If new segments are empty, accumulator is unchanged.
     * - Otherwise, performs a cross-join (concatenation) of existing lines + new segments.
     */
    private static void mergeCommands(List<String> accumulator, List<String> newSegments) {
        if (newSegments == null || newSegments.isEmpty()) return;

        if (accumulator.isEmpty()) {
            accumulator.addAll(newSegments);
        } else {
            // Create a new list to avoid concurrent modification issues during iteration
            List<String> nextStage = new ArrayList<>();
            for (String base : accumulator) {
                for (String append : newSegments) {
                    if (append.isEmpty()) {
                        nextStage.add(base);
                    } else {
                        nextStage.add(base + " " + append);
                    }
                }
            }
            accumulator.clear();
            accumulator.addAll(nextStage);
        }
    }

    /**
     * Execute command chain depending on config mode.
     */
    private static void executeChain(List<String> commands, Map<String, String> variables, String remainingInput) {
        if (commands.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean useVanilla = ConfigManager.getInstance().getConfig().settings.useVanillaChatInput;

        for (String rawCmd : commands) {
            if (rawCmd == null || rawCmd.trim().isEmpty()) continue;

            // Resolve variables
            String finalCmd = VariableResolver.resolve(rawCmd, variables);

            // Append remaining input (Space + Input)
            if (remainingInput != null && !remainingInput.isEmpty()) {
                finalCmd = finalCmd + " " + remainingInput;
            }

            if (finalCmd.isEmpty()) continue;

            if (useVanilla) {
                // Vanilla Chat Input
                sendThroughClientChat(mc, finalCmd);
            } else {
                // Direct Packet Send
                if (finalCmd.startsWith("/")) {
                    mc.player.connection.sendCommand(finalCmd.substring(1));
                } else {
                    mc.player.connection.sendChat(finalCmd);
                }
            }
        }
    }

    /**
     * Sends the final message through the client chat screen instead of directly sending packets.
     * This allows client-side mods to inspect the input before it is submitted.
     */
    public static void sendThroughClientChat(Minecraft mc, String message) {
        try {
            ChatScreen chatScreen = new ChatScreen("", false);
            mc.setScreen(chatScreen);

            chatScreen.handleChatInput(message, true);

        } catch (Throwable ignored) {
            // Fallback to direct send if anything fails
            if (message.startsWith("/")) {
                mc.player.connection.sendCommand(message.substring(1));
            } else {
                mc.player.connection.sendChat(message);
            }
        }
    }
}
