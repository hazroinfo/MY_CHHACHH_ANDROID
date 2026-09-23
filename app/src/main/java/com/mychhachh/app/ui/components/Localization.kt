package com.mychhachh.app.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

object AppLanguage {
    var current by mutableStateOf("en")
        private set

    val isUrdu: Boolean
        get() = current == "ur"

    private val urdu = mapOf(
        "Request for Blue Tick" to "بلیو ٹک کی درخواست",
        "Total Users" to "کل یوزرز",
        "Offline" to "آف لائن",
        "Read comments" to "کمنٹس پڑھیں",
        "Views" to "ویوز",
        "View Shop" to "شاپ دیکھیں",
        "Home" to "ہوم",
        "Search" to "تلاش",
        "Messages" to "پیغامات",
        "Notifications" to "اطلاعات",
        "Profile" to "پروفائل",
        "Local" to "شاپس",
        "Settings" to "ترتیبات",
        "Admin" to "ایڈمن",
        "Logout" to "لاگ آؤٹ",
        "Local Shops & Services" to "مقامی شاپس اور خدمات",
        "Discover local businesses." to "قریبی کاروبار تلاش کریں۔",
        "Create one professional shop for your account." to "اپنے اکاؤنٹ کے لیے ایک پروفیشنل شاپ بنائیں۔",
        "Create Shop" to "شاپ بنائیں",
        "Open My Shop" to "میری شاپ کھولیں",
        "All Shops" to "تمام شاپس",
        "Shop Details" to "شاپ کی تفصیلات",
        "Category" to "زمرہ",
        "Phone" to "فون",
        "City" to "شہر",
        "Village" to "گاؤں",
        "Mohalla" to "محلہ",
        "Address" to "مکمل پتہ",
        "Location" to "لوکیشن",
        "Google Maps Location" to "گوگل میپس لوکیشن",
        "Call" to "کال",
        "Follow Shop" to "شاپ فالو کریں",
        "Following" to "فالو کر رہے ہیں",
        "Share Shop" to "شاپ شیئر کریں",
        "Edit Shop Profile" to "شاپ پروفائل ایڈٹ کریں",
        "Save Shop Profile" to "شاپ پروفائل محفوظ کریں",
        "Cancel" to "منسوخ",
        "Upload Photo" to "فوٹو اپلوڈ کریں",
        "Shop profile photo" to "شاپ پروفائل فوٹو",
        "Choose a new photo" to "نئی فوٹو منتخب کریں",
        "Business name" to "شاپ کا نام",
        "Phone (numbers only)" to "فون نمبر (صرف ہندسے)",
        "Description" to "تفصیل",
        "Shop username" to "شاپ یوزرنیم",
        "Shop Post" to "شاپ پوسٹ",
        "Publish Shop Post" to "شاپ پوسٹ شائع کریں",
        "Photo" to "فوٹو",
        "Everyone" to "سب لوگ",
        "Followers" to "فالوورز",
        "Shop Posts" to "شاپ پوسٹس",
        "No shop posts yet." to "ابھی کوئی شاپ پوسٹ نہیں ہے۔",
        "People You May Know" to "آپ شاید ان لوگوں کو جانتے ہوں",
        "Language" to "زبان",
        "Switch the website between English and Urdu." to "ویب سائٹ کو اردو اور انگلش کے درمیان تبدیل کریں۔",
        "Share Current Location" to "موجودہ لوکیشن شیئر کریں",
        "Location updated with your permission." to "آپ کی اجازت سے لوکیشن محفوظ ہو گئی۔",
        "Location update failed." to "لوکیشن محفوظ نہیں ہو سکی۔",
        "Location permission was denied or unavailable." to "لوکیشن کی اجازت نہیں دی گئی یا دستیاب نہیں۔",
        "This shop's full details are shown here." to "اس شاپ کی مکمل تفصیلات یہاں موجود ہیں۔",
        "Activity Audit" to "ایکٹیویٹی ریکارڈ",
        "User Search" to "یوزر تلاش",
        "Admin Center" to "ایڈمن سینٹر",
        "Users" to "یوزرز",
        "Posts" to "پوسٹس",
        "Shops" to "شاپس",
        "Activity" to "ایکٹیویٹی",
        "Feature Controls" to "فیچر کنٹرول",
        "Admin Profile" to "ایڈمن پروفائل",
        "Admin Post" to "ایڈمن پوسٹ",
        "Search Activity" to "ایکٹیویٹی تلاش کریں",
        "Online" to "آن لائن",
        "Chhachh" to "چھچھ",
        "Privacy" to "پرائیویسی",
        "Change Password" to "پاس ورڈ تبدیل کریں",
        "Delete Account" to "اکاؤنٹ ڈیلیٹ کریں",
        "Save Privacy" to "پرائیویسی محفوظ کریں",
        "Save Profile" to "پروفائل محفوظ کریں",
        "Edit profile" to "پروفائل ایڈٹ کریں",
        "Create account" to "اکاؤنٹ بنائیں",
        "Login" to "لاگ اِن",
        "Sign up" to "اکاؤنٹ بنائیں",
        "View Profile" to "پروفائل دیکھیں",
        "Verify Profile" to "پروفائل ویریفائی کریں",
        "Verification Complete" to "ویریفکیشن مکمل",
        "Wait" to "انتظار کریں",
        "Profile Verification" to "پروفائل ویریفکیشن",
        "Submit Verification" to "ویریفکیشن بھیجیں",
        "Resubmit Verification" to "دوبارہ ویریفکیشن بھیجیں",
        "Open Location" to "لوکیشن کھولیں",
        "Video" to "وڈیو",
        "Photo selected" to "فوٹو منتخب ہو گئی",
        "Video selected" to "وڈیو منتخب ہو گئی"
    )

    fun set(language: String) {
        current = if (language == "ur") "ur" else "en"
    }

    fun translate(value: String): String {
        if (!isUrdu) return value
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return value
        val translated = urdu[trimmed] ?: return value
        return value.replace(trimmed, translated)
    }
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    MaterialText(
        text = AppLanguage.translate(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}
