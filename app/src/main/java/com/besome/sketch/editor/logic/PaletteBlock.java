package com.besome.sketch.editor.logic;

import static pro.sketchware.utility.ThemeUtils.getColor;
import static pro.sketchware.utility.ThemeUtils.isDarkThemeEnabled;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;

import a.a.a.Rs;
import a.a.a.Ts;
import a.a.a.wB;
import mod.hilal.saif.activities.tools.ConfigActivity;
import pro.sketchware.R;
import pro.sketchware.databinding.PaletteBlockBinding;
import pro.sketchware.databinding.PaletteBlockHorizontalBinding;

public class PaletteBlock extends LinearLayout {

    private float f = 0.0F;
    private Context context;

    @Nullable
    private PaletteBlockBinding bindingVertical;
    @Nullable
    private PaletteBlockHorizontalBinding bindingHorizontal;

    private boolean isHorizontalMode;
    private boolean needsReinitialization = false;

    public PaletteBlock(Context context) {
        super(context);
        initialize(context, null);
    }

    public PaletteBlock(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (needsReinitialization) {
            reinitialize();
        }
    }

    private void initialize(Context context, AttributeSet attrs) {
        this.context = context;
        this.isHorizontalMode = ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_PALETTE_ON_VERTICAL);
        this.f = wB.a(context, 1.0F);
        // Limpa bindings antigos para evitar Memory Leaks
        this.bindingHorizontal = null;
        this.bindingVertical = null;
        LayoutInflater inflater = LayoutInflater.from(context);
        if (isHorizontalMode) {
            bindingHorizontal = PaletteBlockHorizontalBinding.inflate(inflater, this, true);
        } else {
            bindingVertical = PaletteBlockBinding.inflate(inflater, this, true);
        }
    }

    private void reinitialize() {
        boolean newIsHorizontalMode = ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_PALETTE_ON_VERTICAL);
        if (newIsHorizontalMode != this.isHorizontalMode) {
            this.isHorizontalMode = newIsHorizontalMode;
            removeAllViews();
            initialize(context, null);
            // O pai (LogicEditor) precisa re-popular esta view após a recriação do layout.
        }
        needsReinitialization = false;
    }

    private LinearLayout getBlockBuilder() {
        return isHorizontalMode && bindingHorizontal != null ? bindingHorizontal.blockBuilder :
                bindingVertical.blockBuilder;
    }

    private LinearLayout getActionsContainer() {
        return isHorizontalMode && bindingHorizontal != null ? bindingHorizontal.actionsContainer :
                bindingVertical.actionsContainer;
    }

    private View getScrollView() {
        return isHorizontalMode && bindingHorizontal != null ? bindingHorizontal.scrollHorizontal :
                bindingVertical.scroll;
    }

    public Ts a(String var1, String var2, String var3) {
        View spacer = new View(context);
        spacer.setLayoutParams(createLayoutParams(8.0F));
        LinearLayout builder = getBlockBuilder();
        builder.addView(spacer);
        Rs blockView = new Rs(context, -1, var1, var2, var3);
        blockView.setContentDescription(generateContentDescription(var3));
        blockView.setBlockType(1);
        builder.addView(blockView);
        return blockView;
    }

    public Ts a(String var1, String var2, String var3, String var4) {
        View spacer = new View(context);
        spacer.setLayoutParams(createLayoutParams(8.0F));
        LinearLayout builder = getBlockBuilder();
        builder.addView(spacer);
        Rs blockView = new Rs(context, -1, var1, var2, var3, var4);
        blockView.setContentDescription(generateContentDescription(var4));
        blockView.setBlockType(1);
        builder.addView(blockView);
        return blockView;
    }

    public TextView a(String title) {
        TextView textView = new TextView(context);
        textView.setText(title);
        textView.setTextSize(10.0F);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setGravity(Gravity.CENTER);
        int padding8 = (int) (f * 8.0F);
        textView.setPadding(padding8, 0, padding8, 0);
        MaterialCardView cardView = new MaterialCardView(context);
        LinearLayout.LayoutParams params = createLayoutParams(30.0F);
        params.setMargins(0, 0, (int) (f * 4), (int) (f * 6));
        cardView.setLayoutParams(params);
        cardView.setCardBackgroundColor(getColor(context,
                isDarkThemeEnabled(context) ? R.attr.colorSurfaceContainerHigh : R.attr.colorSurfaceContainerHighest));
        cardView.addView(textView);
        getActionsContainer().addView(cardView);
        return textView;
    }

    public void a() {
        getBlockBuilder().removeAllViews();
        getActionsContainer().removeAllViews();
    }

    public void a(String title, int color) {
        MaterialCardView cardView = new MaterialCardView(context);
        LinearLayout.LayoutParams params = createLayoutParams(18.0F);
        params.topMargin = (int) (f * 16.0F);
        cardView.setLayoutParams(params);
        cardView.setCardBackgroundColor(color);
        cardView.setRadius(f * 8f);
        TextView textView = new TextView(context);
        textView.setText(title);
        textView.setTextColor(getColor(context,
                isDarkThemeEnabled(context) ? R.attr.colorOnSurface : R.attr.colorOnSurfaceInverse));
        textView.setTextSize(10.0F);
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        int padding12 = (int) (f * 12.0F);
        textView.setPadding(padding12, 0, padding12, 0);
        cardView.addView(textView);
        getBlockBuilder().addView(cardView);
    }

    public void addDeprecatedBlock(String message, String type, String opCode) {
        if (message != null && !message.isEmpty()) {
            a(message, getColor(context,
                    isDarkThemeEnabled(context) ? R.attr.colorSurfaceContainerHigh : R.attr.colorSurfaceInverse));
        }
        Ts blockView = a("", type, opCode);
        blockView.e = 0xFFBDBDBD;
        blockView.setTag(opCode);
    }

    private String generateContentDescription(String name) {
        if (name == null || name.isEmpty()) return "";
        int len = name.length();
        StringBuilder result = new StringBuilder(len + 5);
        result.append(Character.toUpperCase(name.charAt(0)));
        for (int i = 1; i < len; i++) {
            char current = name.charAt(i);
            char previous = name.charAt(i - 1);
            if (Character.isUpperCase(current)) {
                if (Character.isLowerCase(previous) || (i + 1 < len && Character.isLowerCase(name.charAt(i + 1)))) {
                    result.append(' ');
                }
            }
            result.append(current);
        }
        return result.toString();
    }

    // Renomeado para não conflitar com View.getLayoutParams() nativo
    private LinearLayout.LayoutParams createLayoutParams(float heightMultiplier) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (f * heightMultiplier)
        );
    }

    public void setDragEnabled(boolean dragEnabled) {
        if (isHorizontalMode && bindingHorizontal != null) {
            if (dragEnabled) {
                bindingHorizontal.scroll.b();
                bindingHorizontal.scrollHorizontal.b();
            } else {
                bindingHorizontal.scroll.a();
                bindingHorizontal.scrollHorizontal.a();
            }
        } else if (!isHorizontalMode && bindingVertical != null) {
            if (dragEnabled) {
                bindingVertical.scroll.b();
                bindingVertical.scrollHorizontal.b();
            } else {
                bindingVertical.scroll.a();
                bindingVertical.scrollHorizontal.a();
            }
        }
    }

    public void setMinWidth(int minWidth) {
        int adjustedWidth = minWidth - (int) (f * 5.0F);
        if (isHorizontalMode && bindingHorizontal != null) {
            bindingHorizontal.scroll.setMinimumWidth(adjustedWidth);
            bindingHorizontal.scrollHorizontal.setMinimumWidth(adjustedWidth);
        } else if (!isHorizontalMode && bindingVertical != null) {
            bindingVertical.scroll.setMinimumWidth(adjustedWidth);
            bindingVertical.scrollHorizontal.setMinimumWidth(adjustedWidth);
        }
        // Forma correta e segura de aplicar a largura para forçar renderização
        ViewGroup.LayoutParams layoutParams = super.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.width = minWidth;
            super.setLayoutParams(layoutParams);
        }
    }

    public void setUseScroll(boolean useScroll) {
        if (isHorizontalMode && bindingHorizontal != null) {
            bindingHorizontal.scroll.setUseScroll(useScroll);
            bindingHorizontal.scrollHorizontal.setUseScroll(useScroll);
        } else if (!isHorizontalMode && bindingVertical != null) {
            bindingVertical.scroll.setUseScroll(useScroll);
            bindingVertical.scrollHorizontal.setUseScroll(useScroll);
        }
    }
}
