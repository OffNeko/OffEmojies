package ru.offneko;

import org.bukkit.plugin.java.JavaPlugin;

public class OffEmojies extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getCommand("emoji").setExecutor(new EmojiCommand(this));
        getLogger().info("OffEmojies > Plugin enabled. Hellooo ");
    }

    @Override
    public void onDisable() {
        getLogger().info("OffEmojies > Plugin disabled...");
    }
}
