package Exports;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import Bricklink.BricklinkAPI;
import Bricklink.org.kleini.bricklink.api.BrickLinkClient;
import Bricklink.org.kleini.bricklink.api.Color.ColorsRequest;
import Bricklink.org.kleini.bricklink.api.Color.ColorsResponse;
import Bricklink.org.kleini.bricklink.data.ColorDT;
import Builder.BuilderConfigurationManager;
import Color.BricklinkColorT;
import Command.LDrawColorT;
import LDraw.Support.LDrawUtilities;
import LDraw.Support.PartCache;

public class CompatiblePartManager {
	private static CompatiblePartManager _instance = null;

	private HashMap<PartDomainT, HashMap<String, PartIds>> partIdMapPerDomain;
	private HashMap<String, PartIds> ldrawPartIdMap;
	private HashMap<String, PartIds> lddPartIdMap;
	private HashMap<String, PartIds> bricklinkPartIdMap;

	private HashMap<PartDomainT, HashMap<Integer, PartColors>> partColorMapPerDomain;
	private HashMap<Integer, PartColors> ldrawPartColorMap;
	private HashMap<Integer, PartColors> lddPartColorMap;
	private HashMap<Integer, PartColors> bricklinkPartColorMap;

	public static void main(String args[]) {
		CompatiblePartManager manager = CompatiblePartManager.getInstance();

		HashMap<String, PartIds> partIdMap = manager
				.getAllPartsInDomain(PartDomainT.BRICKLINK);
		for (Entry<String, PartIds> partId : partIdMap.entrySet()) {
			ArrayList<String> ldrawIdList = partId.getValue().getId(
					PartDomainT.LDRAW);
			if (ldrawIdList == null)
				continue;
			for (String ldrawId : ldrawIdList) {
				if (ldrawId == null)
					continue;
				ldrawId += ".dat";
				String partName = PartCache.getInstance().getPartName(ldrawId);
				if (partName == null)
					continue;
				if (partName.contains("=")) {
					System.out.println(partId.getKey()
							+ "->"
							+ ldrawId
							+ ": "
							+ PartCache.getInstance().getRepresentPartName(
									ldrawId));
					partId.getValue().setId(
							PartDomainT.LDRAW,
							LDrawUtilities
									.excludeExtensionFromPartName(PartCache
											.getInstance()
											.getRepresentPartName(ldrawId)));
				}
			}
		}
		manager.writeMappingListToFileCache();
	}

	private CompatiblePartManager() {
		partIdMapPerDomain = new HashMap<PartDomainT, HashMap<String, PartIds>>();

		ldrawPartIdMap = new HashMap<String, PartIds>();
		lddPartIdMap = new HashMap<String, PartIds>();
		bricklinkPartIdMap = new HashMap<String, PartIds>();

		partIdMapPerDomain.put(PartDomainT.BRICKLINK, bricklinkPartIdMap);
		partIdMapPerDomain.put(PartDomainT.LDD, lddPartIdMap);
		partIdMapPerDomain.put(PartDomainT.LDRAW, ldrawPartIdMap);

		partColorMapPerDomain = new HashMap<PartDomainT, HashMap<Integer, PartColors>>();

		ldrawPartColorMap = new HashMap<Integer, PartColors>();
		lddPartColorMap = new HashMap<Integer, PartColors>();
		bricklinkPartColorMap = new HashMap<Integer, PartColors>();

		partColorMapPerDomain.put(PartDomainT.BRICKLINK, bricklinkPartColorMap);
		partColorMapPerDomain.put(PartDomainT.LDD, lddPartColorMap);
		partColorMapPerDomain.put(PartDomainT.LDRAW, ldrawPartColorMap);

		loadLDrawIdInfo();
		loadLDrawColorInfo();
		loadBricklinkInfo();

		// loadMappingInfo_CompatibleJS();

		loadIdMappingInfoFromLDraw();
		loadIdMappingInfoFromBricklink();

		loadColorMappingInfoFromBricklink();
		loadColorMappingInfoFromLDraw();

		System.out.println("Num Of Compatible Item From compatible.js:"
				+ bricklinkPartIdMap.size());
		// loadMappingInfo_BrickLink();
		System.out.println("Accumulated Num Of Compatible Item From Bricklink:"
				+ bricklinkPartIdMap.size());

	}

