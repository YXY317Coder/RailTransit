package me.yxy317coder.slimefunaddon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * 列车寻路（编译）引擎。
 * 寻路规则：
 * - 只能通过铁轨材质寻路（普通/高速/站停/侧线），不区分是否插件铁轨
 * - 寻路防绕圈：同一条无向边不允许重复走（visitedEdges 去重），普通节点允许重复经过
 * （分叉/往返式布局中，去往下一站或回程必然要再次经过去程走过的汇合节点，
 * 只拦边不拦节点即可防 1912<->1913 式往返振荡，又不会误杀合法分叉回程）
 * - 站台判定：station.json 的 platform 中有该坐标，且该坐标是站停铁轨（ACTIVATOR_RAIL）
 * - 是否回厂(back)：
 * true -> 单行线，目标列表 [站1..站n]，从侧线出发，最后还要寻回侧线（完整记录回侧线路段的拐角）
 * false -> 环线，目标列表 [站1..站n, 站1]，从侧线出发，最后一站后寻路回第一站
 * - DFS：遇到目标列表上的当前项（站台编号相同）就往后移一位；
 * 到达已标记站点但非当前期望目标时直接剪枝（不允许在到达目标站点之前经历其它站点）；
 * 未标记站点的站停铁轨与普通/高速铁轨可作为路线一部分通行；
 * 站台节点与起点侧线允许重复经过（环线需要回到第一站、回厂需要回到侧线）
 */
public class CartRunning {

    /**
     * 编译寻路：从玩家选择的侧线铁轨出发，按线路站点顺序走完全程。
     *
     * @param startSide 玩家选择的侧线铁轨（线路起点/终点）
     * @param line      station.json 中的线路 JsonObject（含 station / back）
     * @param platforms station.json 中的 platform 数组
     * @return 是否按顺序走完全程（true 表示编译成功）
     */
    public boolean compile(Block startSide, JsonObject line, JsonArray platforms) {
        return !compilePath(startSide, line, platforms, null, null).isEmpty();
    }

    /**
     * 编译寻路（带调试输出）。
     *
     * @param startSide 玩家选择的侧线铁轨（线路起点/终点）
     * @param line      station.json 中的线路 JsonObject（含 station / back）
     * @param platforms station.json 中的 platform 数组
     * @param debugLog  控制台调试输出（每一步 DFS 的 x,y,z），可为 null
     * @param failMsg   失败原因回调（输出到聊天框），可为 null
     * @return 是否按顺序走完全程（true 表示编译成功）
     */
    public boolean compile(Block startSide, JsonObject line, JsonArray platforms,
            Consumer<String> debugLog, Consumer<String> failMsg) {
        return !compilePath(startSide, line, platforms, debugLog, failMsg).isEmpty();
    }

