package ru.offneko;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ChatListener implements Listener {

    private final OffEmojies plugin;

    public ChatListener(OffEmojies plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String original = message;

        Map<String, EmojiEntry> emojis = loadEmojis();
        if (emojis.isEmpty()) return;

        String lowerMessage = message.toLowerCase();
        String[] words = message.split(" ");
        boolean changed = false;

        for (int i = 0; i < words.length; i++) {
            String lowerWord = words[i].toLowerCase();
            EmojiEntry match = findEmoji(lowerWord, emojis);
            if (match != null) {
                words[i] = replaceInWord(words[i], lowerWord, match);
                changed = true;
            }
        }

        if (changed) {
            String newMessage = String.join(" ", words);
            event.setMessage(newMessage);

            if (plugin.getConfig().getBoolean("sound.enabled", true)) {
                playSound(player);
            }
        }
    }

    private EmojiEntry findEmoji(String word, Map<String, EmojiEntry> emojis) {
        EmojiEntry exact = emojis.get(word);
        if (exact != null) return exact;

        EmojiEntry best = null;
        int bestLen = 0;

        for (Map.Entry<String, EmojiEntry> entry : emojis.entrySet()) {
            String key = entry.getKey();
            if (word.contains(key) && key.length() > bestLen) {
                best = entry.getValue();
                bestLen = key.length();
            }
            for (String alias : entry.getValue().aliases) {
                if (word.contains(alias) && alias.length() > bestLen) {
                    best = entry.getValue();
                    bestLen = alias.length();
                }
            }
        }

        return best;
    }

    private String replaceInWord(String originalWord, String lowerWord, EmojiEntry emoji) {
        String lower = lowerWord;
        int idx = -1;
        int len = 0;

        if (lower.contains(emoji.primaryKey)) {
            idx = lower.indexOf(emoji.primaryKey);
            len = emoji.primaryKey.length();
        } else {
            for (String alias : emoji.aliases) {
                int found = lower.indexOf(alias);
                if (found != -1 && (idx == -1 || found < idx)) {
                    idx = found;
                    len = alias.length();
                }
            }
        }

        if (idx == -1) return originalWord;

        String prefix = originalWord.substring(0, idx);
        String suffix = originalWord.substring(idx + len);
        return prefix + emoji.chat + suffix;
    }

    private Map<String, EmojiEntry> loadEmojis() {
        Map<String, EmojiEntry> map = new HashMap<>();
        ConfigurationSection emojies = plugin.getConfig().getConfigurationSection("emojies");
        if (emojies == null) return map;

        for (String category : emojies.getKeys(false)) {
            ConfigurationSection catSection = emojies.getConfigurationSection(category + ".emojis");
            if (catSection == null) continue;

            for (String key : catSection.getKeys(false)) {
                String chat = catSection.getString(key + ".chat", "");
                if (chat.isEmpty()) continue;

                List<String> aliases = catSection.getStringList(key + ".aliases");
                map.put(key, new EmojiEntry(key, chat, aliases));
                for (String alias : aliases) {
                    map.putIfAbsent(alias, new EmojiEntry(key, chat, aliases));
                }
            }
        }

        return map;
    }

    private void playSound(Player player) {
        String soundName = plugin.getConfig().getString("sound.type", "BLOCK_NOTE_BLOCK_PLING");
        float volume = (float) plugin.getConfig().getDouble("sound.volume", 0.5);
        float pitch = (float) plugin.getConfig().getDouble("sound.pitch", 1.0);

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[OffEmojies] Unknown sound: " + soundName);
        }
    }

    private static class EmojiEntry {
        final String primaryKey;
        final String chat;
        final List<String> aliases;

        EmojiEntry(String primaryKey, String chat, List<String> aliases) {
            this.primaryKey = primaryKey;
            this.chat = chat;
            this.aliases = aliases != null ? aliases : new ArrayList<>();
        }
    }
}
