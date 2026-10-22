# Offline project translation helpers

These Java helpers translate text using an offline translation engine and
write translated Sketchware project string resources. They are in the
`mod.loddv.dev.tools.offlineTranslator` package.

## Translating a Sketchware project

The project helper reads
`<project>/files/resource/values/strings.xml` and creates
`<project>/files/resource/values-<locale>/strings.xml`. For example:

```java
AidlBatchTranslator batchTranslator = new AidlBatchTranslator(offlineTranslatorClient, "en", "pt");
SketchwareProjectTranslator projectTranslator = new SketchwareProjectTranslator(batchTranslator);
File translatedStrings = projectTranslator.translateStrings(projectDirectory, "pt-rBR");
```

Only non-empty, direct `<string>` resources are sent to the translator. Strings with
`translatable="false"` and strings containing inline XML markup are left unchanged.
The source resource is never modified; the generated locale file replaces any
previous file for that same locale. The locale argument must be an Android language
qualifier such as `es` or a language-region qualifier such as `pt-rBR`.

The translation engine must be configured for offline use by its caller. These
helpers do not download language models or perform network requests.
