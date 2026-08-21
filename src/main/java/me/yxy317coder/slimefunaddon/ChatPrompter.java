package me.yxy317coder.slimefunaddon;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatPrompter implements Listener {

    private static final long TIMEOUT_SECONDS = 60;

    private final Map<UUID, CompletableFuture<String>> pending = new ConcurrentHashMap<>();

    public ChatPrompter(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        CompletableFuture<String> future = pending.get(uuid);
        if (future == null || future.isDone()) {
            return;
        }
        // 私聊回复：拦截消息，不广播到公屏
        event.setCancelled(true);
        String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("cancel")) {
            future.complete(null);
        } else {
            future.complete(message);
        }
        pending.remove(uuid);
    }

    /**
     * 私聊询问玩家 thing 并阻塞等待回复。
     * <ul>
     * <li>玩家在聊天框输入内容 -> 返回该内容（不会广播到公屏）</li>
     * <li>玩家输入 cancel -> 返回 null（取消回复）</li>
     * <li>超过 60 秒未回复 -> 返回 null（自动取消）</li>
     * </ul>
     * 注意：本方法会阻塞调用线程，必须在异步线程中调用，禁止在服务器主线程调用。
     */
    public String Ask(Player player, String thing) {
        UUID uuid = player.getUniqueId();
        if (pending.containsKey(uuid)) {
            player.sendMessage("§c你已有一个正在等待回答的问题，请先输入 §ccancel§c 取消或回答上一个问题");
            return null;
        }
        player.sendMessage("§6" + thing);
        player.sendMessage("§7请在聊天框输入你的回复（输入 §ccancel§7 取消，§7" + TIMEOUT_SECONDS + " 秒内有效）");
        CompletableFuture<String> future = new CompletableFuture<>();
        pending.put(uuid, future);
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            player.sendMessage("§c回复超时，已自动取消");
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            player.sendMessage("§c回复等待被中断，已取消");
            return null;
        } catch (Exception e) {
            player.sendMessage("§c回复等待出错，已取消");
            return null;
        } finally {
            pending.remove(uuid);
        }
    }
}
