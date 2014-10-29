package LDraw.Support;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import Command.LDrawPart;
import Command.LDrawQuadrilateral;
import Command.LDrawTexture;
import Command.LDrawTriangle;
import LDraw.Files.LDrawFile;
import LDraw.Files.LDrawModel;
import LDraw.Files.LDrawStep;
import LDraw.Support.type.LDrawDomainT;

//==============================================================================
//
//File:		PartLibrary.m
//
//Purpose:		This is the centralized repository for obtaining information 
//				about the contents of the LDraw folder. The part library is 
//				first created by scanning the LDraw folder and collecting all 
//				the part names, categories, and drawing instructions for each 
//				part. This information is then saved into an XML file and 
//				retrieved each time the program is relaunched. During runtime, 
//				other objects query the part library to draw and display 
//				information about parts.
//
//Created by Allen Smith on 3/12/05.
//Copyright 2005. All rights reserved.
//==============================================================================
public class PartLibrary {
	// The part catalog was regenerated from disk.
	// Object is the new catalog. No userInfo.
	// public static final String LDrawPartLibraryDidChangeNotification =
	// "LDrawPartLibraryDidChangeNotification";

	// The parts list file is stored at LDraw/PARTS_LIST_NAME.
	// It contains a dictionary of parts. Each element in the dictionary
	// is an array of parts for a category; the key under which the array
	// is stored is the category name.
	//
	// The part catalog is a dictionary of parts filed by Category name.
	public static final String PARTS_CATALOG_KEY = "Part Catalog";
	// subdictionary keys.
	public static final String PART_NUMBER_KEY = "Part Number";
	public static final String PART_NAME_KEY = "Part Name";
	public static final String PART_CATEGORY_KEY = "Category";
	public static final String PART_KEYWORDS_KEY = "Keywords";

	// Raw dictionary containing each part filed by number.
	public static final String PARTS_LIST_KEY = "Part List";
	// subdictionary keys.
	// PART_NUMBER_KEY (defined above)
	// PART_NAME_KEY (defined above)

	public static final String VERSION_KEY = "Version";
	public static final String COMPATIBILITY_VERSION_KEY = "CompatibilityVersion";

	public static final String CategoryNameKey = "Name";
	public static final String CategoryDisplayNameKey = "DisplayName";
	public static final String CategoryChildrenKey = "Children";

	public static final String Category_All = "AllCategories";
	public static final String Category_Favorites = "Favorites";
	public static final String Category_Alias = "Alias";
	public static final String Category_Moved = "Moved";
	public static final String Category_Primitives = "Primitives";
	public static final String Category_Subparts = "Subparts";

	private static PartLibrary SharedPartLibrary = null;

	/**
	 * @uml.property name="delegate"
	 * @uml.associationEnd
	 */
	IPartLibraryDelegate delegate;
	/**
	 * @uml.property name="partCatalog"
	 * @uml.associationEnd 
	 *                     qualifier="constant:java.lang.String java.lang.String"
	 */
	HashMap<String, Object> partCatalog;
	/**
	 * @uml.property name="favorites"
	 * @uml.associationEnd multiplicity="(0 -1)" elementType="java.lang.String"
	 */
	ArrayList<String> favorites; // parts names in the "Favorites"
									// pseduocategory
	/**
	 * @uml.property name="loadedFiles"
	 * @uml.associationEnd 
	 *                     qualifier="imageName:java.lang.String LDraw.Files.LDrawModel"
	 */
	HashMap<String, LDrawDirective> loadedFiles; // list of LDrawFiles which
													// have been read off disk.

	/**
	 * @uml.property name="loadedImages"
	 */
	HashMap<String, String> loadedImages;
	/**
	 * @uml.property name="optimizedTextures"
	 * @uml.associationEnd qualifier="name:java.lang.String java.lang.Integer"
	 */
	HashMap<String, Integer> optimizedTextures; // GLuint texture tags
	/**
	 * @uml.property name="optimizedRepresentations"
	 * @uml.associationEnd 
	 *                     qualifier="referenceName:java.lang.String LDraw.Support.LDrawVertices"
	 */

	// dispatch_queue_t catalogAccessQueue; // serial queue to mutex changes to
	// the part catalog
	/**
	 * @uml.property name="parsingGroups"
	 */
	HashMap<String, String> parsingGroups; // arrays of DispatchGroup's which
											// have requested each file
											// currently being parsed

	public PartLibrary() {
		init();
		// reloadParts();
	}

	// ---------- sharedPartLibrary
	// ---------------------------------------[static]--
	//
	// Purpose: Returns the part libary, which contains the part catalog, which
	// is read in from the file LDRAW_PATH_KEY/PART_CATALOG_NAME when
	// the application launches.
	// This is a rather big XML file, so it behooves us to read it
	// once then save it in memory.
	//
	// ------------------------------------------------------------------------------
	public synchronized static PartLibrary sharedPartLibrary() {
		if (SharedPartLibrary == null) {
			SharedPartLibrary = new PartLibrary();
		}

		return SharedPartLibrary;

	}// end sharedPartLibrary

	// ========== init
	// ==============================================================
	//
	// Purpose: Creates a part library with no parts loaded.
	//
	// ==============================================================================
	private PartLibrary init() {
		loadedFiles = new HashMap<String, LDrawDirective>(400);
		loadedImages = new HashMap<String, String>();
		optimizedTextures = new HashMap<String, Integer>();

		favorites = new ArrayList<String>();

		// todo
		// #if USE_BLOCKS
		// catalogAccessQueue =
		// dispatch_queue_create("com.AllenSmith.Bricksmith.CatalogAccess",
		// null);
		// #endif
		parsingGroups = new HashMap<String, String>();

		setPartCatalog(new HashMap<String, Object>());

		return this;

	}// end init

	//
	// #pragma mark -
	// #pragma mark ACCESSORS
	// #pragma mark -

	// ========== allPartCatalogRecords
	// =============================================
	//
	// Purpose: Returns all the part numbers in the library.
	//
	// ==============================================================================
	public ArrayList<Object> allPartCatalogRecords() {
		HashMap<String, Object> partList = (HashMap<String, Object>) partCatalog
				.get(PARTS_LIST_KEY);

		// all the reference numbers for parts.
		return (ArrayList<Object>) partList.values();

	}// end allPartCatalogRecords

	// ========== categories
	// ========================================================
	//
	// Purpose: Returns all the categories in the library, sorted in no
	// particular order.
	//
	// ==============================================================================
	public ArrayList<String> categories() {
		HashMap<String, Object> catalogKey = (HashMap<String, Object>) partCatalog
				.get(PARTS_CATALOG_KEY);
		return (ArrayList<String>) catalogKey.keySet();

	}// end categories

