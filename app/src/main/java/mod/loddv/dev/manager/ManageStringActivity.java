package mod.loddv.dev.manager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar.LayoutParams;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.json.JSONArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import a.a.a.xB;
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
    private LinearLayout translateBar;
    private TextView selectionCount;
    private EditText sourceLang;
    private EditText targetLang;

    public String getSourceLang() {
        String v = sourceLang.getText().toString().trim();
        return v.isEmpty() ? "auto" : v;
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
        sourceLang.setInputType(InputType.TYPE_CLASS_TEXT);
        sourceLang.setEms(3);
        sourceLang.setSingleLine();
        targetLang = new EditText(this);
        targetLang.setHint("pt");
        targetLang.setText("pt");
        targetLang.setInputType(InputType.TYPE_CLASS_TEXT);
        targetLang.setEms(3);
        targetLang.setSingleLine();
        TextView arrow = new TextView(this);
        arrow.setText("→");
        Button start = new Button(this);
        start.setText("Translate");
        start.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add(0, 0, 0, "Selecionados");
            popup.getMenu().add(0, 1, 1, "Todos");
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

    public void translateItems(java.util.List<HashMap<String, Object>> items) {
        if (items.isEmpty()) {
            SketchwareUtil.toast("No strings to translate", Toast.LENGTH_SHORT);
            return;
        }
        String src = getSourceLang();
        String dst = getTargetLang();
        final int total = items.size();
        androidx.appcompat.app.AlertDialog progressDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Translating")
                .setMessage("0/" + total)
                .setCancelable(false)
                .show();
        executorService.execute(() -> {
            int ok = 0;
            int current = 0;
            for (HashMap<String, Object> item : items) {
                final int shown = ++current;
                runOnUiThread(() -> progressDialog.setMessage(shown + "/" + total));
                Object text = item.get("text");
                if (text == null || text.toString().isEmpty()) continue;
                String result = translateText(text.toString(), src, dst);
                if (result != null) {
                    item.put("text", result);
                    ok++;
                }
            }
            final int done = ok;
            runOnUiThread(() -> {
                progressDialog.dismiss();
                adapter.notifyDataSetChanged();
                saveStringsAsync();
                SketchwareUtil.toast("Translated " + done + "/" + items.size(), Toast.LENGTH_SHORT);
            });
        });
    }

    public void translateSingle(HashMap<String, Object> item, String text, java.util.function.Consumer<String> cb) {
        String src = getSourceLang();
        String dst = getTargetLang();
        executorService.execute(() -> {
            String r = translateText(text, src, dst);
            runOnUiThread(() -> {
                if (r == null) {
                    SketchwareUtil.toast("Translation failed", Toast.LENGTH_SHORT);
                } else {
                    cb.accept(r);
                }
            });
        });
    }

    private static String translateText(String text, String src, String dst) {
        HttpURLConnection conn = null;
        try {
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl="
                    + URLEncoder.encode(src, "UTF-8") + "&tl=" + URLEncoder.encode(dst, "UTF-8")
                    + "&dt=t&q=" + URLEncoder.encode(text, "UTF-8");
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            if (conn.getResponseCode() != 200) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            JSONArray parts = new JSONArray(sb.toString()).getJSONArray(0);
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < parts.length(); i++) {
                out.append(parts.getJSONArray(i).getString(0));
            }
            return out.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            FileUtil.writeFile(stringsFile.getAbsolutePath(), "<resources>\n</resources>");
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
        if (searchItem != null) {
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

        public ArrayList<HashMap<String, Object>> getOriginalData() {
            return originalData;
        }

        public void filter(String query) {
            if (query == null || query.isEmpty()) {
                filteredData = originalData;
            } else {
                ArrayList<HashMap<String, Object>> filtered = new ArrayList<>();
                String lowerQuery = query.toLowerCase();
                for (HashMap<String, Object> item : originalData) {
                    String key = (String) item.get("key");
                    String text = (String) item.get("text");
                    if ((key != null && key.toLowerCase().contains(lowerQuery)) ||
                            (text != null && text.toLowerCase().contains(lowerQuery))) {
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
            if (notesMap.containsKey(position)) {
                holder.binding.tvTitle.setText(notesMap.get(position));
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
                dialogBinding.stringHeaderInput.setText(notesMap.getOrDefault(adapterPosition, ""));
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
                        ((ManageStringActivity) activity).translateSingle(currentItem,
                                String.valueOf(dialogBinding.stringValueInput.getText()),
                                r -> dialogBinding.stringValueInput.setText(r));
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
