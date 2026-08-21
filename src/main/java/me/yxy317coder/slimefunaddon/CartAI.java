package me.yxy317coder.slimefunaddon;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 列车自动驾驶（沿编译路径循环运行）。
 * 启动后取消矿车默认物理 AI（重力/碰撞），由本类逐 tick 沿 compile 路径关键点驱动列车。
 * 路径关键点类型：起点/终点侧线（DETECTOR_RAIL）、拐角、每站位置（ACTIVATOR_RAIL）。
 *
 * 线路信息（组织 id / 线路 id）写入矿车 PersistentDataContainer，避免列车运行后丢失 RT_TRAIN 身份。
 * 循环规则：
 * - 到站（ACTIVATOR_RAIL）停留 wait 秒
 * - 完成一圈回到起点（回厂=侧线 / 环线=第一站）后等待 out 秒再次发车
 * - 所有等待均为逐 tick 倒计时，不阻塞服务器主线程
 */
public class CartAI {

    private final ExampleAddon addon;
    private final NamespacedKey keyOrg;
    private final NamespacedKey keyLine;
    // 运行中的列车：矿车UUID -> 任务
    private final Map<UUID, CartTask> running = new HashMap<>();
    // 每 tick 前进格数（约 7 格/秒）
    private static final double SPEED = 0.5D;

    public CartAI(ExampleAddon addon) {
        this.addon = addon;
        this.keyOrg = new NamespacedKey(addon.getJavaPlugin(), "rt_org");
        this.keyLine = new NamespacedKey(addon.getJavaPlugin(), "rt_line");
    }

    public boolean isRunning(Minecart cart) {
        return cart != null && running.containsKey(cart.getUniqueId());
    }

    /** 判断矿车实体是否为已绑定线路的 RT_TRAIN 列车 */
    public boolean isTrainCart(Minecart cart) {
        if (cart == null || cart.isDead()) {
            return false;
        }
        PersistentDataContainer pdc = cart.getPersistentDataContainer();
        return pdc.has(keyOrg, PersistentDataType.STRING) || pdc.has(keyLine, PersistentDataType.STRING);
    }

    /**
     * 读取列车绑定的线路信息（用于服务器重启后恢复运行）。
     *
     * @param cart 矿车实体
     * @return [组织id, 线路id]；非列车（无 PDC 标记）返回 null
     */
    public String[] getTrainLine(Minecart cart) {
        if (cart == null || cart.isDead() || !isTrainCart(cart)) {
            return null;
        }
        PersistentDataContainer pdc = cart.getPersistentDataContainer();
        String org = pdc.get(keyOrg, PersistentDataType.STRING);
        String line = pdc.get(keyLine, PersistentDataType.STRING);
        if (org == null || line == null) {
            return null;
        }
        return new String[] { org, line };
    }

    /**
     * 让列车沿编译路径循环运行，并把线路信息绑定到列车实体。
     *
     * @param cart       列车实体（矿车）
     * @param path       编译路径关键点：["x,y,z", ...]，首个为起点侧线位置
     * @param back       是否回厂（true=单行线，false=环线）
     * @param outTicks   回车厂（完成一圈）后再次发车所需 tick（1 秒 = 20 tick）
     * @param waitTicks  每站停留 tick
     * @param orgId      组织 id（绑定到列车实体）
     * @param lineId     线路 id（绑定到列车实体）
     * @param startIndex 起始路径索引（0=起点侧线；环线从非侧线关键点启动时为该点索引）
     * @return 是否成功启动（参数非法或已在运行返回 false）
     */
    public boolean start(Minecart cart, List<int[]> path, boolean back, int outTicks, int waitTicks,
            int orgId, String lineId, int startIndex) {
        // addon.getLogger().info("[CartAI] start called: cart=" + (cart == null ?
        // "null" : cart.getUniqueId())
        // + " pathSize=" + (path == null ? "null" : path.size()) + " back=" + back
        // + " outTicks=" + outTicks + " waitTicks=" + waitTicks + " org=" + orgId + "
        // line=" + lineId);
        if (cart == null || cart.isDead() || path == null || path.size() < 2) {
            // addon.getLogger().warning("[CartAI] start rejected: cartNull=" + (cart ==
            // null)
            // + " cartDead=" + (cart != null && cart.isDead())
            // + " pathNull=" + (path == null) + " pathSize=" + (path == null ? 0 :
            // path.size()));
            return false;
        }
        UUID uid = cart.getUniqueId();
        if (running.containsKey(uid)) {
            // addon.getLogger().warning("[CartAI] start rejected: already running " + uid);
            return false;
        }
        // 线路信息绑定到列车实体（防止运行后丢失 RT_TRAIN 身份）
        PersistentDataContainer pdc = cart.getPersistentDataContainer();
        pdc.set(keyOrg, PersistentDataType.STRING, String.valueOf(orgId));
        pdc.set(keyLine, PersistentDataType.STRING, lineId);
        // 取消矿车默认 AI：关闭重力，交由本类逐 tick 驱动
        cart.setGravity(false);
        CartTask task = new CartTask(cart, path, back, outTicks, waitTicks, startIndex);
        task.bukkitTask = new CartRunnable(task).runTaskTimer(addon.getJavaPlugin(), 0L, 1L);
        running.put(uid, task);
        // addon.getLogger().info("[CartAI] start OK, scheduled runnable for " + uid
        // + " startIndex=" + startIndex);
        return true;
    }

