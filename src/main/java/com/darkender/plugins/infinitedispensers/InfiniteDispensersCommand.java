package com.darkender.plugins.infinitedispensers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InfiniteDispensersCommand implements CommandExecutor, TabCompleter
{
    private final InfiniteDispensers infiniteDispensers;
    
    public InfiniteDispensersCommand(InfiniteDispensers infiniteDispensers)
    {
        this.infiniteDispensers = infiniteDispensers;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(ChatColor.RED + "No arguments specified");
            return true;
        }
    
        switch(args[0])
        {
            case "get":
            {
                if(!(sender instanceof Player))
                {
                    sender.sendMessage(ChatColor.RED + "This command must be used by a player");
                    return true;
                }
                Player p = (Player) sender;
                p.getInventory().addItem(getInfinite(args.length >= 2 ? args[1] : ""));
                break;
            }
            case "give":
                if(args.length < 2)
                {
                    sender.sendMessage(ChatColor.RED + "No player specified!");
                    return true;
                }
                Player to = findPlayer(args[1]);
                if(to == null)
                {
                    sender.sendMessage(ChatColor.RED + "No player found!");
                    return true;
                }
                to.getInventory().addItem(getInfinite(args.length >= 3 ? args[2] : ""));
                sender.sendMessage(ChatColor.GREEN + "Gave item to " + to.getName());
                break;
            case "toggle":
            {
                if(!(sender instanceof Player))
                {
                    sender.sendMessage(ChatColor.RED + "This command must be used by a player");
                    return true;
                }
                Player p = (Player) sender;
                infiniteDispensers.toggleTarget(p, false, null);
                break;
            }
            default:
            {
                sender.sendMessage(ChatColor.RED + "Unknown command!");
                break;
            }
        }
        
        return true;
    }
    
    private Player findPlayer(String name)
    {
        Player match = null;
        for(Player player : Bukkit.getOnlinePlayers())
        {
            if(player.getName().equals(name))
            {
                return player;
            }
            else if(player.getName().toLowerCase().startsWith(name.toLowerCase()))
            {
                match = player;
            }
        }
        return match;
    }
    
    private ItemStack getInfinite(String name)
    {
        if(name.equals("wand"))
        {
            return infiniteDispensers.getWand();
        }
        else if(name.equals("dropper"))
        {
            return infiniteDispensers.getInfinite(Material.DROPPER);
        }
        else
        {
            return infiniteDispensers.getInfinite(Material.DISPENSER);
        }
    }
    
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        List<String> options = new ArrayList<>();
        if(args.length == 1)
        {
            options.add("get");
            options.add("give");
            options.add("toggle");
        }
        else if(args.length == 2)
        {
            if(args[0].equals("give"))
            {
                for(Player player : Bukkit.getOnlinePlayers())
                {
                    options.add(player.getName());
                }
            }
            else if(args[0].equals("get"))
            {
                options.add("wand");
                options.add("dispenser");
                options.add("dropper");
            }
        }
        else if(args.length == 3 && args[0].equals("give"))
        {
            options.add("wand");
            options.add("dispenser");
            options.add("dropper");
        }
    
        List<String> completions = new ArrayList<>();
        StringUtil.copyPartialMatches(args[args.length - 1], options, completions);
        Collections.sort(completions);
        return completions;
    }
}