	// ========== categoryHierarchy
	// =================================================
	//
	// Purpose: Returns an outline-conducive list of all available categories.
	//
	// ==============================================================================
	public ArrayList<HashMap<String, Object>> categoryHierarchy() {
		ArrayList<HashMap<String, Object>> fullCategoryList = new ArrayList<HashMap<String, Object>>();
		ArrayList<HashMap<String, Object>> libraryItems = new ArrayList<HashMap<String, Object>>();
		ArrayList<HashMap<String, Object>> categoryItems = new ArrayList<HashMap<String, Object>>();
		ArrayList<HashMap<String, Object>> otherItems = new ArrayList<HashMap<String, Object>>();

		// Library group
		HashMap<String, Object> tempHashMap = new HashMap<String, Object>();
		tempHashMap.put(CategoryNameKey, Category_All);
		tempHashMap.put(CategoryDisplayNameKey,
				displayNameForCategory(Category_All));
		libraryItems.add(tempHashMap);

		tempHashMap = new HashMap<String, Object>();
		tempHashMap.put(CategoryNameKey, Category_Favorites);
		tempHashMap.put(CategoryDisplayNameKey,
				displayNameForCategory(Category_Favorites));
		libraryItems.add(tempHashMap);

		tempHashMap = new HashMap<String, Object>();
		tempHashMap.put(CategoryNameKey, "Library");
		tempHashMap.put(CategoryDisplayNameKey, "CategoryGroup_Library");
		tempHashMap.put(CategoryChildrenKey, libraryItems);
		fullCategoryList.add(tempHashMap);

		// Main categories
		// todo
		// sorting �ʿ��ϴ�
		ArrayList<String> categories = categories();
		// ArrayList<String> categories = [categories().
		// sortedArrayUsingSelector:@selector(compare:)();
		for (String name : categories) {
			if (name != Category_Alias && name != Category_Moved
					&& name != Category_Primitives && name != Category_Subparts) {
				tempHashMap = new HashMap<String, Object>();
				tempHashMap.put(CategoryNameKey, name);
				tempHashMap.put(CategoryDisplayNameKey,
						displayNameForCategory(name));
				categoryItems.add(tempHashMap);
			}
		}
		tempHashMap = new HashMap<String, Object>();
		tempHashMap.put(CategoryNameKey, "Part Categories");
		tempHashMap.put(CategoryDisplayNameKey, "CategoryGroup_PartCategories");
		tempHashMap.put(CategoryChildrenKey, categoryItems);
		fullCategoryList.add(tempHashMap);

		// Other categories
		tempHashMap = new HashMap<String, Object>();
		tempHashMap.put(CategoryNameKey, Category_Alias);
		tempHashMap.put(CategoryDisplayNameKey,
				displayNameForCategory(Category_Alias));
		otherItems.add(tempHashMap);

		tempHashMap = new HashMap<String, Object>();
		tempHashMap.put(CategoryNameKey, Category_Moved);
		tempHashMap.put(CategoryDisplayNameKey,
				displayNameForCategory(Category_Moved));
		otherItems.add(tempHashMap);

		tempHashMap = new HashMap<String, Object>();
		tempHashMap.put(CategoryNameKey, Category_Primitives);
		tempHashMap.put(CategoryDisplayNameKey,
				displayNameForCategory(Category_Primitives));
		otherItems.add(tempHashMap);

		tempHashMap = new HashMap<String, Object>();
		tempHashMap.put(CategoryNameKey, Category_Subparts);
		tempHashMap.put(CategoryDisplayNameKey,
				displayNameForCategory(Category_Subparts));
		otherItems.add(tempHashMap);

		tempHashMap = new HashMap<String, Object>();
		tempHashMap.put(CategoryNameKey, "Other");
		tempHashMap.put(CategoryDisplayNameKey, "CategoryGroup_Other");
		tempHashMap.put(CategoryChildrenKey, otherItems);
		fullCategoryList.add(tempHashMap);

		return fullCategoryList;

	}// end categoryHierarchy

	// ========== categoryForPartName:
	// ==============================================
	//
	// Purpose: Returns the part's category.
	//
	// ==============================================================================
	public String categoryForPartName(String partName) {
		HashMap<String, Object> partList = (HashMap<String, Object>) partCatalog
				.get(PARTS_LIST_KEY);
		HashMap<String, Object> catalogInfo = (HashMap<String, Object>) partList
				.get(partName);
		String category = (String) (catalogInfo.get(PART_CATEGORY_KEY));

		return category;
	}

	// ========== favoritePartNames
	// =================================================
	//
	// Purpose: Returns all the part names the user has bookmarked as his
	// favorites.
	//
	// ==============================================================================
	public ArrayList<String> favoritePartNames() {
		return favorites;

	}// end favoritePartNames

	// ========== displayNameForCategory:
	// ===========================================
	//
	// Purpose: Returns the human-friendly category name
	//
	// ==============================================================================
	public String displayNameForCategory(String categoryName) {
		String displayName = null;

		if (categoryName == Category_All) {
			displayName = "AllCategories";
		} else if (categoryName == Category_Favorites) {
			displayName = "FavoritesCategory";
		} else {
			displayName = categoryName;
		}
		return displayName;
	}

	// ========== favoritePartCatalogRecords
	// ========================================
	//
	// Purpose: Returns all the part info records the user has bookmarked as his
	// favorites.
	//
	// ==============================================================================
	public ArrayList<Object> favoritePartCatalogRecords() {
		HashMap<String, Object> partList = (HashMap<String, Object>) partCatalog
				.get(PARTS_LIST_KEY);
		ArrayList<Object> parts = new ArrayList<Object>();
		HashMap<String, Object> partInfo = null;

		for (String partName : favorites) {
			partInfo = (HashMap<String, Object>) partList.get(partName);

			if (partInfo != null)
				parts.add(partInfo);
		}

		return parts;

	}// end favoritePartNames

	// ========== partCatalogRecordsInCategory:
	// =====================================
	//
	// Purpose: Returns all the parts in the given category. Returns null if the
	// category doesn't exist.
	//
	// ==============================================================================
	public ArrayList<Object> partCatalogRecordsInCategory(String categoryName) {
		ArrayList<Object> parts = null;

		if (categoryName == Category_All) {
			// Retrieve all parts. We can do this by getting the entire
			// (unsorted)
			// contents of PARTS_LIST_KEY in the partCatalog, which is actually
			// a dictionary of all parts.
			parts = allPartCatalogRecords();

		} else if (categoryName == Category_Favorites) {
			parts = favoritePartCatalogRecords();
		} else {
			HashMap<String, Object> partCatalogMap = (HashMap<String, Object>) partCatalog
					.get(PARTS_CATALOG_KEY);
			ArrayList<HashMap<String, Object>> category = (ArrayList<HashMap<String, Object>>) partCatalogMap
					.get(categoryName);
			HashMap<String, Object> partList = (HashMap<String, Object>) partCatalog
					.get(PARTS_LIST_KEY);
			ArrayList<Object> partsInCategory = new ArrayList<Object>();
			String partName = null;
			HashMap<String, Object> partInfo = null;

			for (HashMap<String, Object> categoryRecord : category) {
				partName = (String) categoryRecord.get(PART_NUMBER_KEY);
				partInfo = (HashMap<String, Object>) partList.get(partName);

				if (partInfo != null)
					partsInCategory.add(partInfo);
			}

			parts = partsInCategory;
		}

		return parts;

	}// end partCatalogRecordsInCategory:

	// #pragma mark -

	// ========== setDelegate:
	// ======================================================
	//
	// Purpose: Set the object responsible for receiving important notifications
	// from us.
	//
	// ==============================================================================
	/**
	 * @param delegateIn
	 * @uml.property name="delegate"
	 */
	public void setDelegate(IPartLibraryDelegate delegateIn) {
		delegate = delegateIn;
	}