	public void loadMappingInfo_BrickLink() {
		loadIdMappingInfo_BrickLink();
		loadColorMappingInfo_BrickLink();
	}

	public void loadColorMappingInfo_BrickLink() {
		for (Entry<Integer, Integer> entry : UpdateManager.getInstance()
				.getColorMappingInfoMapFromLDraw().entrySet())
			updateColorMappingInfoFromLDraw(entry.getKey(), entry.getValue());

		for (Entry<Integer, Integer> entry : UpdateManager.getInstance()
				.getColorMappingInfoMapFromBricklink().entrySet())
			updateColorMappingInfoFromBricklink(entry.getKey(),
					entry.getValue());
	}

	public void loadIdMappingInfo_BrickLink() {
		for (Entry<String, String> entry : UpdateManager.getInstance()
				.getIdMappingInfoMapFromLDraw().entrySet())
			updateIdMappingInfoFromLDraw(entry.getKey(), entry.getValue());

		for (Entry<String, String> entry : UpdateManager.getInstance()
				.getIdMappingInfoMapFromBricklink().entrySet())
			updateIdMappingInfoFromBricklink(entry.getKey(), entry.getValue());
	}

	private void loadLDrawColorInfo() {
		for (LDrawColorT colorT : LDrawColorT.values()) {
			PartColors partColor = new PartColors();
			partColor.setColorId(PartDomainT.LDRAW, colorT.getValue());
			ldrawPartColorMap.put(colorT.getValue(), partColor);
		}
	}

	public void updateColorMappingInfoFromBricklink(Integer bricklinkColorId,
			Integer ldrawColorId) {
		if (ldrawColorId == null) {
			bricklinkPartColorMap.remove(bricklinkColorId);
			return;
		}

		PartColors bricklinkColor = bricklinkPartColorMap.get(bricklinkColorId);

		PartColors partColor;

		if (bricklinkColor == null) {
			partColor = new PartColors();
			partColor.setColorId(PartDomainT.BRICKLINK, bricklinkColorId);
			partColor.setColorId(PartDomainT.LDRAW, ldrawColorId);

			bricklinkPartColorMap.put(bricklinkColorId, partColor);
		} else if (bricklinkColor.getColorId(PartDomainT.LDRAW) == null) {
			bricklinkColor.setColorId(PartDomainT.LDRAW, ldrawColorId);
		} else {
			// System.out.println("################################");
			// System.out
			// .println("updateColorMappingInfoFromBricklink: 1 to n Mapping ");
			// System.out.println("previous mapping:"
			// + bricklinkColor.getColorId(PartDomainT.BRICKLINK) + "<->"
			// + bricklinkColor.getColorId(PartDomainT.LDRAW));
			// System.out.println("new mapping: " + bricklinkColorId + "<->"
			// + ldrawColorId);
			bricklinkColor.setColorId(PartDomainT.LDRAW, ldrawColorId);
		}

		PartColors colorsInLDraw = ldrawPartColorMap.get(ldrawColorId);
		if (colorsInLDraw == null) {
			partColor = new PartColors();
			partColor.setColorId(PartDomainT.BRICKLINK, bricklinkColorId);
			partColor.setColorId(PartDomainT.LDRAW, ldrawColorId);
			ldrawPartColorMap.put(ldrawColorId, partColor);
		} else if (colorsInLDraw.getColorId(PartDomainT.BRICKLINK) == null) {
			colorsInLDraw.setColorId(PartDomainT.BRICKLINK, bricklinkColorId);
		}
	}

