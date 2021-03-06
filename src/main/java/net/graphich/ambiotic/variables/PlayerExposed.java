package net.graphich.ambiotic.variables;

import com.google.gson.annotations.SerializedName;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.graphich.ambiotic.util.Helpers;
import net.graphich.ambiotic.util.StrictJsonException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.util.*;

public class PlayerExposed extends Variable {

    @SerializedName("AirPermeableBlocks")
    protected String[] mPermeableBlockSpecs;
    @SerializedName("SearchDepth")
    protected int mDepth;

    protected transient List<Integer> mPermeableBlockIds;
    protected transient Deque<Pos> mOpen;
    protected transient List<Pos> mClosed;

    public PlayerExposed(String name) {
        super(name);
        initialize();
    }

    @Override
    public void validate() throws StrictJsonException {
        super.validate();
        if(mPermeableBlockSpecs == null)
            throw new StrictJsonException("AirPermeableBlocks must be defined");
    }

    @Override
    public void initialize() {
        super.initialize();
        mPermeableBlockIds = new ArrayList<Integer>();
        //TODO: Log bad block specs?
        for(String spec : mPermeableBlockSpecs) {
            ArrayList<Integer> ids = Helpers.buildBlockIdList(spec);
            mPermeableBlockIds.addAll(ids);
        }
        mClosed = new ArrayList<Pos>();
        mOpen = new ArrayDeque<Pos>();
        if(mDepth == 0)
            mDepth = 5;
    }

    @Override
    public boolean update(TickEvent event) {
        World world = Minecraft.getMinecraft().theWorld;
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if(world == null || player == null)
            return false;
        mOpen.clear();
        mClosed.clear();
        int newValue = 0;
        // Boundry problems because doubles to ints suck, always pick the "good position"
        Pos current = new Pos(Math.ceil(player.posX), Math.ceil(player.posY), Math.ceil(player.posZ));
        if(!goodSuccessor(current, null))
            current = new Pos(Math.floor(player.posX), Math.floor(player.posY), Math.floor(player.posZ));
        while(current != null && newValue == 0) {
            if(current.isExposed()) {
                newValue = 1;
                break;
            }
            mOpen.addAll(successors(current));
            mClosed.add(current);
            current = mOpen.poll();
        }
        if(mValue == newValue)
            return false;
        mValue = newValue;
        return true;
    }

    public List<Pos> successors(Pos p) {
        ArrayList<Pos> rv = new ArrayList<Pos>();
        Pos possible = new Pos(p.X+1,p.Y,p.Z);
        if(goodSuccessor(possible, p))
            rv.add(possible);
        possible = new Pos(p.X-1,p.Y,p.Z);
        if(goodSuccessor(possible, p))
            rv.add(possible);
        possible = new Pos(p.X,p.Y+1,p.Z);
        if(goodSuccessor(possible, p))
            rv.add(possible);
        possible = new Pos(p.X,p.Y-1,p.Z);
        if(goodSuccessor(possible, p))
            rv.add(possible);
        possible = new Pos(p.X,p.Y,p.Z+1);
        if(goodSuccessor(possible, p))
            rv.add(possible);
        possible = new Pos(p.X,p.Y,p.Z-1);
        if(goodSuccessor(possible, p))
            rv.add(possible);
        return rv;
    }

    protected boolean doorIsBlocking(Pos into, Pos from) {
        BlockDoor door = (BlockDoor)Minecraft.getMinecraft().theWorld.getBlock(into.X,into.Y,into.Z);
        double fromCoord, intoCoord, boundry;
        if(into.X != from.X)
        {
            fromCoord = from.X + 0.5f;
            intoCoord = into.X + 0.5f;
            boundry = into.X+door.getBlockBoundsMaxX();
        }
        else
        {
            fromCoord = from.Z + 0.5f;
            intoCoord = into.Z + 0.5f;
            boundry = into.Z+door.getBlockBoundsMaxZ();
        }
        if(fromCoord > boundry && boundry > intoCoord)
            return true;
        if(fromCoord < boundry && boundry < intoCoord)
            return true;
        return false;
    }

    protected boolean goodSuccessor(Pos into, Pos from) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        Block block = Minecraft.getMinecraft().theWorld.getBlock(into.X,into.Y,into.Z);
        int id = Block.getIdFromBlock(block);

        if(mOpen.contains(into))
            return false;
        if(mClosed.contains(into))
            return false;
        if(block instanceof BlockDoor && from != null)
            return !doorIsBlocking(into, from);
        if(!mPermeableBlockIds.contains(id))
            return false;
        if(into.X > player.posX+mDepth || into.X < player.posX-mDepth)
            return false;
        if(into.Y > player.posY+mDepth || into.Y < player.posY-mDepth)
            return false;
        if(into.Z > player.posZ+mDepth || into.Z < player.posZ-mDepth)
            return false;

        return true;
    }

    protected class Pos {
        public int X;
        public int Y;
        public int Z;

        public Pos(double x, double y, double z) {
            X = (int)x;
            Y = (int)y;
            Z = (int)z;
        }

        public boolean isExposed() {
            World world = Minecraft.getMinecraft().theWorld;
            return world.canBlockSeeTheSky(X,Y,Z) && world.getSavedLightValue(EnumSkyBlock.Sky,X,Y,Z) > 10;
        }

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof Pos))
                return false;
            Pos ot = (Pos)o;
            return X == ot.X && Y == ot.Y && Z == ot.Z;
        }

        @Override
        public String toString() {
            return "X: "+X+", Y: "+Y+", Z: "+Z;
        }
    }
}
