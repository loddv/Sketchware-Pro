package com.besome.sketch.editor.manage;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.manage.view.AddCustomViewActivity;
import com.besome.sketch.editor.manage.view.AddViewActivity;
import com.besome.sketch.editor.manage.view.PresetSettingActivity;
import com.besome.sketch.lib.base.BaseAppCompatActivity;

import java.util.ArrayList;

import a.a.a.eC;
import a.a.a.hC;
import a.a.a.jC;
import a.a.a.mB;
import a.a.a.rq;
import a.a.a.wq;
import pro.sketchware.R;
import pro.sketchware.databinding.FileSelectorPopupSelectXmlActivityItemBinding;
import pro.sketchware.databinding.FileSelectorPopupSelectXmlBinding;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.ThemeUtils;
import pro.sketchware.utility.UI;

public class ViewSelectorActivity extends BaseAppCompatActivity {
    private final int[] x = new int[19];
    private final int TAB_ACTIVITY = ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY;
    private final int TAB_CUSTOM_VIEW = ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW;
    private ViewSelectorAdapter viewSelectorAdapter;
    private String sc_id;
    private ProjectFileBean projectFile;
    private String currentXml;
    private int selectedTab;
    private boolean isCustomView = false;
    private FileSelectorPopupSelectXmlBinding binding;

