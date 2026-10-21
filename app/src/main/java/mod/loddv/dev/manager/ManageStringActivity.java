package mod.loddv.dev.manager;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
            holder.binding.backgroundCard.setOnClickListener(v -> {
                int adapterPosition = holder.getAbsoluteAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return;
                HashMap<String, Object> currentItem = filteredData.get(adapterPosition);
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
                dialog.setView(dialogBinding.getRoot());
                dialog.show();
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
