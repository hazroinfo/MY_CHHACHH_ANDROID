# MY CHHACHH — Native Android V5 Final UI Rebuild

یہ WebView نہیں ہے۔ یہ Kotlin + Jetpack Compose native Android project ہے اور موجودہ My Chhachh backend/API سے جڑتا ہے۔

## اس ورژن میں ایک ساتھ کی گئی اہم تبدیلیاں

- ہر اندرونی صفحے سے بڑا دہرایا جانے والا Home header ہٹا دیا گیا ہے۔
- Home پر compact top bar ہے؛ Voting، Weather اور Announcements صرف Home کے quick actions میں ہیں۔
- Home / People / Shop / Map / Messages مستقل bottom navigation میں ہیں۔
- Notifications، Saved، Search، Profile، Shop Detail، Settings، Weather، Voting، Announcements اور Chat کے لیے compact back-title bar ہے۔
- back navigation کے لیے proper in-app stack شامل کیا گیا ہے تاکہ Settings/Search/Profile سے واپس صحیح جگہ آیا جائے۔
- Jelly UI کو کم بھاری، زیادہ صاف اور موبائل-app جیسا بنایا گیا ہے؛ cards، buttons، spacing اور typography دوبارہ balanced کیے گئے ہیں۔
- Post video اب external player میں نہیں کھلتی؛ Media3 کے ذریعے app کے اندر player dialog میں چلتی ہے۔
- Voice announcement اور message audio app کے اندر play/pause ہوتے ہیں۔
- Map اب صرف search box نہیں؛ OpenStreetMap/osmdroid کا حقیقی native interactive map ہے، search result پر marker اور zoom آتا ہے۔
- Voting ختم ہونے پر Vote disabled رہتا ہے، vote counts اور proportion bar واضح دکھتے ہیں۔
- Shop detail کے posts اب مکمل post cards ہیں: Like / Comment / Share / Save کے ساتھ۔
- Share action Home، Saved، Profile اور Shop posts میں Android share sheet کھولتا ہے۔
- Profile میں اپنے account کے لیے Edit profile button دیا گیا ہے۔
- Settings میں profile photo picker، username validation اور private/public profile control شامل ہے۔
- People list میں Follow/Following state دکھتی ہے اور اپنا account Follow button نہیں دکھاتا۔
- Shop list میں Follow/Following، location details اور followers count بہتر طریقے سے دکھتے ہیں۔
- Shop detail میں Call، WhatsApp اور saved map/location link buttons شامل ہیں جہاں data موجود ہو۔
- Messages screen اور chat UI compact کیے گئے ہیں؛ chat نئی message پر bottom تک scroll کرتا ہے اور received photo/audio render کرتا ہے۔
- Notifications کھولنے پر layout compact ہے؛ Announcements کھولنے پر announcement unread badge clear کرنے کی API call کی جاتی ہے۔
- Version code 5 اور version name 1.0.5 رکھا گیا ہے۔

## Dependencies

- Jetpack Compose / Material 3
- OkHttp
- Coil
- AndroidX Media3 (native video playback)
- osmdroid / OpenStreetMap (native interactive map)

## Build

Android Studio میں project root کھولیں، Gradle sync ہونے دیں اور پھر app module run/build کریں۔

نوٹ: اس workspace میں Android SDK/Gradle runtime موجود نہیں تھا، اس لیے یہاں APK compile نہیں کیا جا سکا۔ Source structure اور Kotlin syntax کی parser-level جانچ کی گئی ہے، مگر آخری device build Android Studio میں ضروری ہے۔

## V6 اصلاح
اس پیکج میں Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle-wrapper.properties`) شامل کر دیا گیا ہے تاکہ پروجیکٹ Android Studio یا command line سے مکمل Gradle project کے طور پر کھلے۔ Version code 6 / version 1.0.6 ہے۔
