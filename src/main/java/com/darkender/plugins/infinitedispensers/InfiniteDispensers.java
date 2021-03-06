package com.darkender.plugins.infinitedispensers;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InfiniteDispensers extends JavaPlugin implements Listener
{
    private static NamespacedKey isInfiniteKey, isWandKey, isDisplaySlimeKey;
    private HashMap<World, Set<InfiniteDispenserBlock>> infiniteDispensers;
    private final static double DISPENSER_RADIUS = 30.0;
    
    @Override
    public void onEnable()
    {
        infiniteDispensers = new HashMap<>();
        isInfiniteKey = new NamespacedKey(this, "is-infinite");
        isWandKey = new NamespacedKey(this, "is-wand");
        isDisplaySlimeKey = new NamespacedKey(this, "is-display-slime");
        
        InfiniteDispensersCommand infiniteDispensersCommand = new InfiniteDispensersCommand(this);
        getCommand("infinitedispensers").setExecutor(infiniteDispensersCommand);
        getCommand("infinitedispensers").setTabCompleter(infiniteDispensersCommand);
        
        getServer().getPluginManager().registerEvents(this, this);
        
        // Highlight infinite dispensers while the wand is out
        getServer().getScheduler().scheduleSyncRepeatingTask(this, () ->
        {
            Set<Player> wandPlayers = new HashSet<>();
            for(Player player : getServer().getOnlinePlayers())
            {
                if(isWand(player.getInventory().getItemInMainHand()) || isWand(player.getInventory().getItemInOffHand()))
                {
                    wandPlayers.add(player);
                }
            }
            
            for(Map.Entry<World, Set<InfiniteDispenserBlock>> entry : infiniteDispensers.entrySet())
            {
                for(InfiniteDispenserBlock infiniteDispenserBlock : entry.getValue())
                {
                    boolean inRange = false;
                    Location loc = infiniteDispenserBlock.getBlock().getLocation();
                    for(Player wand : wandPlayers)
                    {
                        if(wand.getWorld().equals(loc.getWorld()) && wand.getLocation().distance(loc) <= DISPENSER_RADIUS)
                        {
                            inRange = true;
                            break;
                        }
                    }
                    
                    if(inRange && !infiniteDispenserBlock.isGlowing())
                    {
                        infiniteDispenserBlock.setGlowing(true);
                    }
                    else if(!inRange && infiniteDispenserBlock.isGlowing())
                    {
                        infiniteDispenserBlock.setGlowing(false);
                    }
                }
            }
        }, 1L, 1L);
        
        for(World world : getServer().getWorlds())
        {
            for(Chunk chunk : world.getLoadedChunks())
            {
                checkChunk(chunk);
            }
        }
    }
    
    @Override
    public void onDisable()
    {
        for(World world : getServer().getWorlds())
        {
            for(Chunk chunk : world.getLoadedChunks())
            {
                cleanupChunk(chunk);
            }
        }
    }
    
    private boolean hasKey(ItemStack item, NamespacedKey key, PersistentDataType type)
    {
        if(item == null)
        {
            return false;
        }
        if(!item.hasItemMeta())
        {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(key, type);
    }
    
    public boolean isWand(ItemStack item)
    {
        return hasKey(item, isWandKey, PersistentDataType.BYTE);
    }
    
    public boolean isInfiniteItem(ItemStack item)
    {
        return hasKey(item, isInfiniteKey, PersistentDataType.BYTE);
    }
    
    public boolean isInfinite(Block block)
    {
        if(block.getState() instanceof TileState)
        {
            TileState tile = (TileState) block.getState();
            return tile.getPersistentDataContainer().has(isInfiniteKey, PersistentDataType.BYTE);
        }
        return false;
    }
    
    public static Slime spawnDisplaySlime(Block block)
    {
        return block.getWorld().spawn(block.getLocation().add(0.5, 0.2, 0.5), Slime.class, slime ->
        {
            slime.setGlowing(true);
            slime.setSize(1);
            slime.setAI(false);
            slime.setInvulnerable(true);
            slime.getPersistentDataContainer().set(isDisplaySlimeKey, PersistentDataType.BYTE, (byte) 1);
        });
    }
    
    public ItemStack getWand()
    {
        ItemStack wand = new ItemStack(Material.STICK, 1);
        ItemMeta meta = wand.getItemMeta();
        meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        meta.setDisplayName(ChatColor.GOLD + "Infinite Dispenser Wand");
        meta.getPersistentDataContainer().set(isWandKey, PersistentDataType.BYTE, (byte) 1);
        wand.setItemMeta(meta);
        return wand;
    }
    
    public ItemStack getInfinite(Material material)
    {
        ItemStack infinite = new ItemStack(material, 1);
        ItemMeta meta = infinite.getItemMeta();
        meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        String name = "Container";
        if(material == Material.DROPPER)
        {
            name = "Dropper";
        }
        else if(material == Material.DISPENSER)
        {
            name = "Dispenser";
        }
        meta.setDisplayName(ChatColor.GOLD + "Infinite " + name);
        meta.getPersistentDataContainer().set(isInfiniteKey, PersistentDataType.BYTE, (byte) 1);
        infinite.setItemMeta(meta);
        return infinite;
    }
    
    public void makeInfinite(Block block)
    {
        if(block.getState() instanceof TileState)
        {
            TileState tile = (TileState) block.getState();
            tile.getPersistentDataContainer().set(isInfiniteKey, PersistentDataType.BYTE, (byte) 1);
            tile.update();
            
            if(!infiniteDispensers.containsKey(block.getWorld()))
            {
                infiniteDispensers.put(block.getWorld(), new HashSet<>());
            }
            infiniteDispensers.get(block.getWorld()).add(new InfiniteDispenserBlock(block));
        }
    }
    
    public void removeInfiniteTracker(Block block)
    {
        if(infiniteDispensers.containsKey(block.getWorld()))
        {
            infiniteDispensers.get(block.getWorld()).removeIf(infiniteDispenserBlock ->
            {
                if(infiniteDispenserBlock.getBlock().equals(block))
                {
                    infiniteDispenserBlock.setGlowing(false);
                    return true;
                }
                return false;
            });
        }
    }
    
    public void removeInfinite(Block block)
    {
        if(block.getState() instanceof TileState)
        {
            TileState tile = (TileState) block.getState();
            tile.getPersistentDataContainer().remove(isInfiniteKey);
            tile.update();
            removeInfiniteTracker(block);
        }
    }
    
    public static Location getHandScreenLocation(Location loc, boolean offhand)
    {
        Location spawnFrom = loc.clone();
        Vector normal2D = spawnFrom.getDirection().clone().setY(0).normalize()
                .rotateAroundY((offhand ? 1 : -1) * (Math.PI / 2))
                .multiply(0.40).setY(-0.35);
        spawnFrom.add(normal2D);
        spawnFrom.add(loc.getDirection().clone().multiply(-0.3));
        return spawnFrom;
    }
    
    public static void displayParticles(Location from, Block to, boolean infinite)
    {
        Location center = to.getLocation().add(0.5, 0.5, 0.5);
        double distance = from.distance(center);
        Vector direction = center.toVector().subtract(from.toVector()).normalize();
        Vector step = direction.multiply(0.3);
        
        double distanceProgress = 0.0;
        Location current = from.clone();
        while(distanceProgress < distance)
        {
            current.getWorld().spawnParticle(Particle.REDSTONE, current, 0, new Particle.DustOptions(Color.GRAY, 0.5F));
            distanceProgress += 0.3;
            current.add(step);
        }
        
        for(int x = to.getX(); x <= to.getX() + 1; x++)
        {
            for(int y = to.getY(); y <= to.getY() + 1; y++)
            {
                for(int z = to.getZ(); z <= to.getZ() + 1; z++)
                {
                    to.getWorld().spawnParticle(Particle.REDSTONE,
                            new Location(to.getWorld(), x, y, z), 0,
                            new Particle.DustOptions(infinite ? Color.LIME : Color.RED, 1.2F));
                }
            }
        }
    }
    
    public void toggleTarget(Player from, boolean offhand, Block block)
    {
        Block target;
        if(block != null)
        {
            target = block;
        }
        else
        {
            RayTraceResult result = from.rayTraceBlocks(DISPENSER_RADIUS, FluidCollisionMode.NEVER);
            if(result == null)
            {
                target = null;
            }
            else
            {
                target = result.getHitBlock();
            }
        }
    
        if(target == null || (target.getType() != Material.DISPENSER && target.getType() != Material.DROPPER))
        {
            from.sendMessage(ChatColor.RED + "No dispenser or dropper in range!");
        }
        else
        {
            if(isInfinite(target))
            {
                removeInfinite(target);
                from.playSound(from.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                        SoundCategory.MASTER, 1.0F, 0.6F);
                displayParticles(getHandScreenLocation(from.getEyeLocation(), offhand), target, false);
            }
            else
            {
                makeInfinite(target);
                from.playSound(from.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                        SoundCategory.MASTER, 1.0F, 1.0F);
                displayParticles(getHandScreenLocation(from.getEyeLocation(), offhand), target, true);
            }
        }
    }
    
    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event)
    {
        if((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && isWand(event.getItem()))
        {
            if(!event.getPlayer().hasPermission("infinitedispensers.toggle"))
            {
                event.getPlayer().sendMessage(ChatColor.RED + "No permission!");
                return;
            }
            event.setCancelled(true);
            toggleTarget(event.getPlayer(), event.getHand() == EquipmentSlot.OFF_HAND, event.getClickedBlock());
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onBlockPlace(BlockPlaceEvent event)
    {
        if(isInfiniteItem(event.getItemInHand()))
        {
            makeInfinite(event.getBlock());
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.MASTER, 1.0F, 1.0F);
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onBlockBreak(BlockBreakEvent event)
    {
        if(isInfinite(event.getBlock()))
        {
            removeInfiniteTracker(event.getBlock());
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onBlockExplode(BlockExplodeEvent event)
    {
        for(Block check : event.blockList())
        {
            if(isInfinite(check))
            {
                removeInfiniteTracker(check);
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onEntityExplode(EntityExplodeEvent event)
    {
        for(Block check : event.blockList())
        {
            if(isInfinite(check))
            {
                removeInfiniteTracker(check);
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onDispense(BlockDispenseEvent event)
    {
        if(isInfinite(event.getBlock()))
        {
            ItemStack clone = event.getItem().clone();
            getServer().getScheduler().runTaskLater(this, () ->
            {
                Container container = (Container) event.getBlock().getState();
                container.getInventory().addItem(clone);
            }, 1L);
        }
    }
    
    private void cleanupChunk(Chunk chunk)
    {
        for(Entity e : chunk.getEntities())
        {
            if(e.getPersistentDataContainer().has(isDisplaySlimeKey, PersistentDataType.BYTE))
            {
                e.remove();
            }
        }
    }
    
    private void checkChunk(Chunk chunk)
    {
        for(BlockState state : chunk.getTileEntities())
        {
            if(!(state instanceof TileState))
            {
                continue;
            }
            
            TileState tile = (TileState) state;
            if(tile.getPersistentDataContainer().has(isInfiniteKey, PersistentDataType.BYTE))
            {
                if(!infiniteDispensers.containsKey(chunk.getWorld()))
                {
                    infiniteDispensers.put(chunk.getWorld(), new HashSet<>());
                }
                infiniteDispensers.get(chunk.getWorld()).add(new InfiniteDispenserBlock(state.getBlock()));
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onChunkUnload(ChunkUnloadEvent event)
    {
        cleanupChunk(event.getChunk());
        if(infiniteDispensers.containsKey(event.getWorld()))
        {
            infiniteDispensers.get(event.getWorld()).removeIf(block ->
            {
                if(block.isInChunk(event.getChunk()))
                {
                    block.setGlowing(false);
                    return true;
                }
                return false;
            });
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onChunkLoad(ChunkLoadEvent event)
    {
        cleanupChunk(event.getChunk());
        checkChunk(event.getChunk());
    }
}
