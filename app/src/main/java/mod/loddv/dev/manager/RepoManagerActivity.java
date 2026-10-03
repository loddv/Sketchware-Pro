package mod.loddv.dev.manager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pro.sketchware.R;
import pro.sketchware.utility.SketchwareUtil;

public class RepoManagerActivity extends AppCompatActivity {

    private static final String TAG = "RepoManagerActivity";

    public static final File CONFIGURED_REPOSITORIES_FILE = new File(Environment.getExternalStorageDirectory(),
            ".sketchware/libs/repositories.json");
    private final ArrayList<Repository> filteredList = new ArrayList<>();
    private ArrayList<Repository> REPOSITORY_LIST = new ArrayList<>();
    private RepositoryAdapter adapter;
    private TextInputEditText searchEditText;
    private TextView indexSize;
    private FloatingActionButton addFab;
    private RecyclerView recyclerView;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.repository_list_dialog);
        setTheme(R.style.ThemeOverlay_SketchwarePro_BottomSheetDialog);
        initViews();
        setupRecyclerView();
        setupSearch();
        setupListeners();
        loadRepositories();
    }

    private void initViews() {
        searchEditText = findViewById(R.id.search_edit_text);
        recyclerView = findViewById(R.id.list_view);
        addFab = findViewById(R.id.add_fab);
        indexSize = findViewById(R.id.repo_index);
    }

    private void setupListeners() {
        addFab.setOnClickListener(v -> showAddRepositoryDialog());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RepositoryAdapter(filteredList);
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilter(s.toString().trim());
            }
        });
    }

    private void applyFilter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(REPOSITORY_LIST);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Repository repo : REPOSITORY_LIST) {
                String name = repo.getName();
                String url = repo.getUrl();
                if ((name != null && name.toLowerCase().contains(lowerQuery)) ||
                        (url != null && url.toLowerCase().contains(lowerQuery))) {
                    filteredList.add(repo);
                }
            }
        }
        updateIndex();
    }

    private void updateIndex() {
        indexSize.setText("index: " + filteredList.size());
        adapter.notifyDataSetChanged();
        addFab.show();
    }

    private void showAddRepositoryDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        Context context = RepoManagerActivity.this;
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_rename,
                null,
                false);
        TextView title = view.findViewById(R.id.title);
        TextInputEditText nameEditText = view.findViewById(R.id.textInputEditText);
        TextInputLayout nameInputLayout = view.findViewById(R.id.text_input_layout_library);
        TextInputEditText urlEditText = view.findViewById(R.id.text2InputEditText);
        TextInputLayout urlInputLayout = view.findViewById(R.id.text2_input_layout_library);
        TextView description = view.findViewById(R.id.description);
        MaterialButton buttonSave = view.findViewById(R.id.button_rename);
        MaterialButton buttonCancel = view.findViewById(R.id.button_cancel);
        buttonCancel.setText(getString(R.string.cancel));
        buttonSave.setText(getString(R.string.common_word_add));
        title.setText("Add Repository");
        nameInputLayout.setHint("Local library name");
        urlInputLayout.setHint("Repository URL");
        urlInputLayout.setVisibility(View.VISIBLE);
        description.setText("Enter the name and URL of the new repository.");
        buttonSave.setText(R.string.common_word_add);
        buttonCancel.setText(R.string.cancel);
        buttonSave.setOnClickListener(v -> {
            String name = Objects.requireNonNull(nameEditText.getText()).toString().trim();
            String url = Objects.requireNonNull(urlEditText.getText()).toString().trim();
            nameInputLayout.setError(null);
            urlInputLayout.setError(null);
            if (name.isEmpty()) {
                nameInputLayout.setError("Name cannot be empty");
                return;
            }
            if (url.isEmpty()) {
                urlInputLayout.setError("URL cannot be empty");
                return;
            }
            if (!Patterns.WEB_URL.matcher(url).matches()) {
                urlInputLayout.setError("Invalid URL format");
                return;
            }
            if (REPOSITORY_LIST.stream().anyMatch(repo -> Objects.equals(repo.getName(), name))) {
                nameInputLayout.setError("A repository with this name already exists");
                return;
            }
            if (REPOSITORY_LIST.stream().anyMatch(repo -> Objects.equals(repo.getUrl(), url))) {
                urlInputLayout.setError("A repository with this URL already exists");
                return;
            }
            if (name.length() > 20) {
                nameInputLayout.setError("Name cannot be longer than 20 characters");
                return;
            }
            Repository repo = new Repository(name, url);
            repo.setMenuExpanded(View.GONE);
            REPOSITORY_LIST.add(repo);
            applyFilter(Objects.requireNonNull(searchEditText.getText()).toString());
            saveRepositories();
            dialog.dismiss();
            SketchwareUtil.showMessage(getApplicationContext(),
                    "Added successfully!");
        });
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(view);
        dialog.show();
    }

    private void saveRepositories() {
        File parentDir = CONFIGURED_REPOSITORIES_FILE.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        try (FileWriter writer = new FileWriter(CONFIGURED_REPOSITORIES_FILE)) {
            gson.toJson(REPOSITORY_LIST, writer);
        } catch (IOException e) {
            Log.e(TAG, "Error saving repositories", e);
            SketchwareUtil.showMessage(getApplicationContext(),
                    "Error saving repositories: " + e.getMessage());
        }
        updateIndex();
    }

    private void loadRepositories() {
        if (!CONFIGURED_REPOSITORIES_FILE.exists()) {
            REPOSITORY_LIST = new ArrayList<>();
            saveRepositories();
            return;
        }

        try (FileReader reader = new FileReader(CONFIGURED_REPOSITORIES_FILE)) {
            REPOSITORY_LIST = gson.fromJson(reader,
                    new TypeToken<ArrayList<Repository>>() {}.getType());
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            Log.e(TAG, "Failed to load repositories", e);
            REPOSITORY_LIST = new ArrayList<>();
        }

        // Initialize menu_expanded for UI state
        for (Repository repo : REPOSITORY_LIST) {
            repo.setMenuExpanded(View.GONE);
        }

        filteredList.clear();
        filteredList.addAll(REPOSITORY_LIST);
        updateIndex();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private int getColorFromAttr(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr,
                typedValue,
                true);
        return typedValue.data;
    }

    /// ========= ADAPTER =========
    private class RepositoryAdapter extends RecyclerView.Adapter<RepositoryAdapter.ViewHolder> {

        private final List<Repository> data;

        public RepositoryAdapter(List<Repository> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.repository_item,
                    parent,
                    false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Repository repo = data.get(position);
            holder.bind(repo,
                    position);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameTv, urlTv;
            ImageButton expandBtn;
            MaterialButton deleteBtn, editBtn;
            LinearLayout optionsLayout;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                nameTv = itemView.findViewById(R.id.name_text_view);
                urlTv = itemView.findViewById(R.id.url_text_view);
                expandBtn = itemView.findViewById(R.id.expand_button_view);
                optionsLayout = itemView.findViewById(R.id.expand_options_view);
                deleteBtn = itemView.findViewById(R.id.expand_options_view_delete);
                editBtn = itemView.findViewById(R.id.expand_options_view_edit);
            }

            void bind(Repository repo, int position) {
                String name = repo.getName();
                String url = repo.getUrl();
                int visibility = repo.getMenuExpanded();
                nameTv.setText(name);
                urlTv.setText(url);
                optionsLayout.setVisibility(visibility == View.GONE ? View.GONE : (visibility == View.INVISIBLE ? View.INVISIBLE : View.VISIBLE));
                expandBtn.setImageResource(visibility == View.GONE ? R.drawable.selector_ic_expand_more_24 :
                        R.drawable.selector_ic_expand_less_24);
                expandBtn.setOnClickListener(v -> toggleExpand(repo,
                        optionsLayout,
                        expandBtn));
                deleteBtn.setOnClickListener(v -> showDeleteDialog(repo,
                        position));
                editBtn.setOnClickListener(v -> showEditDialog(repo,
                        position));
            }

            private void toggleExpand(Repository repo, LinearLayout options, ImageButton btn) {
                final boolean isExpanded = options.getVisibility() == View.VISIBLE;
                final int targetVisibility = isExpanded ? View.GONE : View.VISIBLE;
                repo.setMenuExpanded(targetVisibility);
                // Animate button rotation
                ObjectAnimator.ofFloat(btn,
                        "rotation",
                        isExpanded ? 180f : 0f,
                        isExpanded ? 0f : 180f).setDuration(200).start();
                if (targetVisibility == View.VISIBLE) {
                    options.measure(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    final int targetHeight = options.getMeasuredHeight();
                    options.getLayoutParams().height = 0;
                    options.setVisibility(View.VISIBLE);
                    options.setAlpha(0f);
                    android.animation.ValueAnimator va = android.animation.ValueAnimator.ofInt(0,
                            targetHeight);
                    va.addUpdateListener(animation -> {
                        options.getLayoutParams().height = (Integer) animation.getAnimatedValue();
                        options.requestLayout();
                    });
                    va.setDuration(200);
                    ObjectAnimator alpha = ObjectAnimator.ofFloat(options,
                            "alpha",
                            0f,
                            1f);
                    alpha.setDuration(200);
                    AnimatorSet set = new AnimatorSet();
                    set.playTogether(va,
                            alpha);
                    set.start();
                } else {
                    final int initialHeight = options.getMeasuredHeight();
                    android.animation.ValueAnimator va = android.animation.ValueAnimator.ofInt(initialHeight,
                            0);
                    va.addUpdateListener(animation -> {
                        options.getLayoutParams().height = (Integer) animation.getAnimatedValue();
                        options.requestLayout();
                    });
                    ObjectAnimator alpha = ObjectAnimator.ofFloat(options,
                            "alpha",
                            1f,
                            0f);
                    alpha.setDuration(200);
                    AnimatorSet set = new AnimatorSet();
                    set.playTogether(va,
                            alpha);
                    set.setDuration(200);
                    set.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            options.setVisibility(View.GONE);
                            options.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        }
                    });
                    set.start();
                }
                btn.setImageResource(targetVisibility == View.VISIBLE ? R.drawable.selector_ic_expand_less_24 :
                        R.drawable.selector_ic_expand_more_24);
            }

            private void showDeleteDialog(Repository repo, int position) {
                @SuppressWarnings("unused")
                int pos = position; // Used for notifyItemChanged in future enhancements
                BottomSheetDialog dialog = new BottomSheetDialog(RepoManagerActivity.this);
                View view = getLayoutInflater().inflate(R.layout.bottom_sheet_rename,
                        null,
                        false);
                TextView title = view.findViewById(R.id.title);
                TextInputLayout inputLayout = view.findViewById(R.id.text_input_layout_library);
                EditText nameEt = view.findViewById(R.id.textInputEditText);
                MaterialButton deleteBtn = view.findViewById(R.id.button_rename);
                MaterialButton cancelBtn = view.findViewById(R.id.button_cancel);
                title.setText("Delete Repository");
                inputLayout.setHint("This repository will be permanently removed!");
                nameEt.setText(repo.getName());
                nameEt.setEnabled(false);
                deleteBtn.setText("Delete");
                deleteBtn.setBackgroundColor(getResources().getColor(R.color.scolor_red_02));
                deleteBtn.setOnClickListener(v -> {
                    REPOSITORY_LIST.remove(repo);
                    applyFilter(Objects.requireNonNull(searchEditText.getText()).toString());
                    saveRepositories();
                    dialog.dismiss();
                    SketchwareUtil.showMessage(getApplicationContext(),
                            "Removed successfully!");
                });
                cancelBtn.setOnClickListener(v -> dialog.dismiss());
                dialog.setContentView(view);
                dialog.show();
            }

            private void showEditDialog(Repository repo, int position) {
                BottomSheetDialog dialog = new BottomSheetDialog(RepoManagerActivity.this);
                Context context = RepoManagerActivity.this;
                View view = getLayoutInflater().inflate(R.layout.bottom_sheet_rename,
                        null,
                        false);
                TextView title = view.findViewById(R.id.title);
                TextInputEditText nameEditText = view.findViewById(R.id.textInputEditText);
                TextInputLayout nameInputLayout = view.findViewById(R.id.text_input_layout_library);
                TextInputEditText urlEditText = view.findViewById(R.id.text2InputEditText);
                TextInputLayout urlInputLayout = view.findViewById(R.id.text2_input_layout_library);
                TextView description = view.findViewById(R.id.description);
                MaterialButton buttonSave = view.findViewById(R.id.button_rename);
                MaterialButton buttonCancel = view.findViewById(R.id.button_cancel);
                title.setText("Edit Repository");
                nameInputLayout.setHint("Local library name");
                // Preenche com o nome atual
                String currentName = repo.getName();
                if (currentName != null) {
                    nameEditText.setText(currentName);
                    nameEditText.setSelection(currentName.length());
                }
                urlInputLayout.setVisibility(View.VISIBLE);
                urlInputLayout.setHint("Repository URL");
                // Preenche com a URL atual
                String currentUrl = repo.getUrl();
                if (currentUrl != null) {
                    urlEditText.setText(currentUrl);
                }
                description.setText("Update the name and URL of the repository. Changes will be saved to your build " + "configuration.");
                buttonCancel.setText(getString(R.string.common_word_cancel));
                buttonSave.setText("Save");
                buttonSave.setOnClickListener(v -> {
                    String newName = Objects.requireNonNull(nameEditText.getText()).toString().trim();
                    String newUrl = Objects.requireNonNull(urlEditText.getText()).toString().trim();
                    if (newName.isEmpty()) {
                        nameInputLayout.setError("Name is required");
                        urlInputLayout.setError(null);
                        return;
                    }
                    nameInputLayout.setError(null);
                    if (newUrl.isEmpty()) {
                        urlInputLayout.setError("URL is required");
                        return;
                    }
                    if (!android.util.Patterns.WEB_URL.matcher(newUrl).matches()) {
                        urlInputLayout.setError("Enter a valid URL");
                        return;
                    }
                    urlInputLayout.setError(null);
                    // Atualiza o repo
                    repo.setName(newName);
                    repo.setUrl(newUrl);
                    saveRepositories();
                    adapter.notifyItemChanged(position);
                    dialog.dismiss();
                    SketchwareUtil.showMessage(getApplicationContext(),
                            "Repository updated successfully!");
                });
                buttonCancel.setOnClickListener(v -> dialog.dismiss());
                dialog.setContentView(view);
                dialog.show();
            }
        }
    }
}
