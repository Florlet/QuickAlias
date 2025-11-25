package com.florlet.quickalias.core;

import com.florlet.quickalias.QuickAliasLogger;
import com.florlet.quickalias.config.AliasNode;
import com.florlet.quickalias.config.ConfigManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Manages the injection and removal of aliases in the game's command dispatcher.
 *
 * @author Florlet
 */
public class SuggestionManager {
    private static final SuggestionManager INSTANCE = new SuggestionManager();

    // Reflection fields cache
    private static Field childrenField;
    private static Field literalsField;
    private static Field argumentsField;

    static {
        try {
            // Brigadier fields are not obfuscated
            childrenField = CommandNode.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            literalsField = CommandNode.class.getDeclaredField("literals");
            literalsField.setAccessible(true);
            argumentsField = CommandNode.class.getDeclaredField("arguments");
            argumentsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            QuickAliasLogger.error("Failed to initialize reflection for CommandNode", e);
        }
    }

    private CommandDispatcher<SharedSuggestionProvider> dispatcher;
    private List<AliasNode> registeredAliases = new ArrayList<>();

    private SuggestionManager() {
    }

    public static SuggestionManager getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings ("unchecked")
    public void init(CommandDispatcher<?> dispatcher) {
        this.dispatcher = (CommandDispatcher<SharedSuggestionProvider>) dispatcher;
        this.update();
    }

    public void update() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            try {
                this.dispatcher = mc.getConnection().getCommands();
            } catch (ClassCastException e) {
                QuickAliasLogger.error("Failed to cast command dispatcher", e);
            }
        }

        if (dispatcher == null) return;

        try {
            removeAliases(dispatcher.getRoot(), registeredAliases);
        } catch (Exception e) {
            QuickAliasLogger.error("Failed to remove old aliases", e);
        }

        try {
            List<AliasNode> currentAliases = ConfigManager.getInstance().getConfig().aliases;
            registerAliases(currentAliases);
            registeredAliases = deepCopyNodes(currentAliases);
        } catch (Exception e) {
            QuickAliasLogger.error("Failed to update command suggestions", e);
        }
    }

    private void registerAliases(List<AliasNode> nodes) {
        for (AliasNode node : nodes) {
            registerNode(node, dispatcher.getRoot());
        }
    }

    private void registerNode(AliasNode aliasNode, CommandNode<SharedSuggestionProvider> parent) {
        // Skip {END} nodes for auto-completion
        if (aliasNode.isEndNode()) return;

        String name = aliasNode.getName();
        if (name == null || name.trim().isEmpty()) return;

        boolean isVariable = aliasNode.isVariable();
        String nodeName = isVariable ? aliasNode.getVariableName() : name;

        ArgumentBuilder<SharedSuggestionProvider, ?> builder;

        if (isVariable) {
            // Variable Logic - Case Sensitive Matching
            if ("id".equals(nodeName)) {
                // Player Name Completion
                builder = RequiredArgumentBuilder.<SharedSuggestionProvider, String>argument(nodeName,
                                                                                             StringArgumentType.word())
                        .suggests((context, suggestionsBuilder) -> {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.getConnection() != null) {
                                Collection<PlayerInfo> players = mc.getConnection().getOnlinePlayers();
                                for (PlayerInfo info : players) {
                                    suggestionsBuilder.suggest(info.getProfile().getName());
                                }
                            }
                            return suggestionsBuilder.buildFuture();
                        });
            } else if ("dim".equals(nodeName)) {
                // Dimension ID Completion
                builder = RequiredArgumentBuilder.<SharedSuggestionProvider, ResourceLocation>argument(nodeName,
                                                                                                       ResourceLocationArgument.id())
                        .suggests((context, suggestionsBuilder) -> {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.getConnection() != null) {
                                Set<ResourceKey<Level>> levels = mc.getConnection().levels();
                                for (ResourceKey<Level> levelKey : levels) {
                                    suggestionsBuilder.suggest(levelKey.location().toString());
                                }
                            }
                            return suggestionsBuilder.buildFuture();
                        });
            } else {
                // Default Variable
                builder = RequiredArgumentBuilder.argument(nodeName, StringArgumentType.string());
            }
        } else {
            // Literal
            builder = LiteralArgumentBuilder.literal(nodeName);
        }

        if (!aliasNode.getCommands().isEmpty() || aliasNode.getChildren().isEmpty()) {
            builder.executes(context -> Command.SINGLE_SUCCESS);
        }

        CommandNode<SharedSuggestionProvider> commandNode = builder.build();

        CommandNode<SharedSuggestionProvider> existingNode = parent.getChild(commandNode.getName());
        CommandNode<SharedSuggestionProvider> targetNode;

        if (existingNode != null) {
            targetNode = existingNode;
        } else {
            parent.addChild(commandNode);
            targetNode = commandNode;
        }

        for (AliasNode child : aliasNode.getChildren()) {
            registerNode(child, targetNode);
        }
    }

    @SuppressWarnings ("unchecked")
    private void removeAliases(CommandNode<SharedSuggestionProvider> parent, List<AliasNode> nodesToRemove) {
        if (nodesToRemove == null || nodesToRemove.isEmpty()) return;
        if (parent == null) return;

        Map<String, CommandNode<SharedSuggestionProvider>> childrenMap;
        Map<String, LiteralCommandNode<SharedSuggestionProvider>> literalsMap;
        Map<String, CommandNode<SharedSuggestionProvider>> argumentsMap;

        try {
            childrenMap = (Map<String, CommandNode<SharedSuggestionProvider>>) childrenField.get(parent);
            literalsMap = (Map<String, LiteralCommandNode<SharedSuggestionProvider>>) literalsField.get(parent);
            argumentsMap = (Map<String, CommandNode<SharedSuggestionProvider>>) argumentsField.get(parent);
        } catch (Exception e) {
            QuickAliasLogger.error("Failed to access CommandNode fields via reflection", e);
            return;
        }

        for (AliasNode alias : nodesToRemove) {
            // If it's an END node, it wasn't registered, so no need to remove
            if (alias.isEndNode()) continue;

            boolean isVariable = alias.isVariable();
            String name = isVariable ? alias.getVariableName() : alias.getName();

            CommandNode<SharedSuggestionProvider> childNode = childrenMap.get(name);

            if (childNode != null) {
                removeAliases(childNode, alias.getChildren());

                boolean hasChildren = !childNode.getChildren().isEmpty();

                if (!hasChildren) {
                    childrenMap.remove(name);
                    if (isVariable) {
                        argumentsMap.remove(name);
                    } else {
                        literalsMap.remove(name);
                    }
                }
            }
        }
    }

    private List<AliasNode> deepCopyNodes(List<AliasNode> source) {
        List<AliasNode> list = new ArrayList<>();
        for (AliasNode n : source) {
            AliasNode newNode = new AliasNode(n.getName());
            newNode.setCommands(new ArrayList<>(n.getCommands()));
            newNode.setChildren(deepCopyNodes(n.getChildren()));
            list.add(newNode);
        }
        return list;
    }
}