    /** 停止指定列车的 AI（恢复默认物理） */
    public void stop(UUID uid) {
        if (uid == null) {
            return;
        }
        CartTask task = running.remove(uid);
        if (task == null) {
            return;
        }
        if (task.bukkitTask != null) {
            task.bukkitTask.cancel();
        }
        if (task.cart != null && !task.cart.isDead()) {
            task.cart.setGravity(true);
        }
    }

    private static class CartTask {
        final Minecart cart;
        final List<int[]> path;
        final boolean back;
        final int outTicks;
        final int waitTicks;
        int targetIndex; // 当前目标点索引（0 为起点侧线；环线从匹配项开始时=匹配索引）
        int restartIndex; // 完成一圈后的重启索引（回厂=1；环线=第一站首次出现位置，不再绕回侧线）
        boolean skipArrival = false; // 环线重启点与当前所在站同坐标时，跳过重复停靠
        int stallLeft = 0; // 停留剩余 tick（站停 wait / 回车厂 out）
        boolean lapDone = false; // 已完成一圈（回到起点，等待 out 后重新发车）
        BukkitTask bukkitTask;
        int stuckTicks = 0;
        Location lastTickPos = null;
        int terminalStationIndex = -1; // 末站关键点索引（回厂=最后一个站；环线=回第一站前的最后一站），-1 表示未识别

        CartTask(Minecart cart, List<int[]> path, boolean back, int outTicks, int waitTicks, int startIndex) {
            this.cart = cart;
            this.path = path;
            this.back = back;
            this.outTicks = outTicks;
            this.waitTicks = waitTicks;
            this.targetIndex = startIndex;
            // 循环重启索引：回厂=侧线后第一个关键点(1)；环线=路径末点（第一站）首次出现位置
            this.restartIndex = 1;
            if (!back && !path.isEmpty()) {
                int[] last = path.get(path.size() - 1);
                for (int i = 0; i < path.size(); i++) {
                    int[] p = path.get(i);
                    if (p[0] == last[0] && p[1] == last[1] && p[2] == last[2]) {
                        this.restartIndex = i;
                        break;
                    }
                }
            }
            // 识别末站：回厂=最后一个站台关键点；环线=倒数第二个站台关键点（最后一个为终点第一站）
            if (cart.getWorld() != null) {
                List<Integer> stationIdx = new ArrayList<>();
                for (int i = 0; i < path.size(); i++) {
                    int[] p = path.get(i);
                    if (cart.getWorld().getBlockAt(p[0], p[1], p[2]).getType() == Material.ACTIVATOR_RAIL) {
                        stationIdx.add(i);
                    }
                }
                if (!stationIdx.isEmpty()) {
                    int lastIdx = stationIdx.get(stationIdx.size() - 1);
                    this.terminalStationIndex = back ? lastIdx
                            : (stationIdx.size() >= 2 ? stationIdx.get(stationIdx.size() - 2) : lastIdx);
                }
            }
        }
    }

