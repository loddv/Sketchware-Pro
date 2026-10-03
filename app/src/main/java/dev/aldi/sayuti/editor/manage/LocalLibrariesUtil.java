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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalLibrariesUtil {
    private static final String localLibsPath = getExternalStorageDir().concat("/.sketchware/libs/local_libs/");
    private static final Path LOCAL_LIBS_PATH = Path.of(localLibsPath);
    private static final String CACHE_FILE_PATH = localLibsPath.concat("cache.json");

    private static final Gson GSON = GsonHolder.INSTANCE;
    private static final Type LIST_MAP_TYPE = new TypeToken<ArrayList<HashMap<String, Object>>>() {}.getType();
    private static final Type LIST_LOCAL_LIB_TYPE = new TypeToken<ArrayList<LocalLibrary>>() {}.getType();

    /**
     * Obtém todas as bibliotecas locais. Tenta ler primeiro do cache (cache.json).
     * Se não existir ou falhar, realiza a varredura no sistema de arquivos e gera o cache.
     */
    public static List<LocalLibrary> getAllLocalLibraries() {
        // 1. Tentar ler do Cache JSON
        if (isExistFile(CACHE_FILE_PATH)) {
            String cacheContent = readFile(CACHE_FILE_PATH);
            if (cacheContent != null && !cacheContent.isBlank()) {
                try {
                    List<LocalLibrary> cachedList = GSON.fromJson(cacheContent, LIST_LOCAL_LIB_TYPE);
                    if (cachedList != null) {
                        return cachedList;
                    }
                } catch (Exception ignored) {
                    // Cache corrompido: ignora e reconstrói via I/O
                }
            }
        }
        // 2. Cache Miss: Fazer varredura no disco usando DirectoryStream (mais rápido que Files.list)
        List<LocalLibrary> libraries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(LOCAL_LIBS_PATH, Files::isDirectory)) {
            for (Path path : stream) {
                LocalLibrary lib = LocalLibrary.fromFile(path.toFile());
                if (lib != null) {
                    libraries.add(lib);
                }
            }
            libraries.sort(Comparator.comparing(LocalLibrary::getName, String.CASE_INSENSITIVE_ORDER));
            // 3. Salvar o resultado no cache JSON para as próximas requisições
            saveCacheAsync(libraries);

        } catch (IOException e) {
            return List.of();
        }
        return libraries;
    }

    /**
     * Salva a lista tratada em formato JSON de maneira assíncrona para não travar a UI.
     */
    private static void saveCacheAsync(List<LocalLibrary> libraries) {
        new Thread(() -> {
            try {
                String json = GSON.toJson(libraries);
                writeFile(CACHE_FILE_PATH, json);
            } catch (Exception ignored) {}
        }).start();
    }

    /**
     * Invalida ou remove o arquivo de cache ao modificar as bibliotecas.
     */
    public static void invalidateCache() {
        deleteFile(CACHE_FILE_PATH);
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
            if (library.isSelected()) {
                toRemove.add(library);
            }
        }
        for (LocalLibrary library : toRemove) {
            deleteFile(localLibsPath.concat(library.getName()));
            if (projectUsedLibs != null) {
                projectUsedLibs.removeIf(lib -> library.getName().equals(String.valueOf(lib.get("name"))));
            }
        }
        localLibraries.removeAll(toRemove);
        if (projectUsedLibs != null) {
            rewriteLocalLibFile(scId, GSON.toJson(projectUsedLibs));
        }
        // Invalida o cache após remoção para forçar nova sincronização no próximo carregamento
        invalidateCache();
    }

    public static void renameSelectedLocalLibraryPath(String scId, String newName, String oldName,
                                                      List<LocalLibrary> localLibraries,
                                                      ArrayList<HashMap<String, Object>> projectUsedLibs) {
        File oldPath = new File(localLibsPath, oldName);
        File newPath = new File(localLibsPath, newName);
        if (!renameFile(oldPath.toString(), newPath.toString())) {
            return;
        }
        boolean hasChanges = false;
        if (projectUsedLibs != null) {
            for (Map<String, Object> libraryMap : projectUsedLibs) {
                if (oldName.equals(String.valueOf(libraryMap.get("name")))) {
                    libraryMap.put("name", newName);
                    hasChanges = true;
                    break;
                }
            }
        }
        for (LocalLibrary library : localLibraries) {
            if (library.getName().equals(oldName)) {
                library.setName(newName);
                break;
            }
        }
        if (hasChanges) {
            rewriteLocalLibFile(scId, GSON.toJson(projectUsedLibs));
        }
        // Atualiza ou limpa o cache após renomear
        saveCacheAsync(localLibraries);
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

    private static final class GsonHolder {
        static final Gson INSTANCE = new Gson();
    }
}