    /**
     * 编译寻路并返回路径关键点列表（侧线起点、拐角、每站位置、终点侧线）。
     * 返回空列表表示编译失败或未编译。
     *
     * @param startSide 玩家选择的侧线铁轨（线路起点/终点）
     * @param line      station.json 中的线路 JsonObject（含 station / back）
     * @param platforms station.json 中的 platform 数组
     * @param debugLog  控制台调试输出（每一步 DFS 的 x,y,z），可为 null
     * @param failMsg   失败原因回调（输出到聊天框），可为 null
     * @return 路径关键点列表；编译失败返回空列表
     */
    public List<Block> compilePath(Block startSide, JsonObject line, JsonArray platforms,
            Consumer<String> debugLog, Consumer<String> failMsg) {
        List<Block> path = new ArrayList<>();
        if (startSide == null || !isSideRail(startSide)) {
            if (failMsg != null) {
                failMsg.accept("§c编译失败：选择的方块变滚木了");
            }
            return path;
        }
        JsonArray stationArr = line.getAsJsonArray("station");
        List<Integer> targets = new ArrayList<>();
        for (int i = 0; i < stationArr.size(); i++) {
            targets.add(stationArr.get(i).getAsInt());
        }
        if (targets.isEmpty()) {
            if (failMsg != null) {
                failMsg.accept("§c编译失败：线路还没有任何站台");
            }
            return path;
        }
        // 是否回厂（默认开启）
        boolean back = !line.has("back") || line.get("back").getAsBoolean();
        // 目标列表：回厂=全部站台；不回厂（环线）=全部站台 + 第一站
        List<Integer> goals = new ArrayList<>(targets);
        if (!back) {
            goals.add(targets.get(0));
        }
        // 站台坐标 -> 站台编号（platform 数组索引）；站台编号 -> 名称
        Map<String, Integer> platformIndex = new HashMap<>();
        Map<Integer, String> platformNames = new HashMap<>();
        for (int i = 0; i < platforms.size(); i++) {
            JsonObject pl = platforms.get(i).getAsJsonObject();
            JsonObject pos = pl.getAsJsonObject("position");
            platformIndex.put(pos.get("x").getAsInt() + "," + pos.get("y").getAsInt() + "," + pos.get("z").getAsInt(),
                    i);
            platformNames.put(i, pl.has("name") ? pl.get("name").getAsString() : ("#" + i));
        }
        if (debugLog != null) {
            debugLog.accept("[DFS] 开始编译：起点侧线 (" + blockKey(startSide) + ")，目标列表 " + goals
                    + "，回厂=" + back);
        }
        CompileCtx ctx = new CompileCtx(goals, platformIndex, platformNames, startSide, back, debugLog, failMsg, path);
        path.add(startSide); // 路径起点：侧线铁轨位置
        boolean ok = dfs(startSide, null, null, 0, startSide, ctx);
        if (!ok && failMsg != null) {
            if (!ctx.unloadedChunks.isEmpty()) {
                // 优先提示未加载区块：指引玩家移动到该位置加载区块后输入 /railtransfer continue 继续编译，
                // 而不是笼统报编译失败、编译终止
                StringBuilder sb = new StringBuilder();
                sb.append("§e编译暂停：检测到未加载区块，请移动到以下位置附近（区块加载后）输入 §f/railtransfer continue §e继续编译：");
                int count = 0;
                for (String c : ctx.unloadedChunks) {
                    if (count >= 5) {
                        sb.append(" 等");
                        break;
                    }
                    sb.append(" §f(").append(c).append(")");
                    count++;
                }
                failMsg.accept(sb.toString());
            } else if (ctx.failInfo.isEmpty()) {
                failMsg.accept("§c编译失败：从侧线出发未找到可行路径，请检查铁轨连接");
            } else {
                failMsg.accept("§c编译失败：在 " + ctx.failInfo.get(0) + " 处无法继续，请检查铁轨连接");
            }
        }
        if (debugLog != null) {
            debugLog.accept("[DFS] 编译" + (ok ? "成功" : "失败") + "：" + blockKey(startSide));
        }
        // 编译失败必须返回空列表：path 至少含起点侧线，不能作为成功标志（否则会误报编译成功）
        return ok ? path : new ArrayList<>();
    }

    private static final int MAX_DFS_NODES = 50000; // 节点上限：防止 DFS 遍历爆炸导致服务器卡死

    // 编译上下文：承载 DFS 全程不变的状态集合。
    // 打包成单个对象传递，可显著减小递归栈帧，配合 MAX_DFS_DEPTH 兜底，
    // 避免长线路直线铁轨（候选恒为 1）递归过深导致 StackOverflowError、
    // 任务异常中断而丢失"编译成功/失败"收尾消息。
    private static final class CompileCtx {
        final List<Integer> goals;
        final Map<String, Integer> platformIndex;
        final Map<Integer, String> platformNames;
        final Set<String> visitedEdges = new HashSet<>();
        final Set<String> visitedNodes = new HashSet<>();
        final Block startSide;
        final boolean back;
        final Consumer<String> debugLog;
        final Consumer<String> msgOut;
        final List<String> failInfo = new ArrayList<>();
        final List<Block> path;
        final int[] nodeCounter = new int[1];
        final Set<String> unloadedChunks = new LinkedHashSet<>();

