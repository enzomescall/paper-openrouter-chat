package dev.enzomescall.openrouterchat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public final class OpenRouterChatPlugin extends JavaPlugin {

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    private HttpClient httpClient;
    private String apiKey;
    private String model;
    private String siteUrl;
    private String siteName;
    private String systemPrompt;
    private int maxTokens;
    private double temperature;
    private int timeoutSeconds;
    private String responsePrefix;
    private String errorPrefix;
    private String thinkingMessage;
    private boolean opsOnlyChat;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();
        getLogger().info("OpenRouterChat enabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("openrouter")) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize(errorPrefix + "Only players can use this command."));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player, label);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "add-key":
                return handleAddKey(player, args, label);
            case "clear-key":
                return handleClearKey(player);
            case "reload":
                return handleReload(player);
            case "status":
                return handleStatus(player);
            default:
                return handlePrompt(player, args);
        }
    }

    private boolean handleAddKey(Player player, String[] args, String label) {
        if (!player.hasPermission("openrouterchat.manage")) {
            player.sendMessage(colorize(errorPrefix + "You do not have permission to manage this plugin."));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(colorize("&eUsage: /" + label + " add-key <api-key>"));
            return true;
        }

        StringJoiner joiner = new StringJoiner(" ");
        for (int i = 1; i < args.length; i++) {
            joiner.add(args[i]);
        }

        String newKey = joiner.toString().trim();
        getConfig().set("api-key", newKey);
        saveConfig();
        reloadSettings();
        player.sendMessage(colorize("&a[OpenRouter] &7API key saved."));
        return true;
    }

    private boolean handleClearKey(Player player) {
        if (!player.hasPermission("openrouterchat.manage")) {
            player.sendMessage(colorize(errorPrefix + "You do not have permission to manage this plugin."));
            return true;
        }

        getConfig().set("api-key", "");
        saveConfig();
        reloadSettings();
        player.sendMessage(colorize("&a[OpenRouter] &7API key cleared."));
        return true;
    }

    private boolean handleReload(Player player) {
        if (!player.hasPermission("openrouterchat.manage")) {
            player.sendMessage(colorize(errorPrefix + "You do not have permission to manage this plugin."));
            return true;
        }

        reloadConfig();
        reloadSettings();
        player.sendMessage(colorize("&a[OpenRouter] &7Configuration reloaded."));
        return true;
    }

    private boolean handleStatus(Player player) {
        if (!player.hasPermission("openrouterchat.manage")) {
            player.sendMessage(colorize(errorPrefix + "You do not have permission to view plugin status."));
            return true;
        }

        player.sendMessage(colorize("&b[OpenRouter] &fModel: &a" + model));
        player.sendMessage(colorize("&b[OpenRouter] &fAPI key configured: " + (apiKey.isBlank() ? "&cno" : "&ayes")));
        player.sendMessage(colorize("&b[OpenRouter] &fOps-only chat: " + (opsOnlyChat ? "&ayes" : "&cno")));
        return true;
    }

    private boolean handlePrompt(Player player, String[] args) {
        if (!player.hasPermission("openrouterchat.use")) {
            player.sendMessage(colorize(errorPrefix + "You do not have permission to use this command."));
            return true;
        }

        if (opsOnlyChat && !player.isOp()) {
            player.sendMessage(colorize(errorPrefix + "Only server operators can use chat mode right now."));
            return true;
        }

        if (apiKey == null || apiKey.isBlank()) {
            player.sendMessage(colorize(errorPrefix + "No API key is configured. Use /openrouter add-key <key>."));
            return true;
        }

        StringJoiner joiner = new StringJoiner(" ");
        for (String arg : args) {
            joiner.add(arg);
        }
        String message = joiner.toString().trim();
        if (message.isEmpty()) {
            sendUsage(player, "openrouter");
            return true;
        }

        player.sendMessage(colorize(thinkingMessage));
        String playerName = player.getName();

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String jsonBody = buildRequestBody(playerName, message);
                    HttpRequest.Builder builder = HttpRequest.newBuilder()
                            .uri(URI.create(API_URL))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + apiKey)
                            .timeout(Duration.ofSeconds(timeoutSeconds))
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

                    if (!siteUrl.isBlank()) {
                        builder.header("HTTP-Referer", siteUrl);
                    }
                    if (!siteName.isBlank()) {
                        builder.header("X-Title", siteName);
                    }

                    HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        String content = extractMessageContent(response.body());
                        if (content == null || content.isBlank()) {
                            sendError(player, "Received an empty response.");
                            return;
                        }

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                for (String line : wrapLines(content, 220)) {
                                    if (!line.isBlank()) {
                                        player.sendMessage(colorize(responsePrefix + line));
                                    }
                                }
                            }
                        }.runTask(OpenRouterChatPlugin.this);
                    } else {
                        String error = extractErrorMessage(response.body());
                        sendError(player, "OpenRouter error (HTTP " + response.statusCode() + ")" + (error == null ? "." : ": " + error));
                    }
                } catch (java.net.ConnectException e) {
                    sendError(player, "Could not connect to OpenRouter.");
                } catch (java.net.http.HttpTimeoutException e) {
                    sendError(player, "OpenRouter took too long to respond.");
                } catch (IOException e) {
                    sendError(player, "Connection failed: " + e.getMessage());
                    getLogger().warning("Network error: " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    sendError(player, "Request interrupted.");
                } catch (Exception e) {
                    sendError(player, "Something went wrong.");
                    getLogger().severe("Unexpected error: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(this);

        return true;
    }

    private void sendUsage(Player player, String label) {
        player.sendMessage(colorize("&eUsage: /" + label + " <message>"));
        player.sendMessage(colorize("&eAdmin: /" + label + " add-key <api-key>, /" + label + " clear-key, /" + label + " reload, /" + label + " status"));
    }

    private void sendError(Player player, String msg) {
        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage(colorize(errorPrefix + msg));
            }
        }.runTask(this);
    }

    private void reloadSettings() {
        FileConfiguration config = getConfig();
        apiKey = config.getString("api-key", "").trim();
        model = config.getString("model", "openai/gpt-4.1-mini");
        siteUrl = config.getString("site-url", "").trim();
        siteName = config.getString("site-name", "Paper OpenRouter Chat").trim();
        systemPrompt = config.getString("system-prompt", "You are a helpful Minecraft server assistant. Keep answers concise and useful for in-game chat.");
        maxTokens = Math.max(1, config.getInt("max-tokens", 300));
        temperature = config.getDouble("temperature", 0.7D);
        timeoutSeconds = Math.max(5, config.getInt("timeout-seconds", 45));
        responsePrefix = config.getString("response-prefix", "&b[OpenRouter] &f");
        errorPrefix = config.getString("error-prefix", "&c[OpenRouter] &7");
        thinkingMessage = config.getString("thinking-message", "&e[OpenRouter] &7&oThinking...");
        opsOnlyChat = config.getBoolean("ops-only-chat", false);

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 10)))
                .build();
    }

    private String buildRequestBody(String playerName, String message) {
        return "{" +
                "\"model\":\"" + escapeJson(model) + "\"," +
                "\"max_tokens\":" + maxTokens + "," +
                "\"temperature\":" + temperature + "," +
                "\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"}," +
                "{\"role\":\"user\",\"content\":\"Player " + escapeJson(playerName) + " says: " + escapeJson(message) + "\"}" +
                "]}";
    }

    private List<String> wrapLines(String text, int maxLength) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        for (String paragraph : text.split("\\r?\\n")) {
            String remaining = paragraph.trim();
            if (remaining.isEmpty()) {
                continue;
            }
            while (remaining.length() > maxLength) {
                int split = remaining.lastIndexOf(' ', maxLength);
                if (split <= 0) {
                    split = maxLength;
                }
                lines.add(remaining.substring(0, split).trim());
                remaining = remaining.substring(split).trim();
            }
            if (!remaining.isEmpty()) {
                lines.add(remaining);
            }
        }
        return lines;
    }

    private String extractMessageContent(String json) {
        int choicesIndex = json.indexOf("\"choices\"");
        if (choicesIndex < 0) return null;
        int contentIndex = json.indexOf("\"content\"", choicesIndex);
        if (contentIndex < 0) return null;
        return extractJsonStringValue(json, contentIndex);
    }

    private String extractErrorMessage(String json) {
        int messageIndex = json.indexOf("\"message\"");
        if (messageIndex < 0) return null;
        return extractJsonStringValue(json, messageIndex);
    }

    private String extractJsonStringValue(String json, int keyIndex) {
        int colonIdx = json.indexOf(':', keyIndex);
        if (colonIdx < 0) return null;
        int start = json.indexOf('"', colonIdx + 1);
        if (start < 0) return null;
        start++;
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default: sb.append(next); break;
                }
                i += 2;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
                i++;
            }
        }
        return null;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
