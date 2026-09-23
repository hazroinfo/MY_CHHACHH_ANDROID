# V6 Self Check

اس پیکج کو فائل دینے سے پہلے درج ذیل چیکس کیے گئے:

- Gradle Wrapper کی چاروں ضروری فائلیں موجود ہیں۔
- Wrapper bootstrap کو مقامی test distribution کے ساتھ چلا کر verify کیا گیا۔
- AndroidManifest.xml اور تمام resource XML فائلیں parse ہوئیں۔
- تمام 60 PNG assets verify ہوئے۔
- کوئی zero-byte فائل نہیں ملی۔
- Kotlin source میں duplicate `.kt` filename نہیں ملا۔
- Kotlin parser scan میں `expecting` / `unexpected tokens` جیسی syntax error نہیں ملی۔
- ZIP integrity test مکمل کیا گیا۔

مکمل Android APK compile/run اس container میں Android SDK اور بیرونی dependency network دستیاب نہ ہونے کی وجہ سے یہاں execute نہیں کیا گیا۔
