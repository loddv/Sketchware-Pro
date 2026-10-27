package dev.aldi.sayuti.block;

import static pro.sketchware.utility.ThemeUtils.getColor;
import static pro.sketchware.utility.ThemeUtils.isDarkThemeEnabled;

import android.util.Pair;

import androidx.annotation.ColorInt;

import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.LogicEditorActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import a.a.a.Ox;
import a.a.a.jC;
import a.a.a.jq;
import a.a.a.kq;
import mod.agus.jcoderz.beans.ViewBeans;
import mod.hey.studios.editor.view.IdGenerator;
import mod.hey.studios.moreblock.ReturnMoreblockManager;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.activities.tools.ConfigActivity;
import mod.hilal.saif.blocks.BlocksHandler;
import mod.pranav.viewbinding.ViewBindingBuilder;
import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.components.utils.StringsEditorManager;
import pro.sketchware.blocks.ExtraBlocks;
import pro.sketchware.control.logic.LogicClickListener;
import pro.sketchware.utility.CustomVariableUtil;
import pro.sketchware.utility.FileResConfig;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class ExtraPaletteBlock {

    public enum BlockType {
        VOID(" "),
        BOOLEAN("b"),
        DOUBLE("d"),
        STRING("s"),
        MAP("a"),
        VIEW("v"),
        COMPONENT("p"),
        CLICK("c"),
        FRAGMENT("f"),
        LIST("l");

        private final String code;

        BlockType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    private final String eventName;
    private final String javaName;
    private final String xmlName;
    private final String sc_id;

    private final LogicClickListener clickListener;
    private final ExtraBlocks extraBlocks;
    private final FileResConfig frc;
    private final HashMap<String, Object> mapSave = new HashMap<>();
    private final ProjectFileBean projectFile;
    private final Boolean isViewBindingEnabled;
    public LogicEditorActivity logicEditor;

    public ExtraPaletteBlock(LogicEditorActivity logicEditorActivity, Boolean isViewBindingEnabled) {
        logicEditor = logicEditorActivity;
        eventName = logicEditorActivity.eventName;
        projectFile = logicEditor.projectFileBean;
        javaName = projectFile.getJavaName();
        xmlName = projectFile.getXmlName();
        sc_id = logicEditor.scId;
        this.isViewBindingEnabled = isViewBindingEnabled;
        frc = new FileResConfig(sc_id);
        extraBlocks = new ExtraBlocks(logicEditor);
        clickListener = new LogicClickListener(logicEditor);
    }

    private boolean isWidgetUsed(String str) {
        if (ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_SHOW_EVERY_SINGLE_BLOCK)) {
            return true;
        }
        if (mapSave.containsKey(str)) {
            Object strValueInMapSave = mapSave.get(str);
            if (strValueInMapSave instanceof Boolean) {
                return (boolean) strValueInMapSave;
            } else {
                return false;
            }
        }
        if (eventName.equals("onBindCustomView")) {
            var eC = jC.a(sc_id);
            var view = eC.c(xmlName, logicEditor.id);
            if (view == null) {
                // in case the View's in a Drawer
                view = eC.c("_drawer_" + xmlName, logicEditor.id);
            }
            String customView = view.customView;
            if (customView != null && !customView.isEmpty()) {
                for (ViewBean viewBean : jC.a(sc_id).d(ProjectFileBean.getXmlName(customView))) {
                    if (viewBean.getClassInfo().a(str)) {
                        mapSave.put(str, true);
                        return true;
                    }
                }
            }
        } else if (jC.a(sc_id).y(xmlName, str)) {
            mapSave.put(str, true);
            return true;
        }
        mapSave.put(str, false);
        return false;
    }

    /*
     * ExtraPaletteBlock#f(Ss) moved to mod.w3wide.menu.ExtraMenuBean#defineMenuSelector(Ss)
     * for better block menu selections and to add new stuff easily.
     */

    public boolean e(String str, String str2) {
        return switch (str) {
            case "circleimageview" -> jC.a(sc_id).g(xmlName, ViewBeans.VIEW_TYPE_WIDGET_CIRCLEIMAGEVIEW, str2);
            case "asynctask" -> jC.a(sc_id).d(javaName, 36, str2);
            case "otpview" -> jC.a(sc_id).g(xmlName, ViewBeans.VIEW_TYPE_WIDGET_OTPVIEW, str2);
            case "lottie" -> jC.a(sc_id).g(xmlName, ViewBeans.VIEW_TYPE_WIDGET_LOTTIEANIMATIONVIEW, str2);
            case "phoneauth" -> jC.a(sc_id).d(javaName, ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_PHONE, str2);
            case "codeview" -> jC.a(sc_id).g(xmlName, ViewBeans.VIEW_TYPE_WIDGET_CODEVIEW, str2);
            case "recyclerview" -> jC.a(sc_id).g(xmlName, ViewBeans.VIEW_TYPE_WIDGET_RECYCLERVIEW, str2);
            case "googlelogin" ->
                    jC.a(sc_id).d(javaName, ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_GOOGLE_LOGIN, str2);
            case "youtubeview" -> jC.a(sc_id).g(xmlName, ViewBeans.VIEW_TYPE_WIDGET_YOUTUBEPLAYERVIEW, str2);
            case "signinbutton" -> jC.a(sc_id).g(xmlName, ViewBeans.VIEW_TYPE_WIDGET_SIGNINBUTTON, str2);
            case "cardview" -> jC.a(sc_id).g(xmlName, ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW, str2);
            case "radiogroup" -> jC.a(sc_id).g(xmlName, ViewBeans.VIEW_TYPE_LAYOUT_RADIOGROUP, str2);
            case "textinputlayout" -> jC.a(sc_id).g(xmlName, ViewBeans.VIEW_TYPE_LAYOUT_TEXTINPUTLAYOUT, str2);
            case "collapsingtoolbar" ->
                    jC.a(sc_id).g(xmlName, ViewBeans.VIEW_TYPE_LAYOUT_COLLAPSINGTOOLBARLAYOUT, str2);
            case "cloudmessage" -> jC.a(sc_id).d(javaName, ComponentBean.COMPONENT_TYPE_FIREBASE_CLOUD_MESSAGE, str2);
            case "datepicker" -> jC.a(sc_id).g(xmlName, ViewBeans.VIEW_TYPE_WIDGET_DATEPICKER, str2);
            case "customVar" -> jC.a(sc_id).f(xmlName, 5, str2);
            case "timepicker" -> jC.a(sc_id).g(xmlName, ViewBeans.VIEW_TYPE_WIDGET_TIMEPICKER, str2);
            case "swiperefreshlayout" -> jC.a(sc_id).g(xmlName, ViewBeans.VIEW_TYPE_LAYOUT_SWIPEREFRESHLAYOUT, str2);
            default -> true;
        };
    }

    /**
     * @see ReturnMoreblockManager#listMoreblocks(Iterator, LogicEditorActivity)
     */
    private void moreBlocks() {
        ReturnMoreblockManager.listMoreblocks(jC.a(sc_id).i(javaName).iterator(), logicEditor);
    }

    private void variables() {
        boolean customVariableHeaderShown = false;
        ArrayList<String> booleanVariables = jC.a(sc_id).e(javaName, 0);
        for (int i = 0; i < booleanVariables.size(); i++) {
            if (i == 0) logicEditor.a("Boolean", getTitleBgColor());
            logicEditor.a(booleanVariables.get(i), BlockType.BOOLEAN.getCode(), "getVar").setTag(booleanVariables.get(i));
        }
        ArrayList<String> numberVariables = jC.a(sc_id).e(javaName, 1);
        for (int i = 0; i < numberVariables.size(); i++) {
            if (i == 0) logicEditor.a("Number", getTitleBgColor());
            logicEditor.a(numberVariables.get(i), BlockType.DOUBLE.getCode(), "getVar").setTag(numberVariables.get(i));
        }
        ArrayList<String> stringVariables = jC.a(sc_id).e(javaName, 2);
        for (int i = 0; i < stringVariables.size(); i++) {
            if (i == 0) logicEditor.a("String", getTitleBgColor());
            logicEditor.a(stringVariables.get(i), BlockType.STRING.getCode(), "getVar").setTag(stringVariables.get(i));
        }
        ArrayList<String> mapVariables = jC.a(sc_id).e(javaName, 3);
        for (int i = 0; i < mapVariables.size(); i++) {
            if (i == 0) logicEditor.a("Map", getTitleBgColor());
            logicEditor.a(mapVariables.get(i), BlockType.MAP.getCode(), "getVar").setTag(mapVariables.get(i));
        }
        ArrayList<String> customVariables = jC.a(sc_id).e(javaName, 5);
        for (int i = 0; i < customVariables.size(); i++) {
            if (i == 0) {
                logicEditor.a("Custom Variable", getTitleBgColor());
                customVariableHeaderShown = true;
            }
            String[] split = customVariables.get(i).split(BlockType.VOID.getCode());
            if (split.length > 1) {
                logicEditor.a(split[1], BlockType.VIEW.getCode(), split[0], "getVar").setTag(customVariables.get(i));
            } else {
                SketchwareUtil.toastError("Found invalid data of Custom Variable #" + (i + 1) + ": \"" + customVariables.get(i) + "\"");
            }
        }
        ArrayList<String> customVariables2 = jC.a(sc_id).e(javaName, 6);
        for (int i = 0; i < customVariables2.size(); i++) {
            if (i == 0 && !customVariableHeaderShown) {
                logicEditor.a("Custom Variable", getTitleBgColor());
                customVariableHeaderShown = true;
            }
            String variable = customVariables2.get(i);
            String variableType = CustomVariableUtil.getVariableType(variable);
            String variableName = CustomVariableUtil.getVariableName(variable);
            if (variableType != null && variableName != null) {
                String type = switch (variableType) {
                    case "boolean", "Boolean" -> "b";
                    case "String" -> "s";
                    case "double", "Double", "int", "Integer", "float", "Float", "long", "Long",
                         "short", "Short" -> "d";
                    default -> "v";
                };
                logicEditor.a(variableName, type, variableType, "getVar").setTag(variable);
            } else {
                logicEditor.a("Invalid: " + variable, getColor(logicEditor, R.attr.colorError));
            }
        }
        BlocksHandler.primaryBlocksA(
                logicEditor,
                extraBlocks.isVariableUsed(0),
                extraBlocks.isVariableUsed(1),
                extraBlocks.isVariableUsed(2),
                extraBlocks.isVariableUsed(3)
        );
        blockCustomViews();
        blockDrawer();
        blockEvents();
        extraBlocks.eventBlocks();
        blockComponents();
    }

    private void blockComponents() {
        ArrayList<ComponentBean> components = jC.a(sc_id).e(javaName);
        for (int i = 0, componentsSize = components.size(); i < componentsSize; i++) {
            ComponentBean component = components.get(i);
            if (i == 0) {
                logicEditor.a("Components", getTitleBgColor());
            }
            if (component.type != 27) {
                logicEditor.a(component.componentId, BlockType.COMPONENT.getCode(),
                        ComponentBean.getComponentTypeName(component.type), "getVar"
                ).setTag(component.componentId);
            }
        }
    }

    private void blockCustomViews() {
        if (eventName.equals("onBindCustomView")) {
            String viewId = logicEditor.id;
            var eC = jC.a(sc_id);
            ViewBean viewBean = eC.c(xmlName, viewId);
            if (viewBean == null) {
                // Event is of a Drawer View
                viewBean = eC.c("_drawer_" + xmlName, viewId);
            }
            String viewBeanCustomView = viewBean.customView;
            if (viewBeanCustomView != null && !viewBeanCustomView.isEmpty()) {
                ArrayList<ViewBean> customViews = jC.a(sc_id).d(ProjectFileBean.getXmlName(viewBeanCustomView));
                for (int i = 0, customViewsSize = customViews.size(); i < customViewsSize; i++) {
                    ViewBean customView = customViews.get(i);
                    if (i == 0) {
                        logicEditor.a("Custom Views", getTitleBgColor());
                    }
                    if (!customView.convert.equals("include")) {
                        String typeName = customView.convert.isEmpty() ? ViewBean.getViewTypeName(customView.type) :
                                IdGenerator.getLastPath(customView.convert);
                        String resultId = isViewBindingEnabled ?
                                "binding." + ViewBindingBuilder.generateParameterFromId(customView.id) : customView.id;
                        logicEditor.a(resultId, BlockType.VIEW.getCode(), typeName, "getVar").setTag(resultId);
                    }
                }
            }
            logicEditor.a(BlockType.VOID.getCode(), "notifyDataSetChanged");
            logicEditor.a(BlockType.CLICK.getCode(), "viewOnClick");
            logicEditor.a(BlockType.CLICK.getCode(), "viewOnLongClick");
            logicEditor.a(BlockType.CLICK.getCode(), "checkboxOnChecked");
            logicEditor.a(BlockType.BOOLEAN.getCode(), "checkboxIsChecked");
            return;
        }
        ArrayList<ViewBean> views = jC.a(sc_id).d(xmlName);
        for (int i = 0, viewsSize = views.size(); i < viewsSize; i++) {
            ViewBean view = views.get(i);
            Set<String> toNotAdd = new Ox(new jq(), projectFile).readAttributesToReplace(view);
            if (i == 0) {
                logicEditor.a("Views", getTitleBgColor());
            }
            if (!view.convert.equals("include")) {
                if (!toNotAdd.contains("android:id")) {
                    String typeName = view.convert.isEmpty() ? ViewBean.getViewTypeName(view.type) :
                            IdGenerator.getLastPath(view.convert);
                    logicEditor.a(isViewBindingEnabled ?
                                    "binding." + ViewBindingBuilder.generateParameterFromId(view.id) : view.id,
                            BlockType.VIEW.getCode(),
                            typeName
                            , "getVar").setTag(isViewBindingEnabled ?
                            "binding." + ViewBindingBuilder.generateParameterFromId(view.id) : view.id);
                }
            }
        }
    }

    private void blockDrawer() {
        if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)) {
            ArrayList<ViewBean> drawerViews = jC.a(sc_id).d(projectFile.getDrawerXmlName());
            if (drawerViews != null) {
                for (int i = 0, drawerViewsSize = drawerViews.size(); i < drawerViewsSize; i++) {
                    ViewBean drawerView = drawerViews.get(i);
                    if (i == 0) {
                        logicEditor.a("Drawer Views", getTitleBgColor());
                    }
                    if (!drawerView.convert.equals("include")) {
                        String id = "_drawer_" + drawerView.id;
                        String typeName = drawerView.convert.isEmpty() ? ViewBean.getViewTypeName(drawerView.type) :
                                IdGenerator.getLastPath(drawerView.convert);
                        logicEditor.a(isViewBindingEnabled ?
                                        "binding.drawer." + ViewBindingBuilder.generateParameterFromId(drawerView.id) : id,
                                BlockType.VIEW.getCode(), typeName, "getVar").setTag(id);
                    }
                }
            }
        }
    }

    private void blockEvents() {
        switch (eventName) {
            case "onTabAdded", "onTabLayoutNewTabAdded" -> {
                logicEditor.a("Fragment & TabLayout", getTitleBgColor());
                logicEditor.a(BlockType.FRAGMENT.getCode(), "returnTitle");
            }
            case "onFragmentAdded" -> {
                logicEditor.a("Fragment & TabLayout", getTitleBgColor());
                logicEditor.a(BlockType.FRAGMENT.getCode(), "returnFragment");
            }
            case "onScrollChanged" -> {
                logicEditor.a("ListView", getTitleBgColor());
                logicEditor.a(BlockType.DOUBLE.getCode(), "listscrollparam");
                logicEditor.a(BlockType.DOUBLE.getCode(), "getLastVisiblePosition");
            }
            case "onScrollChanged2" -> {
                logicEditor.a("RecyclerView", getTitleBgColor());
                logicEditor.a(BlockType.DOUBLE.getCode(), "recyclerscrollparam");
            }
            case "onPageChanged" -> {
                logicEditor.a("ViewPager", getTitleBgColor());
                logicEditor.a(BlockType.DOUBLE.getCode(), "pagerscrollparam");
            }
            case "onCreateOptionsMenu" -> {
                logicEditor.a("Menu", getTitleBgColor());
                logicEditor.a(BlockType.VOID.getCode(), "menuInflater");
                logicEditor.a(BlockType.VOID.getCode(), "menuAddItem");
                logicEditor.a(BlockType.VOID.getCode(), "menuAddMenuItem");
                logicEditor.a(BlockType.CLICK.getCode(), "menuAddSubmenu");
                logicEditor.a(BlockType.VOID.getCode(), "submenuAddItem");
            }
            default -> {
            }
        }
    }

    private void list() {
        for (Pair<Integer, String> list : jC.a(sc_id).j(javaName)) {
            int type = list.first;
            String name = list.second;
            switch (type) {
                case 1, 2, 3 -> logicEditor.a(name, BlockType.LIST.getCode(), kq.a(type), "getVar").setTag(name);
                default -> {
                    String variableName = CustomVariableUtil.getVariableName(name);
                    if (variableName != null) {
                        logicEditor.a(variableName, BlockType.LIST.getCode(), "List", "getVar").setTag(name);
                    } else {
                        logicEditor.a("Invalid: " + name, getColor(logicEditor, R.attr.colorError));
                    }
                }
            }
        }
        BlocksHandler.primaryBlocksB(
                logicEditor,
                extraBlocks.isListUsed(1),
                extraBlocks.isListUsed(2),
                extraBlocks.isListUsed(3)
        );
    }

    public void setBlock(int paletteId, int paletteColor) {
        // Remove previous palette's blocks
        logicEditor.paletteBlock.a();
        if (eventName.equals("Import")) {
            if (paletteId == 3) {
                logicEditor.a(BlockType.VOID.getCode(), "addSourceDirectly");
            } else {
                logicEditor.a("Enter the path without import & semicolon", getTitleBgColor());
                logicEditor.a(BlockType.VOID.getCode(), "customImport");
                logicEditor.a(BlockType.VOID.getCode(), "customImport2");
            }
            return;
        }
        switch (paletteId) {
            case -1:
                String filePath = FileUtil.getExternalStorageDir().concat("/.sketchware/data/").concat(sc_id.concat(
                        "/files/resource/values/strings.xml"));
                ArrayList<HashMap<String, Object>> StringsListMap = new ArrayList<>();
                StringsEditorManager stringsEditorManager = new StringsEditorManager();
                stringsEditorManager.convertXmlStringsToListMap(FileUtil.readFileIfExist(filePath), StringsListMap);
                logicEditor.b("Open Resources editor", "openResourcesEditor");
                logicEditor.a(BlockType.STRING.getCode(), "getResString");
                logicEditor.a("Saved Res Strings :", getTitleBgColor());
                if (!stringsEditorManager.isXmlStringsExist(StringsListMap, "app_name")) {
                    logicEditor.a("app_name", BlockType.STRING.getCode(), "getResStr").setTag("S98ZCSapp_name");
                }
                for (int i = 0; i < StringsListMap.size(); i++) {
                    String key = Objects.requireNonNull(StringsListMap.get(i).get("key")).toString();
                    logicEditor.a(key, BlockType.STRING.getCode(), "getResStr").setTag("S98ZCS" + key);
                }
                return;
            case 0:
                logicEditor.b(Helper.getResString(R.string.logic_editor_panel_button_add_variable), "variableAdd");
                logicEditor.b(Helper.getResString(R.string.add_custom_variable), "variableAddNew", clickListener);
                // Check if any variables (boolean=0, number=1, string=2, map=3, custom=5/6) exist and are used
                if (areAnyVariablesUsed()) {
                    logicEditor.b(Helper.getResString(R.string.logic_editor_panel_button_remove_variable),
                            "variableRemove", clickListener);
                    logicEditor.b(Helper.getResString(R.string.edit_variable), "variableEdit", clickListener);
                }
                variables();
                return;
            case 1:
                logicEditor.b(Helper.getResString(R.string.logic_editor_panel_button_add_list), "listAdd");
                logicEditor.b(Helper.getResString(R.string.add_custom_list), "listAddCustom", clickListener);
                logicEditor.b(Helper.getResString(R.string.logic_editor_panel_button_remove_list), "listRemove",
                        clickListener);
                list();
                return;
            case 2:
                BlocksHandler.primaryBlocksC(logicEditor);
                return;
            case 3:
                BlocksHandler.primaryBlocksD(logicEditor);
                return;
            case 4:
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathGetDip");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathGetDisplayWidth");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathGetDisplayHeight");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathPi");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathE");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathPow");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathMin");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathMax");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathSqrt");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathAbs");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathRound");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathCeil");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathFloor");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathSin");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathCos");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathTan");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathAsin");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathAcos");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathAtan");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathExp");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathLog");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathLog10");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathToRadian");
                logicEditor.a(BlockType.DOUBLE.getCode(), "mathToDegree");
                return;
            case 5:
                extraBlocks.fileBlocks();
                logicEditor.a("FileUtil Blocks", getTitleBgColor());
                if (!frc.getAssetsFile().isEmpty()) {
                    logicEditor.a(BlockType.VOID.getCode(), "getAssetFile");
                    logicEditor.a(BlockType.STRING.getCode(), "copyAssetFile");
                }
                logicEditor.a(BlockType.STRING.getCode(), "fileutilread");
                logicEditor.a(BlockType.VOID.getCode(), "fileutilwrite");
                logicEditor.a(BlockType.VOID.getCode(), "fileutilcopy");
                logicEditor.a(BlockType.VOID.getCode(), "fileutilcopydir");
                logicEditor.a(BlockType.VOID.getCode(), "fileutilmove");
                logicEditor.a(BlockType.VOID.getCode(), "fileutildelete");
                logicEditor.a(BlockType.VOID.getCode(), "renameFile");
                logicEditor.a(BlockType.BOOLEAN.getCode(), "fileutilisexist");
                logicEditor.a(BlockType.VOID.getCode(), "fileutilmakedir");
                logicEditor.a(BlockType.VOID.getCode(), "fileutillistdir");
                logicEditor.a(BlockType.BOOLEAN.getCode(), "fileutilisdir");
                logicEditor.a(BlockType.BOOLEAN.getCode(), "fileutilisfile");
                logicEditor.a(BlockType.DOUBLE.getCode(), "fileutillength");
                logicEditor.a(BlockType.BOOLEAN.getCode(), "fileutilStartsWith");
                logicEditor.a(BlockType.BOOLEAN.getCode(), "fileutilEndsWith");
                logicEditor.a(BlockType.STRING.getCode(), "fileutilGetLastSegmentPath");
                logicEditor.a(BlockType.STRING.getCode(), "getExternalStorageDir");
                logicEditor.a(BlockType.STRING.getCode(), "getPackageDataDir");
                logicEditor.a(BlockType.STRING.getCode(), "getPublicDir");
                logicEditor.a(BlockType.VOID.getCode(), "resizeBitmapFileRetainRatio");
                logicEditor.a(BlockType.VOID.getCode(), "resizeBitmapFileToSquare");
                logicEditor.a(BlockType.VOID.getCode(), "resizeBitmapFileToCircle");
                logicEditor.a(BlockType.VOID.getCode(), "resizeBitmapFileWithRoundedBorder");
                logicEditor.a(BlockType.VOID.getCode(), "cropBitmapFileFromCenter");
                logicEditor.a(BlockType.VOID.getCode(), "rotateBitmapFile");
                logicEditor.a(BlockType.VOID.getCode(), "scaleBitmapFile");
                logicEditor.a(BlockType.VOID.getCode(), "skewBitmapFile");
                logicEditor.a(BlockType.VOID.getCode(), "setBitmapFileColorFilter");
                logicEditor.a(BlockType.VOID.getCode(), "setBitmapFileBrightness");
                logicEditor.a(BlockType.VOID.getCode(), "setBitmapFileContrast");
                logicEditor.a(BlockType.DOUBLE.getCode(), "getJpegRotate");
                return;
            case 6:
                logicEditor.a(BlockType.VOID.getCode(), "setEnable");
                logicEditor.a(BlockType.BOOLEAN.getCode(), "getEnable");
                logicEditor.a(BlockType.VOID.getCode(), "setVisible");
                logicEditor.a(BlockType.BOOLEAN.getCode(), "checkViewVisibility");
                logicEditor.a(BlockType.VOID.getCode(), "setElevation");
                logicEditor.a(BlockType.VOID.getCode(), "setRotate");
                logicEditor.a(BlockType.DOUBLE.getCode(), "getRotate");
                logicEditor.a(BlockType.VOID.getCode(), "setAlpha");
                logicEditor.a(BlockType.DOUBLE.getCode(), "getAlpha");
                logicEditor.a(BlockType.VOID.getCode(), "setTranslationX");
                logicEditor.a(BlockType.DOUBLE.getCode(), "getTranslationX");
                logicEditor.a(BlockType.VOID.getCode(), "setTranslationY");
                logicEditor.a(BlockType.DOUBLE.getCode(), "getTranslationY");
                logicEditor.a(BlockType.VOID.getCode(), "setScaleX");
                logicEditor.a(BlockType.DOUBLE.getCode(), "getScaleX");
                logicEditor.a(BlockType.VOID.getCode(), "setScaleY");
                logicEditor.a(BlockType.DOUBLE.getCode(), "getScaleY");
                logicEditor.a(BlockType.DOUBLE.getCode(), "getLocationX");
                logicEditor.a(BlockType.DOUBLE.getCode(), "getLocationY");
                logicEditor.a(BlockType.DOUBLE.getCode(), "getHeight");
                logicEditor.a(BlockType.DOUBLE.getCode(), "getWidth");
                logicEditor.a(BlockType.VOID.getCode(), "requestFocus");
                logicEditor.a(BlockType.VOID.getCode(), "removeView");
                logicEditor.a(BlockType.VOID.getCode(), "removeViews");
                logicEditor.a(BlockType.VOID.getCode(), "addView");
                logicEditor.a(BlockType.VIEW.getCode(), "viewGetChildAt");
                logicEditor.a(BlockType.VOID.getCode(), "addViews");
                logicEditor.a(BlockType.VOID.getCode(), "setGravity");
                logicEditor.a(BlockType.VOID.getCode(), "setColorFilterView");
                logicEditor.a(BlockType.VOID.getCode(), "setBgColor");
                logicEditor.a(BlockType.VOID.getCode(), "setBgResource");
                logicEditor.a(BlockType.VOID.getCode(), "setBgDrawable");
                logicEditor.a(BlockType.VOID.getCode(), "setStrokeView");
                logicEditor.a(BlockType.VOID.getCode(), "setCornerRadiusView");
                logicEditor.a(BlockType.VOID.getCode(), "setGradientBackground");
                logicEditor.a(BlockType.VOID.getCode(), "setRadiusAndStrokeView");
            {
                boolean editTextUsed = isWidgetUsed("EditText")
                        || extraBlocks.isCustomVarUsed("EditText");
                boolean textViewUsed = isWidgetUsed("TextView")
                        || extraBlocks.isCustomVarUsed("TextView") || editTextUsed;
                boolean compoundButtonUsed = isWidgetUsed("CompoundButton")
                        || extraBlocks.isCustomVarUsed("CompoundButton")
                        || extraBlocks.isCustomVarUsed("CheckBox")
                        || extraBlocks.isCustomVarUsed("RadioButton")
                        || extraBlocks.isCustomVarUsed("Switch")
                        || extraBlocks.isCustomVarUsed("ToggleButton");
                boolean autoCompleteTextViewUsed = isWidgetUsed("AutoCompleteTextView")
                        || extraBlocks.isCustomVarUsed("AutoCompleteTextView");
                boolean multiAutoCompleteTextViewUsed = isWidgetUsed("MultiAutoCompleteTextView")
                        || extraBlocks.isCustomVarUsed("MultiAutoCompleteTextView");
                boolean imageViewUsed = isWidgetUsed("ImageView") || isWidgetUsed("CircleImageView")
                        || extraBlocks.isCustomVarUsed("ImageView");
                boolean ratingBarUsed = isWidgetUsed("RatingBar")
                        || extraBlocks.isCustomVarUsed("RatingBar");
                boolean seekBarUsed = isWidgetUsed("SeekBar")
                        || extraBlocks.isCustomVarUsed("SeekBar");
                boolean progressBarUsed = isWidgetUsed("ProgressBar")
                        || extraBlocks.isCustomVarUsed("ProgressBar");
                boolean videoViewUsed = isWidgetUsed("VideoView")
                        || extraBlocks.isCustomVarUsed("VideoView");
                boolean webViewUsed = isWidgetUsed("WebView")
                        || extraBlocks.isCustomVarUsed("WebView");
                if (textViewUsed || compoundButtonUsed || autoCompleteTextViewUsed
                        || multiAutoCompleteTextViewUsed || imageViewUsed || ratingBarUsed
                        || seekBarUsed || progressBarUsed || videoViewUsed || webViewUsed) {
                    logicEditor.a("Widgets", getTitleBgColor());
                    if (textViewUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "setText");
                        logicEditor.a(BlockType.STRING.getCode(), "getText");
                        logicEditor.a(BlockType.VOID.getCode(), "setTypeface");
                        logicEditor.a(BlockType.VOID.getCode(), "setTextColor");
                        logicEditor.a(BlockType.VOID.getCode(), "setTextSize");
                    }
                    if (editTextUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "setHint");
                        logicEditor.a(BlockType.VOID.getCode(), "setHintTextColor");
                        logicEditor.a(BlockType.VOID.getCode(), "EditTextdiableSuggestion");
                        logicEditor.a(BlockType.VOID.getCode(), "EditTextLines");
                        logicEditor.a(BlockType.VOID.getCode(), "EditTextSingleLine");
                        logicEditor.a(BlockType.VOID.getCode(), "EditTextShowError");
                        logicEditor.a(BlockType.VOID.getCode(), "EditTextSelectAll");
                        logicEditor.a(BlockType.VOID.getCode(), "EditTextSetSelection");
                        logicEditor.a(BlockType.VOID.getCode(), "EditTextSetMaxLines");
                        logicEditor.a(BlockType.DOUBLE.getCode(), "EdittextGetselectionStart");
                        logicEditor.a(BlockType.DOUBLE.getCode(), "EdittextGetselectionEnd");
                    }
                    if (compoundButtonUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "setChecked");
                        logicEditor.a(BlockType.BOOLEAN.getCode(), "getChecked");
                    }
                    if (autoCompleteTextViewUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "autoComSetData");
                    }
                    if (multiAutoCompleteTextViewUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "multiAutoComSetData");
                        logicEditor.a(BlockType.VOID.getCode(), "setThreshold");
                        logicEditor.a(BlockType.VOID.getCode(), "setTokenizer");
                    }
                    if (imageViewUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "setImage");
                        logicEditor.a(BlockType.VOID.getCode(), "setImageCustomRes");
                        logicEditor.a(BlockType.VOID.getCode(), "setImageIdentifier");
                        logicEditor.a(BlockType.VOID.getCode(), "setImageFilePath");
                        logicEditor.a(BlockType.VOID.getCode(), "setImageUrl");
                        logicEditor.a(BlockType.VOID.getCode(), "setColorFilter");
                    }
                    if (ratingBarUsed) {
                        logicEditor.a(BlockType.DOUBLE.getCode(), "getRating");
                        logicEditor.a(BlockType.VOID.getCode(), "setRating");
                        logicEditor.a(BlockType.VOID.getCode(), "setNumStars");
                        logicEditor.a(BlockType.VOID.getCode(), "setStepSize");
                    }
                    if (seekBarUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "seekBarSetProgress");
                        logicEditor.a(BlockType.DOUBLE.getCode(), "seekBarGetProgress");
                        logicEditor.a(BlockType.VOID.getCode(), "seekBarSetMax");
                        logicEditor.a(BlockType.DOUBLE.getCode(), "seekBarGetMax");
                    }
                    if (progressBarUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "progressBarSetIndeterminate");
                    }
                    if (videoViewUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "videoviewSetVideoUri");
                        logicEditor.a(BlockType.VOID.getCode(), "videoviewStart");
                        logicEditor.a(BlockType.VOID.getCode(), "videoviewPause");
                        logicEditor.a(BlockType.VOID.getCode(), "videoviewStop");
                        logicEditor.a(BlockType.BOOLEAN.getCode(), "videoviewIsPlaying");
                        logicEditor.a(BlockType.BOOLEAN.getCode(), "videoviewCanPause");
                        logicEditor.a(BlockType.BOOLEAN.getCode(), "videoviewCanSeekForward");
                        logicEditor.a(BlockType.BOOLEAN.getCode(), "videoviewCanSeekBackward");
                        logicEditor.a(BlockType.DOUBLE.getCode(), "videoviewGetCurrentPosition");
                        logicEditor.a(BlockType.DOUBLE.getCode(), "videoviewGetDuration");
                    }
                    if (webViewUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "webViewLoadUrl");
                        logicEditor.a(BlockType.STRING.getCode(), "webViewGetUrl");
                        logicEditor.a(BlockType.DOUBLE.getCode(), "webviewGetProgress");
                        logicEditor.a(BlockType.VOID.getCode(), "webViewSetCacheMode");
                        logicEditor.a(BlockType.BOOLEAN.getCode(), "webViewCanGoBack");
                        logicEditor.a(BlockType.BOOLEAN.getCode(), "webViewCanGoForward");
                        logicEditor.a(BlockType.VOID.getCode(), "webViewGoBack");
                        logicEditor.a(BlockType.VOID.getCode(), "webViewGoForward");
                        logicEditor.a(BlockType.VOID.getCode(), "webViewClearCache");
                        logicEditor.a(BlockType.VOID.getCode(), "webViewClearHistory");
                        logicEditor.a(BlockType.VOID.getCode(), "webViewStopLoading");
                        logicEditor.a(BlockType.VOID.getCode(), "webViewZoomIn");
                        logicEditor.a(BlockType.VOID.getCode(), "webViewZoomOut");
                    }
                }
            }
            {
                boolean inOnBindCustomView = eventName.equals("onBindCustomView");
                boolean spinnerUsed = isWidgetUsed("Spinner");
                boolean listViewUsed = isWidgetUsed("ListView");
                boolean recyclerViewUsed = isWidgetUsed("RecyclerView");
                boolean gridViewUsed = isWidgetUsed("GridView") || extraBlocks.isCustomVarUsed("GridView");
                boolean viewPagerUsed = isWidgetUsed("ViewPager");
                if (spinnerUsed || listViewUsed || recyclerViewUsed || gridViewUsed || viewPagerUsed) {
                    logicEditor.a("List", getTitleBgColor());
                    if (spinnerUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "spnSetData");
                        logicEditor.a(BlockType.VOID.getCode(), "spnSetCustomViewData");
                        logicEditor.a(BlockType.VOID.getCode(), "spnRefresh");
                        logicEditor.a(BlockType.VOID.getCode(), "spnSetSelection");
                        logicEditor.a(BlockType.DOUBLE.getCode(), "spnGetSelection");
                    }
                    if (!inOnBindCustomView) {
                        if (listViewUsed) {
                            logicEditor.a(BlockType.VOID.getCode(), "listSetData");
                            logicEditor.a(BlockType.VOID.getCode(), "listSetCustomViewData");
                            logicEditor.a(BlockType.VOID.getCode(), "listRefresh");
                            logicEditor.a(BlockType.VOID.getCode(), "refreshingList");
                            logicEditor.a(BlockType.VOID.getCode(), "listSmoothScrollTo");
                            logicEditor.a(BlockType.VOID.getCode(), "listViewSetSelection");
                            logicEditor.a(BlockType.VOID.getCode(), "listSetTranscriptMode");
                            logicEditor.a(BlockType.VOID.getCode(), "listSetStackFromBottom");
                            logicEditor.a(BlockType.VOID.getCode(), "ListViewAddHeader");
                            logicEditor.a(BlockType.VOID.getCode(), "listViewRemoveHeader");
                            logicEditor.a(BlockType.VOID.getCode(), "ListViewAddFooter");
                            logicEditor.a(BlockType.VOID.getCode(), "listViewRemoveFooter");
                        }
                        if (recyclerViewUsed) {
                            logicEditor.a(BlockType.VOID.getCode(), "recyclerSetCustomViewData");
                            logicEditor.a(BlockType.VOID.getCode(), "recyclerSetLayoutManager");
                            logicEditor.a(BlockType.VOID.getCode(), "recyclerSetLayoutManagerHorizontal");
                            logicEditor.a(BlockType.VOID.getCode(), "recyclerSetHasFixedSize");
                            logicEditor.a(BlockType.VOID.getCode(), "recyclerSmoothScrollToPosition");
                            logicEditor.a(BlockType.VOID.getCode(), "recyclerScrollToPositionWithOffset");
                        }
                        if (gridViewUsed) {
                            logicEditor.a(BlockType.VOID.getCode(), "gridSetCustomViewData");
                            logicEditor.a(BlockType.VOID.getCode(), "gridSetNumColumns");
                            logicEditor.a(BlockType.VOID.getCode(), "gridSetColumnWidth");
                            logicEditor.a(BlockType.VOID.getCode(), "gridSetVerticalSpacing");
                            logicEditor.a(BlockType.VOID.getCode(), "gridSetHorizontalSpacing");
                            logicEditor.a(BlockType.VOID.getCode(), "gridSetStretchMode");
                        }
                        if (viewPagerUsed) {
                            logicEditor.a(BlockType.VOID.getCode(), "pagerSetCustomViewData");
                            logicEditor.a(BlockType.VOID.getCode(), "pagerSetFragmentAdapter");
                            logicEditor.a(BlockType.DOUBLE.getCode(), "pagerGetOffscreenPageLimit");
                            logicEditor.a(BlockType.VOID.getCode(), "pagerSetOffscreenPageLimit");
                            logicEditor.a(BlockType.DOUBLE.getCode(), "pagerGetCurrentItem");
                            logicEditor.a(BlockType.VOID.getCode(), "pagerSetCurrentItem");
                            logicEditor.a(BlockType.VOID.getCode(), "ViewPagerNotifyOnDtatChange");
                        }
                    } else {
                        logicEditor.a(BlockType.VOID.getCode(), "setRecyclerViewLayoutParams");
                    }
                }
            }
            {
                boolean drawerUsed = projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER);
                boolean fabUsed = projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB);
                boolean bottomNavigationViewUsed = isWidgetUsed("BottomNavigationView");
                boolean swipeRefreshLayoutUsed = isWidgetUsed("SwipeRefreshLayout");
                boolean cardViewUsed = isWidgetUsed("CardView");
                boolean tabLayoutUsed = isWidgetUsed("TabLayout");
                boolean textInputLayoutUsed = isWidgetUsed("TextInputLayout") || extraBlocks.isCustomVarUsed(
                        "TextInputLayout");
                if (drawerUsed || fabUsed || bottomNavigationViewUsed || swipeRefreshLayoutUsed || cardViewUsed || tabLayoutUsed || textInputLayoutUsed) {
                    logicEditor.a("AndroidX components", getTitleBgColor());
                    if (drawerUsed) {
                        logicEditor.a(BlockType.BOOLEAN.getCode(), "isDrawerOpen");
                        logicEditor.a(BlockType.VOID.getCode(), "openDrawer");
                        logicEditor.a(BlockType.VOID.getCode(), "closeDrawer");
                    }
                    if (fabUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "fabIcon");
                        logicEditor.a(BlockType.VOID.getCode(), "fabSize");
                        logicEditor.a(BlockType.VOID.getCode(), "fabVisibility");
                    }
                    if (bottomNavigationViewUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "bottomMenuAddItem");
                    }
                    if (swipeRefreshLayoutUsed) {
                        logicEditor.a(BlockType.CLICK.getCode(), "onSwipeRefreshLayout");
                        logicEditor.a(BlockType.VOID.getCode(), "setRefreshing");
                    }
                    if (cardViewUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "setCardBackgroundColor");
                        logicEditor.a(BlockType.VOID.getCode(), "setCardRadius");
                        logicEditor.a(BlockType.VOID.getCode(), "setCardElevation");
                        logicEditor.a(BlockType.VOID.getCode(), "setPreventCornerOverlap");
                        logicEditor.a(BlockType.VOID.getCode(), "setUseCompatPadding");
                    }
                    if (tabLayoutUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "addTab");
                        logicEditor.a(BlockType.VOID.getCode(), "setupWithViewPager");
                        logicEditor.a(BlockType.VOID.getCode(), "setInlineLabel");
                        logicEditor.a(BlockType.VOID.getCode(), "setTabTextColors");
                        logicEditor.a(BlockType.VOID.getCode(), "setTabRippleColor");
                        logicEditor.a(BlockType.VOID.getCode(), "setSelectedTabIndicatorColor");
                        logicEditor.a(BlockType.VOID.getCode(), "setSelectedTabIndicatorHeight");
                    }
                    if (textInputLayoutUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "tilSetBoxBgColor");
                        logicEditor.a(BlockType.VOID.getCode(), "tilSetBoxStrokeColor");
                        logicEditor.a(BlockType.VOID.getCode(), "tilSetBoxBgMode");
                        logicEditor.a(BlockType.VOID.getCode(), "tilSetBoxCornerRadii");
                        logicEditor.a(BlockType.VOID.getCode(), "tilSetError");
                        logicEditor.a(BlockType.VOID.getCode(), "tilSetErrorEnabled");
                        logicEditor.a(BlockType.VOID.getCode(), "tilSetCounterEnabled");
                        logicEditor.a(BlockType.VOID.getCode(), "tilSetCounterMaxLength");
                        logicEditor.a(BlockType.DOUBLE.getCode(), "tilGetCounterMaxLength");
                    }
                }
            }
            {
                boolean waveSideBarUsed = isWidgetUsed("WaveSideBar");
                boolean badgeViewUsed = isWidgetUsed("BadgeView");
                boolean bubbleLayoutUsed = isWidgetUsed("BubbleLayout");
                boolean patternLockViewUsed = isWidgetUsed("PatternLockView");
                boolean codeViewUsed = isWidgetUsed("CodeView");
                boolean lottieAnimationViewUsed = isWidgetUsed("LottieAnimationView");
                boolean otpViewUsed = isWidgetUsed("OTPView");
                if (waveSideBarUsed || badgeViewUsed || bubbleLayoutUsed || patternLockViewUsed || codeViewUsed || lottieAnimationViewUsed) {
                    logicEditor.a("Library", getTitleBgColor());
                    if (otpViewUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "otpViewSetFieldCount");
                        logicEditor.a(BlockType.VOID.getCode(), "otpViewSetOTPText");
                        logicEditor.a(BlockType.STRING.getCode(), "otpViewGetOTPText");
                        logicEditor.a(BlockType.CLICK.getCode(), "otpViewSetOTPListener");
                    }
                    if (waveSideBarUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "setCustomLetter");
                    }
                    if (badgeViewUsed) {
                        logicEditor.a(BlockType.DOUBLE.getCode(), "getBadgeCount");
                        logicEditor.a(BlockType.VOID.getCode(), "setBadgeNumber");
                        logicEditor.a(BlockType.VOID.getCode(), "setBadgeString");
                        logicEditor.a(BlockType.VOID.getCode(), "setBadgeBackground");
                        logicEditor.a(BlockType.VOID.getCode(), "setBadgeTextColor");
                        logicEditor.a(BlockType.VOID.getCode(), "setBadgeTextSize");
                    }
                    if (bubbleLayoutUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "setBubbleColor");
                        logicEditor.a(BlockType.VOID.getCode(), "setBubbleStrokeColor");
                        logicEditor.a(BlockType.VOID.getCode(), "setBubbleStrokeWidth");
                        logicEditor.a(BlockType.VOID.getCode(), "setBubbleCornerRadius");
                        logicEditor.a(BlockType.VOID.getCode(), "setBubbleArrowHeight");
                        logicEditor.a(BlockType.VOID.getCode(), "setBubbleArrowWidth");
                        logicEditor.a(BlockType.VOID.getCode(), "setBubbleArrowPosition");
                    }
                    if (patternLockViewUsed) {
                        logicEditor.a(BlockType.STRING.getCode(), "patternToString");
                        logicEditor.a(BlockType.STRING.getCode(), "patternToMD5");
                        logicEditor.a(BlockType.STRING.getCode(), "patternToSha1");
                        logicEditor.a(BlockType.VOID.getCode(), "patternSetDotCount");
                        logicEditor.a(BlockType.VOID.getCode(), "patternSetNormalStateColor");
                        logicEditor.a(BlockType.VOID.getCode(), "patternSetCorrectStateColor");
                        logicEditor.a(BlockType.VOID.getCode(), "patternSetWrongStateColor");
                        logicEditor.a(BlockType.VOID.getCode(), "patternSetViewMode");
                        logicEditor.a(BlockType.VOID.getCode(), "patternLockClear");
                    }
                    if (codeViewUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "codeviewSetCode");
                        logicEditor.a(BlockType.VOID.getCode(), "codeviewSetLanguage");
                        logicEditor.a(BlockType.VOID.getCode(), "codeviewSetTheme");
                        logicEditor.a(BlockType.VOID.getCode(), "codeviewApply");
                    }
                    if (lottieAnimationViewUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "lottieSetAnimationFromAsset");
                        logicEditor.a(BlockType.VOID.getCode(), "lottieSetAnimationFromJson");
                        logicEditor.a(BlockType.VOID.getCode(), "lottieSetAnimationFromUrl");
                        logicEditor.a(BlockType.VOID.getCode(), "lottieSetRepeatCount");
                        logicEditor.a(BlockType.VOID.getCode(), "lottieSetSpeed");
                    }
                }
            }
            {
                boolean signInButtonUsed = isWidgetUsed("SignInButton");
                boolean youtubePlayerViewUsed = isWidgetUsed("YoutubePlayerView");
                boolean adMobUsed = "Y".equals(jC.c(sc_id).b().useYn);
                boolean mapViewUsed = isWidgetUsed("MapView");
                if (signInButtonUsed || youtubePlayerViewUsed || adMobUsed || mapViewUsed) {
                    logicEditor.a("Google", getTitleBgColor());
                    if (signInButtonUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "signInButtonSetColorScheme");
                        logicEditor.a(BlockType.VOID.getCode(), "signInButtonSetSize");
                    }
                    if (youtubePlayerViewUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "YTPVLifecycle");
                        logicEditor.a(BlockType.CLICK.getCode(), "YTPVSetListener");
                    }
                    if (adMobUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "bannerAdViewLoadAd");
                    }
                    if (mapViewUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "mapViewSetMapType");
                        logicEditor.a(BlockType.VOID.getCode(), "mapViewMoveCamera");
                        logicEditor.a(BlockType.VOID.getCode(), "mapViewZoomTo");
                        logicEditor.a(BlockType.VOID.getCode(), "mapViewZoomIn");
                        logicEditor.a(BlockType.VOID.getCode(), "mapViewZoomOut");
                        logicEditor.a(BlockType.VOID.getCode(), "mapViewAddMarker");
                        logicEditor.a(BlockType.VOID.getCode(), "mapViewSetMarkerInfo");
                        logicEditor.a(BlockType.VOID.getCode(), "mapViewSetMarkerPosition");
                        logicEditor.a(BlockType.VOID.getCode(), "mapViewSetMarkerColor");
                        logicEditor.a(BlockType.VOID.getCode(), "mapViewSetMarkerIcon");
                        logicEditor.a(BlockType.VOID.getCode(), "mapViewSetMarkerVisible");
                    }
                }
            }
            {
                boolean timePickerUsed = isWidgetUsed("TimePicker");
                boolean calendarViewUsed = isWidgetUsed("CalendarView");
                if (timePickerUsed || calendarViewUsed) {
                    logicEditor.a("Date & Time", getTitleBgColor());
                    if (timePickerUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "timepickerSetHour");
                        logicEditor.a(BlockType.VOID.getCode(), "timepickerSetMinute");
                        logicEditor.a(BlockType.VOID.getCode(), "timepickerSetCurrentHour");
                        logicEditor.a(BlockType.VOID.getCode(), "timepickerSetCurrentMinute");
                        logicEditor.a(BlockType.VOID.getCode(), "timepickerSetIs24Hour");
                    }
                    if (calendarViewUsed) {
                        logicEditor.a(BlockType.VOID.getCode(), "calendarViewSetDate");
                        logicEditor.a(BlockType.VOID.getCode(), "calendarViewSetMinDate");
                        logicEditor.a(BlockType.VOID.getCode(), "calnedarViewSetMaxDate");
                    }
                }
            }
            logicEditor.a("Function", getTitleBgColor());
            logicEditor.a(BlockType.VOID.getCode(), "performClick");
            logicEditor.a(BlockType.CLICK.getCode(), "viewOnClick");
            logicEditor.a(BlockType.CLICK.getCode(), "viewOnLongClick");
            logicEditor.a(BlockType.CLICK.getCode(), "viewOnTouch");
            logicEditor.a(BlockType.CLICK.getCode(), "showSnackbar");
            return;
            case 7:
                logicEditor.b("Add component", "componentAdd");
                logicEditor.a(BlockType.VOID.getCode(), "changeStatebarColour");
                logicEditor.a(BlockType.VOID.getCode(), "LightStatusBar");
                logicEditor.a(BlockType.VOID.getCode(), "showKeyboard");
                logicEditor.a(BlockType.VOID.getCode(), "hideKeyboard");
                logicEditor.a(BlockType.VOID.getCode(), "doToast");
                logicEditor.a(BlockType.VOID.getCode(), "copyToClipboard");
                logicEditor.a(BlockType.VOID.getCode(), "setTitle");
                logicEditor.a(BlockType.BOOLEAN.getCode(), "intentHasExtra");
                logicEditor.a(BlockType.STRING.getCode(), "intentGetString");
                logicEditor.a(BlockType.FRAGMENT.getCode(), "finishActivity");
                logicEditor.a(BlockType.FRAGMENT.getCode(), "finishAffinity");
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_INTENT)
                        || extraBlocks.isCustomVarUsed("Intent")) {
                    logicEditor.a("Intent", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "intentSetAction");
                    logicEditor.a(BlockType.VOID.getCode(), "intentSetData");
                    logicEditor.a(BlockType.VOID.getCode(), "intentSetType");
                    logicEditor.a(BlockType.VOID.getCode(), "intentSetScreen");
                    logicEditor.a(BlockType.VOID.getCode(), "launchApp");
                    logicEditor.a(BlockType.VOID.getCode(), "intentPutExtra");
                    logicEditor.a(BlockType.VOID.getCode(), "intentRemoveExtra");
                    logicEditor.a(BlockType.VOID.getCode(), "intentSetFlags");
                    logicEditor.a(BlockType.VOID.getCode(), "startActivity");
                    logicEditor.a(BlockType.VOID.getCode(), "startActivityWithChooser");
                }
                if (!frc.getBroadcastFile().isEmpty()) {
                    logicEditor.a("Broadcast", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "sendBroadcast");
                }
                if (!frc.getServiceFile().isEmpty()) {
                    logicEditor.a("Service", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "startService");
                    logicEditor.a(BlockType.VOID.getCode(), "stopService");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_SHAREDPREF)) {
                    logicEditor.a("SharedPreferences", getTitleBgColor());
                    logicEditor.a(BlockType.BOOLEAN.getCode(), "fileContainsData");
                    logicEditor.a(BlockType.STRING.getCode(), "fileGetData");
                    logicEditor.a(BlockType.VOID.getCode(), "fileSetData");
                    logicEditor.a(BlockType.VOID.getCode(), "fileRemoveData");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_DATE_PICKER_DIALOG)) {
                    logicEditor.a("DatePickerDialog", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "datePickerDialogShow");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_TIME_PICKER_DIALOG)) {
                    logicEditor.a("TimePickerDialog", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "timePickerDialogShow");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_CALENDAR)) {
                    logicEditor.a("Calendar", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "calendarGetNow");
                    logicEditor.a(BlockType.VOID.getCode(), "calendarAdd");
                    logicEditor.a(BlockType.VOID.getCode(), "calendarSet");
                    logicEditor.a(BlockType.STRING.getCode(), "calendarFormat");
                    logicEditor.a(BlockType.DOUBLE.getCode(), "calendarDiff");
                    logicEditor.a(BlockType.DOUBLE.getCode(), "calendarGetTime");
                    logicEditor.a(BlockType.VOID.getCode(), "calendarSetTime");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_VIBRATOR)) {
                    logicEditor.a("Vibrator", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "vibratorAction");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_TIMERTASK)
                        || extraBlocks.isCustomVarUsed("Timer")) {
                    logicEditor.a("Timer", getTitleBgColor());
                    logicEditor.a(BlockType.CLICK.getCode(), "timerAfter");
                    logicEditor.a(BlockType.CLICK.getCode(), "timerEvery");
                    logicEditor.a(BlockType.VOID.getCode(), "timerCancel");
                }
                if (extraBlocks.isComponentUsed(36)) {
                    logicEditor.a("AsyncTask", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "AsyncTaskExecute");
                    logicEditor.a(BlockType.VOID.getCode(), "AsyncTaskPublishProgress");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_DIALOG)
                        || extraBlocks.isCustomVarUsed("Dialog")) {
                    logicEditor.a("Dialog", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "dialogSetTitle");
                    logicEditor.a(BlockType.VOID.getCode(), "Dialog SetIcon");
                    logicEditor.a(BlockType.VOID.getCode(), "dialogSetMessage");
                    logicEditor.a(BlockType.CLICK.getCode(), "dialogOkButton");
                    logicEditor.a(BlockType.CLICK.getCode(), "dialogCancelButton");
                    logicEditor.a(BlockType.CLICK.getCode(), "dialogNeutralButton");
                    logicEditor.a(BlockType.VOID.getCode(), "dialogShow");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_MEDIAPLAYER)) {
                    logicEditor.a("MediaPlayer", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "mediaplayerCreate");
                    logicEditor.a(BlockType.VOID.getCode(), "mediaplayerStart");
                    logicEditor.a(BlockType.VOID.getCode(), "mediaplayerPause");
                    logicEditor.a(BlockType.VOID.getCode(), "mediaplayerSeek");
                    logicEditor.a(BlockType.DOUBLE.getCode(), "mediaplayerGetCurrent");
                    logicEditor.a(BlockType.DOUBLE.getCode(), "mediaplayerGetDuration");
                    logicEditor.a(BlockType.BOOLEAN.getCode(), "mediaplayerIsPlaying");
                    logicEditor.a(BlockType.VOID.getCode(), "mediaplayerSetLooping");
                    logicEditor.a(BlockType.BOOLEAN.getCode(), "mediaplayerIsLooping");
                    logicEditor.a(BlockType.VOID.getCode(), "mediaplayerReset");
                    logicEditor.a(BlockType.VOID.getCode(), "mediaplayerRelease");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_SOUNDPOOL)) {
                    logicEditor.a("SoundPool", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "soundpoolCreate");
                    logicEditor.a(BlockType.DOUBLE.getCode(), "soundpoolLoad");
                    logicEditor.a(BlockType.DOUBLE.getCode(), "soundpoolStreamPlay");
                    logicEditor.a(BlockType.VOID.getCode(), "soundpoolStreamStop");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_OBJECTANIMATOR)) {
                    logicEditor.a("ObjectAnimator", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "objectanimatorSetTarget");
                    logicEditor.a(BlockType.VOID.getCode(), "objectanimatorSetProperty");
                    logicEditor.a(BlockType.VOID.getCode(), "objectanimatorSetValue");
                    logicEditor.a(BlockType.VOID.getCode(), "objectanimatorSetFromTo");
                    logicEditor.a(BlockType.VOID.getCode(), "objectanimatorSetDuration");
                    logicEditor.a(BlockType.VOID.getCode(), "objectanimatorSetRepeatMode");
                    logicEditor.a(BlockType.VOID.getCode(), "objectanimatorSetRepeatCount");
                    logicEditor.a(BlockType.VOID.getCode(), "objectanimatorSetInterpolator");
                    logicEditor.a(BlockType.VOID.getCode(), "objectanimatorStart");
                    logicEditor.a(BlockType.VOID.getCode(), "objectanimatorCancel");
                    logicEditor.a(BlockType.BOOLEAN.getCode(), "objectanimatorIsRunning");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_FIREBASE)) {
                    logicEditor.a("Firebase", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "firebaseAdd");
                    logicEditor.a(BlockType.VOID.getCode(), "firebasePush");
                    logicEditor.a(BlockType.STRING.getCode(), "firebaseGetPushKey");
                    logicEditor.a(BlockType.VOID.getCode(), "firebaseDelete");
                    logicEditor.a(BlockType.CLICK.getCode(), "firebaseGetChildren");
                    logicEditor.a(BlockType.VOID.getCode(), "firebaseStartListen");
                    logicEditor.a(BlockType.VOID.getCode(), "firebaseStopListen");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH)) {
                    logicEditor.a("FirebaseAuth", getTitleBgColor());
                    logicEditor.a(BlockType.BOOLEAN.getCode(), "firebaseauthIsLoggedIn");
                    logicEditor.a(BlockType.STRING.getCode(), "firebaseauthGetCurrentUser");
                    logicEditor.a(BlockType.STRING.getCode(), "firebaseauthGetUid");
                    logicEditor.a(BlockType.VOID.getCode(), "firebaseauthCreateUser");
                    logicEditor.a(BlockType.VOID.getCode(), "firebaseauthSignInUser");
                    logicEditor.a(BlockType.VOID.getCode(), "firebaseauthSignInAnonymously");
                    logicEditor.a(BlockType.VOID.getCode(), "firebaseauthResetPassword");
                    logicEditor.a(BlockType.VOID.getCode(), "firebaseauthSignOutUser");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_GYROSCOPE)) {
                    logicEditor.a("Gyroscope", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "gyroscopeStartListen");
                    logicEditor.a(BlockType.VOID.getCode(), "gyroscopeStopListen");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_INTERSTITIAL_AD)) {
                    logicEditor.a("AdMob Interstitial", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "interstitialAdLoad");
                    logicEditor.a(BlockType.VOID.getCode(), "interstitialAdShow");
                    logicEditor.a(BlockType.BOOLEAN.getCode(), "interstitialAdIsLoaded");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD)) {
                    logicEditor.a("RewardedVideoAd", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "rewardedVideoAdLoad");
                    logicEditor.a(BlockType.VOID.getCode(), "rewardedVideoAdShow");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_FIREBASE_STORAGE)) {
                    logicEditor.a("Firebase Storage", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "firebasestorageUploadFile");
                    logicEditor.a(BlockType.VOID.getCode(), "firebasestorageDownloadFile");
                    logicEditor.a(BlockType.VOID.getCode(), "firebasestorageDelete");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_CAMERA)) {
                    logicEditor.a("Camera", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "camerastarttakepicture");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_FILE_PICKER)) {
                    logicEditor.a("FilePicker", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "filepickerstartpickfiles");
                    logicEditor.a(BlockType.VOID.getCode(), "imageCrop");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_REQUEST_NETWORK)) {
                    logicEditor.a("RequestNetwork", getTitleBgColor());
                    logicEditor.a(BlockType.BOOLEAN.getCode(), "isConnected");
                    logicEditor.a(BlockType.VOID.getCode(), "requestnetworkSetParams");
                    logicEditor.a(BlockType.VOID.getCode(), "requestnetworkSetHeaders");
                    logicEditor.a(BlockType.VOID.getCode(), "requestnetworkStartRequestNetwork");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_TEXT_TO_SPEECH)) {
                    logicEditor.a("TextToSpeech", getTitleBgColor());
                    logicEditor.a(BlockType.BOOLEAN.getCode(), "textToSpeechIsSpeaking");
                    logicEditor.a(BlockType.VOID.getCode(), "textToSpeechSetPitch");
                    logicEditor.a(BlockType.VOID.getCode(), "textToSpeechSetSpeechRate");
                    logicEditor.a(BlockType.VOID.getCode(), "textToSpeechSpeak");
                    logicEditor.a(BlockType.VOID.getCode(), "textToSpeechStop");
                    logicEditor.a(BlockType.VOID.getCode(), "textToSpeechShutdown");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_SPEECH_TO_TEXT)) {
                    logicEditor.a("SpeechToText", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "speechToTextStartListening");
                    logicEditor.a(BlockType.VOID.getCode(), "speechToTextStopListening");
                    logicEditor.a(BlockType.VOID.getCode(), "speechToTextShutdown");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_BLUETOOTH_CONNECT)) {
                    logicEditor.a("Bluetooth", getTitleBgColor());
                    logicEditor.a(BlockType.BOOLEAN.getCode(), "bluetoothConnectIsBluetoothEnabled");
                    logicEditor.a(BlockType.BOOLEAN.getCode(), "bluetoothConnectIsBluetoothActivated");
                    logicEditor.a(BlockType.STRING.getCode(), "bluetoothConnectGetRandomUuid");
                    logicEditor.a(BlockType.VOID.getCode(), "bluetoothConnectReadyConnection");
                    logicEditor.a(BlockType.VOID.getCode(), "bluetoothConnectReadyConnectionToUuid");
                    logicEditor.a(BlockType.VOID.getCode(), "bluetoothConnectStartConnection");
                    logicEditor.a(BlockType.VOID.getCode(), "bluetoothConnectStartConnectionToUuid");
                    logicEditor.a(BlockType.VOID.getCode(), "bluetoothConnectStopConnection");
                    logicEditor.a(BlockType.VOID.getCode(), "bluetoothConnectSendData");
                    logicEditor.a(BlockType.VOID.getCode(), "bluetoothConnectActivateBluetooth");
                    logicEditor.a(BlockType.VOID.getCode(), "bluetoothConnectGetPairedDevices");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_LOCATION_MANAGER)) {
                    logicEditor.a("LocationManager", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "locationManagerRequestLocationUpdates");
                    logicEditor.a(BlockType.VOID.getCode(), "locationManagerRemoveUpdates");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_PROGRESS_DIALOG)
                        || extraBlocks.isCustomVarUsed("ProgressDialog")
                        || eventName.equals("onPreExecute") || eventName.equals("onProgressUpdate")
                        || eventName.equals("onPostExecute")) {
                    logicEditor.a("ProgressDialog", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "progressdialogCreate");
                    logicEditor.a(BlockType.VOID.getCode(), "progressdialogSetTitle");
                    logicEditor.a(BlockType.VOID.getCode(), "progressdialogSetMessage");
                    logicEditor.a(BlockType.VOID.getCode(), "progressdialogSetMax");
                    logicEditor.a(BlockType.VOID.getCode(), "progressdialogSetProgress");
                    logicEditor.a(BlockType.VOID.getCode(), "progressdialogSetCancelable");
                    logicEditor.a(BlockType.VOID.getCode(), "progressdialogSetCanceledOutside");
                    logicEditor.a(BlockType.VOID.getCode(), "progressdialogSetStyle");
                    logicEditor.a(BlockType.VOID.getCode(), "progressdialogShow");
                    logicEditor.a(BlockType.VOID.getCode(), "progressdialogDismiss");
                    return;
                }
                return;
            case 8:
                logicEditor.b("Create", "blockAdd");
                logicEditor.b("Import From Collection", "blockImport");
                if (ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_SHOW_BUILT_IN_BLOCKS)) {
                    logicEditor.a(BlockType.VOID.getCode(), "customToast");
                    logicEditor.a(BlockType.VOID.getCode(), "customToastWithIcon");
                }
                moreBlocks();
                if (ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_SHOW_BUILT_IN_BLOCKS)) {
                    logicEditor.a("Command Blocks", getTitleBgColor());
                    logicEditor.a(BlockType.CLICK.getCode(), "CommandBlockJava");
                    logicEditor.addDeprecatedBlock("Deprecated: Use XML Command Manager", BlockType.CLICK.getCode(),
                            "CommandBlockXML");
                    logicEditor.a("Permission Command Blocks", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "addPermission");
                    logicEditor.a(BlockType.VOID.getCode(), "removePermission");
                    logicEditor.a("Other Command Blocks", getTitleBgColor());
                    logicEditor.a(BlockType.VOID.getCode(), "addCustomVariable");
                    logicEditor.a(BlockType.VOID.getCode(), "addInitializer");
                    return;
                }
                return;
            default:
                int paletteIndex = -1, paletteBlocks = 0;
                ArrayList<HashMap<String, Object>> extraBlockData = ExtraBlockFile.getExtraBlockData();
                for (int i = 0, extraBlockDataSize = extraBlockData.size(); i < extraBlockDataSize; i++) {
                    HashMap<String, Object> map = extraBlockData.get(i);
                    Object palette = map.get("palette");
                    if (palette instanceof String paletteString) {
                        if (paletteString.equals(String.valueOf(paletteId))) {
                            if (paletteIndex == -1) paletteIndex = Integer.parseInt(paletteString);
                            paletteBlocks++;
                            Object type = map.get("type");
                            if (type instanceof String typeString) {
                                if (typeString.equals("h")) {
                                    Object spec = map.get("spec");
                                    if (spec instanceof String specString) {
                                        logicEditor.a(specString, getTitleBgColor());
                                    } else {
                                        SketchwareUtil.toastError("Custom Block #" + paletteBlocks +
                                                " of current palette has an invalid spec data type");
                                    }
                                } else {
                                    Object name = map.get("name");
                                    if (name instanceof String nameString) {
                                        Object typeName = map.get("typeName");
                                        if (typeName instanceof String typeNameString) {
                                            logicEditor.a("", typeString, typeNameString, nameString);
                                        } else {
                                            logicEditor.a("", typeString, "", nameString);
                                        }
                                    } else {
                                        SketchwareUtil.toastError("Custom Block #" + paletteBlocks +
                                                " of current palette has an invalid name data type");
                                    }
                                }
                            } else {
                                SketchwareUtil.toastError("Custom Block #" + paletteBlocks +
                                        " of current palette has an invalid block type data type");
                            }
                        }
                    } else {
                        SketchwareUtil.toastError("Custom Block #" + paletteBlocks +
                                " of current palette has an invalid block palette data type");
                    }
                }
                break;
        }
    }

    private boolean areAnyVariablesUsed() {
        // Primitive types: 0=boolean, 1=number, 2=string, 3=map
        for (int i = 0; i <= 3; i++) {
            if (extraBlocks.isVariableUsed(i)) return true;
        }
        // Custom variables: 5 (legacy "Type name") and 6 (new format)
        var projectDataManager = jC.a(sc_id);
        return !projectDataManager.e(javaName, 5).isEmpty()
                || !projectDataManager.e(javaName, 6).isEmpty();
    }

    private @ColorInt int getTitleBgColor() {
        return getColor(logicEditor, isDarkThemeEnabled(logicEditor) ? R.attr.colorSurfaceContainerHigh :
                R.attr.colorSurfaceInverse);
    }
}