    private int getViewIcon(int i) {
        String replace = String.format("%4s", Integer.toBinaryString(i)).replace(' ', '0');
        return getApplicationContext().getResources().getIdentifier("activity_" + replace, "drawable",
                getApplicationContext().getPackageName());
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.ani_fade_in, R.anim.ani_fade_out);
    }

    private ArrayList<String> getScreenNames() {
        ArrayList<String> screenNames = new ArrayList<>();
        ArrayList<ProjectFileBean> activities = jC.b(sc_id).b();
        if (activities != null) {
            for (ProjectFileBean projectFileBean : activities) {
                screenNames.add(projectFileBean.fileName);
            }
        }
        ArrayList<ProjectFileBean> customViews = jC.b(sc_id).c();
        if (customViews != null) {
            for (ProjectFileBean projectFileBean : customViews) {
                screenNames.add(projectFileBean.fileName);
            }
        }
        return screenNames;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 264:
                if (resultCode == RESULT_OK) {
                    ProjectFileBean projectFile = data.getParcelableExtra("project_file");
                    jC.b(sc_id).a(projectFile);
                    if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)) {
                        jC.b(sc_id).a(2, projectFile.getDrawerName());
                    }
                    if (data.hasExtra("preset_views")) {
                        a(projectFile, data.getParcelableArrayListExtra("preset_views"));
                    }
                    jC.b(sc_id).j();
                    jC.b(sc_id).l();
                    viewSelectorAdapter.notifyDataSetChanged();
                }
                break;
            case 265:
                if (resultCode == RESULT_OK) {
                    ProjectFileBean projectFile = data.getParcelableExtra("project_file");
                    ProjectFileBean activity = jC.b(sc_id).b().get(viewSelectorAdapter.selectedItem);
                    activity.keyboardSetting = projectFile.keyboardSetting;
                    activity.orientation = projectFile.orientation;
                    activity.options = projectFile.options;
                    if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)) {
                        if (jC.b(sc_id).b(projectFile.getDrawerXmlName()) == null) {
                            jC.b(sc_id).a(2, projectFile.getDrawerName());
                        }
                    } else {
                        jC.b(sc_id).b(2, projectFile.getDrawerName());
                    }
                    if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)
                            || projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB)) {
                        jC.c(sc_id).c().useYn = "Y";
                    }
                    viewSelectorAdapter.notifyItemChanged(viewSelectorAdapter.selectedItem);
                    Intent intent = new Intent();
                    intent.putExtra("project_file", projectFile);
                    setResult(RESULT_OK, intent);
                }
                break;
            case 266:
                if (resultCode == RESULT_OK) {
                    ProjectFileBean projectFile = data.getParcelableExtra("project_file");
                    jC.b(sc_id).a(projectFile);
                    if (data.hasExtra("preset_views")) {
                        a(projectFile, data.getParcelableArrayListExtra("preset_views"));
                    }
                    jC.b(sc_id).j();
                    jC.b(sc_id).l();
                    viewSelectorAdapter.notifyDataSetChanged();
                }
                break;
            case 276:
                if (resultCode == RESULT_OK) {
                    ProjectFileBean presetData = data.getParcelableExtra("preset_data");
                    ProjectFileBean activity = jC.b(sc_id).b().get(viewSelectorAdapter.selectedItem);
                    activity.keyboardSetting = presetData.keyboardSetting;
                    activity.orientation = presetData.orientation;
                    activity.options = presetData.options;
                    if (presetData.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)
                            || presetData.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB)) {
                        jC.c(sc_id).c().useYn = "Y";
                    }
                    a(presetData, activity, requestCode);
                    jC.b(sc_id).j();
                    viewSelectorAdapter.notifyDataSetChanged();
                    Intent intent2 = new Intent();
                    intent2.putExtra("project_file", activity);
                    setResult(RESULT_OK, intent2);
                }
                break;
            case 277:
            case 278:
                if (resultCode == RESULT_OK) {
                    ProjectFileBean presetData = data.getParcelableExtra("preset_data");
                    ProjectFileBean customView = jC.b(sc_id).c().get(viewSelectorAdapter.selectedItem);
                    a(presetData, customView, requestCode);
                    jC.b(sc_id).j();
                    viewSelectorAdapter.notifyDataSetChanged();
                    Intent intent3 = new Intent();
                    intent3.putExtra("project_file", customView);
                    setResult(RESULT_OK, intent3);
                }
                break;
            default:
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = FileSelectorPopupSelectXmlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            sc_id = intent.getStringExtra("sc_id");
            currentXml = intent.getStringExtra("current_xml");
            isCustomView = intent.getBooleanExtra("is_custom_view", false);
        } else {
            sc_id = savedInstanceState.getString("sc_id");
            currentXml = savedInstanceState.getString("current_xml");
            isCustomView = savedInstanceState.getBoolean("is_custom_view");
        }
        if (isCustomView) {
            selectedTab = TAB_CUSTOM_VIEW;
        } else {
            selectedTab = TAB_ACTIVITY;
        }
        binding.optionsSelector.check(selectedTab == TAB_ACTIVITY ? R.id.option_view : R.id.option_custom_view);
        binding.emptyMessage.setText(R.string.design_manager_view_message_no_view);
        viewSelectorAdapter = new ViewSelectorAdapter();
        binding.listXml.setHasFixedSize(true);
        binding.listXml.setAdapter(viewSelectorAdapter);
        //resetRecyclerViewPositionAndFocus();
        binding.listXml.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    binding.createNewView.hide();
                } else {
                    binding.createNewView.show();
                }
            }
        });
        binding.optionsSelector.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.option_view) {
                    selectedTab = TAB_ACTIVITY;
                } else if (checkedId == R.id.option_custom_view) {
                    selectedTab = TAB_CUSTOM_VIEW;
                }
                viewSelectorAdapter.notifyDataSetChanged();
                binding.emptyMessage.setVisibility(viewSelectorAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });
        binding.createNewView.setOnClickListener(v -> {
            if (!mB.a()) {
                if (selectedTab == TAB_ACTIVITY) {
                    Intent intent = new Intent(getApplicationContext(), AddViewActivity.class);
                    intent.putStringArrayListExtra("screen_names", getScreenNames());
                    intent.putExtra("request_code", 264);
                    startActivityForResult(intent, 264);
                } else if (selectedTab == TAB_CUSTOM_VIEW) {
                    Intent intent = new Intent(getApplicationContext(), AddCustomViewActivity.class);
                    intent.putStringArrayListExtra("screen_names", getScreenNames());
                    startActivityForResult(intent, 266);

                }
            }
        });
        binding.container.setOnClickListener(v -> finish());
        overridePendingTransition(R.anim.ani_fade_in, R.anim.ani_fade_out);
        UI.addSystemWindowInsetToPadding(binding.container, true, true, true, false);
        UI.addSystemWindowInsetToMargin(binding.createNewView, false, false, false, true);
    }

    private void resetRecyclerViewPositionAndFocus() {
        if (viewSelectorAdapter != null && viewSelectorAdapter.getItemCount() > 0) {
            binding.listXml.scrollToPosition(0);
            binding.listXml.post(() -> {
                RecyclerView.ViewHolder holder = binding.listXml.findViewHolderForAdapterPosition(0);
                if (holder != null) {
                    holder.itemView.requestFocus();
                } else {
                    binding.listXml.requestFocus();
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        outState.putString("current_xml", currentXml);
        outState.putBoolean("is_custom_view", isCustomView);
        super.onSaveInstanceState(outState);
    }

    private void a(ProjectFileBean projectFile, ArrayList<ViewBean> presetViews) {
        jC.a(sc_id);
        for (ViewBean view : eC.a(presetViews)) {
            view.id = a(view.type, projectFile.getXmlName());
            jC.a(sc_id).a(projectFile.getXmlName(), view);
            if (view.type == ViewBean.VIEW_TYPE_WIDGET_BUTTON
                    && projectFile.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY) {
                jC.a(sc_id).a(projectFile.getJavaName(), 1, view.type, view.id, "onClick");
            }
        }
    }

    private void a(ProjectFileBean presetData, ProjectFileBean projectFile, int requestCode) {
        ArrayList<ViewBean> d = jC.a(sc_id).d(projectFile.getXmlName());
        for (int size = d.size() - 1; size >= 0; size--) {
            jC.a(sc_id).a(projectFile, d.get(size));
        }
        ArrayList<ViewBean> a = a(presetData.presetName, requestCode);
        jC.a(sc_id);
        for (ViewBean view : eC.a(a)) {
            view.id = a(view.type, projectFile.getXmlName());
            jC.a(sc_id).a(projectFile.getXmlName(), view);
            if (view.type == ViewBean.VIEW_TYPE_WIDGET_BUTTON
                    && projectFile.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY) {
                jC.a(sc_id).a(projectFile.getJavaName(), 1, view.type, view.id, "onClick");
            }
        }
    }

    private ArrayList<ViewBean> a(String presetName, int requestCode) {
        ArrayList<ViewBean> views = new ArrayList<>();
        return switch (requestCode) {
            case 276 -> rq.f(presetName);
            case 277 -> rq.b(presetName);
            case 278 -> rq.d(presetName);
            default -> views;
        };
    }

    private String a(int viewType, String xmlName) {
        String b = wq.b(viewType);
        StringBuilder sb = new StringBuilder();
        sb.append(b);
        int i2 = x[viewType] + 1;
        x[viewType] = i2;
        sb.append(i2);
        String sb2 = sb.toString();
        ArrayList<ViewBean> d = jC.a(sc_id).d(xmlName);
        while (true) {
            boolean z = false;
            for (ViewBean viewBean : d) {
                if (sb2.equals(viewBean.id)) {
                    z = true;
                    break;
                }
            }
            if (!z) {
                return sb2;
            }
            sb = new StringBuilder();
            sb.append(b);
            i2 = x[viewType] + 1;
            x[viewType] = i2;
            sb.append(i2);
            sb2 = sb.toString();
        }
    }

    private int a(ProjectFileBean projectFileBean) {
        if (selectedTab == 0) {
            return 276;
        }
        return projectFileBean.fileType == ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW ? 277 : 278;
    }

    private class ViewSelectorAdapter extends RecyclerView.Adapter<ViewSelectorAdapter.ViewHolder> {
        private int selectedItem = -1;

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
            if (selectedTab == TAB_ACTIVITY) {
                viewHolder.itemBinding.tvFilename.setVisibility(View.VISIBLE);
                viewHolder.itemBinding.tvLinkedFilename.setVisibility(View.VISIBLE);
                ProjectFileBean projectFileBean = jC.b(sc_id).b().get(position);
                String xmlName = projectFileBean.getXmlName();
                if (currentXml.equals(xmlName)) {
                    viewHolder.itemBinding.cardView.setStrokeColor(
                            ThemeUtils.getColor(ViewSelectorActivity.this, R.attr.colorPrimary));
                    viewHolder.itemBinding.cardView.setStrokeWidth(SketchwareUtil.dpToPx(3f));
                } else {
                    viewHolder.itemBinding.cardView.setStrokeWidth(SketchwareUtil.dpToPx(0f));
                }
                String javaName = projectFileBean.getJavaName();
                viewHolder.itemBinding.imgEdit.setVisibility(View.VISIBLE);
                viewHolder.itemBinding.imgView.setImageResource(getViewIcon(projectFileBean.options));
                viewHolder.itemBinding.tvFilename.setText(xmlName);
                viewHolder.itemBinding.tvLinkedFilename.setVisibility(View.VISIBLE);
                viewHolder.itemBinding.tvLinkedFilename.setText(javaName);
            } else if (selectedTab == TAB_CUSTOM_VIEW) {
                viewHolder.itemBinding.imgEdit.setVisibility(View.GONE);
                viewHolder.itemBinding.tvLinkedFilename.setVisibility(View.GONE);
                ProjectFileBean customView = jC.b(sc_id).c().get(position);
                if (currentXml.equals(customView.getXmlName())) {
                    viewHolder.itemBinding.cardView.setStrokeColor(
                            ThemeUtils.getColor(ViewSelectorActivity.this, R.attr.colorPrimary));
                    viewHolder.itemBinding.cardView.setStrokeWidth(SketchwareUtil.dpToPx(3f));
                } else {
                    viewHolder.itemBinding.cardView.setStrokeWidth(SketchwareUtil.dpToPx(0f));
                }
                if (customView.fileType == ProjectFileBean.PROJECT_FILE_TYPE_DRAWER) {
                    viewHolder.itemBinding.imgView.setImageResource(getViewIcon(4));
                    viewHolder.itemBinding.tvFilename.setText(customView.fileName.substring(1));
                } else {
                    viewHolder.itemBinding.imgView.setImageResource(getViewIcon(3));
                    viewHolder.itemBinding.tvFilename.setText(customView.getXmlName());
                }
            }
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            FileSelectorPopupSelectXmlActivityItemBinding binding =
                    FileSelectorPopupSelectXmlActivityItemBinding.inflate(inflater, parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public int getItemCount() {
            binding.emptyMessage.setVisibility(View.GONE);
            hC hC = jC.b(sc_id);
            ArrayList<ProjectFileBean> list = switch (selectedTab) {
                case TAB_ACTIVITY -> hC.b();
                case TAB_CUSTOM_VIEW -> hC.c();
                default -> null;
            };
            int size = list != null ? list.size() : 0;
            if (size == 0) {
                binding.emptyMessage.setVisibility(View.VISIBLE);
            }
            return size;
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final FileSelectorPopupSelectXmlActivityItemBinding itemBinding;

            public ViewHolder(@NonNull FileSelectorPopupSelectXmlActivityItemBinding binding2) {
                super(binding2.getRoot());
                itemBinding = binding2;
                // Clique normal (selecionar item)
                itemBinding.cardView.setOnClickListener(v -> {
                    if (!mB.a()) {
                        int position = getLayoutPosition();
                        if (position == RecyclerView.NO_POSITION) return;
                        selectedItem = position;
                        hC hC = jC.b(sc_id);
                        ArrayList<ProjectFileBean> list = switch (selectedTab) {
                            case TAB_ACTIVITY -> hC.b();
                            case TAB_CUSTOM_VIEW -> hC.c();
                            default -> null;
                        };
                        if (list != null) {
                            projectFile = list.get(getLayoutPosition());
                        }
                        Intent intent = new Intent();
                        intent.putExtra("project_file", projectFile);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
                itemBinding.cardView.setOnLongClickListener(v -> {
                    if (!mB.a()) {
                        int position = getLayoutPosition();
                        if (position == RecyclerView.NO_POSITION) return true;
                        if (position == 0) {
                            SketchwareUtil.toast("Main activity cannot be removed", Toast.LENGTH_SHORT);
                            return true;
                        }
                        binding.createNewView.setVisibility(View.GONE);
                        binding.removeView.setVisibility(View.VISIBLE);
                        //binding.createNewView.setText(R.string.common_word_remove);
                        //binding.createNewView.setIcon(AppCompatResources.getDrawable(getApplicationContext(),
                        //        R.drawable.icon_delete_active));
                        final int adapterPosition = position;
                        binding.removeView.setOnClickListener(v3 -> {
                            if (!mB.a()) {
                                new com.google.android.material.dialog.MaterialAlertDialogBuilder(v.getContext())
                                        .setTitle(R.string.title_remove_activity)
                                        .setMessage(R.string.msg_remove_item)
                                        .setPositiveButton(R.string.common_word_remove, (dialog, which) -> {
                                            hC fileStorage = jC.b(sc_id);
                                            if (selectedTab == TAB_ACTIVITY) {
                                                if (fileStorage.b() != null && adapterPosition < fileStorage.b().size()) {
                                                    fileStorage.b().remove(adapterPosition);
                                                }
                                            } else if (selectedTab == TAB_CUSTOM_VIEW) {
                                                if (fileStorage.c() != null && adapterPosition < fileStorage.c().size()) {
                                                    fileStorage.c().remove(adapterPosition);
                                                }
                                            }
                                            fileStorage.j();
                                            fileStorage.l();
                                            notifyItemRemoved(adapterPosition);
                                            if (adapterPosition < getItemCount()) {
                                                notifyItemRangeChanged(adapterPosition,
                                                        getItemCount() - adapterPosition);
                                            }
                                            SketchwareUtil.toast(getString(R.string.common_message_complete_delete));
                                            hC updatedFileStorage = jC.b(sc_id);
                                            ArrayList<ProjectFileBean> list = switch (selectedTab) {
                                                case TAB_ACTIVITY -> updatedFileStorage.b();
                                                case TAB_CUSTOM_VIEW -> updatedFileStorage.c();
                                                default -> null;
                                            };
                                            if (list != null && !list.isEmpty()) {
                                                projectFile = list.get(Math.min(adapterPosition, list.size() - 1));
                                            }
                                            Intent intent = new Intent();
                                            intent.putExtra("project_file", projectFile);
                                            setResult(RESULT_OK, intent);
                                            dialog.dismiss();
                                            binding.createNewView.setVisibility(View.VISIBLE);
                                            binding.removeView.setVisibility(View.GONE);
                                            //finish();
                                        })
                                        .setNegativeButton(R.string.common_word_cancel, (dialog, which) -> {
                                            binding.createNewView.setVisibility(View.VISIBLE);
                                            binding.removeView.setVisibility(View.GONE);
                                            // TODO FIX
                                            //binding.createNewView.setText("Create new view");
                                            //binding.createNewView.setIcon(AppCompatResources.getDrawable
                                            // (getApplicationContext(),
                                            //       R.drawable.ic_mtrl_add));
                                            dialog.dismiss();
                                            //finish();
                                        })
                                        .show();
                            }
                        });
                        return true;
                    }
                    return false;
                });
                itemBinding.actionContainer.setOnClickListener(v -> {
                    if (selectedTab == TAB_ACTIVITY && !mB.a()) {
                        selectedItem = getLayoutPosition();
                        Intent intent = new Intent(getApplicationContext(), AddViewActivity.class);
                        intent.putExtra("project_file", jC.b(sc_id).b().get(getLayoutPosition()));
                        intent.putExtra("request_code", 265);
                        startActivityForResult(intent, 265);
                    }
                });
                itemBinding.imgPresetSetting.setOnClickListener(v -> {
                    if (!mB.a()) {
                        selectedItem = getLayoutPosition();
                        int requestCode = a(jC.b(sc_id).b().get(getLayoutPosition()));
                        Intent intent = new Intent(getApplicationContext(), PresetSettingActivity.class);
                        intent.putExtra("request_code", requestCode);
                        intent.putExtra("edit_mode", true);
                        startActivityForResult(intent, requestCode);
                    }
                });
            }
        }
    }
}
