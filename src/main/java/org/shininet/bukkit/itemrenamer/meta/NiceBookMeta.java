package org.shininet.bukkit.itemrenamer.meta;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;

/**
 * Represents an ItemMeta for books that doesn't remove custom NBT tags.
 * @author Kristian
 */
public class NiceBookMeta extends NiceItemMeta {
	private static final String NBT_PAGES = "pages";
	private static final String NBT_TITLE = "title";
	private static final String NBT_AUTHOR = "author";

	private NiceBookMeta(ItemStack stack) {
		super(validate(stack));
	}

	/**
	 * Ensure the given stack is a valid book.
	 * @param stack - the stack to test.
	 * @return Determine if the stack is valid.
	 */
	private static ItemStack validate(ItemStack stack) {
		// Make sure the stack is
		if (stack.getType() != Material.WRITTEN_BOOK && stack.getType() != Material.BOOK_AND_QUILL)
			throw new IllegalArgumentException("stack must be a book.");
		return stack;
	}

	/**
	 * Construct a new nice BookMeta from a given stack.
	 * <p>
	 * Note that the item stack may be changed and must be retrieved from the getStack method when done.
	 * @param stack - the book to modify.
	 * @return The nice BookMeta.
	 */
	public static NiceBookMeta fromStack(ItemStack stack) {
		return new NiceBookMeta(stack);
	}
	
	/**
	 * Gets the author of this book,
	 * @return The author.
	 */
	public String getAuthor() {
		return tag.containsKey(NBT_AUTHOR) ? tag.getString(NBT_AUTHOR) : null;
	}
	
	/**
	 * Set the author of this book.
	 * @param name - the new author.
	 */
	public void setAuthor(String author) {
		tag.put(NBT_AUTHOR, author);
	}
	
	/**
	 * Gets the title of this book,
	 * @return The title.
	 */
	public String getTitle() {
		return tag.containsKey(NBT_TITLE) ? tag.getString(NBT_TITLE) : null;
	}
	
	/**
	 * Set the title of this book.
	 * @param name - the new title.
	 */
	public void setTitle(String title) {
		tag.put(NBT_TITLE, title);
	}
	
	/**
	 * Retrieve the page at a specific location.
	 * @param index - the index.
	 * @return The content of the page.
	 */
	public String getPage(int index) {
		if (!tag.containsKey(NBT_PAGES))
			throw new IndexOutOfBoundsException("Index " + index + " is out of bounds. No pages in the book.");
		return (String) tag.getList(NBT_PAGES).getValue(index);
	}
	
	/**
	 * Set the page at a specific location.
	 * @param index - index of the page.
	 * @param page - the page.
	 */
	public void setPage(int index, String page) {
		tag.<String>getListOrDefault(NBT_PAGES).getValue().
			set(index, NbtFactory.of("", handlePage(page)));
	}
	
	/**
	 * Set every page in the book.
	 * @param pages - the pages.
	 */
	public void setPages(String... pages) {
		NbtList<String> list = tag.<String>getListOrDefault(NBT_PAGES);
		
		for (String page : pages) {
			list.add(handlePage(page));
		}
	}
	
	/**
	 * Ensure a page is not invalid when inserted.
	 * @param page - the page to insert.
	 * @return The processed page.
	 */
	private String handlePage(String page) {
		if (page == null)
			return "";
		else if (page.length() > 256) {
			return page.substring(0, 256);
		}
		return page;
	}
	
	/**
	 * Retrieve the number of pages.
	 * @return Number of pages.
	 */
	public int getPageCount() {
		return tag.containsKey(NBT_PAGES) ? tag.getList(NBT_PAGES).size() : 0;
	}
}
