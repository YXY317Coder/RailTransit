package me.yxy317coder.slimefunaddon.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.yxy317coder.slimefunaddon.ExampleAddon;

public class RTCommand implements CommandExecutor, TabCompleter {

    private final ExampleAddon addon;

    public RTCommand(ExampleAddon addon) {
        this.addon = addon;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        try{
            switch (args[0].toLowerCase()) {
                case "help":
                    help(sender);
                    return true;
                case "select":
                    select(sender, args);
                    return true;
                case "out":
                    setTime(sender, args, "out");
                    return true;
                case "wait":
                    setTime(sender, args, "wait");
                    return true;
                case "compile":
                    compile(sender, args);
                    return true;
                case "continue":
                    continueCompile(sender, args);
                    return true;
                default:
                    sender.sendMessage("§c未知子命令: " + args[0]);
                    help(sender);
                    return true;
            }
        }
        catch (Exception e){
            sender.sendMessage("§4发生错误: " + e.getClass().getName());
            return true;
        }
    }

    // /railtransfer select [组织id] [线路id]
    private void select(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c仅玩家可执行该指令");
            return;
        }
        if (args.length != 3) {
            sender.sendMessage("§c用法: /railtransfer select [组织id] [线路id]");
            return;
        }
        int orgId;
        try {
            orgId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c组织id必须是数字");
            return;
        }
        addon.selectLine((Player) sender, orgId, args[2]);
    }

    // /railtransfer out|wait [组织id] [线路id]
    private void setTime(CommandSender sender, String[] args, String kind) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c仅玩家可执行该指令");
            return;
        }
        if (args.length != 3) {
            sender.sendMessage("§c用法: /railtransfer " + kind + " [组织id] [线路id]");
            return;
        }
        int orgId;
        try {
            orgId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c组织id必须是数字");
            return;
        }
        if ("out".equals(kind)) {
            addon.setLineOut((Player) sender, orgId, args[2]);
        } else {
            addon.setLineWait((Player) sender, orgId, args[2]);
        }
    }

    // /railtransfer compile [组织id] [线路id]
    private void compile(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c仅玩家可执行该指令");
            return;
        }
        if (args.length != 3) {
            sender.sendMessage("§c用法: /railtransfer compile [组织id] [线路id]");
            return;
        }
        int orgId;
        try {
            orgId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c组织id必须是数字");
            return;
        }
        addon.compileLine((Player) sender, orgId, args[2]);
    }

    // /railtransfer continue [组织id] [线路id]（可省略参数，使用最近编译的线路）
    private void continueCompile(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c仅玩家可执行该指令");
            return;
        }
        if (args.length == 1) {
            // 无参：使用该玩家最近一次右键侧线编译的线路
            addon.continueCompile((Player) sender, -1, null);
            return;
        }
        if (args.length != 3) {
            sender.sendMessage("§c用法: /railtransfer continue [组织id] [线路id]");
            return;
        }
        int orgId;
        try {
            orgId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c组织id必须是数字");
            return;
        }
        addon.continueCompile((Player) sender, orgId, args[2]);
    }

    public void hr(CommandSender sender){
        sender.sendMessage("§4---");
    }

    public void hr(CommandSender sender, String color){
        sender.sendMessage("§" + color + "---");
    }

    private void help(CommandSender sender){
        sender.sendMessage("§9RailTransit §5v1.0.0-beta");
        sender.sendMessage("§aMade by §6317");
        sender.sendMessage("§b轨道交通");
        hr(sender);
        sender.sendMessage("§6/railtransfer select [组织id] [线路id] §7- 选中线路，列车自动运行");
        sender.sendMessage("§6/railtransfer out [组织id] [线路id] §7- 设置回车厂后再次发车时间（秒）");
        sender.sendMessage("§6/railtransfer wait [组织id] [线路id] §7- 设置每站停留时间（秒）");
        sender.sendMessage("§6/railtransfer compile [组织id] [线路id] §7- 编译线路（等待空手右键侧线铁轨）");
        sender.sendMessage("§6/railtransfer continue [组织id] [线路id] §7- 继续编译（移动到未加载区块位置后使用，可省略参数）");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("help");
            completions.add("select");
            completions.add("out");
            completions.add("wait");
            completions.add("compile");
            completions.add("continue");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("select")
                || args[0].equalsIgnoreCase("out") || args[0].equalsIgnoreCase("wait")
                || args[0].equalsIgnoreCase("compile") || args[0].equalsIgnoreCase("continue"))) {
            for (int i = 0; i < 10; i++) {
                completions.add(String.valueOf(i));
            }
        }
        return completions;
    }
}
