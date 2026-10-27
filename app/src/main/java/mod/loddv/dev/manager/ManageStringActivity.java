package mod.loddv.dev.manager;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.Toolbar.LayoutParams;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONArray;

import java.io.File;
import mod.loddv.dev.tools.extrator.StringExportUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import a.a.a.xB;
import mod.hey.studios.util.Helper;
import mod.loddv.dev.tools.offlineTranslator.AidlBatchTranslator;
import mod.loddv.dev.tools.offlineTranslator.TranslationManager;
import mod.loddv.dev.tools.offlineTranslator.TranslatorUtils;
import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.components.utils.StringsEditorManager;
import pro.sketchware.databinding.ActivityManageStringBinding;
import pro.sketchware.databinding.PalletCustomviewBinding;
import pro.sketchware.databinding.ViewStringEditorAddBinding;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class ManageStringActivity extends AppCompatActivity {

    private ActivityManageStringBinding binding;
    private final ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
    private final HashMap<Integer, String> notesMap = new HashMap<>();
    private StringsEditorManager stringsEditorManager;
    private ManageStringAdapter adapter;
    private File stringsFile;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private TranslationManager translationManager;
    private LinearLayout translateBar;
    private TextView selectionCount;
    private EditText sourceLang;
    private EditText targetLang;

    public String getSourceLang() {
        String v = sourceLang.getText().toString().trim();
        return v.isEmpty() ? "auto" : v;
    }

    public String getEffectiveSourceLang() {
        String src = getSourceLang();
        return "auto".equals(src) ? "en" : src; // Or handle auto-detection appropriately
    }

    public String getTargetLang() {
        String v = targetLang.getText().toString().trim();
        return v.isEmpty() ? "pt" : v;
    }

    public void onSelectionChanged() {
        boolean active = adapter.isSelectionMode();
        translateBar.setVisibility(active ? View.VISIBLE : View.GONE);
        selectionCount.setText(adapter.getSelectedCount() + "/" + adapter.getOriginalData().size());
        if (!active && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(null);
        }
    }

    @Override
    public void onBackPressed() {
        if (adapter != null && adapter.isSelectionMode()) {
            adapter.clearSelection();
        } else {
            super.onBackPressed();
        }
    }

    private void showLanguagePickerDialog(boolean isSource) {
        java.util.List<String> codes = new ArrayList<>();
        if (isSource) {
            codes.add("auto");
        }
        boolean fromService = false;
        if (translationManager != null && translationManager.isAidlConnected()) {
            try {
                String rawLangs = translationManager.getAvailableLanguages();
                if (rawLangs != null && !rawLangs.trim().isEmpty()) {
                    rawLangs = rawLangs.trim();
                    if (rawLangs.startsWith("[")) {
                        JSONArray array = new JSONArray(rawLangs);
                        for (int i = 0; i < array.length(); i++) {
                            String code = array.getString(i).trim();
                            if (!codes.contains(code)) codes.add(code);
                        }
                    } else {
                        String[] parts = rawLangs.split(",");
                        for (String part : parts) {
                            String code = part.trim();
                            if (!code.isEmpty() && !codes.contains(code)) codes.add(code);
                        }
                    }
                    fromService = true;
                }
            } catch (Exception ignored) {
            }
        }
        if (!fromService) {
            String[] defaultCodes = {"en", "pt", "es", "fr", "de", "it", "ru", "zh", "ja", "ar"};
            for (String code : defaultCodes) {
                if (!codes.contains(code)) codes.add(code);
            }
        }
        CharSequence[] displayNames = new CharSequence[codes.size()];
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            if ("auto".equalsIgnoreCase(code)) {
                displayNames[i] = "Auto Detect (auto)";
            } else {
                java.util.Locale locale = java.util.Locale.forLanguageTag(code);
                String name = locale.getDisplayLanguage(java.util.Locale.getDefault());
                if (!name.isEmpty() && !name.equalsIgnoreCase(code)) {
                    displayNames[i] = name.substring(0, 1).toUpperCase() + name.substring(1) + " (" + code + ")";
                } else {
                    displayNames[i] = code;
                }
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(isSource ? "Source Language" : "Target Language")
                .setItems(displayNames, (dialog, which) -> {
                    String selectedCode = codes.get(which);
                    if (isSource) {
                        sourceLang.setText(selectedCode);
                    } else {
                        targetLang.setText(selectedCode);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void buildTranslateBar(Toolbar toolbar) {
        translateBar = new LinearLayout(this);
        translateBar.setOrientation(LinearLayout.HORIZONTAL);
        translateBar.setGravity(Gravity.CENTER_VERTICAL);
        translateBar.setVisibility(View.GONE);
        selectionCount = new TextView(this);
        selectionCount.setPadding(16, 0, 16, 0);
        sourceLang = new EditText(this);
        sourceLang.setHint("auto");
        sourceLang.setText("auto");
        sourceLang.setFocusable(false);
        sourceLang.setClickable(true);
        sourceLang.setEms(3);
        sourceLang.setSingleLine();
        sourceLang.setOnClickListener(v -> showLanguagePickerDialog(true));
        targetLang = new EditText(this);
        targetLang.setHint("pt");
        targetLang.setText("pt");
        targetLang.setFocusable(false);
        targetLang.setClickable(true);
        targetLang.setEms(3);
        targetLang.setSingleLine();
        targetLang.setOnClickListener(v -> showLanguagePickerDialog(false));
        TextView arrow = new TextView(this);
        arrow.setText(" → ");
        Button start = new Button(this);
        start.setText("Translate");
        start.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add(0, 0, 0, Helper.getResString(R.string.common_word_select));
            popup.getMenu().add(0, 1, 1, Helper.getResString(R.string.common_word_all));
            popup.setOnMenuItemClickListener(mi -> {
                translateItems(mi.getItemId() == 1 ? new ArrayList<>(adapter.getOriginalData())
                        : adapter.getSelectedItems());
                return true;
            });
            popup.show();
        });
        translateBar.addView(selectionCount);
        translateBar.addView(sourceLang);
        translateBar.addView(arrow);
        translateBar.addView(targetLang);
        translateBar.addView(start);
        android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(this);
        scroll.addView(translateBar);
        toolbar.addView(scroll, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                Gravity.END));
    }

    private void showNotInstalledDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Offline Translator not installed")
                .setMessage("Install Offline Translator from F-Droid to use offline translation.")
                .setPositiveButton("Go to F-Droid",
                        (d, w) -> TranslatorUtils.openOfflineTranslatorInFdroid(this))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Blocking; call off the UI thread. Returns null on success, otherwise an error message.
     */
    private String ensureAidlConnected() {
        if (translationManager == null || translationManager.getAidlClient() == null) {
            return "Offline Translator service unavailable";
        }
        if (translationManager.isAidlConnected()) return null;
        if (translationManager.getAidlClient().connectAndWait(3000)) return null;
        return TranslatorUtils.isOfflineTranslatorInstalled(this)
                ? "Could not connect to Offline Translator service (timeout)"
                : "NOT_INSTALLED";
    }

    private boolean isActive() {
        return !isFinishing() && !isDestroyed();
    }

    public void translateItems(java.util.List<HashMap<String, Object>> items) {
        if (items.isEmpty()) {
            SketchwareUtil.toast("No strings to translate", Toast.LENGTH_SHORT);
            return;
        }
        String src = getSourceLang();
        String dst = getTargetLang();
        final int total = items.size();
        LinearProgressIndicator progressBar = new LinearProgressIndicator(this);
        progressBar.setIndeterminate(true);
        int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        container.addView(progressBar);
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(Helper.getResString(R.string.translating))
                .setMessage(Helper.getResString(R.string.translation_progress, 0, total))
                .setView(container)
                .setCancelable(false)
                .show();
        executorService.execute(() -> {
            String connError = ensureAidlConnected();
            if (connError != null) {
                runOnUiThread(() -> {
                    if (!isActive()) return;
                    progressDialog.dismiss();
                    if ("NOT_INSTALLED".equals(connError)) showNotInstalledDialog();
                    else SketchwareUtil.toast(connError, Toast.LENGTH_SHORT);
                });
                return;
            }
            java.util.List<HashMap<String, Object>> targets = new ArrayList<>();
            java.util.List<String> texts = new ArrayList<>();
            for (HashMap<String, Object> item : items) {
                Object text = item.get("text");
                if (text == null || text.toString().isEmpty()) continue;
                targets.add(item);
                texts.add(text.toString());
            }
            int changed = 0;
            int unchanged = 0;
            String error = null;
            try {
                AidlBatchTranslator aidlBatch = new AidlBatchTranslator(translationManager.getAidlClient(), src, dst);
                java.util.List<String> translated = aidlBatch.translate(texts);
                for (int i = 0; i < translated.size(); i++) {
                    String res = translated.get(i);
                    if (res != null && !res.equals(texts.get(i))) {
                        changed++;
                        targets.get(i).put("text", res);
                    } else {
                        unchanged++;
                    }
                }
            } catch (Exception e) {
                error = e.getMessage();
            }
            final int done = changed;
            final int same = unchanged;
            final String err = error;
            runOnUiThread(() -> {
                if (!isActive()) return;
                progressDialog.dismiss();
                adapter.notifyDataSetChanged();
                saveStringsAsync();
                String msg = "Translated " + done + "/" + total;
                if (same > 0)
                    msg += " (" + same + " unchanged" + ("auto".equals(src) ? ", try choosing the source language" :
                            "") + ")";
                if (err != null) msg += ". Error: " + err;
                SketchwareUtil.toast(msg, Toast.LENGTH_LONG);
            });
        });
    }

    public void translateSingle(HashMap<String, Object> item, String text, java.util.function.Consumer<String> cb,
                                Runnable onFinish) {
        if (text == null || text.isEmpty()) {
            if (onFinish != null) onFinish.run();
            return;
        }
        // Use getEffectiveSourceLang() if text is too short, otherwise use getSourceLang()
        String src = (text.length() < 25) ? getEffectiveSourceLang() : getSourceLang();
        String dst = getTargetLang();
        SketchwareUtil.toast("Translating...", Toast.LENGTH_SHORT);
        executorService.execute(() -> {
            String connError = ensureAidlConnected();
            String result = null;
            String error = connError;
            if (connError == null) {
                try {
                    result = translationManager.getAidlClient().translateSync(text, src, dst, 10000);
                } catch (Exception e) {
                    error = e.getMessage() != null ? e.getMessage() : "Translation failed";
                }
            }
            final String res = result;
            final String err = error;
            runOnUiThread(() -> {
                if (onFinish != null) onFinish.run();
                if (!isActive()) return;
                if ("NOT_INSTALLED".equals(err)) {
                    showNotInstalledDialog();
                } else if (err != null) {
                    SketchwareUtil.toast("Translation failed: " + err, Toast.LENGTH_LONG);
                } else if (res == null || res.equals(text)) {
                    SketchwareUtil.toast("Text unchanged" + ("auto".equals(src)
                            ? " - language detection may have failed; choose the source language explicitly"
                            : " - check source/target languages"), Toast.LENGTH_LONG);
                } else {
                    cb.accept(res);
                }
            });
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        translationManager = new TranslationManager(this);
        binding = ActivityManageStringBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        buildTranslateBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle("Localization Strings");
        stringsEditorManager = new StringsEditorManager();
        File localizationDir = new File(Environment.getExternalStorageDirectory(), "sketchware/localization");
        if (!localizationDir.exists()) {
            boolean ignored = localizationDir.mkdirs();
        }
        stringsFile = new File(localizationDir, "strings.xml");
        if (!stringsFile.exists()) {
            StringExportUtil.exportDefaultStrings(this, stringsFile);
        }
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        adapter = new ManageStringAdapter(this, listMap, notesMap);
        recyclerView.setAdapter(adapter);
        loadStringsAsync();
        onSelectionChanged();
        FloatingActionButton fab = binding.fab;
        fab.setImageResource(android.R.drawable.ic_menu_save);
        fab.setOnClickListener(v -> saveStringsAsync(() -> SketchwareUtil.toast("Strings saved successfully",
                Toast.LENGTH_SHORT)));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_manage_string, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        MenuItem scopeItem = menu.findItem(R.id.action_search_scope);
        if (searchItem != null) {
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    if (scopeItem != null) scopeItem.setVisible(true);
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    if (scopeItem != null) scopeItem.setVisible(false);
                    return true;
                }
            });
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint("Search strings...");
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        adapter.filter(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        adapter.filter(newText);
                        return true;
                    }
                });
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        int scope = -1;
        if (id == R.id.search_scope_all) scope = 0;
        else if (id == R.id.search_scope_keys) scope = 1;
        else if (id == R.id.search_scope_values) scope = 2;
        if (scope >= 0) {
            item.setChecked(true);
            adapter.setSearchScope(scope);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadStringsAsync() {
        executorService.execute(() -> {
            ArrayList<HashMap<String, Object>> loadedList = new ArrayList<>();
            HashMap<Integer, String> loadedNotes = new HashMap<>();
            StringsEditorManager manager = new StringsEditorManager();
            String content = FileUtil.readFileIfExist(stringsFile.getAbsolutePath());
            manager.convertXmlStringsToListMap(content, loadedList);
            loadedNotes.putAll(manager.notesMap);
            runOnUiThread(() -> {
                listMap.clear();
                listMap.addAll(loadedList);
                notesMap.clear();
                notesMap.putAll(loadedNotes);
                adapter.updateData(listMap, notesMap);
                onSelectionChanged();
            });
        });
    }

    public void saveStringsAsync() {
        saveStringsAsync(null);
    }

    public void saveStringsAsync(Runnable onComplete) {
        ArrayList<HashMap<String, Object>> dataSnapshot = new ArrayList<>(adapter.getOriginalData());
        HashMap<Integer, String> notesSnapshot = new HashMap<>(notesMap);
        executorService.execute(() -> {
            String xmlContent = stringsEditorManager.convertListMapToXmlStrings(dataSnapshot, notesSnapshot);
            FileUtil.writeFile(stringsFile.getAbsolutePath(), xmlContent);
            Context appContext = getApplicationContext();
            runOnUiThread(() -> {
                xB.b().b(appContext);
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveStringsAsync();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (translationManager != null) {
            translationManager.disconnect();
        }
        executorService.shutdown();
    }

    public static class ManageStringAdapter extends RecyclerView.Adapter<ManageStringAdapter.ViewHolder> {

        private final AppCompatActivity activity;
        private ArrayList<HashMap<String, Object>> originalData;
        private ArrayList<HashMap<String, Object>> filteredData;
        private final HashMap<Integer, String> notesMap;

        public ManageStringAdapter(AppCompatActivity activity, ArrayList<HashMap<String, Object>> data,
                                   HashMap<Integer, String> notesMap) {
            this.activity = activity;
            this.originalData = new ArrayList<>(data);
            this.filteredData = data;
            this.notesMap = notesMap;
        }

        public void updateData(ArrayList<HashMap<String, Object>> newData, HashMap<Integer, String> newNotesMap) {
            this.originalData = new ArrayList<>(newData);
            this.filteredData = newData;
            this.notesMap.clear();
            this.notesMap.putAll(newNotesMap);
            notifyDataSetChanged();
        }

        private final Set<HashMap<String, Object>> selected =
                Collections.newSetFromMap(new IdentityHashMap<>());

        public boolean isSelectionMode() {
            return !selected.isEmpty();
        }

        public int getSelectedCount() {
            return selected.size();
        }

        public ArrayList<HashMap<String, Object>> getSelectedItems() {
            ArrayList<HashMap<String, Object>> list = new ArrayList<>();
            for (HashMap<String, Object> item : originalData) {
                if (selected.contains(item)) list.add(item);
            }
            return list;
        }

        public void clearSelection() {
            selected.clear();
            notifyDataSetChanged();
            notifySelection();
        }

        private void toggle(HashMap<String, Object> item) {
            if (!selected.remove(item)) selected.add(item);
            notifyDataSetChanged();
            notifySelection();
        }

        private void notifySelection() {
            if (activity instanceof ManageStringActivity) {
                ((ManageStringActivity) activity).onSelectionChanged();
            }
        }

        private int originalIndexOf(HashMap<String, Object> item) {
            for (int i = 0; i < originalData.size(); i++) {
                if (originalData.get(i) == item) return i;
            }
            return -1;
        }

        public ArrayList<HashMap<String, Object>> getOriginalData() {
            return originalData;
        }

        private int searchScope = 0;
        private String lastQuery = "";

        public void setSearchScope(int scope) {
            searchScope = scope;
            filter(lastQuery);
        }

        public void filter(String query) {
            lastQuery = query;
            if (query == null || query.isEmpty()) {
                filteredData = originalData;
            } else {
                ArrayList<HashMap<String, Object>> filtered = new ArrayList<>();
                String lowerQuery = query.toLowerCase();
                for (HashMap<String, Object> item : originalData) {
                    String key = (String) item.get("key");
                    String text = (String) item.get("text");
                    boolean matchKey = searchScope != 2 && key != null && key.toLowerCase().contains(lowerQuery);
                    boolean matchText = searchScope != 1 && text != null && text.toLowerCase().contains(lowerQuery);
                    if (matchKey || matchText) {
                        filtered.add(item);
                    }
                }
                filteredData = filtered;
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            PalletCustomviewBinding binding =
                    PalletCustomviewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HashMap<String, Object> item = filteredData.get(position);
            String key = (String) item.get("key");
            String text = (String) item.get("text");
            holder.binding.title.setText(key);
            holder.binding.sub.setText(text);
            int noteIndex = originalIndexOf(item);
            if (notesMap.containsKey(noteIndex)) {
                holder.binding.tvTitle.setText(notesMap.get(noteIndex));
                holder.binding.tvTitle.setVisibility(View.VISIBLE);
            } else {
                holder.binding.tvTitle.setVisibility(View.GONE);
            }
            holder.binding.backgroundCard.setCheckable(true);
            holder.binding.backgroundCard.setChecked(selected.contains(item));
            holder.binding.backgroundCard.setOnLongClickListener(v -> {
                int adapterPosition = holder.getAbsoluteAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return false;
                toggle(filteredData.get(adapterPosition));
                return true;
            });
            holder.binding.backgroundCard.setOnClickListener(v -> {
                int adapterPosition = holder.getAbsoluteAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return;
                HashMap<String, Object> currentItem = filteredData.get(adapterPosition);
                if (isSelectionMode()) {
                    toggle(currentItem);
                    return;
                }
                MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(activity);
                ViewStringEditorAddBinding dialogBinding =
                        ViewStringEditorAddBinding.inflate(activity.getLayoutInflater());
                dialogBinding.stringKeyInput.setText((String) currentItem.get("key"));
                dialogBinding.stringKeyInput.setEnabled(false); // Key cannot be edited, only value (translation)
                dialogBinding.stringValueInput.setText((String) currentItem.get("text"));
                dialogBinding.stringHeaderInput.setText(notesMap.getOrDefault(originalIndexOf(currentItem), ""));
                dialogBinding.stringHeaderInput.setVisibility(View.GONE);
                dialog.setTitle("Translate string value");
                dialog.setPositiveButton("Save", (d, which) -> {
                    String valueInput = Objects.requireNonNull(dialogBinding.stringValueInput.getText()).toString();
                    if (valueInput.isEmpty()) {
                        SketchwareUtil.toast("Value cannot be empty", Toast.LENGTH_SHORT);
                        return;
                    }
                    currentItem.put("text", valueInput);
                    notifyItemChanged(adapterPosition);
                    if (activity instanceof ManageStringActivity) {
                        ((ManageStringActivity) activity).saveStringsAsync();
                    }
                    SketchwareUtil.toast("Translation updated", Toast.LENGTH_SHORT);
                });
                dialog.setNegativeButton(android.R.string.cancel, null);
                dialog.setNeutralButton("Translate", null);
                dialog.setView(dialogBinding.getRoot());
                androidx.appcompat.app.AlertDialog shown = dialog.show();
                shown.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(nv -> {
                    if (activity instanceof ManageStringActivity) {
                        nv.setEnabled(false);
                        ((ManageStringActivity) activity).translateSingle(currentItem,
                                String.valueOf(dialogBinding.stringValueInput.getText()).trim(),
                                r -> {
                                    if (shown.isShowing()) dialogBinding.stringValueInput.setText(r);
                                },
                                () -> nv.setEnabled(true));
                    }
                });
            });
        }

        @Override
        public int getItemCount() {
            return filteredData.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            PalletCustomviewBinding binding;

            public ViewHolder(PalletCustomviewBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