    private Reader loadStationReader() throws IOException {
        File file = new File(this.addon.getJavaPlugin().getDataFolder(), "station.json");
        if (file.exists()) {
            return new FileReader(file);
        }
        java.io.InputStream in = this.addon.getJavaPlugin().getResource("station.json");
        if (in != null) {
            return new java.io.InputStreamReader(in, StandardCharsets.UTF_8);
        }
        throw new IOException("station.json 不存在且 jar 内无 station.json");
    }

    private JsonObject loadStationJson() {
        Gson gson = new Gson();
        JsonElement station_root = null;
        try (Reader reader = loadStationReader()) {
            station_root = gson.fromJson(reader, JsonElement.class);
        } catch (IOException e) {
            throw new RuntimeException("无法加载配置文件", e);
        }
        return station_root.getAsJsonObject();
    }

    private class CartRunnable extends BukkitRunnable {
        private final CartTask task;
        private int tickCount = 0;

        CartRunnable(CartTask task) {
            this.task = task;
        }

        private String vall(World world, Location location) {
            JsonObject station_json = loadStationJson();
            JsonArray platforms = station_json.getAsJsonArray("platform");
            for (int i = 0; i < platforms.size(); i++) {
                JsonObject pl = platforms.get(i).getAsJsonObject();
                JsonObject pos = pl.getAsJsonObject("position");
                // 站台关键点中心 = 方块坐标 + 0.5 / +0.05（与 run() 中 goal 构造保持一致）
                double tx = pos.get("x").getAsInt() + 0.5D;
                double ty = pos.get("y").getAsInt() + 0.05D;
                double tz = pos.get("z").getAsInt() + 0.5D;
                double dx = tx - location.getX();
                double dy = ty - location.getY();
                double dz = tz - location.getZ();
                if (Math.sqrt(dx * dx + dy * dy + dz * dz) < 0.4D) {
                    return pl.has("name") ? pl.get("name").getAsString() : ("#" + i);
                }
            }
            return null;
        }

        private boolean isLoopline() {
            JsonObject station_json = loadStationJson();
            JsonArray platforms = station_json.getAsJsonArray("platform");
            if (platforms.size() < 2) {
                return false;
            }
            // 环线 = 首尾站台坐标相同（比较 position 坐标，而非 JsonObject 引用）
            JsonObject first = platforms.get(0).getAsJsonObject().getAsJsonObject("position");
            JsonObject last = platforms.get(platforms.size() - 1).getAsJsonObject().getAsJsonObject("position");
            return first.get("x").getAsInt() == last.get("x").getAsInt()
                    && first.get("y").getAsInt() == last.get("y").getAsInt()
                    && first.get("z").getAsInt() == last.get("z").getAsInt();
        }

