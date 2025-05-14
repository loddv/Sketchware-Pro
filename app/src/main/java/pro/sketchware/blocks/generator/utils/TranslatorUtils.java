package pro.sketchware.blocks.generator.utils;

import android.util.Log;

import com.besome.sketch.beans.BlockBean;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import mod.hey.studios.editor.manage.block.ExtraBlockInfo;
import mod.hey.studios.editor.manage.block.v2.BlockLoader;

public class TranslatorUtils {

    public static String safeGetString(Object obj) {
        if (obj == null) return "";
        return obj instanceof String ? (String) obj : String.valueOf(obj);
    }

    public static String getParamHolderRegex() {
        return "%\\d*\\$?m\\.(\\w+)|%\\d+\\$s|%s\\.inputOnly|%s|%d|%b";
    }

    public static void reorderBlocks(List<BlockBean> blocks) {
        blocks.sort(Comparator.comparingInt(b -> Integer.parseInt(b.id.replaceAll("\\D", ""))));
    }

    public static void removeUnnecessaryNextIds(List<BlockBean> blocks, ArrayList<String> noNextBlocks) {
        for (int i = 0; i < blocks.size(); i++) {
            BlockBean bean = blocks.get(i);
            if (noNextBlocks.contains(bean.id)) {
                bean.nextBlock = -1;
            }
        }
    }

    public static boolean isLiteralString(String param) {
        return param.length() >= 2 && param.startsWith("\"") && param.endsWith("\"");
    }

    public static boolean isLiteralBoolean(String param) {
        Pattern literalBool = Pattern.compile("^(true|false)$");
        return literalBool.matcher(param).matches();
    }

    public static boolean isLiteralNumber(String param) {
        Pattern literalNum = Pattern.compile("^-?\\d+(\\.\\d+)?$");
        return literalNum.matcher(param).matches();
    }

    public static boolean isCastExpression(String param) {
        param = param.trim();
        return param.matches("^\\(\\s*\\w+\\s*\\)\\s*.+");
    }

    public static ArrayList<HashMap<String, Object>> getPreLoadedBlocks(ArrayList<BlockBean> preLoadedBlockBeans, String sc_id) {
        ArrayList<HashMap<String, Object>> result = new ArrayList<>();

        for (BlockBean blockBean : preLoadedBlockBeans) {

            ExtraBlockInfo blockInfo = BlockLoader.getBlockInfo(blockBean.opCode);

            if (blockInfo.isMissing) {
                blockInfo = BlockLoader.getBlockFromProject(sc_id, blockBean.opCode);
            }

            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("name", blockBean.opCode);
            hashMap.put("type", blockBean.type);
            hashMap.put("typeName", blockBean.typeName);
            hashMap.put("code", blockInfo.getCode());
            hashMap.put("spec", blockBean.spec);
            result.add(hashMap);
        }

        return result;
    }

}
