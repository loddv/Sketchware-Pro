# Offline project translation helpers

These Java helpers batch text through a caller-provided offline translation engine and
write translated Sketchware project string resources. They are in the
`mod.loddv.dev.tools.offlineTranslator` package and do not depend on a particular
translation library.

## Batch translation

Implement `BatchTranslatorManager.OfflineTranslator` using an offline engine whose
model is already available on the device. The adapter receives one immutable list
per batch and must return exactly one non-null translation for every input, in the
same order.

```java
BatchTranslatorManager batches = new BatchTranslatorManager(offlineEngine, 32);
List<String> translations = batches.translate(sourceTexts);
```

The manager divides larger input lists into batches of the configured maximum size,
then combines the results in their original order. It returns an empty list for
empty input and reports an error if the engine returns a mismatched batch.

## Translating a Sketchware project

The project helper reads
`<project>/files/resource/values/strings.xml` and creates
`<project>/files/resource/values-<locale>/strings.xml`. For example:

```java
SketchwareProjectTranslator projectTranslator =
        new SketchwareProjectTranslator(batches);
File translatedStrings = projectTranslator.translateStrings(projectDirectory, "pt-rBR");
```

Only non-empty, direct `<string>` resources are sent to the translator. Strings with
`translatable="false"` and strings containing inline XML markup are left unchanged.
The source resource is never modified; the generated locale file replaces any
previous file for that same locale. The locale argument must be an Android language
qualifier such as `es` or a language-region qualifier such as `pt-rBR`.

The translation engine must be configured for offline use by its caller. These
helpers do not download language models or perform network requests.
