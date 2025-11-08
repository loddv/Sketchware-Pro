package dev.aldi.sayuti.editor.manage;

import static pro.sketchware.utility.FileUtil.deleteFile;
import static pro.sketchware.utility.FileUtil.getExternalStorageDir;
import static pro.sketchware.utility.FileUtil.isExistFile;
import static pro.sketchware.utility.FileUtil.listDirAsFile;
import static pro.sketchware.utility.FileUtil.readFile;
import static pro.sketchware.utility.FileUtil.renameFile;
import static pro.sketchware.utility.FileUtil.writeFile;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import mod.hey.studios.util.Helper;

public class LocalLibrariesUtil {
	private static final String localLibsPath = getExternalStorageDir().concat("/.sketchware/libs/local_libs/");

	public static List<LocalLibrary> getAllLocalLibraries() {
		ArrayList<File> localLibraryFiles = new ArrayList<>();
		listDirAsFile(localLibsPath, localLibraryFiles);
		localLibraryFiles.sort(new LocalLibrariesComparator());
		List<LocalLibrary> localLibraries = new LinkedList<>();
		for (File libraryFile : localLibraryFiles) {
			if (libraryFile.isDirectory()) {
				localLibraries.add(LocalLibrary.fromFile(libraryFile));
			}
		}

		return localLibraries;
	}

	public static ArrayList<HashMap<String, Object>> getLocalLibraries(String scId) {
		File localLibFile = getLocalLibFile(scId);
		String fileContent;
		if (! localLibFile.exists() || (fileContent = readFile(localLibFile.getAbsolutePath())).isEmpty()) {
			writeFile(localLibFile.getAbsolutePath(), "[]");
			return new ArrayList<>();
		}
		return new Gson().fromJson(fileContent, Helper.TYPE_MAP_LIST);
	}

	public static void deleteSelectedLocalLibraries(String scId, List<LocalLibrary> localLibraries, ArrayList<HashMap<String, Object>> projectUsedLibs) {
		localLibraries.removeIf(library -> {
			if (library.isSelected()) {
				deleteFile(localLibsPath.concat(library.getName()));
				if (projectUsedLibs != null) {
					int indexToRemove = - 1;
					for (int i = 0; i < projectUsedLibs.size(); i++) {
						Map<String, Object> libraryMap = projectUsedLibs.get(i);
						if (library.getName().equals(Objects.requireNonNull(libraryMap.get("name")).toString())) {
							indexToRemove = i;
							break;
						}
					}
					if (indexToRemove != - 1) {
						projectUsedLibs.remove(indexToRemove);
					}
				}
				return true;
			}
			return false;
		});
		if (projectUsedLibs != null)
			rewriteLocalLibFile(scId, new Gson().toJson(projectUsedLibs));
	}

	public static void renameSelectedLocalLibraryPath(String scId, String newName, String oldName, List<LocalLibrary> localLibraries, ArrayList<HashMap<String, Object>> projectUsedLibs) {
		File oldPath = new File(localLibsPath, oldName);
		File newPath = new File(localLibsPath, newName);

		// Only proceed if the file rename is successful
		if (! renameFile(oldPath.toString(), newPath.toString())) {
			// Optional: Add logging here to indicate the failure
			// Log.e("LibraryRename", "Failed to rename file from " + oldName + " to " + newName);
			return; // Exit if the core file operation failed
		}

		boolean hasChanges = false;

		// Update the project-specific library list
		if (projectUsedLibs != null) {
			for (Map<String, Object> libraryMap : projectUsedLibs) {
				// Use String.valueOf() for null-safety
				if (oldName.equals(String.valueOf(libraryMap.get("name")))) {
					libraryMap.put("name", newName);
					hasChanges = true;
					// The break should be INSIDE the if-block to stop after finding the match
					break;
				}
			}
		}

		// Update the global list of local libraries
		for (LocalLibrary library : localLibraries) {
			if (library.getName().equals(oldName)) {
				library.setName(newName);
				// No need to set hasChanges here, as this is an in-memory object update
				break; // Stop after finding and updating the library
			}
		}

		// Write to the file only ONCE, and only if changes were made to the list that gets serialized.
		if (hasChanges) {
			rewriteLocalLibFile(scId, new Gson().toJson(projectUsedLibs));
		}
	}

	public static File getLocalLibFile(String scId) {
		return new File(getExternalStorageDir().concat("/.sketchware/data/").concat(scId.concat("/local_library")));
	}

	public static void rewriteLocalLibFile(String scId, String newContent) {
		writeFile(getLocalLibFile(scId).getAbsolutePath(), newContent);
	}

	public static HashMap<String, Object> createLibraryMap(String name, String dependency) {
		String configPath = localLibsPath + name + "/config";
		String resPath = localLibsPath + name + "/res";
		String jarPath = localLibsPath + name + "/classes.jar";
		String dexPath = localLibsPath + name + "/classes.dex";
		String manifestPath = localLibsPath + name + "/AndroidManifest.xml";
		String pgRulesPath = localLibsPath + name + "/proguard.txt";
		String assetsPath = localLibsPath + name + "/assets";

		HashMap<String, Object> localLibrary = new HashMap<>();
		localLibrary.put("name", name);
		if (dependency != null) {
			localLibrary.put("dependency", dependency);
		}
		if (isExistFile(configPath)) {
			localLibrary.put("packageName", readFile(configPath));
		}
		if (isExistFile(resPath)) {
			localLibrary.put("resPath", resPath);
		}
		if (isExistFile(jarPath)) {
			localLibrary.put("jarPath", jarPath);
		}
		if (isExistFile(dexPath)) {
			localLibrary.put("dexPath", dexPath);
		}
		if (isExistFile(manifestPath)) {
			localLibrary.put("manifestPath", manifestPath);
		}
		if (isExistFile(pgRulesPath)) {
			localLibrary.put("pgRulesPath", pgRulesPath);
		}
		if (isExistFile(assetsPath)) {
			localLibrary.put("assetsPath", assetsPath);
		}
		return localLibrary;
	}
}
