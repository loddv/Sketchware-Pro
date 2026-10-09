package pro.sketchware.util.library;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import a.a.a.Jp;
import a.a.a.ProjectBuilder;
import mod.jbk.build.BuiltInLibraries;
import mod.jbk.editor.manage.library.ExcludeBuiltInLibrariesActivity;

/**
 * A class to keep track of a project's built-in libraries.
 */

public class BuiltInLibraryManager {

    private final ArrayList<String> libraryNames = new ArrayList<>();
    private final ArrayList<Jp> libraries = new ArrayList<>();
    private final List<BuiltInLibraries.BuiltInLibrary> excludedLibraries;

    public BuiltInLibraryManager(String projectId) {
        excludedLibraries = ExcludeBuiltInLibrariesActivity.getExcludedLibraries(projectId);
    }

    private boolean isValidLibrary(String libraryName, Optional<BuiltInLibraries.BuiltInLibrary> library) {
        return library.isPresent() && !excludedLibraries.contains(library.get());
    }

    private void logLibraryAdded(String libraryName) {
        Log.d(ProjectBuilder.TAG, "Added built-in library \"" + libraryName + "\" to project's dependencies");
    }

    private void logLibrarySkipped(String libraryName, String reason) {
        Log.v(ProjectBuilder.TAG, "Skipped library \"" + libraryName + "\": " + reason);
    }

    /**
     * Add a built-in library to the project libraries list.
     * Won't add a library if it's in the list already,
     * or it got excluded with {@link ExcludeBuiltInLibrariesActivity}.
     *
     * @param libraryName The built-in library's name, e.g. material-1.0.0
     */
    public void addLibrary(String libraryName) {
        if (libraryName == null) {
            logLibrarySkipped("null", "null library name provided");
            return;
        }
        Optional<BuiltInLibraries.BuiltInLibrary> library = BuiltInLibraries.BuiltInLibrary.ofName(libraryName);
        if (library.isEmpty()) {
            logLibrarySkipped(libraryName, "library not found");
            return;
        }
        if (excludedLibraries.contains(library.get())) {
            logLibrarySkipped(libraryName, "library is excluded");
            addDependencies(libraryName);
            return;
        }
        if (!libraryNames.contains(libraryName)) {
            logLibraryAdded(libraryName);
            libraryNames.add(libraryName);
            libraries.add(new Jp(libraryName));
            addDependencies(libraryName);
        } else {
            logLibrarySkipped(libraryName, "already added");
        }
    }

    private void addDependencies(String libraryName) {
        for (String libraryDependency : BuiltInLibraryUtils.getKnownDependencies(libraryName)) {
            addLibrary(libraryDependency);
        }
    }

    public boolean containsLibrary(String libraryName) {
        Optional<BuiltInLibraries.BuiltInLibrary> library = BuiltInLibraries.BuiltInLibrary.ofName(libraryName);
        return library.filter(builtInLibrary -> libraries.contains(new Jp(builtInLibrary.getName()))).isPresent();
    }

    /**
     * @return {@link BuiltInLibraryManager#libraries}
     */
    public ArrayList<Jp> getLibraries() {
        return libraries;
    }
}
