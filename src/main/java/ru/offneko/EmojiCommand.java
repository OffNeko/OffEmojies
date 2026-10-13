package ru.offneko;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiCommand implements CommandExecutor, TabCompleter {

    private final OffEmojies plugin;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public EmojiCommand(OffEmojies plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = colorize(plugin.getConfig().getString("messages.prefix", ""));

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                if (!sender.hasPermission("offemojies.list")) {
                    sender.sendMessage(prefix + colorize(plugin.getConfig().getString("messages.no-permission", "")));
                    return true;
                }
                if (args.length >= 2) {
                    sendCategory(sender, args[1]);
                } else {
                    sendCategoryList(sender);
                }
                return true;

            case "search":
                if (!sender.hasPermission("offemojies.list")) {
                    sender.sendMessage(prefix + colorize(plugin.getConfig().getString("messages.no-permission", "")));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(prefix + colorize("&cUsage: /emoji search <запрос>"));
                    return true;
                }
                StringBuilder query = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) query.append(" ");
                    query.append(args[i]);
                }
                searchEmojis(sender, query.toString());
                return true;

            case "reload":
                if (!sender.hasPermission("offemojies.reload")) {
                    sender.sendMessage(prefix + colorize(plugin.getConfig().getString("messages.no-permission", "")));
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage(prefix + colorize(plugin.getConfig().getString("messages.reload-success", "")));
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendCategoryList(CommandSender sender) {
        ConfigurationSection emojies = plugin.getConfig().getConfigurationSection("emojies");
        if (emojies == null) {
            sender.sendMessage(colorize("&No categories found."));
            return;
        }

        String headerRaw = plugin.getConfig().getString("messages.list-header", "&#ff8800=== Доступные эмодзи ===");
        sender.sendMessage(colorize(headerRaw));

        String lineRaw = plugin.getConfig().getString("messages.category-line", "&r{icon} &f{name} &7({count})");
        String hoverRaw = plugin.getConfig().getString("messages.category-hover", "&eНажми, чтобы открыть категорию");

        for (String category : emojies.getKeys(false)) {
            String name = emojies.getString(category + ".display-name", category);
            String icon = emojies.getString(category + ".icon", "");

            ConfigurationSection emojisSection = emojies.getConfigurationSection(category + ".emojis");
            int count = emojisSection != null ? emojisSection.getKeys(false).size() : 0;

            String line = colorize(lineRaw
                    .replace("{icon}", icon)
                    .replace("{name}", name)
                    .replace("{count}", String.valueOf(count)));

            String hover = colorize(hoverRaw.replace("{name}", name));

            TextComponent component = new TextComponent(TextComponent.fromLegacyText(line));
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/emoji list " + category));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hover)));

            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).spigot().sendMessage(component);
            } else {
                sender.sendMessage(line);
            }
        }
    }

    private void sendCategory(CommandSender sender, String categoryKey) {
        ConfigurationSection emojies = plugin.getConfig().getConfigurationSection("emojies");
        if (emojies == null) {
            sender.sendMessage(colorize("&cNo categories found."));
            return;
        }

        ConfigurationSection catSection = emojies.getConfigurationSection(categoryKey);
        if (catSection == null) {
            sender.sendMessage(colorize("&cCategory not found: " + categoryKey));
            return;
        }

        String name = catSection.getString("display-name", categoryKey);
        String icon = catSection.getString("icon", "");

        ConfigurationSection emojisSection = catSection.getConfigurationSection("emojis");
        if (emojisSection == null) {
            sender.sendMessage(colorize("&cВ этой категории нет эмодзи."));
            return;
        }

        int count = emojisSection.getKeys(false).size();

        String headerRaw = plugin.getConfig().getString("messages.list-category-header",
                "&#ff8800=== {icon} {name} &7({count}) ===");
        String header = colorize(headerRaw
                .replace("{icon}", icon)
                .replace("{name}", name)
                .replace("{count}", String.valueOf(count)));
        sender.sendMessage(header);

        String formatRaw = plugin.getConfig().getString("messages.list-format", "&7{key} &f→ &r{emoji}");
        String hoverRaw = plugin.getConfig().getString("messages.list-hover", "&eНажми, чтобы вставить: &f{key}");

        for (String key : emojisSection.getKeys(false)) {
            String emoji = emojisSection.getString(key + ".chat", "");
            List<String> aliases = emojisSection.getStringList(key + ".aliases");

            String displayText = colorize(formatRaw.replace("{key}", key).replace("{emoji}", emoji));
            String hoverText = colorize(hoverRaw.replace("{key}", key).replace("{emoji}", emoji));

            TextComponent component = new TextComponent(TextComponent.fromLegacyText(displayText));
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, key + " "));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hoverText)));

            if (sender instanceof org.bukkit.entity.Player) {
                ((org.bukkit.entity.Player) sender).spigot().sendMessage(component);
            } else {
                sender.sendMessage(displayText);
            }
        }
    }

    private void searchEmojis(CommandSender sender, String query) {
        ConfigurationSection emojies = plugin.getConfig().getConfigurationSection("emojies");
        if (emojies == null) {
            sender.sendMessage(colorize("&cКатегории не найдены."));
            return;
        }

        String headerRaw = plugin.getConfig().getString("messages.search-header",
                "&#ff8800=== Поиск: &f{query}&#ff8800 ===");
        sender.sendMessage(colorize(headerRaw.replace("{query}", query)));

        String formatRaw = plugin.getConfig().getString("messages.search-format", "&7{key} &f→ &r{emoji} &7[{category}]");
        String hoverRaw = plugin.getConfig().getString("messages.list-hover", "&eНажми, чтобы вставить: &f{key}");
        String emptyRaw = plugin.getConfig().getString("messages.search-empty", "&cНичего не найдено.");

        boolean found = false;
        String lowerQuery = query.toLowerCase();

        for (String category : emojies.getKeys(false)) {
            String catName = emojies.getString(category + ".display-name", category);
            ConfigurationSection emojisSection = emojies.getConfigurationSection(category + ".emojis");
            if (emojisSection == null) continue;

            for (String key : emojisSection.getKeys(false)) {
                String emoji = emojisSection.getString(key + ".chat", "");
                List<String> aliases = emojisSection.getStringList(key + ".aliases");

                boolean match = key.toLowerCase().contains(lowerQuery);
                if (!match) {
                    for (String alias : aliases) {
                        if (alias.toLowerCase().contains(lowerQuery)) {
                            match = true;
                            break;
                        }
                    }
                }

                if (match) {
                    found = true;
                    String displayText = colorize(formatRaw
                            .replace("{key}", key)
                            .replace("{emoji}", emoji)
                            .replace("{category}", catName));
                    String hoverText = colorize(hoverRaw.replace("{key}", key).replace("{emoji}", emoji));

                    TextComponent component = new TextComponent(TextComponent.fromLegacyText(displayText));
                    component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, key + " "));
                    component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hoverText)));

                    if (sender instanceof org.bukkit.entity.Player) {
                        ((org.bukkit.entity.Player) sender).spigot().sendMessage(component);
                    } else {
                        sender.sendMessage(displayText);
                    }
                }
            }
        }

        if (!found) {
            sender.sendMessage(colorize(emptyRaw));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("offemojies.list")) {
                suggestions.add("list");
                suggestions.add("search");
            }
            if (sender.hasPermission("offemojies.reload")) {
                suggestions.add("reload");
            }
            String input = args[0].toLowerCase();
            suggestions.removeIf(s -> !s.startsWith(input));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            ConfigurationSection emojies = plugin.getConfig().getConfigurationSection("emojies");
            if (emojies != null) {
                for (String category : emojies.getKeys(false)) {
                    if (category.startsWith(args[1].toLowerCase())) {
                        suggestions.add(category);
                    }
                }
            }
        }

        return suggestions;
    }

    private void sendHelp(CommandSender sender) {
        List<String> helpLines = plugin.getConfig().getStringList("messages.help");
        for (String line : helpLines) {
            sender.sendMessage(colorize(line));
        }
    }

    private String colorize(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(net.md_5.bungee.api.ChatColor.of("#" + hex).toString()));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
