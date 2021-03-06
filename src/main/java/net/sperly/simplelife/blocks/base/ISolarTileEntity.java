package net.sperly.simplelife.blocks.base;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.sperly.simplelife.blocks.solarfurnace.SolarFurnaceTileEntity;
import net.sperly.simplelife.blocks.solargrinder.SolarGrinderTileEntity;
import net.sperly.simplelife.helpers.GrinderRecipes;
import net.sperly.simplelife.items.SolarCellUpgrade1Item;
import net.sperly.simplelife.items.SolarCellUpgrade2Item;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class ISolarTileEntity extends TileEntity implements ITickable, ISidedInventory
{
    public static final int SIZE = 11;

    public static final int[] UPGRADE_SLOTS = {0};
    public static final int INPUT_SLOT = 1;
    public static final int[] OUTPUT_SLOTS = {2, 3, 4, 5, 6, 7, 8, 9, 10};

    protected int solarUpgradeLevel = 0;
    protected boolean isWorking = false;
    protected int workTime = 0;
    protected int workTimeRemaining = 0;
    private static final short WORK_TIME_FOR_COMPLETION = 200;  // vanilla value is 200 = 10 seconds

    // This item handler will hold our nine inventory slots
    protected ItemStackHandler itemStackHandler = new ItemStackHandler(SIZE)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            checkUpgradeSlot();
        }
    };

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        if (compound.hasKey("items"))
        {
            itemStackHandler.deserializeNBT((NBTTagCompound) compound.getTag("items"));
        }
        this.checkUpgradeSlot();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setTag("items", itemStackHandler.serializeNBT());
        return compound;
    }

    public int getUpgradeLevel()
    {
        return solarUpgradeLevel;
    }

    public boolean hasEnoughSun()
    {
        return isWorking;
    }

    public boolean canInteractWith(EntityPlayer playerIn)
    {
        // If we are too far away from this tile entity you cannot use it
        return !isInvalid() && playerIn.getDistanceSq(pos.add(0.5D, 0.5D, 0.5D)) <= 64D;
    }

    public double fractionOfWorkTimeComplete()
    {
        double fraction = 1.0 - (double)this.workTimeRemaining / (double)(this.workTime);
        return MathHelper.clamp(fraction, 0.0, 1.0);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            if (facing == EnumFacing.UP)
                return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
        {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemStackHandler);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void update()
    {
        if (!getWorld().isRemote)
        {
            long time = getWorld().getWorldTime() % 24000;
            if (((time < 450) || (time > 7700)) && (solarUpgradeLevel == 0))
            {
                isWorking = false;
            }
            else if (((time > 12500) && (time < 23000)) && solarUpgradeLevel == 1)
            {
                isWorking = false;
            }
            else
            {
                isWorking = true;
            }
        }
    }

    public void checkUpgradeSlot()
    {
        // We need to tell the tile entity that something has changed so
        // that the chest contents is persisted
        if (!itemStackHandler.getStackInSlot(UPGRADE_SLOTS[0]).isEmpty())
        {
            Item upgrades = itemStackHandler.getStackInSlot(UPGRADE_SLOTS[0]).getItem();
            if (upgrades instanceof SolarCellUpgrade1Item)
                solarUpgradeLevel = 1;
            else if (upgrades instanceof SolarCellUpgrade2Item)
                solarUpgradeLevel = 2;
            else
                solarUpgradeLevel = 0;
        }
        else
            solarUpgradeLevel = 0;

        workTime = WORK_TIME_FOR_COMPLETION - (solarUpgradeLevel * 50);
        markDirty();
    }

    protected int mergeStacksInInventory(ItemStack inStack)
    {
        int restCount = -1;

        int slot = OUTPUT_SLOTS[0];
        while(!inStack.isEmpty() && slot <= OUTPUT_SLOTS[8]) {
            ItemStack toStack = itemStackHandler.getStackInSlot(slot);
            if(toStack == ItemStack.EMPTY)
            {
                itemStackHandler.setStackInSlot(slot, inStack);
                inStack = ItemStack.EMPTY;
                restCount = 0;
            }
            else if(toStack.getItem() == inStack.getItem() && toStack.getCount() + inStack.getCount() <= 64)
            {
                toStack.setCount(toStack.getCount() + inStack.getCount());
                itemStackHandler.setStackInSlot(slot, toStack);
                inStack = ItemStack.EMPTY;
                restCount = 0;
            }
            else if(toStack.getItem() == inStack.getItem())
            {
                int rest = 64 - toStack.getCount();
                toStack.setCount(64);
                inStack.setCount(inStack.getCount() - rest);
            }
            ++slot;
        }

        if(inStack.getCount() > 0)
            restCount = inStack.getCount();

        return restCount;
    }

    @Override
    public int getSizeInventory()
    {
        return itemStackHandler.getSlots();
    }

    @Override
    public boolean isEmpty()
    {
        for(int i =0 ; i < itemStackHandler.getSlots();++i)
        {
            if(!itemStackHandler.getStackInSlot(i).isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return itemStackHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack decrStackSize(int slot, int count)
    {
        ItemStack stack = itemStackHandler.getStackInSlot(slot);
        ItemStack returnStack = ItemStack.EMPTY;
        if(stack.isEmpty())
        {
            itemStackHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        else if( stack.getCount() <= count)
        {
            itemStackHandler.setStackInSlot(slot, ItemStack.EMPTY);
            returnStack = stack;
        }
        else
        {
            returnStack = stack.splitStack(count);
            itemStackHandler.setStackInSlot(slot, stack);
        }

        return returnStack;
    }

    @Override
    public ItemStack removeStackFromSlot(int slot)
    {
        ItemStack stack = itemStackHandler.getStackInSlot(slot);
        itemStackHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack itemStack)
    {
        if (!itemStack.isEmpty() && itemStack.getCount() > getInventoryStackLimit()) {
            itemStack.setCount(getInventoryStackLimit(slot));
        }
        itemStackHandler.setStackInSlot(slot, itemStack);
        markDirty();
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 64;
    }

    public int getInventoryStackLimit(int slot)
    {
        if(slot == UPGRADE_SLOTS[0])
            return 1;
        else return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer entityPlayer)
    {
        if (getWorld().getTileEntity(this.pos) != this) return false;

        return true;
    }

    @Override
    public void openInventory(EntityPlayer entityPlayer)
    {

    }

    @Override
    public void closeInventory(EntityPlayer entityPlayer)
    {

    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack itemStack)
    {
        if(slot != INPUT_SLOT)
            return false;
        else
            return true;
    }

    private static final byte WORKTIME_FIELD_ID = 0;
    private static final byte WORKTIME_REMAINING_FIELD_ID = 1;
    private static final byte UPGRADE_LEVEL_FIELD_ID = 2;
    private static final byte NUMBER_OF_FIELDS = 3;

    @Override
    public int getField(int id) {
        if (id == WORKTIME_FIELD_ID) return workTime;
        else if (id == WORKTIME_REMAINING_FIELD_ID) return workTimeRemaining;
        else if (id == UPGRADE_LEVEL_FIELD_ID) return solarUpgradeLevel;
        System.err.println("Invalid field ID in TileInventorySmelting.getField:" + id);
        return 0;
    }

    @Override
    public void setField(int id, int value)
    {
        if (id == WORKTIME_FIELD_ID) {
            workTime = value;
        } else if (id == WORKTIME_REMAINING_FIELD_ID) {
            workTimeRemaining = value;
        } else if (id == UPGRADE_LEVEL_FIELD_ID) {
            workTimeRemaining = value;
        } else {
            System.err.println("Invalid field ID in TileInventorySmelting.setField:" + id);
        }
    }

    @Override
    public int getFieldCount() {
        return NUMBER_OF_FIELDS;
    }

    @Override
    public void clear()
    {

    }

    @Override
    public String getName()
    {
        return this.getBlockType().getLocalizedName();
    }

    @Override
    public boolean hasCustomName()
    {
        return false;
    }

    protected boolean moveOneItemToOutput()
    {
        ItemStack workedItems = ItemStack.EMPTY;
        if (this instanceof SolarFurnaceTileEntity)
        {
            workedItems = FurnaceRecipes.instance().getSmeltingResult(itemStackHandler.getStackInSlot(INPUT_SLOT)).copy();
        }
        else if (this instanceof SolarGrinderTileEntity)
        {
            workedItems = GrinderRecipes.instance().getGrindingResult(itemStackHandler.getStackInSlot(INPUT_SLOT)).copy();
        }

        //workedItems.setCount(1);
        int rest = mergeStacksInInventory(workedItems);
        if (rest == 0)
        {
            ItemStack restStack = itemStackHandler.getStackInSlot(INPUT_SLOT);
            restStack.setCount(itemStackHandler.getStackInSlot(INPUT_SLOT).getCount() - 1);
            itemStackHandler.setStackInSlot(INPUT_SLOT, restStack);
            return true;
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing enumFacing)
    {
        int[] inSlots = { INPUT_SLOT };
        int[] outSLots = OUTPUT_SLOTS;
        if(enumFacing != EnumFacing.DOWN)
            return inSlots;
        else
            return outSLots;
    }

    @Override
    public boolean canExtractItem(int i, ItemStack itemStack, EnumFacing enumFacing)
    {
        if(enumFacing == EnumFacing.DOWN)
            return true;
        else
            return false;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStack, EnumFacing facing)
    {
        if(!isItemValidForSlot(index, itemStack))
            return false;

        int[] slots = getSlotsForFace(facing);
        if(!Arrays.asList(slots).contains(index))
            return false;

        return true;

    }
}
