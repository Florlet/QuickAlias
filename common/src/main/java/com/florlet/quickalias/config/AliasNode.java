package com.florlet.quickalias.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * <p> Recursive Data Model for Alias Tree.
 * <p> Represents a single alias node which can contain commands or child nodes.
 *
 * @author Florlet
 */
public class AliasNode {
    private String id;
    private String name;
    private List<String> commands;
    private List<AliasNode> children;

    // Matches {var}, but not \{var}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("^\\{[^}]+}$");

    public AliasNode() {
        this.id = UUID.randomUUID().toString();
        this.children = new ArrayList<>();
        this.commands = new ArrayList<>();
        this.name = "";
    }

    public AliasNode(String name) {
        this();
        this.name = name;
    }

    /**
     * Deep copy constructor.
     *
     * @param other The node to copy.
     */
    public AliasNode(AliasNode other) {
        this.id = UUID.randomUUID().toString();
        this.name = other.name;
        this.commands = new ArrayList<>(other.commands);
        this.children = new ArrayList<>();
        for (AliasNode child : other.children) {
            this.children.add(new AliasNode(child));
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public List<AliasNode> getChildren() {
        return children;
    }

    public void setChildren(List<AliasNode> children) {
        this.children = children;
    }

    /**
     * Checks if this node is a leaf node.
     *
     * @return true if children list is empty
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    public void addCommand(String cmd) {
        this.commands.add(cmd);
    }

    public void addChild(AliasNode node) {
        this.children.add(node);
    }

    /**
     * Checks if this node defines a variable (e.g. "{target}").
     * Must start with '{' and end with '}'.
     * Does not handle escaped brackets, as those are treated as literals by logic.
     * <p>
     * Note: {END} is NOT considered a variable.
     */
    public boolean isVariable() {
        return name != null && name.length() > 2 && name.startsWith("{") && name.endsWith("}") && !isEndNode();
    }

    public String getVariableName() {
        if (isVariable()) {
            return name.substring(1, name.length() - 1);
        }
        return null;
    }

    public boolean isEndNode() {
        return "{END}".equals(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