	public void updateColorMappingInfoFromLDraw(Integer ldrawColorId,
			Integer bricklinkColorId) {
		if (bricklinkColorId == null) {
			ldrawPartColorMap.remove(ldrawColorId);
			return;
		}

		PartColors ldrawColor = ldrawPartColorMap.get(ldrawColorId);

		PartColors partColor;

		if (ldrawColor == null) {
			partColor = new PartColors();
			partColor.setColorId(PartDomainT.BRICKLINK, bricklinkColorId);
			partColor.setColorId(PartDomainT.LDRAW, ldrawColorId);

			ldrawPartColorMap.put(ldrawColorId, partColor);
		} else if (ldrawColor.getColorId(PartDomainT.BRICKLINK) == null) {
			ldrawColor.setColorId(PartDomainT.BRICKLINK, bricklinkColorId);
		} else {
			// System.out.println("################################");
			// System.out
			// .println("updateColorMappingInfoFromLDraw: 1 to n Mapping ");
			// System.out.println("previous mapping:"
			// + ldrawColor.getColorId(PartDomainT.LDRAW) + "<->"
			// + ldrawColor.getColorId(PartDomainT.BRICKLINK));
			// System.out.println("new mapping: " + ldrawColorId + "<->"
			// + bricklinkColorId);
			ldrawColor.setColorId(PartDomainT.BRICKLINK, bricklinkColorId);
		}

		PartColors colorsInBricklink = bricklinkPartColorMap
				.get(bricklinkColorId);
		if (colorsInBricklink == null) {
			partColor = new PartColors();
			partColor.setColorId(PartDomainT.BRICKLINK, bricklinkColorId);
			partColor.setColorId(PartDomainT.LDRAW, ldrawColorId);
			bricklinkPartColorMap.put(ldrawColorId, partColor);
		} else if (colorsInBricklink.getColorId(PartDomainT.LDRAW) == null) {
			colorsInBricklink.setColorId(PartDomainT.LDRAW, ldrawColorId);
		}
	}

	public void updateIdMappingInfoFromBricklink(String bricklinkId,
			String ldrawId) {

		if (ldrawId == null || ldrawId.equals("")) {
			bricklinkPartIdMap.remove(bricklinkId);
			return;
		}

		PartIds newPartIds;
		PartIds partIdsByBricklik = bricklinkPartIdMap.get(bricklinkId);
		ArrayList<String> newIdList = new ArrayList<String>();
		if (ldrawId.contains("+")) {
			for (String id : ldrawId.split("\\+"))
				newIdList.add(id);
		} else if (ldrawId.contains(",")) {
			for (String id : ldrawId.split(","))
				newIdList.add(id);
		} else
			newIdList.add(ldrawId);

		if (partIdsByBricklik == null) {
			newPartIds = new PartIds();
			newPartIds.setId(PartDomainT.BRICKLINK, bricklinkId);
			newPartIds.setId(PartDomainT.LDRAW, newIdList);

			bricklinkPartIdMap.put(bricklinkId, newPartIds);
		} else if (partIdsByBricklik.getId(PartDomainT.LDRAW) == null) {
			partIdsByBricklik.setId(PartDomainT.LDRAW, newIdList);
		} else if (partIdsByBricklik.getId(PartDomainT.LDRAW).equals(ldrawId) == false) {
			partIdsByBricklik.setId(PartDomainT.LDRAW, newIdList);
		}
		if (ldrawId.contains("+") || ldrawId.contains(","))
			return;

		PartIds idsInLDraw = ldrawPartIdMap.get(ldrawId);
		if (idsInLDraw == null) {
			newPartIds = new PartIds();
			newPartIds.setId(PartDomainT.BRICKLINK, bricklinkId);
			newPartIds.setId(PartDomainT.LDRAW, ldrawId);
			ldrawPartIdMap.put(ldrawId, newPartIds);
		} else if (idsInLDraw.getId(PartDomainT.BRICKLINK) == null) {
			idsInLDraw.setId(PartDomainT.BRICKLINK, bricklinkId);
		}
	}

