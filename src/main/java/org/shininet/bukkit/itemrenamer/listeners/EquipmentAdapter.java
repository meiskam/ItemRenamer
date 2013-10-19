package org.shininet.bukkit.itemrenamer.listeners;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang.NullArgumentException;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.google.common.base.Preconditions;

class EquipmentAdapter implements Inventory {	
	// One held armor slot + 4 armor slots
	private static final int INVENTORY_SIZE = 1 + 4;
	private static final int MAX_STACK_SIZE = 64;
	
	private final EntityEquipment equipment;
	private final EquipmentHolder holder;
	private final Player viewer;
	
	// View this inventory as a list
	private transient List<ItemStack> listAdapter;
	
	public EquipmentAdapter(EntityEquipment equipment, LivingEntity owner, Player viewer) {
		this.holder = new EquipmentHolder(owner, this);
		this.equipment = equipment;
		this.viewer = viewer;
	}

	@Override
    public InventoryType getType() {
    	// Like a custom inventory
    	return InventoryType.CHEST;
    }

	@Override
    public void setMaxStackSize(int size) {
		// We cannot support it (for now)
        throw new UnsupportedOperationException("Cannot set stack size.");
    }
	
    @Override
    public int getSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public String getName() {
    	return "EquipmentAdapter";
    }

    @Override
    public ItemStack getItem(int index) {
    	switch (index) {
    		case 0: return equipment.getItemInHand();
    		case 1: return equipment.getBoots();
    		case 2: return equipment.getLeggings();
    		case 3: return equipment.getChestplate();
    		case 4: return equipment.getHelmet();
    		default: 
    			throw new IndexOutOfBoundsException("Index " + index + " is outside the inventory size (" + getSize() + ")");
    	}
    }
    
    @Override
    public void setItem(int index, ItemStack item) {
    	switch (index) {
    		case 0: equipment.setItemInHand(item); break;
    		case 1: equipment.setBoots(item); break;
    		case 2: equipment.setLeggings(item); break;
    		case 3: equipment.setChestplate(item); break;
    		case 4: equipment.setHelmet(item); break;
    		default: 
    			throw new IndexOutOfBoundsException("Index " + index + " is outside the inventory size (" + getSize() + ")");
    	}
    }

    @Override
    public ItemStack[] getContents() {
    	return asList().toArray(new ItemStack[getSize()]);
    }

    @Override
    public void setContents(ItemStack[] items) {
        if (getSize() < items.length) {
            throw new IllegalArgumentException("Invalid inventory size; expected " + getSize() + " or less");
        }

        for (int i = 0; i < items.length; i++) {
        	setItem(i, items[i]);
        }
    }

