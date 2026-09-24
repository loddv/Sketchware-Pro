package dev.aldi.sayuti.editor.manage;

import static pro.sketchware.utility.FileUtil.deleteFile;
import static pro.sketchware.utility.FileUtil.getExternalStorageDir;
import static pro.sketchware.utility.FileUtil.isExistFile;
import static pro.sketchware.utility.FileUtil.readFile;
import static pro.sketchware.utility.FileUtil.renameFile;
import static pro.sketchware.utility.FileUtil.writeFile;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalLibrariesUtil {
    private static final String localLibsPath = getExternalStorageDir().concat("/.sketchware/libs/local_libs/");
    private static final Comparator<File> LOCAL_LIBS_COMPARATOR = new LocalLibrariesComparator();
    private static final Path LOCAL_LIBS_PATH = Path.of(localLibsPath); // cache se for constante

    private static final Gson GSON = GsonHolder.INSTANCE;
    private static final Type LIST_MAP_TYPE = new TypeToken<ArrayList<HashMap<String, Object>>>() {}.getType();

    public static List<LocalLibrary> getAllLocalLibraries() {
        try (Stream<Path> stream = Files.list(LOCAL_LIBS_PATH)) {
            return stream.filter(Files::isDirectory)                                      // só diretórios
                    .map(Path::toFile)                                               // → File (se ainda precisar)
                    .sorted(LOCAL_LIBS_COMPARATOR)                                   // ordenação uma única vez
                    .map(LocalLibrary::fromFile)                                     // converte para LocalLibrary
                    .collect(Collectors.toList());                                   // ou toUnmodifiableList() se
            // não for modificar depois
        } catch (IOException | InvalidPathException e) {
            // Diretório não existe, sem permissão, etc → retorna lista vazia (comportamento seguro)
            return List.of();
        }
    }

    public static ArrayList<HashMap<String, Object>> getLocalLibraries(String scId) {
        File file = getLocalLibFile(scId);
        if (file == null) return new ArrayList<>();
        String content = readFile(String.valueOf(file));
        if (content == null || content.isBlank()) {
            writeFile(String.valueOf(file), "[]");
            return new ArrayList<>();
        }
        try {
            var result = GSON.fromJson(content, LIST_MAP_TYPE);
            return result instanceof ArrayList<?> list ? (ArrayList<HashMap<String, Object>>) list : new ArrayList<>();
        } catch (Exception e) {
            writeFile(String.valueOf(file), "[]");
            return new ArrayList<>();
        }
    }

    public static void deleteSelectedLocalLibraries(String scId, List<LocalLibrary> localLibraries,
                                                    ArrayList<HashMap<String, Object>> projectUsedLibs) {
        List<LocalLibrary> toRemove = new ArrayList<>();
        for (LocalLibrary library : localLibraries) {
            if (library.isSelected()) {toRemove.add(library);}
        }
        for (LocalLibrary library : toRemove) {
            deleteFile(localLibsPath.concat(library.getName()));
            if (projectUsedLibs != null) {
                projectUsedLibs.removeIf(lib -> library.getName().equals(String.valueOf(lib.get("name"))));
            }
        }
        // Using removeAll is much more efficient than using removeIf with a nested loop
        // or removing elements while iterating.
        localLibraries.removeAll(toRemove);
        // Write to the file only ONCE, after all removals are done.
        if (projectUsedLibs != null) {
            rewriteLocalLibFile(scId, new Gson().toJson(projectUsedLibs));
        }
    }

    public static void renameSelectedLocalLibraryPath(String scId, String newName, String oldName,
                                                      List<LocalLibrary> localLibraries, ArrayList<HashMap<String,
                    Object>> projectUsedLibs) {
        File oldPath = new File(localLibsPath, oldName);
        File newPath = new File(localLibsPath, newName);
        // Only proceed if the file rename is successful
        if (!renameFile(oldPath.toString(), newPath.toString())) {
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
        String infoPath = localLibsPath + name + "/info";
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
        if (isExistFile(infoPath)) {
            localLibrary.put("info", readFile(infoPath));
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

    // Gson thread-safe e inicializado apenas uma vez
    private static final class GsonHolder {
        static final Gson INSTANCE = new Gson();
    }
}