	// ========== setFavorites:
	// =====================================================
	//
	// Purpose: Sets the parts which should appear in the Favorites category.
	// This list should have been saved in preferences and loaded by
	// the part library controller.
	//
	// ==============================================================================
	public void setFavorites(ArrayList<String> favoritesIn) {
		favorites.clear();
		favorites.addAll(favoritesIn);
	}

	// ========== setPartCatalog
	// ====================================================
	//
	// Purpose: Saves the local instance of the part catalog, which should be
	// the only copy of it in the program. Use +setSharedPartCatalog to
	// update it outside this class.
	//
	// Notes: The Part Catalog is structured as follows:
	//
	// partCatalog
	// |
	// |--> PARTS_CATALOG_KEY <HashMap<String,String>>
	// | |
	// | Keys are category names, e.g., "Brick"
	// | <ArrayList<String>>
	// | |
	// | <HashMap<String,String>>
	// | |--> PART_NUMBER_KEY <String> (e.g., "3001.dat")
	// |
	// |--> PARTS_LIST_KEY <HashMap<String,String>>
	// |
	// Keys are part reference numbers, e.g., "3001.dat"
	// <HashMap<String,String>>
	// |--> PART_NUMBER_KEY
	// |--> PART_NAME_KEY
	//
	// This data structure is PRIVATE. There is no get accessor. Query
	// this object for its part lists and build your own records.
	//
	// ==============================================================================
	public void setPartCatalog(HashMap<String, Object> newCatalog) {

		partCatalog = newCatalog;

		// Inform any open parts browsers of the change.
		// //todo
		// [[NSNotificationCenter defaultCenter]
		// postNotificationName: LDrawPartLibraryDidChangeNotification
		// object: self ();

	}// end setPartCatalog

	// #pragma mark -
	// #pragma mark ACTIONS
	// #pragma mark -

	// ========== load
	// ==============================================================
	//
	// Purpose: Loads the catalog from the part list stashed in the LDraw
	// folder.
	//
	// Returns: false if no part list exists. (You need to call -reloadParts: in
	// PartLibraryController then.)
	//
	// ==============================================================================
	public boolean load() {
		// NSFileManager *fileManager = [[[NSFileManager alloc] init]
		// autorelease();
		String catalogPath = LDrawPaths.sharedPaths().partCatalogPath();
		boolean partsListExists = false;
		String version = null;
		HashMap<String, Object> newCatalog = null;

		// Do we have an LDraw folder?
		if (catalogPath != null) {
			if (new File(catalogPath).isFile())
				partsListExists = true;
		}

		// Do we have a part list already?
		if (partsListExists == true) {
			// todo
			// newCatalog = [HashMap<String,String>
			// dictionaryWithContentsOfFile:catalogPath();
			version = (String) newCatalog.get(VERSION_KEY);

			if (version != null) {
				setPartCatalog(newCatalog);
			} else {
				// Older part catalogs don't have enough info in them
				partsListExists = false;
			}

		}

		return partsListExists;

	}// end load

