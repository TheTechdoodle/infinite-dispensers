package com.darkender.plugins.infinitedispensers;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.Slime;

public class InfiniteDispenserBlock
{
    private Slime glowingSlime = null;
    private final Block block;
    
    public InfiniteDispenserBlock(Block block)
    {
        this.block = block;
    }
    
    public boolean isGlowing()
    {
        return glowingSlime != null;
    }
    
    public void setGlowing(boolean glowing)
    {
        if(glowingSlime == null && glowing)
        {
            glowingSlime = InfiniteDispensers.spawnDisplaySlime(block);
        }
        else if(glowingSlime != null && !glowing)
        {
            glowingSlime.remove();
            glowingSlime = null;
        }
    }
    
    public Block getBlock()
    {
        return block;
    }
    
    public boolean isInChunk(Chunk chunk)
    {
        return block.getChunk().equals(chunk);
    }
}