	public void updateIdMappingInfoFromLDraw(String ldrawId, String bricklinkId) {
		if (bricklinkId == null || bricklinkId.equals("")) {
			ldrawPartIdMap.remove(ldrawId);
			return;
		}
		PartIds newPartIds;
		PartIds partIdsByLDraw = ldrawPartIdMap.get(ldrawId);

		ArrayList<String> newIdList = new ArrayList<String>();
		if (bricklinkId.contains("+")) {
			for (String id : bricklinkId.split("\\+"))
				newIdList.add(id);
		} else if (bricklinkId.contains(",")) {
			for (String id : bricklinkId.split(","))
				newIdList.add(id);
		} else
			newIdList.add(bricklinkId);

		if (partIdsByLDraw == null) {
			newPartIds = new PartIds();
			newPartIds.setId(PartDomainT.LDRAW, ldrawId);
			newPartIds.setId(PartDomainT.BRICKLINK, newIdList);

			ldrawPartIdMap.put(ldrawId, newPartIds);
		} else if (partIdsByLDraw.getId(PartDomainT.BRICKLINK) == null) {
			partIdsByLDraw.setId(PartDomainT.BRICKLINK, newIdList);
		} else if (partIdsByLDraw.getId(PartDomainT.BRICKLINK).equals(
				bricklinkId) == false) {
			partIdsByLDraw.setId(PartDomainT.BRICKLINK, newIdList);
		}

		if (bricklinkId.contains("+") || bricklinkId.contains(","))
			return;

		PartIds idsInBricklink = bricklinkPartIdMap.get(bricklinkId);
		if (idsInBricklink == null) {
			newPartIds = new PartIds();
			newPartIds.setId(PartDomainT.BRICKLINK, bricklinkId);
			newPartIds.setId(PartDomainT.LDRAW, ldrawId);
			bricklinkPartIdMap.put(bricklinkId, newPartIds);
		} else if (idsInBricklink.getId(PartDomainT.LDRAW) == null) {
			idsInBricklink.setId(PartDomainT.LDRAW, ldrawId);
		}
	}

	private void loadIdMappingInfoFromLDraw() {
		File file = new File(
				BuilderConfigurationManager.getDefaultDataDirectoryPath()
						+ "MappingData" + File.separator
						+ "IDMappingListFromLDraw.js");
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);

			String ldrawMapList = br.readLine();

			if (ldrawMapList != null && ldrawMapList.length() > 2) {
				ldrawMapList = ldrawMapList.substring("{".length(),
						ldrawMapList.length() - 2).replaceAll("\"", "");
				String mappingItems[] = ldrawMapList.split(",");
				for (String item : mappingItems) {
					String ldrawId = item.split(":")[0].toLowerCase();
					String bricklinkId = item.split(":")[1].toLowerCase();
					updateIdMappingInfoFromLDraw(ldrawId, bricklinkId);
				}
			}
			br.close();
			fr.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void loadIdMappingInfoFromBricklink() {
		File file = new File(
				BuilderConfigurationManager.getDefaultDataDirectoryPath()
						+ "MappingData" + File.separator
						+ "IDMappingListFromBricklink.js");
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);

			String brickMapList = br.readLine();

			if (brickMapList != null && brickMapList.length() > 2) {
				brickMapList = brickMapList.substring("{".length(),
						brickMapList.length() - 2).replaceAll("\"", "");
				String mappingItems[] = brickMapList.split(",");
				for (String item : mappingItems) {
					String bricklinkId = item.split(":")[0].toLowerCase();
					String ldrawId = item.split(":")[1].toLowerCase();
					updateIdMappingInfoFromBricklink(bricklinkId, ldrawId);
				}
			}
			br.close();
			fr.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void loadColorMappingInfoFromLDraw() {
		File file = new File(
				BuilderConfigurationManager.getDefaultDataDirectoryPath()
						+ "MappingData" + File.separator
						+ "ColorMappingListFromLDraw.js");
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);