	// ========== reloadParts:
	// ======================================================
	//
	// Purpose: Scans the contents of the LDraw/ folder and produces a
	// Mac-friendly index of parts.
	//
	// Is it fast? No. Is it easy to code? Yes.
	//
	// Someday in the rosy future, this method should be recoded to
	// simply traverse the directory tree and deal with subfolders on
	// the fly. But that's not how it is now. Instead, I'm doing it
	// all manually. Folders searched are:
	//
	// LDraw/p/
	// LDraw/p/48/
	//
	// LDraw/parts/
	// LDraw/parts/s/
	//
	// LDraw/Unofficial/p/
	// LDraw/Unofficial/p/48/
	// LDraw/Unofficial/parts/
	// LDraw/Unofficial/parts/s/
	//
	// It is important that the part name added to the library bear
	// the correct reference style. For LDraw/p/ and LDraw/parts/, it
	// is simply the filename (in lowercase). But for subdirectories,
	// the filename must be prefixed with the subdirectory in DOS
	// format, i.e., "s\file.dat" or "48\file.dat".
	//
	// ==============================================================================
	public boolean reloadParts() {
		// NSFileManager *fileManager = [[[NSFileManager alloc] init]
		// autorelease();
		LDrawPaths sharedPaths = LDrawPaths.sharedPaths();
		String ldrawPath = sharedPaths.preferredLDrawPath();
		ArrayList<Object> searchPaths = new ArrayList<Object>();

		String prefix_primitives48 = String.format("%s\\",
				LDrawPaths.PRIMITIVES_48_DIRECTORY_NAME);
		String prefix_subparts = String.format("%s\\",
				LDrawPaths.SUBPARTS_DIRECTORY_NAME);

		// make sure the LDraw folder is still valid; otherwise, why bother
		// doing anything?
		if (sharedPaths.validateLDrawFolder(ldrawPath) == false)
			return false;

		// Parts
		HashMap<String, String> tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path",
				sharedPaths.partsPathForDomain(LDrawDomainT.LDrawUserOfficial));
		searchPaths.add(tempHashMap);

		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path", sharedPaths
				.partsPathForDomain(LDrawDomainT.LDrawUserUnofficial));
		searchPaths.add(tempHashMap);

		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path", sharedPaths
				.partsPathForDomain(LDrawDomainT.LDrawInternalOfficial));
		searchPaths.add(tempHashMap);

		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path", sharedPaths
				.partsPathForDomain(LDrawDomainT.LDrawInternalUnofficial));
		searchPaths.add(tempHashMap);

		// Primitives
		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path", sharedPaths
				.partsPathForDomain(LDrawDomainT.LDrawInternalUnofficial));
		tempHashMap.put("category", Category_Primitives);
		searchPaths.add(tempHashMap);

		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path", sharedPaths
				.partsPathForDomain(LDrawDomainT.LDrawInternalOfficial));
		tempHashMap.put("category", Category_Primitives);
		searchPaths.add(tempHashMap);

		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path", sharedPaths
				.partsPathForDomain(LDrawDomainT.LDrawInternalOfficial));
		tempHashMap.put("category", Category_Primitives);
		searchPaths.add(tempHashMap);

		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path", sharedPaths
				.partsPathForDomain(LDrawDomainT.LDrawInternalUnofficial));
		tempHashMap.put("category", Category_Primitives);
		searchPaths.add(tempHashMap);

		// Primitives 48
		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path",
				sharedPaths.partsPathForDomain(LDrawDomainT.LDrawUserOfficial));
		tempHashMap.put("category", Category_Primitives);
		tempHashMap.put("prefix", prefix_primitives48);
		searchPaths.add(tempHashMap);

		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path", sharedPaths
				.partsPathForDomain(LDrawDomainT.LDrawUserUnofficial));
		tempHashMap.put("category", Category_Primitives);
		tempHashMap.put("prefix", prefix_primitives48);
		searchPaths.add(tempHashMap);

		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path", sharedPaths
				.partsPathForDomain(LDrawDomainT.LDrawInternalOfficial));
		tempHashMap.put("category", Category_Primitives);
		tempHashMap.put("prefix", prefix_primitives48);
		searchPaths.add(tempHashMap);

		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path", sharedPaths
				.partsPathForDomain(LDrawDomainT.LDrawInternalUnofficial));
		tempHashMap.put("category", Category_Primitives);
		tempHashMap.put("prefix", prefix_primitives48);
		searchPaths.add(tempHashMap);

		// Subparts
		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path",
				sharedPaths.partsPathForDomain(LDrawDomainT.LDrawUserOfficial));
		tempHashMap.put("category", Category_Primitives);
		tempHashMap.put("prefix", prefix_subparts);
		searchPaths.add(tempHashMap);

		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path", sharedPaths
				.partsPathForDomain(LDrawDomainT.LDrawUserUnofficial));
		tempHashMap.put("category", Category_Primitives);
		tempHashMap.put("prefix", prefix_subparts);
		searchPaths.add(tempHashMap);

		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path", sharedPaths
				.partsPathForDomain(LDrawDomainT.LDrawInternalOfficial));
		tempHashMap.put("category", Category_Primitives);
		tempHashMap.put("prefix", prefix_subparts);
		searchPaths.add(tempHashMap);

		tempHashMap = new HashMap<String, String>();
		tempHashMap.put("path", sharedPaths
				.partsPathForDomain(LDrawDomainT.LDrawInternalUnofficial));
		tempHashMap.put("category", Category_Primitives);
		tempHashMap.put("prefix", prefix_subparts);
		searchPaths.add(tempHashMap);

		String partCatalogPath = sharedPaths.partCatalogPath();
		HashMap<String, Object> newPartCatalog = new HashMap<String, Object>();

		int partCount = 0;

		// Start the progress bar so that we know what's happening.
		for (Object item : searchPaths) {
			String path = ((HashMap<String, String>) item).get("path");
			File dir = new File(path);
			if (!dir.exists() || !dir.isDirectory()) {
				System.out.println("Invalid path: " + path);
				continue;
			}

			File[] files = dir.listFiles();
			partCount += files.length;
		}

		// delegate.partLibrary(this, partCount);

		// Create the new part catalog. We will then fill it with folder
		// contents.
		newPartCatalog.put(PARTS_CATALOG_KEY, new HashMap<String, Object>());
		newPartCatalog.put(PARTS_LIST_KEY, new HashMap<String, Object>());

		// Scan for each part folder.
		for (Object item : searchPaths) {
			HashMap<String, String> record = (HashMap<String, String>) item;
			addPartsInFolder(record.get("path"), newPartCatalog,
					record.get("category"), // override all internal categories
					record.get("prefix"));
		}

		// todo
		// String version = [[[NSBundle mainBundle]
		// infoDictionary].get(@"CFBundleVersion"();
		// [newPartCatalog setObject:version VERSION_KEY();
		// [newPartCatalog setObject:@"1.0" COMPATIBILITY_VERSION_KEY();

		// Save the part catalog out for future reference.
		// todo
		// newPartCatalog.writeToFile:partCatalogPath atomically:true();
		//
		setPartCatalog(newPartCatalog);

		// [[NSNotificationCenter defaultCenter]
		// postNotificationName:LDrawPartLibraryReloaded object:self ();

		// We succeeded in loading the parts!
		return true;

	}// end reloadParts:

	// #pragma mark -
	// #pragma mark FAVORITES
	// #pragma mark -

	// ========== addPartNameToFavorites:
	// ===========================================
	//
	// Purpose: Adds the given part name to the "Favorites" category.
	//
	// ==============================================================================
	public void addPartNameToFavorites(String partName) {
		favorites.add(partName);
		saveFavoritesToUserDefaults();

		// Inform any open parts browsers of the change.
		// todo
		// [[NSNotificationCenter defaultCenter]
		// postNotificationName: LDrawPartLibraryDidChangeNotification
		// object: self ();

	}// end addPartNameToFavorites:

	// ========== removePartNameFromFavorites:
	// ======================================
	//
	// Purpose: Removes the given part name to the "Favorites" category.
	//
	// ==============================================================================
	public void removePartNameFromFavorites(String partName) {
		favorites.remove(partName);
		saveFavoritesToUserDefaults();

		// Inform any open parts browsers of the change.
		// todo
		// [[NSNotificationCenter defaultCenter]
		// postNotificationName: LDrawPartLibraryDidChangeNotification
		// object: self ();

	}// end removePartNameFromFavorites:

	// ========== saveFavoritesToUserDefaults
	// =======================================
	//
	// Purpose: Writes the favorite parts list to preferences.
	//
	// ==============================================================================
	public void saveFavoritesToUserDefaults() {
		delegate.partLibrary(this, favorites);

	}// end saveFavoritesToUserDefaults

	// #pragma mark -
	// #pragma mark FINDING PARTS
	// #pragma mark -

	// ========== loadImageForName:inGroup:
	// =========================================
	//
	// Purpose: This is a thread-safe method which causes the texture image of
	// the given name to be loaded out of the LDraw folder.
	//
	// ==============================================================================
	public void loadImageForName(String imageName, DispatchGroup parentGroup) {
		// todo
		// Determine if the model needs to be parsed.
		// Dispatch to a serial queue to effectively mutex the query
		// #if USE_BLOCKS
		// dispatch_group_async(parentGroup, catalogAccessQueue,
		// ^{
		// ArrayList<String> requestingGroups = null;
		// #endif
		// CGImageRef image = null;
		// boolean alreadyParsing = false; // another thread is already parsing
		// partName
		//
		// // Already been parsed?
		// image = (CGImageRef)loadedImages.get(imageName();
		// if(image == null)
		// {
		// #if USE_BLOCKS
		// // Is it being parsed? If so, all we need to do is wait for whoever
		// // is parsing it to finish.
		// requestingGroups = parsingGroups.get(imageName();
		// alreadyParsing = (requestingGroups != null);
		//
		// if(alreadyParsing == false)
		// {
		// // Start a registry for all the dispatch groups which attempt to
		// // load the same model. When parsing is complete, they will all
		// // be signaled.
		// requestingGroups = [[NSMutableArray alloc] init();
		// parsingGroups setObject:requestingGroups imageName();
		// [requestingGroups release();
		// }
		//
		// // Register the calling group as having also requested a parse
		// // for this file. This ensures the calling group cannot complete
		// // until the parse is complete on whatever thread is actually
		// // doing it.
		// dispatch_group_enter(parentGroup);
		// [requestingGroups addObject(NSValue valueWithPointer:parentGroup]();
		// #endif

		// Nobody has started parsing it yet, so we win! Parse from disk.
		// if(alreadyParsing == false)
		// {
		// #if USE_BLOCKS
		// dispatch_group_async(parentGroup,
		// dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0),
		// ^{
		// #endif
		// String *imagePath =
		// LDrawPaths.sharedPaths().pathForTextureName:imageName();

		// #if USE_BLOCKS
		// //------------------------------------------------------
		// readImageAtPath:imagePath asynchronously:true
		// completionHandler:^(CGImageRef image)
		// {
		// if(image) CFRetain(image);
		//
		// // Register new image in the library (serial queue "mutex" protected)
		// dispatch_group_async(parentGroup, catalogAccessQueue,
		// ^{
		// if(image != null)
		// {
		// loadedImages setObject:(id)image imageName();
		// }
		//
		// // Notify waiting threads we are finished parsing this part.
		// for(NSValue *waitingGroupPtr in requestingGroups)
		// {
		// DispatchGroup waitingGroup = [waitingGroupPtr pointerValue();
		// dispatch_group_leave(waitingGroup);
		// }
		// parsingGroups removeObjectForKey:imageName();
		//
		// if(image) CFRelease(image);
		// });
		// }();
		// #else
		// //------------------------------------------------------------------------
		// **** Non-multithreaded fallback code ****
		// image = (CGImageRef)readImageAtPath:imagePath asynchronously:false
		// completionHandler:null();
		// if(image != null)
		// {
		// loadedImages setObject:(id)image imageName();
		// }
		// #endif
		// //-----------------------------------------------------------------------
		// #if USE_BLOCKS
		// });
		// #endif
		// }
		// }
		// #if USE_BLOCKS
		// });
		// #endif

	}// end loadImageForName:

	// ========== loadModelForName:inGroup:
	// =========================================
	//
	// Purpose: This is a thread-safe method which causes the model of the given
	// name to be loaded out of the LDraw folder.
	//
	// ==============================================================================
	public void loadModelForName(String partName, String referenceName, DispatchGroup parentGroup) {
		LDrawModel model = null;
				
		// Already been parsed?		
		model = (LDrawModel) loadedFiles.get(referenceName);
		if (model == null) {
			// Nobody has started parsing it yet, so we win! Parse from disk.
			String partPath = LDrawPaths.sharedPaths()
					.pathForPartName(partName);
			model = readModelAtPath(partPath, parentGroup, null);
			if (model != null) {
				loadedFiles.put(referenceName, model);
			}
		}
	}// end loadModelForName:

	// ========== imageForTextureName:
	// ==============================================
	//
	// Purpose: Returns an image from our library cache.
	//
	// ==============================================================================
	// - (CGImageRef) imageForTextureName(String imageName
	// {
	// CGImageRef image = null;
	// String imagePath = null;
	//
	// // Has it already been parsed?
	// image = (CGImageRef)loadedImages.get(imageName();
	//
	// if(image == null)
	// {
	// // Well, this means we have to try getting it off the disk!
	// imagePath = LDrawPaths.sharedPaths().pathForTextureName:imageName();
	// image = readImageAtPath:imagePath asynchronously:false
	// completionHandler:null();
	//
	// if(image != null)
	// loadedImages setObject:(id)image imageName();
	// }
	//
	// return image;
	//
	// }

	// ========== imageForTexture:
	// ==================================================
	//
	// Purpose: Returns the image specified by the texture object.
	//
	// ==============================================================================
	// - (CGImageRef) imageForTexture:(LDrawTexture *)texture
	// {
	// String imageName = [texture imageReferenceName();
	// CGImageRef image = null;
	//
	// // Try to get a live link if we have parsed this part off disk already.
	// image = imageForTextureName:imageName();
	//
	// if(image == null) {
	// //we're grasping at straws. See if this is a reference to an external
	// // file in the same folder.
	// image = imageFromNeighboringFileForTexture:texture();
	// }
	//
	// return image;
	//
	// }//end imageForTexture:

	// ========== imageFromNeighboringFileForTexture:
	// ===============================
	//
	// Purpose: Attempts to resolve the texture's name reference against a file
	// located in the same parent folder as the file in which the part
	// is contained.
	//
	// This should be a method of last resort, after searching the part
	// library.
	//
	// Note: This is BAD CODE. It caches things permanently. We need to move
	// to the new model manager to track when to get rid of images.
	//
	// ==============================================================================
	// - (CGImageRef) imageFromNeighboringFileForTexture:(LDrawTexture *)texture
	// {
	// LDrawFile *enclosingFile = [texture enclosingFile();
	// String filePath = [enclosingFile path();
	// String fileDirectory = null;
	// String imageName = null;
	// String testPath = null;
	// String imagePath = null;
	// CGImageRef image = null;
	// NSFileManager *fileManager = null;
	//
	// if(filePath != null)
	// {
	// fileManager = [[[NSFileManager alloc] init] autorelease();
	// fileDirectory = [filePath stringByDeletingLastPathComponent();
	// imageName = [texture imageDisplayName(); // handle case-sensitive
	// filesystem
	//
	// // look at path = parentFolder/textures/name
	// {
	// testPath = [fileDirectory
	// stringByAppendingPathComponent:TEXTURES_DIRECTORY_NAME();
	// testPath = [testPath stringByAppendingPathComponent:imageName();
	// if([fileManager fileExistsAtPath:testPath])
	// {
	// imagePath = testPath;
	// }
	// }
	//
	// //look at path = parentFolder/name
	// if(imagePath == null)
	// {
	// testPath = [fileDirectory stringByAppendingPathComponent:imageName();
	// if([fileManager fileExistsAtPath:testPath])
	// {
	// imagePath = testPath;
	// }
	// }
	//
	// // Load if we found something
	// if(imagePath)
	// {
	// image = readImageAtPath:testPath asynchronously:false
	// completionHandler:null();
	// if(image != null)
	// loadedImages setObject:(id)image imageName();
	// }
	// }
	//
	// return image;
	//
	// }//end imageFromNeighboringFileForTexture:

	// ========== modelForName:
	// =====================================================
	//
	// Purpose: Attempts to find the part based only on the given name.
	// This method can only find parts in the LDraw folder; it returns
	// null if fed an MPD submodel name.
	//
	// falseT THREAD SAFE!
	//
	// Notes: The part is looked up by the name specified in the part command.
	// For regular parts and primitives, this is simply the filename
	// as found in LDraw/parts or LDraw/p. But for subparts found in
	// LDraw/parts/s, the filename is "s\partname.dat". (Same goes for
	// LDraw/p/48.) This icky inconsistency is handled in
	// -pathForFileName:.
	//
	// ==============================================================================
	public LDrawModel modelForName(String imageName) {
		LDrawModel model = null;
		String partPath = null;

		// Has it already been parsed?
		model = (LDrawModel) loadedFiles.get(imageName);
		if (model == null) {
			// System.out.println("ImageName:"+imageName);
			// Well, this means we have to try getting it off the disk!
			// This case is only hit when a library part uses another library
			// part, e.g.
			// a brick grabs a collection-of-studs part.
			partPath = LDrawPaths.sharedPaths().pathForPartName(imageName);
			model = readModelAtPath(partPath, new DispatchGroup(), null);

			if (model != null) {
				loadedFiles.put(imageName, model);
			}
		}

		return model;

	}// end modelForName

	// ========== modelForPartInternal:
	// =====================================================
	//
	// Purpose: Returns the model to which this part refers. You can then ask
	// the model to draw itself.
	//
	// falseT THREAD SAFE!
	//
	// Notes: The part is looked up by the name specified in the part command.
	// For regular parts and primitives, this is simply the filename
	// as found in LDraw/parts or LDraw/p. But for subparts found in
	// LDraw/parts/s, the filename is "s\partname.dat". (Same goes for
	// LDraw/p/48.) This icky inconsistency is handled in
	// -pathForFileName:.
	//
	// This has been marked "internal" because the API is now only used
	// _within_ the part library, not by public clients.
	//
	// ==============================================================================
	public LDrawModel modelForPartInternal(LDrawPart part) {
		String imageName = part.referenceName();
		LDrawModel model = null;

		// Try to get a live link if we have parsed this part off disk already.
		// Ben sez: This routine is currently authorized to load on demand, but
		// I never see that code run and I don't think it is suppose to.
		model = modelForName(imageName);

		if (model == null) {
			// We didn't find it in the LDraw folder. Hopefully this is a
			// reference
			// to another model in an MPD file.
			model = part.referencedMPDSubmodel();
		}

		return model;

	}// end modelForPartInternal:

	// ========== modelForName_threadSafe:
	// ==========================================
	//
	// Purpose: Returns the model to which this part name refers, thread-safe.
	//
	// Notes: This will falseT attempt to read the file off disk. This method is
	// only intended to be called during the multi-threaded file
	// loading process, so there should be no need to do lazy loading.
	//
	// ==============================================================================
	public LDrawModel modelForName_threadSafe(String imageName) {
		LDrawModel model = null;

		// #if USE_BLOCKS
		// dispatch_sync(catalogAccessQueue, ^{
		// #endif
		model = (LDrawModel) loadedFiles.get(imageName);
		// #if USE_BLOCKS
		// });
		// #endif

		return model;
	}

	// ========== textureTagForTexture:
	// =============================================
	//
	// Purpose: Returns the OpenGL tag necessary to draw the image represented
	// by the high-level texture object.
	//
	// ==============================================================================
	public int textureTagForTexture(LDrawTexture texture) {
		String name = texture.imageReferenceName();
		Integer tagNumber = optimizedTextures.get(name);
		int textureTag = 0;

		if (tagNumber != null) {
			textureTag = tagNumber.intValue();
		} else {

			// todo
			// CGImageRef image = imageForTexture:texture();
			//
			// if(image)
			// {
			// CGRect canvasRect = CGRectMake( 0, 0,
			// FloorPowerOfTwo(CGImageGetWidth(image)),
			// FloorPowerOfTwo(CGImageGetHeight(image)) );
			// uint8_t *imageBuffer = malloc( (canvasRect.size.width) *
			// (canvasRect.size.height) * 4 );
			// CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
			// CGContextRef bitmapContext = CGBitmapContextCreate(imageBuffer,
			// canvasRect.size.width,
			// canvasRect.size.height,
			// 8, // bits per component
			// canvasRect.size.width * 4, // bytes per row
			// colorSpace,
			// kCGBitmapByteOrder32Host | kCGImageAlphaPremultipliedFirst
			// );
			//
			// // Draw the image into the bitmap context. By doing so, we use
			// the mighty
			// // power of Quartz handle the nasty conversion details necessary
			// to fill up
			// // a pixel buffer in an OpenGL-friendly storage format and color
			// space.
			// CGContextSetBlendMode(bitmapContext, kCGBlendModeCopy);
			// CGContextDrawImage(bitmapContext, canvasRect, image);
			//
			// // CGImageRef output = CGBitmapContextCreateImage(bitmapContext);
			// // CGImageDestinationRef myImageDest =
			// CGImageDestinationCreateWithURL((CFURLRef)[NSURL
			// fileURLWithPath:@"/out.png"], kUTTypePNG, 1, null);
			// // //HashMap<String,String>* options = [HashMap<String,String>
			// dictionaryWithObjectsAndKeys: [NSNumber numberWithInt:1.0],
			// kCGImageDestinationLossyCompressionQuality, null(); // Don't know
			// if this is necessary
			// // CGImageDestinationAddImage(myImageDest, output, null);
			// // CGImageDestinationFinalize(myImageDest);
			// // CFRelease(myImageDest);
			//
			// // Generate a tag for the texture we're about to generate, then
			// set it as
			// // the active texture.
			// // Note: We are using non-rectangular textures here, which
			// started as an
			// // extension (_EXT) and is now ratified by the review board
			// (_ARB)
			// glGenTextures(1, &textureTag);
			// glBindTexture(GL_TEXTURE_2D, textureTag);
			//
			// // Generate Texture!
			// glPixelStorei(GL_PACK_ROW_LENGTH, canvasRect.size.width * 4);
			// glPixelStorei(GL_PACK_ALIGNMENT, 1); // byte alignment
			//
			// glTexImage2D( GL_TEXTURE_2D, 0, GL_RGBA8, // texture type params
			// canvasRect.size.width, canvasRect.size.height, 0, // source image
			// (w, h)
			// GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, // source storage format
			// imageBuffer );
			// // see function notes about the source storage format.
			//
			// // This requires GL_EXT_framebuffer_object, available on all
			// renderers on 10.6.8 and beyond.
			// // Build mipmaps so we can use linear-mipmap-linear
			// glGenerateMipmapEXT(GL_TEXTURE_2D);
			//
			// glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
			// glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
			// glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			// glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
			// GL_LINEAR_MIPMAP_LINEAR); // This enables mip-mapping - makes
			// textures look good when small.
			// glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT,
			// 4.0); // Max anisotropic filtering of all renderers on 10.6.8 is
			// 16.0.
			// // This keeps texture res high when looking at a tile from a low
			// angle.
			//
			// glBindTexture(GL_TEXTURE_2D, 0);
			//
			// optimizedTextures setObject(NSNumber
			// numberWithUnsignedInt:textureTag] name();
			//
			// // free memory
			// // free(imageBuffer);
			// CFRelease(colorSpace);
			// CFRelease(bitmapContext);
			// }
		}

		return textureTag;
	}

	// #pragma mark -
	// #pragma mark UTILITIES
	// #pragma mark -

	// ========== addPartsInFolder:toCatalog:underCategory:
	// =========================
	//
	// Purpose: Scans all the parts in folderPath and adds them to the given
	// catalog, filing them under the given category. Pass null for
	// category if you wish to use the categories defined in the parts
	// themselves.
	//
	// Parameters: categoryOverride - force all parts in the folder to be filed
	// under this category, rather than the one
	// defined inside the part.
	// namePrefix - appends this prefix to each part scanned.
	// Part references in LDraw/parts/s should be
	// prefixed with the DOS path "s\". Pass null
	// to ignore the prefix.
	// progressPanel - a progress panel which is displaying the
	// progress of the creation of the part
	// catalog.
	//
	// ==============================================================================
	public void addPartsInFolder(String folderPath,
			HashMap<String, Object> catalog, String categoryOverride,
			String namePrefix) {
		// NSFileManager *fileManager = [[[NSFileManager alloc] init]
		// autorelease();
		// Not working for some reason. Why?
		// ArrayList<String> *readableFileTypes = [NSDocument readableTypes();
		// System.out.println(String.format("readable types: %s",
		// readableFileTypes);
		ArrayList<String> readableFileTypes = new ArrayList<String>();
		readableFileTypes.add("dat");
		readableFileTypes.add("ldr");

		ArrayList<String> partNames = new ArrayList<String>();
		if (new File(folderPath).isDirectory())
			for (File file : new File(folderPath).listFiles())
				if (file.isFile())
					partNames.add(file.getName());

		int numberOfParts = partNames.size();
		int counter;

		String currentPath = null;
		HashMap<String, String> categoryRecord = null;

		// Get the subreference tables out of the main catalog (they should
		// already exist!).
		HashMap<String, Object> catalog_partNumbers = (HashMap<String, Object>) catalog
				.get(PARTS_LIST_KEY); // lookup parts by number
		HashMap<String, Object> catalog_categories = (HashMap<String, Object>) catalog
				.get(PARTS_CATALOG_KEY); // lookup parts by category
		ArrayList<Object> catalog_category = null;

		// Loop through the entire contents of the directory and extract the
		// information for every part therein.
		for (counter = 0; counter < numberOfParts; counter++) {
			String partName = partNames.get(counter);
			currentPath = folderPath + partName;
			String[] temp = partName.split("\\.");
			String ext = null;
			if (temp.length > 1)
				ext = temp[1];

			if (readableFileTypes.contains(ext) == true) {
				categoryRecord = catalogInfoForFileAtPath(currentPath);

				// Make sure the part file was valid!
				if (categoryRecord != null && categoryRecord.size() > 0) {
					// ---------- Alter catalog info
					// --------------------------------

					if (categoryOverride != null)
						categoryRecord.put(PART_CATEGORY_KEY, categoryOverride);

					// Parts in subfolders of LDraw/parts must have a name
					// prefix of
					// their subpath, e.g., "s\partname.dat" for a part in the
					// LDraw/parts/s folder.
					if (namePrefix != null) {
						String partNumber = null;
						partNumber = categoryRecord.get(PART_NUMBER_KEY);
						partNumber = namePrefix + partNumber;
						categoryRecord.put(PART_NUMBER_KEY, partNumber);
					}

					// ---------- Catalog the part
					// ----------------------------------

					String category = categoryRecord.get(PART_CATEGORY_KEY);
					if (category != null) {
						catalog_category = (ArrayList<Object>) catalog_categories
								.get(category);
						if (catalog_category == null) {
							// We haven't encountered this category yet.
							// Initialize it now.
							catalog_category = new ArrayList<Object>();
							catalog_categories.put(category, catalog_category);
						}

						// For some reason, I made each entry in the category a
						// dictionary with part info. This was a database design
						// mistake; it should have been an array of part
						// reference
						// numbers, if not just built up at runtime.

						String categoryEntry = categoryRecord
								.get(PART_NUMBER_KEY);

						catalog_category.add(categoryEntry);

						// Also file the part in a master list by reference
						// name.
						catalog_partNumbers.put(
								categoryRecord.get(PART_NUMBER_KEY),
								categoryRecord);
					}

					// System.out.println(String.format("processed %s",
					// [partNames objectAtIndex:counter]);
				}
			}
			// todo
			// delegate.partLibraryIncrementLoadProgressCount(this);

		}// end loop through files

	}// end addPartsInFolder:toCatalog:underCategory:

	// ========== categoryForDescription:
	// ===========================================
	//
	// Purpose: Returns the category for the given modelDescription. This is
	// the first line of the file for non-MPD documents. For instance:
	//
	// 0 Brick 2 x 4
	//
	// This part would be in the category "Brick", and has the
	// description "Brick  2 x  4".
	//
	// ==============================================================================
	public String categoryForDescription(String modelDescription) {
		String category = null;
		int firstSpace; // range of the category string in the first line.

		// The category name is the first word in the description.
		firstSpace = modelDescription.indexOf(" ");

		if (firstSpace != -1)
			category = modelDescription.substring(firstSpace);
		else
			category = modelDescription;

		// Deal with any weird notational marks

		// Alias parts begin with an underscore. These things are so annoying
		// I'm
		// going to dump them in a pseudo category. This is kind of a hack, but
		// at
		// least it's a prettifying one.
		if (category.charAt(0) == '_') {
			category = Category_Alias;
		}
		// Moved parts always begin with ~Moved, which is ugly. We'll strip the
		// '~'.
		else if (category.charAt(0) == '~') {
			category = category.substring(1);
		}

		return category;

	}// end categoryForDescription:

	// ========== descriptionForPart:
	// ===============================================
	//
	// Purpose: Returns the description of the given part based on its name.
	//
	// ==============================================================================
	public String descriptionForPart(LDrawPart part) {
		// Look up the verbose part description in the scanned part catalog.
		HashMap<String, Object> partList = (HashMap<String, Object>) partCatalog
				.get(PARTS_LIST_KEY);

		HashMap<String, Object> partRecord = null;
		if (partList != null)
			partRecord = (HashMap<String, Object>) partList.get(part
					.getReferenceName());
		String partDescription = null;
		if (partRecord != null)
			partDescription = (String) partRecord.get(PART_NAME_KEY);

		// Maybe it's an MPD reference?
		if (partDescription == null) {
			LDrawModel mpdModel = part.referencedMPDSubmodel();
			if (mpdModel != null)
				partDescription = mpdModel.browsingDescription();
		}

		// If the part STILL isn't known, all we can really do is just display
		// the
		// number.
		if (partDescription == null) {
			partDescription = part.displayName();
		}

		return partDescription;

	}// end descriptionForPart:

	// ========== descriptionForPartName:
	// ===========================================
	//
	// Purpose: Returns the description associated with the given part name.
	// For example, passing "3001.dat" returns "Brick 2 x 4".
	// If the name isn't known to the Part Library, we just return name.
	//
	// Note: If you have a reference to the LDrawPart itself, you should pass
	// it to -descriptionForPart instead.
	//
	// ==============================================================================
	public String descriptionForPartName(String name) {
		// Look up the verbose part description in the scanned part catalog.
		HashMap<String, Object> partList = (HashMap<String, Object>) partCatalog
				.get(PARTS_LIST_KEY);
		HashMap<String, Object> partRecord = (HashMap<String, Object>) partList
				.get(name);
		String partDescription = (String) partRecord.get(PART_NAME_KEY);
		// If the part isn't known, all we can really do is just display the
		// number.
		if (partDescription == null)
			partDescription = name;

		return partDescription;

	}// end descriptionForPartName:

	// ========== catalogInfoForFileAtPath:
	// =========================================
	//
	// Purpose: Pulls out the catalog-relevate metadata out of the given file.
	// By convention, the first line of an non-MPD LDraw file is the
	// description; e.g.,
	//
	// 0 Brick 2 x 4
	//
	// This part is thus in the category "Brick", and has the
	// description "Brick  2 x  4".
	//
	// Returns: null if the file is not valid.
	//
	// PART_NUMBER_KEY string
	// PART_CATEGORY_KEY string
	// PART_KEYWORDS_KEY array
	// PART_NAME_KEY string
	//
	// ==============================================================================
	public HashMap<String, String> catalogInfoForFileAtPath(String filepath) {
		// NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init();

		String fileContents = LDrawUtilities.stringFromFile(filepath);
		// NSCharacterSet *whitespace = [NSCharacterSet
		// whitespaceAndNewlineCharacterSet();

		String partNumber = null;
		String partDescription = null;
		String category = null;
		ArrayList<String> keywords = null;

		HashMap<String, String> catalogInfo = null;

		// Read the first line of the file. Make sure the file is parsable.
		if (fileContents != null && fileContents.length() > 0) {
			int stringLength = fileContents.length();
			int lineStartIndex = 0;
			int nextlineStartIndex = 0;
			int newlineIndex = 0; // index of the first newline character in the
									// line.
			int lineLength = 0;
			// String line = null;
			String lineCode = null;
			// ByteBuffer lineRemainder = null;

			catalogInfo = new HashMap<String, String>();

			// Get the name of the part.
			// We need a standard way to reference it; use lower-case to avoid
			// any
			// case-sensitivity issues.
			partNumber = new File(filepath).getName().split("\\.")[0]
					.toLowerCase();
			catalogInfo.put(PART_NUMBER_KEY, partNumber);

			String[] lines = fileContents.replace("\r", "").split("\n");
			for (String line : lines) {
				// LDraw uses DOS lineendings
				// fileContents.getLineStart(lineStartIndex, nextlineStartIndex,
				// newlineIndex, new Range(nextlineStartIndex,1) ); //that is,
				// contains the first character.

				// lineLength = newlineIndex - lineStartIndex;
				// line = fileContents.substring(lineStartIndex, newlineIndex);
				// line = lines[]
				StringTokenizer strTokenizer = new StringTokenizer(line);
				lineCode = strTokenizer.nextToken();

				// Check to see if this is a valid LDraw header.
				if (lineStartIndex == 0) {
					if (lineCode != "0")
						break;
					String strTemp = "";
					while (strTokenizer.hasMoreTokens())
						strTemp += strTokenizer.nextToken() + " ";
					partDescription = new String(strTemp);
					catalogInfo.put(PART_NAME_KEY, partDescription);
					break;
				}
				// todo ������ �� �ʿ�����?
				// else if(lineCode =="0")
				// {
				// // Try to find keywords or category
				// String *meta =[LDrawUtilities.readNextField:lineRemainder
				// remainder:&lineRemainder();
				//
				// if([meta ==LDRAW_CATEGORY])
				// {
				// category = [lineRemainder
				// stringByTrimmingCharactersInSet:whitespace();
				//
				// // Turns out !CATEGORY is not as reliable as it ought to be.
				// // In typical LDraw fashion, the feature was not have a
				// // simultaneous, universal deployment. Unfortunately, the
				// // only categories I deem to be consistent and advantageous
				// // under the current system are the two-word categories that
				// // couldn't be represented under the old system.
				// //
				// // Also, allow the !LDRAW_ORG Part Alias to take precedence
				// // if it has already been found.
				// if( [category rangeOfString:@" "].location != NSNotFound
				// && catalogInfo.get(PART_CATEGORY_KEY] == null )
				// {
				// catalogInfo.put(category PART_CATEGORY_KEY();
				// }
				// }
				// else if([meta ==LDRAW_KEYWORDS])
				// {
				// if(keywords == null)
				// {
				// keywords =new ArrayList<String>();
				// catalogInfo.put(keywords PART_KEYWORDS_KEY();
				// }
				// // Keywords can be multiline, so must add to any we've
				// already collected!
				// ArrayList<> newKeywords = [lineRemainder
				// componentsSeparatedByCharactersInSet(NSCharacterSet
				// characterSetWithCharactersInString:@","]();
				// for(String *keyword in newKeywords)
				// {
				// [keywords addObject(keyword
				// stringByTrimmingCharactersInSet:whitespace]();
				// }
				// }
				// else if(meta ==LDRAW_ORG)
				// {
				// // Force alias parts into a ghetto category which will keep
				// // them far away from normal building.
				// String officialStatus = lineRemainder.trim();
				// if[officialStatus.contains("Part Alias"))
				// {
				// category = Category_Alias;
				// catalogInfo.put(PART_CATEGORY_KEY, category);
				// }
				// }
				// }
				// else if([lineCode length] == 0)
				// {
				// // line is blank. Skip.
				// }
				// else
				// {
				// // Non-comment, non-blank line. This cannot be part of the
				// header.
				// break;
				// }
			}

			// If no !CATEGORY directive, the the category is to be derived from
			// the
			// first word of the description.
			if (catalogInfo.get(PART_NAME_KEY) != null
					&& catalogInfo.get(PART_CATEGORY_KEY) == null) {
				partDescription = catalogInfo.get(PART_NAME_KEY);
				category = categoryForDescription(partDescription);
				catalogInfo.put(PART_CATEGORY_KEY, category);
			}
		} else {
			System.out.println(String
					.format("%s is not a valid file", filepath));
		}

		// [catalogInfo retain();
		// [pool drain();

		return catalogInfo;

	}// end catalogInfoForFileAtPath

	// ========== readImageAtPath:
	// ==================================================
	//
	// Purpose: Parses the model found at the given path, adds it to the list of
	// loaded parts, and returns the model.
	//
	// Notes: The model is returned from the method if asynchronous is false.
	// Otherwise, returns null and passes the completed model via the
	// block instead.
	//
	// ==============================================================================
	// - (CGImageRef) readImageAtPath(String imagePath
	// boolean asynchronous
	// completionHandler:(void (^)(CGImageRef))completionBlock
	// {
	// DispatchGroup group = null;
	// #if USE_BLOCKS
	// __block
	// #endif
	// CGImageRef image = null;
	//
	// #if USE_BLOCKS
	// group = dispatch_group_create();
	// #endif
	//
	// #if USE_BLOCKS
	// if(asynchronous == false)
	// {
	// dispatch_group_wait(group, DISPATCH_TIME_FOREVER);
	// #endif
	// image =[LDrawUtilities.imageAtPath:imagePath();
	// #if USE_BLOCKS
	// }
	// else
	// {
	// dispatch_group_notify(group,
	// dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0),
	// ^{
	// image =[LDrawUtilities.imageAtPath:imagePath();
	//
	// if(completionBlock)
	// completionBlock(image);
	// });
	// }
	//
	// dispatch_release(group);
	// #endif
	//
	// return (CGImageRef)[(id)image autorelease();
	//
	// }//end readImageAtPath:

	// ========== readModelAtPath:asynchronously:completionHandler:
	// =================
	//
	// Purpose: Parses the model found at the given path, adds it to the list of
	// loaded parts, and returns the model.
	//
	// Notes: The model is returned from the method if asynchronous is false.
	// Otherwise, returns null and passes the completed model via the
	// block instead.
	//
	// ==============================================================================
	public LDrawModel readModelAtPath(String partPath, DispatchGroup parentGroup,
			LDrawModel completionBlock) {
		return readModelAtPath(partPath, parentGroup, completionBlock, true);
	}

	public LDrawModel readModelAtPath(String partPath, DispatchGroup parentGroup,
			LDrawModel completionBlock, boolean optimize) {
		LDrawFile parsedFile = null;
		LDrawModel model = null;

		if (partPath != null) {
			// We found it in the LDraw folder; now all we need to do is get the
			// model for it.
			long t = System.nanoTime();
			parsedFile = LDrawFile.fileFromContentsAtPath(partPath);
			// System.out.println(partPath + ": " + (System.nanoTime() - t));
		}

		if (parsedFile == null)
			return null;
		model = parsedFile.submodels().get(0);
		if (optimize) {
			long flattenWeight = model.getFlattenWeight(1);
			// long flattenDepth = model.getFlattenDepth(1);
			// System.out.println(allEnclosedElements);
			if (flattenWeight < 100000)
				model.optimizeStructure();
			// else
			// System.out.println(partPath + ": " + flattenWeight);
		}
		if(parentGroup.isCCW()==false){
			for(LDrawDirective step : model.subdirectives()){
				for(LDrawDirective directive : ((LDrawStep)step).subdirectives()){
					if(directive instanceof LDrawTriangle){
						((LDrawTriangle)directive).setToCW();
					}else if(directive instanceof LDrawQuadrilateral){
						((LDrawQuadrilateral)directive).setToCW();
					}
				}
			}
		}

		return model;

	}// end readModelAtPath:
}
