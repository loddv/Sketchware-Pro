package dev.aldi.sayuti.editor.manage;

import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.createLibraryMap;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.deleteSelectedLocalLibraries;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.getAllLocalLibraries;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.getLocalLibFile;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.getLocalLibraries;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.renameSelectedLocalLibraryPath;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.rewriteLocalLibFile;
import static pro.sketchware.utility.FileUtil.getFileSize;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import a.a.a.MA;
import a.a.a.mB;
import mod.hey.studios.build.BuildSettings;
import mod.hey.studios.util.Helper;
import mod.loddv.dev.manager.RepoManagerActivity;
import pro.sketchware.R;
import pro.sketchware.databinding.ManageLocallibrariesBinding;
import pro.sketchware.databinding.ViewItemLocalLibBinding;
import pro.sketchware.databinding.ViewItemLocalLibSearchBinding;
import pro.sketchware.utility.SketchwareUtil;

public class ManageLocalLibraryActivity extends BaseAppCompatActivity {
    private final LibraryAdapter adapter = new LibraryAdapter();
    private final SearchAdapter searchAdapter = new SearchAdapter();
    private ArrayList<HashMap<String, Object>> projectUsedLibs;
    private boolean notAssociatedWithProject;
    private boolean searchBarExpanded;
    private BuildSettings buildSettings;
    private ManageLocallibrariesBinding binding;
    private String scId;

    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = ManageLocallibrariesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        {
            View view1 = binding.searchBar;
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view1.getLayoutParams();
            int end = lp.getMarginEnd();
            int start = lp.getMarginStart();
            ViewCompat.setOnApplyWindowInsetsListener(view1,
                    (v, i) -> {
                        Insets insets = i.getInsets(WindowInsetsCompat.Type.displayCutout());
                        lp.setMarginEnd(end + insets.right);
                        lp.setMarginStart(start + insets.left);
                        v.setLayoutParams(lp);
                        return i;
                    });
        }
        {
            View view1 = binding.contextualToolbarContainer;
            int left = view1.getPaddingLeft();
            int top = view1.getPaddingTop();
            int right = view1.getPaddingRight();
            int bottom = view1.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(view1,
                    (v, i) -> {
                        Insets insets =
                                i.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                        v.setPadding(left + insets.left,
                                top + insets.top,
                                right + insets.right,
                                bottom);
                        return i;
                    });
        }
        {
            View view1 = binding.librariesList;
            int left = view1.getPaddingLeft();
            int top = view1.getPaddingTop();
            int right = view1.getPaddingRight();
            int bottom = view1.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(view1,
                    (v, i) -> {
                        Insets insets =
                                i.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                        v.setPadding(left + insets.left,
                                top,
                                right + insets.right,
                                bottom + insets.bottom);
                        return i;
                    });
        }
        {
            View view1 = binding.searchList;
            int left = view1.getPaddingLeft();
            int top = view1.getPaddingTop();
            int right = view1.getPaddingRight();
            int bottom = view1.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(view1,
                    (v, i) -> {
                        Insets insets =
                                i.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                        v.setPadding(left + insets.left,
                                top,
                                right + insets.right,
                                bottom + insets.bottom);
                        return i;
                    });
        }
        {
            View view1 = binding.downloadLibraryButton;
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view1.getLayoutParams();
            int bottom = lp.bottomMargin;
            ViewCompat.setOnApplyWindowInsetsListener(view1,
                    (v, i) -> {
                        Insets insets = i.getInsets(WindowInsetsCompat.Type.systemBars());
                        lp.bottomMargin = bottom + insets.bottom;
                        v.setLayoutParams(lp);
                        return i;
                    });
        }
        if (getIntent().hasExtra("sc_id")) {
            scId = Objects.requireNonNull(getIntent().getStringExtra("sc_id"));
            buildSettings = new BuildSettings(scId);
            notAssociatedWithProject = scId.equals("system");
        }
        adapter.setOnLocalLibrarySelectedStateChangedListener(item -> {
            long selectedItemCount = getSelectedLocalLibrariesCount();
            if (selectedItemCount > 0 && adapter.isSelectionModeEnabled) {
                binding.contextualToolbar.setTitle(String.valueOf(selectedItemCount));
                expandContextualToolbar();
            } else {
                adapter.isSelectionModeEnabled = false;
                collapseContextualToolbar();
            }
        });
        binding.librariesList.setAdapter(adapter);
        binding.searchList.setAdapter(searchAdapter);
        binding.searchBar.setNavigationOnClickListener(v -> {
            if (!mB.a()) {
                onBackPressed();
            }
        });
        binding.contextualToolbar.setNavigationOnClickListener(v -> hideContextualToolbarAndClearSelection());
        binding.contextualToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_select_all) {
                long selectedCount = getSelectedLocalLibrariesCount();
                boolean selectAll = selectedCount != adapter.getItemCount();
                setLocalLibrariesSelected(selectAll);
                /*SketchwareUtil.toast(selectAll ? "All items selected" : "All items deselected");*/
                binding.contextualToolbar.setTitle(String.valueOf(getSelectedLocalLibrariesCount()));
                return true;
            } else if (id == R.id.action_invert_selection) {
                setLocalLibrariesInvertSelected();
                binding.contextualToolbar.setTitle(String.valueOf(getSelectedLocalLibrariesCount()));
                /*SketchwareUtil.toast("Selection inverted");*/
                return true;
            } else if (id == R.id.action_rename_selected_local_libraries) {
                List<LocalLibrary> selectedLibs = adapter.getLocalLibraries()
                        .stream()
                        .filter(LocalLibrary::isSelected)
                        .collect(Collectors.toList());
                if (selectedLibs.isEmpty()) {
                    SketchwareUtil.toast("Selecione pelo menos uma biblioteca");
                    return true;
                }
                showRenameBottomSheet(selectedLibs);
                return true;
            } else if (id == R.id.action_delete_selected_local_libraries) {
                long selectedCount = getSelectedLocalLibrariesCount();
                if (selectedCount == 0) {
                    SketchwareUtil.toast("Please select at least one item");
                    return true;
                }
                String selectedNames =
                        adapter.getLocalLibraries().stream().filter(LocalLibrary::isSelected).map(LocalLibrary::getName).collect(Collectors.joining(",\n"));
                String message;
                String editTextContent;
                if (selectedCount > 1) {
                    message =
                            "Are you sure you want to delete these " + selectedCount + " libraries?\n" + selectedNames;
                    editTextContent = selectedNames;
                } else {
                    editTextContent =
                            adapter.getLocalLibraries().stream().filter(LocalLibrary::isSelected).findAny().map(LocalLibrary::getName).orElse("");
                    message = "Are you sure you want to delete this library " + editTextContent + "?";
                }
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
                View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_rename,
                        null);
                bottomSheetDialog.setContentView(bottomSheetView);
                final TextView title = bottomSheetView.findViewById(R.id.title);
                title.setText("Delete Library!");
                final TextView description = bottomSheetView.findViewById(R.id.description);
                final TextInputLayout textInputLayout = bottomSheetView.findViewById(R.id.text_input_layout_library);
                final TextInputEditText editText = bottomSheetView.findViewById(R.id.textInputEditText);
                final Button deleteAccountButton = bottomSheetView.findViewById(R.id.button_rename);
                editText.setText(editTextContent);
                editText.setEnabled(false);
                textInputLayout.setHint("Selected libraries");
                description.setText(message);
                deleteAccountButton.setText(R.string.common_word_delete);
                deleteAccountButton.setTextColor(ContextCompat.getColor(this,
                        R.color.black));
                deleteAccountButton.setBackgroundColor(ContextCompat.getColor(this,
                        R.color.scolor_red_02));
                bottomSheetView.findViewById(R.id.button_cancel).setOnClickListener(v -> {
                    hideContextualToolbarAndClearSelection();
                    bottomSheetDialog.dismiss();
                    resetSelectionAndReload(null);
                });
                bottomSheetDialog.setOnDismissListener(v -> hideContextualToolbarAndClearSelection());
                bottomSheetDialog.setOnDismissListener(v -> hideContextualToolbarAndClearSelection());
                bottomSheetView.findViewById(R.id.button_rename).setOnClickListener(v -> {
                    k();
                    Executors.newSingleThreadExecutor().execute(() -> {
                        deleteSelectedLocalLibraries(scId,
                                adapter.getLocalLibraries(),
                                projectUsedLibs);
                        runOnUiThread(() -> {
                            resetSelectionAndReload("Deleted successfully");
                            h();
                        });
                    });
                    bottomSheetDialog.dismiss();
                });
                bottomSheetDialog.show();
            } else if (id == R.id.action_select_update_library) {
                Intent repoManagerIntent = new Intent(this,
                        RepoManagerActivity.class);
                startActivity(repoManagerIntent);
            }
            return false;
        });
        binding.downloadLibraryButton.setOnLongClickListener(v -> {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //noinspection deprecation
                    vibrator.vibrate(50);
                }
            }
            Path repositoriesJson = Paths.get(
                    Environment.getExternalStorageDirectory().getAbsolutePath(),
                    ".sketchware",
                    "libs",
                    "repositories.json"
            );
            // 3. Verificação correta se o arquivo NÃO existe (ou se está vazio)
            if (Files.notExists(repositoriesJson) || getFileSize(repositoriesJson.toFile()) == 0) {
                SketchwareUtil.toastError("Start downloading a library to unlock this menu!");
                return true; // Retorna true para indicar que o evento de clique longo foi consumido
            }
            // 4. Intent e transição de Activity
            Intent repoManagerIntent = new Intent(this, RepoManagerActivity.class);
            startActivity(repoManagerIntent);
            return true;
        });
        binding.downloadLibraryButton.setOnClickListener(v -> {
            if (getSupportFragmentManager().findFragmentByTag("library_downloader_dialog") != null) {
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putBoolean("notAssociatedWithProject",
                    notAssociatedWithProject);
            bundle.putSerializable("buildSettings",
                    buildSettings);
            bundle.putString("localLibFile",
                    getLocalLibFile(scId).getAbsolutePath());
            LibraryDownloaderDialogFragment fragment = new LibraryDownloaderDialogFragment();
            fragment.setArguments(bundle);
            fragment.setOnLibraryDownloadedTask(this::runLoadLocalLibrariesTask);
            fragment.show(getSupportFragmentManager(),
                    "library_downloader_dialog");
        });
        binding.searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().trim();
                searchAdapter.filter(getAdapterLocalLibraries(),
                        value);
            }

            @Override
            public void onTextChanged(CharSequence newText, int start, int before, int count) {
            }
        });
        runLoadLocalLibrariesTask();
    }

    private void showRenameBottomSheet(List<LocalLibrary> selectedLibs) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_rename,
                null);
        dialog.setContentView(view);
        TextInputLayout inputLayout = view.findViewById(R.id.text_input_layout_library);
        TextInputEditText editText = view.findViewById(R.id.textInputEditText);
        View buttonCancel = view.findViewById(R.id.button_cancel);
        View buttonRename = view.findViewById(R.id.button_rename);
        boolean isSingle = selectedLibs.size() == 1;
        LocalLibrary singleLib = isSingle ? selectedLibs.get(0) : null;
        // Configuração inicial do campo
        if (isSingle) {
            editText.setText(singleLib.getName());
            inputLayout.setHint(singleLib.getName());
        } else {
            //"Novo nome base (será adicionado sufixo se necessário)"
            inputLayout.setHint("New name base (will be added suffix if necessary)");
            editText.setText("");
        }
        buttonCancel.setOnClickListener(v -> {
            hideContextualToolbarAndClearSelection();
            dialog.dismiss();
            resetSelectionAndReload(null);
        });
        dialog.setOnDismissListener(d -> hideContextualToolbarAndClearSelection());
        buttonRename.setOnClickListener(v -> {
            String baseNewName = Objects.requireNonNull(editText.getText()).toString().trim();
            if (baseNewName.isEmpty()) {
                //"O nome não pode ficar em branco"
                inputLayout.setError("The name cannot be empty");
                return;
            }
            if (isSingle && baseNewName.equals(singleLib.getName())) {
                //"O novo nome deve ser diferente do atual"
                inputLayout.setError("The new name must be different from the current name");
                return;
            }
            // Verifica nomes já existentes (ignorando os que serão renomeados)
            Set<String> existingNames = adapter.getLocalLibraries()
                    .stream()
                    .filter(lib -> !lib.isSelected())
                    .map(LocalLibrary::getName)
                    .collect(Collectors.toSet());
            List<String> finalNames = generateUniqueNames(baseNewName,
                    selectedLibs.size(),
                    existingNames);
            if (finalNames.isEmpty()) {
                //"Não foi possível gerar nomes únicos"
                inputLayout.setError("Sorry! Don't possible generate unique names.");
                return;
            }
            // Confirmação extra se for lote
            if (!isSingle) {
                String message = "You are sure? " + selectedLibs.size() + " libraries will be renamed:\n\n" +
                        selectedLibs.stream()
                                .map(lib -> "• " + lib.getName() + " → " + finalNames.get(selectedLibs.indexOf(lib)))
                                .collect(Collectors.joining("\n"));
                final BottomSheetDialog dialog1 = new BottomSheetDialog(this);
                View view1 = getLayoutInflater().inflate(R.layout.bottom_sheet_rename,
                        null);
                dialog1.setContentView(view1);
                TextInputLayout inputLayout1 = view1.findViewById(R.id.text_input_layout_library);
                TextInputEditText editText1 = view1.findViewById(R.id.textInputEditText);
                TextView description1 = view1.findViewById(R.id.description);
                View buttonCancel1 = view1.findViewById(R.id.button_cancel);
                View buttonRename1 = view1.findViewById(R.id.button_rename);
                description1.setText(message);
                editText1.setText(baseNewName);
                editText1.setEnabled(false);
                inputLayout1.setHint(baseNewName);
                buttonCancel1.setOnClickListener(v1 -> dialog1.dismiss());
                buttonRename1.setOnClickListener(v1 -> {
                    performBatchRename(selectedLibs,
                            finalNames,
                            dialog1);
                    dialog.dismiss();
                    dialog1.dismiss();
                });
                dialog1.show();

            } else {
                performBatchRename(selectedLibs,
                        finalNames,
                        dialog);
            }
        });
        dialog.show();
    }

    /**
     * Generates a list of unique names based on a base name.
     *
     * @param baseName      The base for generating new names.
     * @param count         The desired number of unique names.
     * @param existingNames A set of names that are already taken.
     * @return A list of generated unique names.
     */
    private List<String> generateUniqueNames(String baseName, int count, Set<String> existingNames) {
        if (count <= 0) {
            return new ArrayList<>();
        }
        List<String> generatedNames = new ArrayList<>(count);
        Set<String> allExistingNames = new HashSet<>(existingNames);
        // First, try to use the baseName itself if it's unique and count is 1.
        // This is a common and desirable case.
        if (count == 1 && !allExistingNames.contains(baseName)) {
            generatedNames.add(baseName);
            return generatedNames;
        }
        int counter = 1;
        final int maxAttempts = count * 10; // Safeguard against potential infinite loops.
        while (generatedNames.size() < count) {
            @SuppressLint("DefaultLocale") String candidateName = String.format("%s (%d)", baseName, counter);
            if (!allExistingNames.contains(candidateName)) {
                generatedNames.add(candidateName);
                allExistingNames.add(candidateName); // Add to the set for efficient lookups.
            }
            counter++;
            if (counter > maxAttempts) {
                throw new IllegalStateException("Could not generate unique names after " + maxAttempts + " attempts.");
            }
        }
        return generatedNames;
    }

    private void performBatchRename(List<LocalLibrary> selectedLibs, List<String> newNames, BottomSheetDialog dialog) {
        k();
        Executors.newSingleThreadExecutor().execute(() -> {
            for (int i = 0; i < selectedLibs.size(); i++) {
                LocalLibrary lib = selectedLibs.get(i);
                String newName = newNames.get(i);
                renameSelectedLocalLibraryPath(scId,
                        newName,
                        lib.getName(),
                        adapter.getLocalLibraries(),
                        projectUsedLibs);
            }
            runOnUiThread(() -> {
                dialog.dismiss();
                //"Renomeação concluída com sucesso"
                resetSelectionAndReload("Renamed successfully!");
                h();
            });
        });
    }

    private void runLoadLocalLibrariesTask() {
        k();
        new Handler().postDelayed(() -> new LoadLocalLibrariesTask(this).execute(),
                500L);
    }

    private List<LocalLibrary> getAdapterLocalLibraries() {
        return adapter.getLocalLibraries();
    }

    private void hideContextualToolbarAndClearSelection() {
        adapter.isSelectionModeEnabled = false;
        if (collapseContextualToolbar()) {
            setLocalLibrariesSelected(false);
        }
    }

    public void setLocalLibrariesSelected(boolean selected) {
        for (LocalLibrary library : getAdapterLocalLibraries()) {
            library.setSelected(selected);
        }
        adapter.notifyDataSetChanged();
    }

    public void setLocalLibrariesInvertSelected() {
        for (LocalLibrary library : getAdapterLocalLibraries()) {
            library.setSelected(!library.isSelected());
        }
        adapter.notifyDataSetChanged();
    }

    public boolean hasSelectedLibrarys() {
        return getAdapterLocalLibraries().stream().anyMatch(LocalLibrary::isSelected);
    }

    private void expandContextualToolbar() {
        searchBarExpanded = true;
        binding.searchBar.expand(binding.contextualToolbarContainer,
                binding.appBarLayout);
    }

    private boolean collapseContextualToolbar() {
        searchBarExpanded = false;
        return binding.searchBar.collapse(binding.contextualToolbarContainer,
                binding.appBarLayout);
    }

    private long getSelectedLocalLibrariesCount() {
        long count = 0;
        for (LocalLibrary library : getAdapterLocalLibraries()) {
            if (library.isSelected()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void onBackPressed() {
        if (searchBarExpanded) {
            hideContextualToolbarAndClearSelection();
        } else if (binding.searchView.isShowing()) {
            binding.searchView.hide();
        } else {
            super.onBackPressed();
        }
    }

    private void resetSelectionAndReload(@Nullable String toastMessage) {
        adapter.isSelectionModeEnabled = false;
        setLocalLibrariesSelected(false);
        collapseContextualToolbar();
        loadLibraries();
        if (toastMessage != null) {
            SketchwareUtil.toast(toastMessage);
        }
    }

    // This method is running from the background thread.
    // So, every UI operation must be called inside `runOnUiThread`.
    private void loadLibraries() {
        var localLibraries = getAllLocalLibraries();
        if (!notAssociatedWithProject) {
            projectUsedLibs = getLocalLibraries(scId);
        }
        runOnUiThread(() -> {
            adapter.setLocalLibraries(localLibraries);
            binding.noContentLayout.setVisibility(localLibraries.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private boolean isUsedLibrary(String libraryName) {
        if (!notAssociatedWithProject) {
            for (Map<String, Object> libraryMap : projectUsedLibs) {
                if (libraryName.equals(Objects.requireNonNull(libraryMap.get("name")).toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    public interface OnLocalLibrarySelectedStateChangedListener {
        void invoke(LocalLibrary library);
    }

    private static class LoadLocalLibrariesTask extends MA {
        private final WeakReference<ManageLocalLibraryActivity> activity;

        public LoadLocalLibrariesTask(ManageLocalLibraryActivity activity) {
            super(activity);
            this.activity = new WeakReference<>(activity);
            activity.addTask(this);
        }

        @Override
        public void a() {
            activity.get().h();
        }

        @Override
        public void a(String idk) {
            activity.get().h();
        }

        @Override
        public void b() {
            try {
                activity.get().loadLibraries();
            } catch (Exception e) {
                Log.e("LoadLocalLibrariesTask",
                        "Error loading libraries",
                        e);
                e.printStackTrace();
            }
        }
    }

    public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolder> {
        private final List<LocalLibrary> localLibraries = new ArrayList<>();
        public boolean isSelectionModeEnabled;
        private @Nullable OnLocalLibrarySelectedStateChangedListener onLocalLibrarySelectedStateChangedListener;

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ViewItemLocalLibBinding.inflate(LayoutInflater.from(parent.getContext()),
                    parent,
                    false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            var binding = holder.binding;
            var library = localLibraries.get(position);
            binding.libraryName.setText(library.getName());
            binding.librarySize.setText(library.getSize());
            binding.libraryStatus.setBackgroundColor(getColor(R.color.transparent));
            binding.libraryName.setSelected(true);
            if (library.getInfo() != null) {
                binding.libraryStatus.setBackgroundColor(getColor(R.color.transparent));
            } else {
                binding.libraryStatus.setBackgroundColor(getColor(R.color.scolor_green_01));
            }
            bindSelectedState(binding.card,
                    library);
            binding.card.setOnClickListener(v -> {
                if (isSelectionModeEnabled) {
                    toggleLocalLibrary(binding.card,
                            library,
                            onLocalLibrarySelectedStateChangedListener);
                } else if (!notAssociatedWithProject) {
                    binding.materialSwitch.performClick();
                }
            });
            binding.card.setOnLongClickListener(v -> {
                if (isSelectionModeEnabled) {
                    return false;
                }
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(50);
                isSelectionModeEnabled = true;
                toggleLocalLibrary(binding.card,
                        library,
                        onLocalLibrarySelectedStateChangedListener);
                return true;
            });
            binding.materialSwitch.setChecked(false);
            if (!notAssociatedWithProject) {
                binding.materialSwitch.setOnClickListener(v -> onItemClicked(binding,
                        library.getName()));
                for (Map<String, Object> libraryMap : projectUsedLibs) {
                    if (library.getName().equals(Objects.requireNonNull(libraryMap.get("name")).toString())) {
                        binding.materialSwitch.setChecked(true);
                    }
                }
            } else {
                binding.materialSwitch.setEnabled(false);
            }
        }

        @Override
        public int getItemCount() {
            return localLibraries.isEmpty() ? 0 : localLibraries.size();
        }

        public void setOnLocalLibrarySelectedStateChangedListener(
                @Nullable OnLocalLibrarySelectedStateChangedListener onLocalLibrarySelectedStateChangedListener) {
            this.onLocalLibrarySelectedStateChangedListener = onLocalLibrarySelectedStateChangedListener;
        }

        private void toggleLocalLibrary(
                MaterialCardView card, LocalLibrary library,
                @Nullable OnLocalLibrarySelectedStateChangedListener onLocalLibrarySelectedStateChangedListener) {
            library.setSelected(!library.isSelected());
            bindSelectedState(card,
                    library);
            if (onLocalLibrarySelectedStateChangedListener != null) {
                onLocalLibrarySelectedStateChangedListener.invoke(library);
            }
            if (library.isSelected() && isUsedLibrary(library.getName())) {
                new MaterialAlertDialogBuilder(ManageLocalLibraryActivity.this).setTitle("Warning").setMessage("This "
                        + "library \"" + library.getName() + "\" already used in your project, removing it may break "
                        + "your project\rDo you want to continue removing it?").setPositiveButton(Helper.getResString(R.string.common_word_yes),
                        (dialog, which) -> dialog.dismiss()).setNegativeButton(Helper.getResString(R.string.common_word_cancel),
                        (dialog, which) -> {
                            toggleLocalLibrary(card,
                                    library,
                                    onLocalLibrarySelectedStateChangedListener);
                            dialog.dismiss();
                        }).show();
            }
        }

        private void bindSelectedState(MaterialCardView card, LocalLibrary library) {
            card.setChecked(library.isSelected());
        }

        private void onItemClicked(ViewItemLocalLibBinding binding, String name) {
            HashMap<String, Object> localLibrary;
            if (!binding.materialSwitch.isChecked()) {
                // Remove the library from the list
                int indexToRemove = -1;
                for (int i = 0; i < projectUsedLibs.size(); i++) {
                    Map<String, Object> libraryMap = projectUsedLibs.get(i);
                    if (name.equals(Objects.requireNonNull(libraryMap.get("name")).toString())) {
                        indexToRemove = i;
                        break;
                    }
                }
                if (indexToRemove != -1) {
                    projectUsedLibs.remove(indexToRemove);
                }
            } else {
                // Add the library to the list
                // Here, we need to find the dependency string if it exists
                String dependency = null;
                for (Map<String, Object> libraryMap : projectUsedLibs) {
                    if (name.equals(Objects.requireNonNull(libraryMap.get("name")).toString())) {
                        dependency = (String) libraryMap.get("dependency");
                        break;
                    }
                }
                localLibrary = createLibraryMap(name,
                        dependency);
                projectUsedLibs.add(localLibrary);
            }
            rewriteLocalLibFile(scId,
                    new Gson().toJson(projectUsedLibs));
        }

        public List<LocalLibrary> getLocalLibraries() {
            return localLibraries;
        }

        public void setLocalLibraries(List<LocalLibrary> localLibraries) {
            this.localLibraries.clear();
            this.localLibraries.addAll(localLibraries);
            notifyDataSetChanged();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final ViewItemLocalLibBinding binding;

            public ViewHolder(@NonNull ViewItemLocalLibBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
        private final List<LocalLibrary> filteredLocalLibraries = new ArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            var binding = ViewItemLocalLibSearchBinding.inflate(LayoutInflater.from(parent.getContext()),
                    parent,
                    false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            var binding = holder.binding;
            var library = filteredLocalLibraries.get(position);
            binding.libraryName.setText(library.getName());
            binding.librarySize.setText(library.getSize());
            binding.libraryName.setSelected(true);
            binding.materialSwitch.setChecked(false);
            if (!notAssociatedWithProject) {
                binding.getRoot().setOnClickListener(v -> binding.materialSwitch.performClick());
                binding.materialSwitch.setOnClickListener(v -> {
                    onItemClicked(binding,
                            library.getName());
                    adapter.notifyItemChanged(position);
                });
                for (Map<String, Object> libraryMap : projectUsedLibs) {
                    if (library.getName().equals(Objects.requireNonNull(libraryMap.get("name")).toString())) {
                        binding.materialSwitch.setChecked(true);
                    }
                }
            } else {
                binding.materialSwitch.setEnabled(false);
            }
        }

        @Override
        public int getItemCount() {
            return filteredLocalLibraries.isEmpty() ? 0 : filteredLocalLibraries.size();
        }

        private void onItemClicked(ViewItemLocalLibSearchBinding binding, String name) {
            HashMap<String, Object> localLibrary;
            if (!binding.materialSwitch.isChecked()) {
                // Remove the library from the list
                int indexToRemove = -1;
                for (int i = 0; i < projectUsedLibs.size(); i++) {
                    Map<String, Object> libraryMap = projectUsedLibs.get(i);
                    if (name.equals(Objects.requireNonNull(libraryMap.get("name")).toString())) {
                        indexToRemove = i;
                        break;
                    }
                }
                if (indexToRemove != -1) {
                    projectUsedLibs.remove(indexToRemove);
                }
            } else {
                // Add the library to the list
                // Here, we need to find the dependency string if it exists
                String dependency = null;
                for (Map<String, Object> libraryMap : projectUsedLibs) {
                    if (name.equals(Objects.requireNonNull(libraryMap.get("name")).toString())) {
                        dependency = (String) libraryMap.get("dependency");
                        break;
                    }
                }
                localLibrary = createLibraryMap(name,
                        dependency);
                projectUsedLibs.add(localLibrary);
            }
            rewriteLocalLibFile(scId,
                    new Gson().toJson(projectUsedLibs));
        }

        public void filter(List<LocalLibrary> localLibraries, String query) {
            filteredLocalLibraries.clear();
            if (query.isEmpty()) {
                filteredLocalLibraries.addAll(localLibraries);
            } else {
                for (LocalLibrary library : localLibraries) {
                    if (library.getName().toLowerCase().contains(query.toLowerCase())) {
                        filteredLocalLibraries.add(library);
                    }
                }
            }
            notifyDataSetChanged();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final ViewItemLocalLibSearchBinding binding;

            public ViewHolder(@NonNull ViewItemLocalLibSearchBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