        CompileCtx(List<Integer> goals, Map<String, Integer> platformIndex, Map<Integer, String> platformNames,
                Block startSide, boolean back, Consumer<String> debugLog, Consumer<String> msgOut, List<Block> path) {
            this.goals = goals;
            this.platformIndex = platformIndex;
            this.platformNames = platformNames;
            this.startSide = startSide;
            this.back = back;
            this.debugLog = debugLog;
            this.msgOut = msgOut;
            this.path = path;
        }
    }

    private boolean dfs(Block current, Block prev, int[] prevDir, int targetIdx, Block segStart, CompileCtx ctx) {
        // 直线推进优化（防爆栈）：单线铁轨（3*3*3 邻域铁轨数 <= 2 且仅一条未走边）直接迭代推进，
        // 不递归深入；仅在岔路口（邻域铁轨数 > 2）或候选 >= 2 时才递归。
        // 递归深度与铁轨总格数解耦，只与岔路嵌套层数相关，长直线线路不再 StackOverflowError。
        // straight 记录直线段经过的 (节点, 提交点mark)，分支失败回溯时逆序清理边/节点/路径点。
        List<Object[]> straight = new ArrayList<>();
        while (true) {
        if (++ctx.nodeCounter[0] > MAX_DFS_NODES) {
            if (ctx.failInfo.isEmpty()) {
                ctx.failInfo.add("已遍历超过 " + MAX_DFS_NODES + " 个铁轨节点仍未完成，疑似铁轨网络过大或存在异常连接");
            }
            return false;
        }
        int mark = ctx.path.size();
        // 标记当前节点已访问（回溯时移除）；仅环线解除标记阶段需修改 visitedNodes，不做拦截
        ctx.visitedNodes.add(blockKey(current));
        // 路径稀疏化：只记录关键点（侧线起点/终点、拐角、每站位置），不逐格记录每个铁轨节点。
        // 拐角/站台/终点侧线由下方 addPathPoint 记录；入口不再逐格记录，避免千级长线路 compile 数组膨胀
        // 更新当前路段起点：到达侧线后，新路段从此侧线出发
        if (isSideRail(current)) {
            segStart = current; // 从侧线出发的路段：报错/剪枝位置指向侧线
        }
        // 当前块命中目标列表当前项（站停铁轨且站台编号相同）-> 目标后移一位
        Integer curIdx = ctx.platformIndex.get(blockKey(current));
        boolean hitStation = false;
        if (curIdx != null && current.getType() == Material.ACTIVATOR_RAIL) {
            if (targetIdx < ctx.goals.size() && curIdx.intValue() == ctx.goals.get(targetIdx)) {
                targetIdx++;
                hitStation = true;
                segStart = current; // 新路段起点 = 该站点
                addPathPoint(ctx.path, current); // 每站位置
                // 站点命中是"当前帧"的确定结果，分支失败回滚时不能被一并删除：
                // 将 mark 推进到站点记录之后，后续 rollbackPath 只回滚分支内新增的拐角点
                mark = ctx.path.size();
                // 向玩家实时输出到达站点信息，便于排查
                if (ctx.msgOut != null) {
                    ctx.msgOut.accept("§a[编译] 已到达站点 #" + curIdx + "("
                            + ctx.platformNames.getOrDefault(curIdx, "?") + ") 进度 "
                            + targetIdx + "/" + ctx.goals.size());
                }
                // 环线最后阶段：刚命中倒数第二个目标（下一目标是回到第一站）。
                // 只在此刻一次性解除"侧线 -> 第一站"段的去重标记（第一站节点 + 含侧线的边），
                // 使回程可复用该段；此后恢复严格去重，防止普通铁轨在最后阶段无限往返
                if (!ctx.back && targetIdx == ctx.goals.size() - 1
                        && ctx.goals.get(targetIdx).intValue() == ctx.goals.get(0).intValue()) {
                    for (Map.Entry<String, Integer> e : ctx.platformIndex.entrySet()) {
                        if (e.getValue().intValue() == ctx.goals.get(0).intValue()) {
                            ctx.visitedNodes.remove(e.getKey()); // 解除第一站节点标记
                            break;
                        }
                    }
                    // 精确解除"侧线 -> 第一站"段边标记：仅删除同时含侧线坐标与第一站坐标的边键，
                    // 避免误删其它普通铁轨边（如 1912-1913）导致无限往返
                    String sideKey = blockKey(ctx.startSide);
                    String firstKey = null;
                    for (Map.Entry<String, Integer> e : ctx.platformIndex.entrySet()) {
                        if (e.getValue().intValue() == ctx.goals.get(0).intValue()) {
                            firstKey = e.getKey();
                            break;
                        }
                    }
                    if (firstKey != null) {
                        final String fk = firstKey;
                        ctx.visitedEdges.removeIf(ek -> {
                            String[] parts = ek.split("->");
                            if (parts.length != 2) {
                                return false;
                            }
                            boolean hasSide = parts[0].equals(sideKey) || parts[1].equals(sideKey);
                            boolean hasFirst = parts[0].equals(fk) || parts[1].equals(fk);
                            return hasSide && hasFirst;
                        });
                    }
                }
            }
            // 到达已标记站点但不是当前期望目标：仅"目标已耗尽的最终路段"才剪枝——
            // 侧线出发路段允许穿过非预期站点（如线路起点与第一站之间夹着其它站台，
            // 强制剪枝会导致 DFS 绕路，把第一站挤到路径末尾）；
            // 站与站之间的路段允许任意铁轨，不剪枝
            if (!hitStation && targetIdx >= ctx.goals.size()) {
                if (ctx.debugLog != null) {
                    ctx.debugLog.accept("[DFS] 剪枝：(" + blockKey(segStart) + ") 路段遇非期望站点 #" + curIdx);
                }
                if (ctx.failInfo.isEmpty()) {
                    // 报错指向路段起点（侧线），而非到达的非期望站点或中间铁轨
                    ctx.failInfo.add("(" + blockKey(segStart) + ") 经过非期望站点 #" + curIdx);
                }
                cleanupStraight(ctx, straight, current);
                ctx.visitedNodes.remove(blockKey(current));
                rollbackPath(ctx.path, mark);
                return false;
            }
        }
        if (ctx.debugLog != null) {
            String expect;
            if (targetIdx < ctx.goals.size()) {
                expect = "站台 #" + ctx.goals.get(targetIdx);
            } else if (ctx.back) {
                expect = "侧线";
            } else {
                expect = "完成";
            }
            ctx.debugLog.accept("[DFS] 到达 (" + blockKey(current) + ") 类型=" + current.getType().name()
                    + " 期望=" + expect + " 进度=" + targetIdx + "/" + ctx.goals.size());
        }
        // 全部目标完成
        if (targetIdx == ctx.goals.size()) {
            if (!ctx.back) {
                return true; // 环线：最后已回到第一站
            }
            // 回厂：当前节点已是起点侧线 -> 回厂完成，记录终点侧线
            if (sameBlock(current, ctx.startSide)) {
                addPathPoint(ctx.path, current); // 终点侧线
                return true;
            }
            // 尚未到侧线，继续寻路
        }
        // 收集邻居与候选（仅边去重防振荡，节点允许重复经过；分叉/往返式布局需要如此）
        List<Block> neighbors = getRailNeighbors(current, ctx.unloadedChunks);
        List<Block> candidates = new ArrayList<>();
        for (Block nb : neighbors) {
            String ek = edgeKey(current, nb);
            if (ctx.visitedEdges.contains(ek)) {
                continue;
            }
            candidates.add(nb);
        }
        if (ctx.debugLog != null) {
            ctx.debugLog.accept("[DFS] (" + blockKey(current) + ") 邻居 " + neighbors.size() + " 候选 "
                    + candidates.size() + " 个：" + candidates.stream().map(CartRunning::blockKey)
                            .collect(java.util.stream.Collectors.joining(" | ")));
        }
        // 死路：无未走边
        if (candidates.isEmpty()) {
            if (ctx.failInfo.isEmpty()) {
                if (targetIdx < ctx.goals.size()) {
                    ctx.failInfo.add("(" + blockKey(current) + ") 期望下一目标站台 #" + ctx.goals.get(targetIdx));
                } else if (ctx.back) {
                    ctx.failInfo.add("(" + blockKey(current) + ") 期望寻回侧线");
                }
            }
            cleanupStraight(ctx, straight, current);
            ctx.visitedNodes.remove(blockKey(current));
            rollbackPath(ctx.path, mark);
            return false;
        }
        // 直线推进：仅一条未走边（候选==1）时迭代推进，不递归。
        // 节点有 2 个邻居时不算岔路口：其中一个邻居是上一个节点（该边已标记访问），
        // 实际只有 1 条可走边，直接直线推进；仅初始点（玩家指定位置，两个邻居都没去过）
        // 候选==2 时才需要递归尝试两个方向。邻居数 >2 但只有 1 条可走边同样直线推进。
        if (candidates.size() == 1) {
            Block nb = candidates.get(0);
            String ek = edgeKey(current, nb);
            ctx.visitedEdges.add(ek);
            straight.add(new Object[] { current, mark });
            int[] dir = dirBetween(current, nb);
            // 拐角记录：移动方向发生变化时记录当前点（拐角位置）
            if (prevDir != null && dir != null && !sameDir(dir, prevDir)) {
                addPathPoint(ctx.path, current);
            }
            if (ctx.debugLog != null) {
                ctx.debugLog.accept("[DFS] 尝试 " + blockKey(current) + " -> " + blockKey(nb)
                        + " 进度=" + targetIdx + "/" + ctx.goals.size());
            }
            prev = current;
            current = nb;
            prevDir = dir;
            continue;
        }
        // 岔路口（邻域铁轨数 > 2 或候选 >= 2）：递归分支
        boolean ok = false;
        for (Block nb : candidates) {
            String ek = edgeKey(current, nb);
            ctx.visitedEdges.add(ek);
            int[] dir = dirBetween(current, nb);
            // 拐角记录：移动方向发生变化时记录当前点（拐角位置）
            if (prevDir != null && dir != null && !sameDir(dir, prevDir)) {
                addPathPoint(ctx.path, current);
            }
            if (ctx.debugLog != null) {
                ctx.debugLog.accept("[DFS] 尝试 " + blockKey(current) + " -> " + blockKey(nb)
                        + " 进度=" + targetIdx + "/" + ctx.goals.size());
            }
            if (dfs(nb, current, dir, targetIdx, segStart, ctx)) {
                ok = true;
                break;
            }
            if (ctx.debugLog != null) {
                ctx.debugLog.accept("[DFS] 分支失败 " + blockKey(nb) + "，回溯到 " + blockKey(current)
                        + " 进度=" + targetIdx + "/" + ctx.goals.size());
            }
            ctx.visitedEdges.remove(ek);
            rollbackPath(ctx.path, mark); // 分支失败，回滚该分支记录的路径点
        }
        if (ok) {
            return true;
        }
        // 分支全失败：清理直线推进段 + 当前节点
        cleanupStraight(ctx, straight, current);
        ctx.visitedNodes.remove(blockKey(current));
        rollbackPath(ctx.path, mark);
        return false;
        }
    }

