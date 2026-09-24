package com.besome.sketch.editor.logic;

import static com.besome.sketch.editor.logic.PaletteSelector.paletteSelectorRecord;
import static com.google.android.material.color.MaterialColors.harmonizeWithPrimary;
import static com.google.android.material.color.MaterialColors.isColorLight;
import static pro.sketchware.utility.ThemeUtils.getColor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import a.a.a.Vs;
import a.a.a.wB;
import mod.hilal.saif.activities.tools.ConfigActivity;
import pro.sketchware.R;
import pro.sketchware.databinding.PaletteSelectorItemBinding;
import pro.sketchware.databinding.PaletteSelectorItemHorizontalBinding;

public class PaletteSelectorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_VERTICAL = 0;
    private static final int VIEW_TYPE_HORIZONTAL = 1;

    private final PaletteSelector paletteSelector;
    private final List<paletteSelectorRecord> paletteList = new ArrayList<>();
    private final Context context;
    private final Vs onBlockCategorySelectListener;
    private int selectedPosition = -1;

    public PaletteSelectorAdapter(PaletteSelector paletteSelector, Vs onBlockCategorySelectListener) {
        this.paletteSelector = paletteSelector;
        this.context = paletteSelector.getContext();
        this.onBlockCategorySelectListener = onBlockCategorySelectListener;
    }

    public void setPalettes(List<paletteSelectorRecord> list) {
        paletteList.clear();
        for (paletteSelectorRecord palette : list) {
            if (paletteSelector.matchesSearch(palette.text())) {
                paletteList.add(palette);
            }
        }
        if (!paletteList.isEmpty() && selectedPosition < 0) {
            selectedPosition = 0;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_PALETTE_ON_VERTICAL)
                ? VIEW_TYPE_HORIZONTAL : VIEW_TYPE_VERTICAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HORIZONTAL) {
            PaletteSelectorItemHorizontalBinding binding = PaletteSelectorItemHorizontalBinding.inflate(inflater,
                    parent, false);
            return new HorizontalViewHolder(binding);
        } else {
            PaletteSelectorItemBinding binding = PaletteSelectorItemBinding.inflate(inflater, parent, false);
            return new VerticalViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        paletteSelectorRecord item = paletteList.get(position);
        int id = item.index();
        String title = item.text();
        int color = harmonizeWithPrimary(context, item.color());
        if (holder instanceof HorizontalViewHolder) {
            HorizontalViewHolder h = (HorizontalViewHolder) holder;
            h.binding.tvCategory.setText(title);
            h.binding.bg.setBackgroundColor(color);
            h.binding.bg1.setBackgroundColor(color);
            h.binding.bg1.setVisibility(position == selectedPosition ? ViewGroup.GONE : ViewGroup.VISIBLE);

        } else if (holder instanceof VerticalViewHolder) {
            VerticalViewHolder v = (VerticalViewHolder) holder;
            v.binding.tvCategory.setText(title);
            v.binding.bg.setBackgroundColor(color);
            // Ajusta largura do item selecionado no modo vertical
            ViewGroup.LayoutParams params = v.binding.bg.getLayoutParams();
            params.width = position == selectedPosition
                    ? ViewGroup.LayoutParams.MATCH_PARENT
                    : (int) wB.a(context, 4f);
            v.binding.bg.setLayoutParams(params);
            // Cor do texto conforme tema claro/escuro da cor de fundo
            int textColor = isColorLight(color)
                    ? getColor(context, R.attr.colorOnSurface)
                    : getColor(context, R.attr.colorOnSurfaceInverse);
            v.binding.tvCategory.setTextColor(
                    position == selectedPosition ? textColor : getColor(context, R.attr.colorOnSurface));
        }
        holder.itemView.setOnClickListener(v -> {
            int previous = selectedPosition;
            selectedPosition = holder.getAbsoluteAdapterPosition();
            if (previous != selectedPosition) {
                notifyItemChanged(previous);
                notifyItemChanged(selectedPosition);
            }
            if (onBlockCategorySelectListener != null) {
                onBlockCategorySelectListener.a(id, item.color());
            }
        });
    }

    @Override
    public int getItemCount() {
        return paletteList.size();
    }

    public void selectPaletteById(int tag) {
        for (int i = 0; i < paletteList.size(); i++) {
            if (paletteList.get(i).index() == tag) {
                selectedPosition = i;
                notifyDataSetChanged();
                if (onBlockCategorySelectListener != null) {
                    onBlockCategorySelectListener.a(tag, paletteList.get(i).color());
                }
                break;
            }
        }
    }

    public void selectPosition(int pos) {
        if (pos >= 0 && pos < paletteList.size()) {
            int previous = selectedPosition;
            selectedPosition = pos;
            if (previous != pos) {
                notifyItemChanged(previous);
                notifyItemChanged(pos);
            }
            if (onBlockCategorySelectListener != null) {
                paletteSelectorRecord item = paletteList.get(pos);
                onBlockCategorySelectListener.a(item.index(), item.color());
            }
        }
    }

    // ViewHolder para modo horizontal
    static class HorizontalViewHolder extends RecyclerView.ViewHolder {
        final PaletteSelectorItemHorizontalBinding binding;

        HorizontalViewHolder(PaletteSelectorItemHorizontalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    // ViewHolder para modo vertical
    static class VerticalViewHolder extends RecyclerView.ViewHolder {
        final PaletteSelectorItemBinding binding;

        VerticalViewHolder(PaletteSelectorItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