        @Override
        public void run() {
            Minecart cart = task.cart;
            if (cart == null || cart.isDead() || !cart.isValid() || cart.getWorld() == null) {
                // addon.getLogger().warning("[CartAI] runnable stopped: cartNull=" + (cart ==
                // null)
                // + " dead=" + (cart != null && cart.isDead())
                // + " valid=" + (cart != null && cart.isValid())
                // + " worldNull=" + (cart == null || cart.getWorld() == null));
                stop(cart == null ? null : cart.getUniqueId());
                return;
            }
            tickCount++;
            if (tickCount % 20 == 0) {
                Location l = cart.getLocation();
                // addon.getLogger().info("[CartAI] tick=" + tickCount + " pos=(" +
                // String.format("%.2f", l.getX())
                // + "," + String.format("%.2f", l.getY()) + "," + String.format("%.2f",
                // l.getZ())
                // + ") target=" + task.targetIndex + "/" + task.path.size()
                // + " stall=" + task.stallLeft + " lapDone=" + task.lapDone);
            }
            // 停留（站停 wait / 回车厂 out）：逐 tick 倒计时，不阻塞主线程
            if (task.stallLeft > 0) {
                task.stallLeft--;
                return;
            }
            if (task.lapDone) {
                // 一圈完成：等待 out 结束，重新从 restartIndex 发车（环线跳到第一站首次出现位置）
                task.lapDone = false;
                task.targetIndex = task.restartIndex;
                // 重启点与当前所在站同坐标时，跳过即时的重复停靠
                if (task.targetIndex < task.path.size()) {
                    int[] restartPt = task.path.get(task.targetIndex);
                    int[] lastPt = task.path.get(task.path.size() - 1);
                    if (restartPt[0] == lastPt[0] && restartPt[1] == lastPt[1] && restartPt[2] == lastPt[2]) {
                        task.skipArrival = true;
                    }
                }
                task.stuckTicks = 0;
                task.lastTickPos = null;
                return;
            }
            if (task.targetIndex >= task.path.size()) {
                // 到达终点（回厂=侧线 / 环线=第一站）：等待 out 后重新发车（循环运行）
                task.stallLeft = task.outTicks;
                task.lapDone = true;
                task.stuckTicks = 0;
                task.lastTickPos = null;
                return;
            }
            int[] target = task.path.get(task.targetIndex);
            Location loc = cart.getLocation();
            Location goal = new Location(cart.getWorld(), target[0] + 0.5D, target[1] + 0.05D, target[2] + 0.5D);
            double dx = goal.getX() - loc.getX();
            double dy = goal.getY() - loc.getY();
            double dz = goal.getZ() - loc.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 0.4D) {
                // 到达关键点：站停铁轨停 wait 秒（skipArrival 时跳过本次重复停靠）
                Material type = cart.getWorld().getBlockAt(target[0], target[1], target[2]).getType();
                if (type == Material.ACTIVATOR_RAIL && !task.skipArrival) {
                    task.stallLeft = task.waitTicks;
                }
                task.skipArrival = false;
                task.targetIndex++;
                task.stuckTicks = 0;
                task.lastTickPos = null;
                List<Entity> arr = cart.getPassengers();
                boolean isTerminal = task.targetIndex == task.terminalStationIndex;
                String val = vall(cart.getWorld(), loc);
                boolean loopline = isLoopline();
                if (val == null) {
                    // 非站台关键点（拐角等）：推进位置但不发到站消息
                    return;
                }
                for (Integer i = 0; i < arr.size(); i++) {
                    if (arr.get(i).getType().equals(EntityType.PLAYER)) {
                        arr.get(i).sendMessage("§6到达站点：§a" + val); // 到站发送信息
                        if (isTerminal && loopline) {
                            arr.get(i).sendMessage("§c本趟列车已到达终点站，请带好随身物品下车");
                        }
                    }
                }
                return;
            }
            // 卡死兜底：位置超过 1 秒（20 tick）没有明显前进，说明列车位于错误位置
            // （如岔路误 tp 到另一方向后仍想往原方向前进），直接强制 tp 到当前目标点。
            if (task.lastTickPos != null) {
                double moved = loc.distance(task.lastTickPos);
                if (moved < 0.05D) {
                    task.stuckTicks++;
                } else {
                    task.stuckTicks = 0;
                }
            }
            task.lastTickPos = loc.clone();
            if (task.stuckTicks >= 20) {
                // addon.getLogger().warning("[CartAI] STUCK " + task.stuckTicks + " ticks at ("
                // + String.format("%.2f", loc.getX()) + "," + String.format("%.2f", loc.getY())
                // + ","
                // + String.format("%.2f", loc.getZ()) + ") force tp to target=" +
                // task.targetIndex);
                Location above = goal.clone();
                above.setY(above.getY() + 1.0D);
                cart.teleport(above);
                cart.teleport(goal);
                cart.setVelocity(new Vector(0, 0, 0));
                task.stuckTicks = 0;
                task.lastTickPos = null;
                return;
            }
            // 沿路径推进
            double step = Math.min(SPEED, dist);
            loc.add(dx / dist * step, dy / dist * step, dz / dist * step);
            // 先抬离铁轨平面再落到目标：绕过 Minecraft 矿车铁轨方向锁定。
            // 矿车在铁轨上会被物理强制拉回铁轨朝向，与 teleport 方向不一致时
            // （如站台 north_south 上向西行驶）teleport 会被抵消导致卡死。
            // 先 teleport 到上方空气格脱离铁轨碰撞，再落到目标位置即可正常推进。
            Location above = loc.clone();
            above.setY(above.getY() + 1.0D);
            cart.teleport(above);
            cart.teleport(loc);
            // 清零速度：避免弯道物理把矿车推向错误方向，与 teleport 形成拉锯
            cart.setVelocity(new Vector(0, 0, 0));
        }
    }
}