    @Override
    public boolean contains(int materialId) {
        for (ItemStack item : asList()) {
            if (item != null && item.getTypeId() == materialId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(Material material) {
        Preconditions.checkNotNull(material, "Material cannot be null");
        return contains(material.getId());
    }
    
    @Override
    public boolean contains(ItemStack item) {
        return asList().contains(item);
    }

    @Override
    public boolean contains(int materialId, int amount) {
        if (amount <= 0) {
            return true;
        }
        for (ItemStack item : asList()) {
            if (item != null && item.getTypeId() == materialId) {
                if ((amount -= item.getAmount()) <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean contains(Material material, int amount) {
        Preconditions.checkNotNull(material, "Material cannot be null");
        return contains(material.getId(), amount);
    }
    
    @Override
    public boolean contains(ItemStack item, int amount) {
        if (item == null) {
            return false;
        }
        if (amount <= 0) {
            return true;
        }
        for (ItemStack i : asList()) {
            if (item.equals(i) && --amount <= 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAtLeast(ItemStack item, int amount) {
        if (item == null) {
            return false;
        }
        if (amount <= 0) {
            return true;
        }
        for (ItemStack i : asList()) {
            if (item.isSimilar(i) && (amount -= i.getAmount()) <= 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public HashMap<Integer, ItemStack> all(int materialId) {
        HashMap<Integer, ItemStack> slots = new HashMap<Integer, ItemStack>();

        for (int i = 0; i < getSize(); i++) {
            ItemStack item = getItem(i);
            if (item != null && item.getTypeId() == materialId) {
                slots.put(i, item);
            }
        }
        return slots;
    }

    @Override
    public HashMap<Integer, ItemStack> all(Material material) {
        Preconditions.checkNotNull(material, "Material cannot be null");
        return all(material.getId());
    }

    @Override
    public HashMap<Integer, ItemStack> all(ItemStack item) {
        HashMap<Integer, ItemStack> slots = new HashMap<Integer, ItemStack>();
        if (item != null) {
            for (int i = 0; i < getSize(); i++) {
            	ItemStack inventoryItem = getItem(i);
            	
                if (item.equals(inventoryItem)) {
                    slots.put(i, inventoryItem);
                }
            }
        }
        return slots;
    }

    @Override 
    public int first(int materialId) {
        for (int i = 0; i < getSize(); i++) {
            ItemStack item = getItem(i);
            if (item != null && item.getTypeId() == materialId) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int first(Material material) {
        Preconditions.checkNotNull(material, "Material cannot be null");
        return first(material.getId());
    }

    @Override
    public int first(ItemStack item) {
        return first(item, true);
    }

    private int first(ItemStack item, boolean withAmount) {
        if (item == null) {
            return -1;
        }
        for (int i = 0; i < getSize(); i++) {
        	ItemStack inventoryItem = getItem(i);
        	
            if (inventoryItem == null) 
            	continue;

            if (withAmount ? item.equals(inventoryItem) : item.isSimilar(inventoryItem)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int firstEmpty() {
        for (int i = 0; i < getSize(); i++) {
            if (getItem(i) == null) {
                return i;
            }
        }
        return -1;
    }

    public int firstPartial(int materialId) {
        for (int i = 0; i < getSize(); i++) {
            ItemStack item = getItem(i);
            
            if (item != null && item.getTypeId() == materialId && item.getAmount() < item.getMaxStackSize()) {
                return i;
            }
        }
        return -1;
    }

    public int firstPartial(Material material) {
        Preconditions.checkNotNull(material, "Material cannot be null");
        return firstPartial(material.getId());
    }

    private int firstPartial(ItemStack item) {
        if (item == null) {
            return -1;
        }
        for (int i = 0; i < getSize(); i++) {
            ItemStack cItem = getItem(i);
            if (cItem != null && cItem.getAmount() < cItem.getMaxStackSize() && cItem.isSimilar(item)) {
                return i;
            }
        }
        return -1;
    }

    @Override
	    public HashMap<Integer, ItemStack> addItem(ItemStack... items) {
    		checkNotNull(items);
	        HashMap<Integer, ItemStack> leftover = new HashMap<Integer, ItemStack>();

	        for (int i = 0; i < items.length; i++) {
	            ItemStack item = items[i];
	            while (true) {
	                // Do we already have a stack of it?
	                int firstPartial = firstPartial(item);

	                // Drat! no partial stack
	                if (firstPartial == -1) {
	                    // Find a free spot!
	                    int firstFree = firstEmpty();

	                    if (firstFree == -1) {
	                        // No space at all!
	                        leftover.put(i, item);
	                        break;
	                    } else {
	                        // More than a single stack!
	                        if (item.getAmount() > getMaxItemStack()) {
	                        	item.setAmount(getMaxItemStack());
	                            setItem(firstFree, item);
	                            item.setAmount(item.getAmount() - getMaxItemStack());
	                        } else {
	                            // Just store it
	                            setItem(firstFree, item);
	                            break;
	                        }
	                    }
	                } else {
	                    // So, apparently it might only partially fit, well lets do just that
	                    ItemStack partialItem = getItem(firstPartial);

	                    int amount = item.getAmount();
	                    int partialAmount = partialItem.getAmount();
	                    int maxAmount = partialItem.getMaxStackSize();

	                    // Check if it fully fits
	                    if (amount + partialAmount <= maxAmount) {
	                        partialItem.setAmount(amount + partialAmount);
	                        break;
	                    }

	                    // It fits partially
	                    partialItem.setAmount(maxAmount);
	                    item.setAmount(amount + partialAmount - maxAmount);
	                }
	            }
	        }
	        return leftover;
	    }

	    public HashMap<Integer, ItemStack> removeItem(ItemStack... items) {
	    	checkNotNull(items);
	        HashMap<Integer, ItemStack> leftover = new HashMap<Integer, ItemStack>();

	        for (int i = 0; i < items.length; i++) {
	            ItemStack item = items[i];
	            int toDelete = item.getAmount();

	            while (true) {
	                int first = first(item, false);

	                // Drat! we don't have this type in the inventory
	                if (first == -1) {
	                    item.setAmount(toDelete);
	                    leftover.put(i, item);
	                    break;
	                } else {
	                    ItemStack itemStack = getItem(first);
	                    int amount = itemStack.getAmount();

	                    if (amount <= toDelete) {
	                        toDelete -= amount;
	                        // clear the slot, all used up
	                        clear(first);
	                    } else {
	                        // split the stack and store
	                        itemStack.setAmount(amount - toDelete);
	                        setItem(first, itemStack);
	                        toDelete = 0;
	                    }
	                }

	                // Bail when done
	                if (toDelete <= 0) {
	                    break;
	                }
	            }
	        }
	        return leftover;
	    }

	    private void checkNotNull(ItemStack... items) {
	    	// Check for NULL items
	    	for (ItemStack stack : items) {
	    		if (stack == null) {
	    			throw new NullArgumentException("Item " + stack + " cannot be null");
	    		}
	    	}
	    }
	    
	    private int getMaxItemStack() {
	        return 64;
	    }

	    @Override
	    public void remove(int materialId) {
	        for (int i = 0; i < getSize(); i++) {
	            if (getItem(i) != null && getItem(i).getTypeId() == materialId) {
	                clear(i);
	            }
	        }
	    }

	    @Override
	    public void remove(Material material) {
	        Preconditions.checkNotNull(material, "Material cannot be null");
	        remove(material.getId());
	    }

	    @Override
	    public void remove(ItemStack item) {
	        for (int i = 0; i < getSize(); i++) {
	            if (getItem(i) != null && getItem(i).equals(item)) {
	                clear(i);
	            }
	        }
	    }

	    @Override
	    public void clear(int index) {
	        setItem(index, null);
	    }

	    @Override
	    public void clear() {
	        for (int i = 0; i < getSize(); i++) {
	            clear(i);
	        }
	    }
	    
	    /**
	     * Return a view of this inventory as a fixed-size list.
	     * @return The fixed-size list.
	     */
	    public List<ItemStack> asList() {
	    	if (listAdapter == null) {
		    	listAdapter = new AbstractList<ItemStack>() {
		    		@Override
		    		public ItemStack set(int index, ItemStack element) {
		    			ItemStack old = getItem(index);
		    			setItem(index, element);
		    			return old;
		    		}
		    		
		    		@Override
		    		public ItemStack get(int index) {
		    			return getItem(index);
		    		}
	
		    		@Override
		    		public int size() {
		    			return getSize();
		    		}
				};
	    	}
	    	return listAdapter;
	    }

	    @Override
	    public ListIterator<ItemStack> iterator() {
	         return asList().listIterator();
	    }

	    @Override
	    public ListIterator<ItemStack> iterator(int index) {
	        if (index < 0) {
	            index += getSize() + 1; // ie, with -1, previous() will return the last element
	        }
	        return asList().listIterator(index);
	    }

	    @Override
	    public List<HumanEntity> getViewers() {
	        return Arrays.asList((HumanEntity) viewer);
	    }

	    @Override
	    public String getTitle() {
	    	String custom = holder.getEntity().getCustomName();
	    	
	        return String.format("Equipment for %s (%s)", 
	        	custom != null ? custom : holder.getEntity().getType(), 
	        	holder.getEntity().getEntityId()
	        );
	    }
	    
	    @Override
	    public InventoryHolder getHolder() {
	        return holder;
	    }

	    @Override
	    public int getMaxStackSize() {
	        return MAX_STACK_SIZE;
	    }
}