			String ldrawMapList = br.readLine();

			if (ldrawMapList != null && ldrawMapList.length() > 2) {
				ldrawMapList = ldrawMapList.substring("{".length(),
						ldrawMapList.length() - 2).replaceAll("\"", "");
				String mappingItems[] = ldrawMapList.split(",");
				for (String item : mappingItems) {

					String bricklinkId = item.split(":")[1].toLowerCase();
					String ldrawId = item.split(":")[0].toLowerCase();

					updateColorMappingInfoFromLDraw(Integer.parseInt(ldrawId),
							Integer.parseInt(bricklinkId));
				}
			}
			br.close();
			fr.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void loadColorMappingInfoFromBricklink() {
		File file = new File(
				BuilderConfigurationManager.getDefaultDataDirectoryPath()
						+ "MappingData" + File.separator
						+ "ColorMappingListFromBricklink.js");
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);

			String bricklinkMapList = br.readLine();

			if (bricklinkMapList != null && bricklinkMapList.length() > 2) {
				bricklinkMapList = bricklinkMapList.substring("{".length(),
						bricklinkMapList.length() - 2).replaceAll("\"", "");
				String mappingItems[] = bricklinkMapList.split(",");
				for (String item : mappingItems) {

					String bricklinkId = item.split(":")[0].toLowerCase();
					String ldrawId = item.split(":")[1].toLowerCase();

					updateColorMappingInfoFromBricklink(
							Integer.parseInt(bricklinkId),
							Integer.parseInt(ldrawId));
				}
			}
			br.close();
			fr.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void loadBricklinkInfo() {
		ColorsRequest request = new ColorsRequest();
		BrickLinkClient client = BricklinkAPI.getInstance()
				.getClientForOpenAPI();

		try {
			ColorsResponse response = client.execute(request);
			for (ColorDT dt : response.getColors()) {
				PartColors newColors = new PartColors();
				newColors.setColorId(PartDomainT.BRICKLINK, dt.getIdentifier());
				BricklinkColorT.byValue(dt.getIdentifier()).setColorCode(
						dt.getCode());
				bricklinkPartColorMap.put(dt.getIdentifier(), newColors);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void loadLDrawIdInfo() {
		PartCache cache = PartCache.getInstance();
		for (String partName : cache.getAllParts()) {
			partName = partName.toLowerCase();
			PartIds partId = new PartIds();
			partId.setId(PartDomainT.LDRAW, partName);

			PartIds ids = ldrawPartIdMap.get(partName);
			if (ids == null)
				ldrawPartIdMap.put(partName, partId);
		}
	}

	public synchronized static CompatiblePartManager getInstance() {
		if (_instance == null)
			_instance = new CompatiblePartManager();
		return _instance;
	}

	public PartIds getPartIds(PartDomainT fromDomain, String fromId) {
		return partIdMapPerDomain.get(fromDomain).get(fromId);
	}

	public PartColors getPartColors(PartDomainT fromDomain, Integer fromId) {
		return partColorMapPerDomain.get(fromDomain).get(fromId);
	}

	public HashMap<String, PartIds> getAllPartsInDomain(PartDomainT domain) {
		return partIdMapPerDomain.get(domain);
	}

	public HashMap<Integer, PartColors> getAllColorsInDomain(PartDomainT domain) {
		return partColorMapPerDomain.get(domain);
	}

	public void writeMappingListToFileCache() {
		writeIDMappingListFromLDrawToFileCache();
		writeIDMappingListFromBricklinkToFileCache();
		writeColorMappingListFromBricklinkToFileCache();
		writeColorMappingListFromLDrawToFileCache();
	}

	private void writeIDMappingListFromLDrawToFileCache() {
		File file = new File(
				BuilderConfigurationManager.getDefaultDataDirectoryPath()
						+ "MappingData/IDMappingListFromLDraw.js");
		try {
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("{");
			StringBuilder builder = new StringBuilder();
			for (PartIds ids : ldrawPartIdMap.values()) {
				if (ids.getId(PartDomainT.BRICKLINK) == null)
					continue;
				builder.append("\"");
				String id = null;
				for (String tempId : ids.getId(PartDomainT.LDRAW))
					if (id == null)
						id = tempId;
					else
						id += "+" + tempId;
				builder.append(id);
				builder.append("\":\"");
				id = null;
				for (String tempId : ids.getId(PartDomainT.BRICKLINK))
					if (id == null)
						id = tempId;
					else
						id += "+" + tempId;
				builder.append(id);
				builder.append("\",");
			}
			if (builder.length() > 0)
				builder.deleteCharAt(builder.length() - 1);
			bw.write(builder.toString());
			bw.write("}\r\n");
			bw.close();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeIDMappingListFromBricklinkToFileCache() {
		File file = new File(
				BuilderConfigurationManager.getDefaultDataDirectoryPath()
						+ "MappingData/IDMappingListFromBricklink.js");
		try {
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("{");
			StringBuilder builder = new StringBuilder();
			for (PartIds ids : bricklinkPartIdMap.values()) {
				if (ids.getId(PartDomainT.LDRAW) == null)
					continue;
				builder.append("\"");
				String id = null;
				for (String tempId : ids.getId(PartDomainT.BRICKLINK))
					if (id == null)
						id = tempId;
					else
						id += "+" + tempId;
				builder.append(id);
				builder.append("\":\"");
				id = null;
				for (String tempId : ids.getId(PartDomainT.LDRAW))
					if (id == null)
						id = tempId;
					else
						id += "+" + tempId;
				builder.append(id);
				builder.append("\",");
			}
			if (builder.length() > 0)
				builder.deleteCharAt(builder.length() - 1);
			bw.write(builder.toString());
			bw.write("}\r\n");
			bw.close();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeColorMappingListFromBricklinkToFileCache() {
		File file = new File(
				BuilderConfigurationManager.getDefaultDataDirectoryPath()
						+ "MappingData/ColorMappingListFromBricklink.js");
		try {
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("{");
			StringBuilder builder = new StringBuilder();
			for (PartColors colors : bricklinkPartColorMap.values()) {
				if (colors.getColorId(PartDomainT.LDRAW) == null)
					continue;
				builder.append("\"");
				builder.append(colors.getColorId(PartDomainT.BRICKLINK));
				builder.append("\":\"");
				builder.append(colors.getColorId(PartDomainT.LDRAW));
				builder.append("\",");
			}
			if (builder.length() > 0)
				builder.deleteCharAt(builder.length() - 1);
			bw.write(builder.toString());
			bw.write("}\r\n");
			bw.close();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeColorMappingListFromLDrawToFileCache() {
		File file = new File(
				BuilderConfigurationManager.getDefaultDataDirectoryPath()
						+ "MappingData/ColorMappingListFromLDraw.js");
		try {
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("{");
			StringBuilder builder = new StringBuilder();
			for (PartColors colors : ldrawPartColorMap.values()) {
				if (colors.getColorId(PartDomainT.BRICKLINK) == null)
					continue;
				builder.append("\"");
				builder.append(colors.getColorId(PartDomainT.LDRAW));
				builder.append("\":\"");
				builder.append(colors.getColorId(PartDomainT.BRICKLINK));
				builder.append("\",");
			}
			if (builder.length() > 0)
				builder.deleteCharAt(builder.length() - 1);
			bw.write(builder.toString());
			bw.write("}\r\n");
			bw.close();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
