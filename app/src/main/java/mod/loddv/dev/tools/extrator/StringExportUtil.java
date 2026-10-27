package mod.loddv.dev.tools.extrator;

import android.content.Context;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

import pro.sketchware.R;
import pro.sketchware.utility.FileUtil;

public class StringExportUtil {

    public static void exportDefaultStrings(Context context, File destinationFile) {
        boolean success = false;
        try {
            FileOutputStream fos = new FileOutputStream(destinationFile);
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(fos, "UTF-8");
            serializer.startDocument("UTF-8", true);
            serializer.startTag(null, "resources");

            Field[] fields = R.string.class.getFields();
            for (Field field : fields) {
                String keyName = field.getName();
                int resourceId = field.getInt(null);
                String textValue;
                try {
                    textValue = context.getString(resourceId);
                } catch (Exception ignored) {
                    continue;
                }
                if (textValue != null) {
                    serializer.startTag(null, "string");
                    serializer.attribute(null, "name", keyName);
                    serializer.text(textValue);
                    serializer.endTag(null, "string");
                }
            }

            serializer.endTag(null, "resources");
            serializer.endDocument();
            fos.close();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!success) {
            try {
                a.a.a.oB fileHelper = new a.a.a.oB();
                fileHelper.a(context, "localization/strings.xml", destinationFile.getAbsolutePath());
                success = destinationFile.exists() && destinationFile.length() > 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!success || !destinationFile.exists()) {
            FileUtil.writeFile(destinationFile.getAbsolutePath(), "<resources>\n</resources>");
        }
    }
}
