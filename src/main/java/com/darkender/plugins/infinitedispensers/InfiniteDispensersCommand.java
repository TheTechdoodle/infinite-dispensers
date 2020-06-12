package com.darkender.plugins.infinitedispensers;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        if(sender instanceof Player)
        {
            Player p = (Player) sender;
            if(args.length == 1)
            {
                if(args[0].equals("wand"))
                {
                    p.getInventory().addItem(infiniteDispensers.getWand());
                }
                else if(args[0].equals("get"))
                {
                    p.getInventory().addItem(infiniteDispensers.getInfinite(Material.DISPENSER));
                }
            }
        }
        return true;
    }
    
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        return null;
    }
}