    // 直线推进段回溯清理：逆序移除推进时标记的边/节点，并回滚各自提交点之后的路径点
    private void cleanupStraight(CompileCtx ctx, List<Object[]> straight, Block current) {
        for (int i = straight.size() - 1; i >= 0; i--) {
            Block sc = (Block) straight.get(i)[0];
            int commitMark = (Integer) straight.get(i)[1];
            Block scNext = (i + 1 < straight.size()) ? (Block) straight.get(i + 1)[0] : current;
            ctx.visitedEdges.remove(edgeKey(sc, scNext));
            ctx.visitedNodes.remove(blockKey(sc));
            rollbackPath(ctx.path, commitMark);
        }
    }

    // 回滚路径到指定长度（用于 DFS 失败分支）
    private static void rollbackPath(List<Block> path, int mark) {
        while (path.size() > mark) {
            path.remove(path.size() - 1);
        }
    }

    private Integer abs(Integer x){
        if (x < 0){
            return -x;
        }
        return x;
    }

    // 相邻的插件铁轨（26 方向：含斜向/对角连接）
    private List<Block> getRailNeighbors(Block block, Set<String> unloadedChunks) {
        List<Block> list = new ArrayList<>(26);
        // z 优先于 x，使 DFS 优先尝试垂直于 x 方向的路径（如侧线后沿 z 轴直达第一站），
        // 避免先走 x 方向绕路导致回溯删除站点记录；z 从 +1 开始让正向优先
        for (int dz = 1; dz >= -1; dz--) {
            for (int dy = 1; dy >= -1; dy--) {
                for (int dx = 1; dx >= -1; dx--) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    if (abs(dx) + abs(dy) + abs(dz) != 1){
                        continue;
                    }
                    Block b = block.getRelative(dx, dy, dz);
                    // 未加载区块的铁轨直接跳过：getType() 会强制同步加载区块，
                    // 若线路跨未加载区块会被 DFS 引发区块加载风暴，拖死服务器主线程
                    if (!b.getWorld().isChunkLoaded(b.getX() >> 4, b.getZ() >> 4)) {
                        if (unloadedChunks != null) {
                            unloadedChunks.add(b.getX() + "," + b.getY() + "," + b.getZ());
                        }
                        continue;
                    }
                    // 按用户要求：不校验铁轨朝向（shape），相邻铁轨一律视为可走。
                    // 否则分叉处铁轨朝向与连接不匹配会被判定为非法，导致分叉分支不可通行
                    if (isRail(b)) {
                        // 排除对角跳跃：矿车无法跨方块对角移动；但 abs(dx)+abs(dy)+abs(dz)!=1
                        // 已在上面过滤掉对角，这里仅保留铁轨材质校验
                        list.add(b);
                    }
                }
            }
        }
        return list;
    }

    // 路径关键点追加（与最后一个点相同时跳过，避免连续重复；终点侧线可与起点相同但保留）
    private static void addPathPoint(List<Block> path, Block b) {
        if (path.isEmpty() || !sameBlock(path.get(path.size() - 1), b)) {
            path.add(b);
        }
    }

    // 两方块间的方向（仅当相邻返回），否则 null
    private static int[] dirBetween(Block from, Block to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        if (Math.abs(dx) > 1 || Math.abs(dy) > 1 || Math.abs(dz) > 1) {
            return null;
        }
        return new int[] { dx, dy, dz };
    }

    private static boolean sameDir(int[] a, int[] b) {
        return a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
    }

    public ItemStack getNamedItem(ItemStack thing, String name, @Nullable List<String> lore) {
        ItemStack item = thing; // copy or new ItemStack(Material.AIR)
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // 是否为铁轨（按材质判断：普通/高速/站停/侧线，不区分是否插件铁轨）
    public boolean isRail(Block block) {
        if (block == null) {
            return false;
        }
        Material type = block.getType();
        return type == Material.RAIL
                || type == Material.POWERED_RAIL
                || type == Material.ACTIVATOR_RAIL
                || type == Material.DETECTOR_RAIL;
    }

    // 是否为侧线铁轨（探测铁轨）
    public boolean isSideRail(Block block) {
        return block != null && block.getType() == Material.DETECTOR_RAIL;
    }

    private static boolean sameBlock(Block a, Block b) {
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ()
                && a.getWorld().getName().equals(b.getWorld().getName());
    }

    private static String blockKey(Block b) {
        return b.getX() + "," + b.getY() + "," + b.getZ();
    }

    private static String edgeKey(Block a, Block b) {
        String ka = blockKey(a);
        String kb = blockKey(b);
        return ka.compareTo(kb) <= 0 ? ka + "->" + kb : kb + "->" + ka;
    }
}
