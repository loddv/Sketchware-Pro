package com.besome.sketch.editor.logic;

import static pro.sketchware.utility.ThemeUtils.getColor;
import static pro.sketchware.utility.ThemeUtils.isDarkThemeEnabled;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

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
    private boolean isPaletteHidden = false;

    @Nullable
    private OnPaletteModeChangedListener modeChangeListener;
    
    @Nullable
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    public interface OnPaletteModeChangedListener {
        void onPaletteModeChanged(boolean isHorizontalMode);
    }

    public PaletteBlock(Context context) {
        super(context);
        initialize(context, null);
    }

    public PaletteBlock(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public void setOnPaletteModeChangedListener(@Nullable OnPaletteModeChangedListener listener) {
        this.modeChangeListener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startMonitoringSettings();
        if (needsReinitialization) {
            reinitialize();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopMonitoringSettings();
    }

    /**
     * Detecta mudanças de visibilidade da janela (Activity minimizada, oculta ou fechada)
     */
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.GONE || visibility == View.INVISIBLE) {
            handlePaletteHidden();
        } else if (visibility == View.VISIBLE) {
            handlePaletteVisible();
        }
    }

    /**
     * Detecta mudanças de visibilidade da própria View
     */
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (changedView == this) {
            if (visibility == View.GONE || visibility == View.INVISIBLE) {
                handlePaletteHidden();
            } else if (visibility == View.VISIBLE) {
                handlePaletteVisible();
            }
        }
    }

    /**
     * Chamado manualmente quando o painel/bottom sheet é deslizado para baixo
     */
    public void onPanelSwipedDown() {
        setPaletteHidden(true);
    }

    /**
     * Chamado manualmente quando o painel/bottom sheet é expandido novamente
     */
    public void onPanelExpanded() {
        setPaletteHidden(false);
    }

    /**
     * Define o estado de ocultação da paleta e atualiza os bindings/scrolls
     */
    public void setPaletteHidden(boolean hidden) {
        if (this.isPaletteHidden == hidden) return;
        
        this.isPaletteHidden = hidden;
        if (hidden) {
            handlePaletteHidden();
        } else {
            handlePaletteVisible();
        }
    }

    /**
     * Trata o desligamento temporário de interações/scroll quando a paleta está oculta
     */
    private void handlePaletteHidden() {
        setDragEnabled(false);
        setUseScroll(false);
    }

    /**
     * Trata a reativação da paleta e aplica re-inicializações pendentes
     */
    private void handlePaletteVisible() {
        setDragEnabled(true);
        setUseScroll(true);
        if (needsReinitialization) {
            reinitialize();
        }
    }

    private void initialize(Context context, AttributeSet attrs) {
        this.context = context;
        this.isHorizontalMode = ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_PALETTE_ON_VERTICAL);
        f = wB.a(context, 1.0F);
        LayoutInflater inflater = LayoutInflater.from(context);
        if (isHorizontalMode) {
            bindingVertical = null;
            bindingHorizontal = PaletteBlockHorizontalBinding.inflate(inflater, this, true);
        } else {
            bindingHorizontal = null;
            bindingVertical = PaletteBlockBinding.inflate(inflater, this, true);
        }
    }

    public void reinitialize() {
        boolean newIsHorizontalMode = ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_PALETTE_ON_VERTICAL);
        if (newIsHorizontalMode != this.isHorizontalMode || getChildCount() == 0) {
            this.isHorizontalMode = newIsHorizontalMode;
            removeAllViews();
            initialize(context, null);
            notifyModeChanged();
        }
        needsReinitialization = false;
    }

    private void notifyModeChanged() {
        if (modeChangeListener != null) {
            modeChangeListener.onPaletteModeChanged(isHorizontalMode);
        }
    }

    private void startMonitoringSettings() {
        if (preferenceChangeListener == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            preferenceChangeListener = (pref, key) -> {
                if (ConfigActivity.SETTING_PALETTE_ON_VERTICAL.equals(key)) {
                    if (isAttachedToWindow() && getVisibility() == View.VISIBLE && !isPaletteHidden) {
                        reinitialize();
                    } else {
                        needsReinitialization = true;
                    }
                }
            };
            prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
    }

    private void stopMonitoringSettings() {
        if (preferenceChangeListener != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
            preferenceChangeListener = null;
        }
    }

    private LinearLayout getBlockBuilder() {
        if (isHorizontalMode) {
            return bindingHorizontal != null ? bindingHorizontal.blockBuilder : null;
        } else {
            return bindingVertical != null ? bindingVertical.blockBuilder : null;
        }
    }

    private LinearLayout getActionsContainer() {
        if (isHorizontalMode) {
            return bindingHorizontal != null ? bindingHorizontal.actionsContainer : null;
        } else {
            return bindingVertical != null ? bindingVertical.actionsContainer : null;
        }
    }

    public Ts a(String var1, String var2, String var3) {
        LinearLayout builder = getBlockBuilder();
        if (builder == null) return null;

        View spacer = new View(context);
        spacer.setLayoutParams(getLayoutParams(8.0F));
        builder.addView(spacer);
        Rs blockView = new Rs(context, -1, var1, var2, var3);
        blockView.setContentDescription(generateContentDescription(var3));
        blockView.setBlockType(1);
        builder.addView(blockView);
        return blockView;
    }

    public Ts a(String var1, String var2, String var3, String var4) {
        LinearLayout builder = getBlockBuilder();
        if (builder == null) return null;

        View spacer = new View(context);
        spacer.setLayoutParams(getLayoutParams(8.0F));
        builder.addView(spacer);
        Rs blockView = new Rs(context, -1, var1, var2, var3, var4);
        blockView.setContentDescription(generateContentDescription(var4));
        blockView.setBlockType(1);
        builder.addView(blockView);
        return blockView;
    }

    public TextView a(String title) {
        LinearLayout actions = getActionsContainer();
        if (actions == null) return null;

        TextView textView = new TextView(context);
        textView.setText(title);
        textView.setTextSize(10.0F);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding((int) (f * 8.0F), 0, (int) (f * 8.0F), 0);
        MaterialCardView cardView = new MaterialCardView(context);
        LinearLayout.LayoutParams params = getLayoutParams(30.0F);
        params.setMargins(0, 0, (int) (f * 4), (int) (f * 6));
        cardView.setLayoutParams(params);
        cardView.setCardBackgroundColor(getColor(context,
                isDarkThemeEnabled(context) ? R.attr.colorSurfaceContainerHigh : R.attr.colorSurfaceContainerHighest));
        cardView.addView(textView);
        actions.addView(cardView);
        return textView;
    }

    public void a() {
        LinearLayout builder = getBlockBuilder();
        LinearLayout actions = getActionsContainer();
        if (builder != null) builder.removeAllViews();
        if (actions != null) actions.removeAllViews();
    }

    public void a(String title, int color) {
        LinearLayout builder = getBlockBuilder();
        if (builder == null) return;

        MaterialCardView cardView = new MaterialCardView(context);
        LinearLayout.LayoutParams params = getLayoutParams(18.0F);
        params.topMargin = (int) (f * 16.0F);
        cardView.setLayoutParams(params);
        cardView.setCardBackgroundColor(color);
        cardView.setRadius(f * 8f);
        TextView textView = new TextView(context);
        textView.setText(title);
        textView.setTextColor(getColor(context,
                isDarkThemeEnabled(context) ? R.attr.colorOnSurface : R.attr.colorOnSurfaceInverse));
        textView.setTextSize(10.0F);
        textView.setGravity(Gravity.CENTER | Gravity.LEFT);
        textView.setPadding((int) (f * 12.0F), 0, (int) (f * 12.0F), 0);
        cardView.addView(textView);
        builder.addView(cardView);
    }

    public void addDeprecatedBlock(String message, String type, String opCode) {
        if (message != null && !message.isEmpty()) {
            a(message, getColor(context,
                    isDarkThemeEnabled(context) ? R.attr.colorSurfaceContainerHigh : R.attr.colorSurfaceInverse));
        }
        Ts blockView = a("", type, opCode);
        if (blockView != null) {
            blockView.e = 0xFFBDBDBD;
            blockView.setTag(opCode);
        }
    }

    private String generateContentDescription(String name) {
        if (name == null || name.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        result.append(Character.toUpperCase(name.charAt(0)));
        for (int i = 1; i < name.length(); i++) {
            char current = name.charAt(i);
            char previous = name.charAt(i - 1);
            if (Character.isUpperCase(current)) {
                if (Character.isLowerCase(previous) || (i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1)))) {
                    result.append(' ');
                }
            }
            result.append(current);
        }
        return result.toString();
    }

    private LinearLayout.LayoutParams getLayoutParams(float heightMultiplier) {
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
        if (isHorizontalMode && bindingHorizontal != null) {
            bindingHorizontal.scroll.setMinimumWidth(minWidth - (int) (f * 5.0F));
            bindingHorizontal.scrollHorizontal.setMinimumWidth(minWidth - (int) (f * 5.0F));
            getLayoutParams().width = minWidth;
        } else if (!isHorizontalMode && bindingVertical != null) {
            bindingVertical.scroll.setMinimumWidth(minWidth - (int) (f * 5.0F));
            bindingVertical.scrollHorizontal.setMinimumWidth(minWidth - (int) (f * 5.0F));
            getLayoutParams().width = minWidth;
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
