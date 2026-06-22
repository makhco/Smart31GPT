# ICODE SLIX2 Programmer - Android Kotlin

این پروژه یک اپ ساده Android/Kotlin برای کار با تگ‌های **NXP ICODE SLIX2 / ISO15693 / NFC-V** است.

## امکانات

1. تب «برنامه‌ریزی و قفل»
   - اسکن تگ ICODE SLIX2 با NFC گوشی
   - نوشتن فقط بلاک‌های زیر:
     - 00 تا 04
     - 48 تا 63
   - خواندن مجدد همان بلاک‌ها و verify دقیق ۴ بایتی
   - تغییر Read Password به `CF 8B 1B 78`
   - تغییر Write Password به `F1 B3 2A 1D`
   - اجرای دستورات نهایی:
     - `22 B6 04 UID 00 11`
     - `22 BB 04 UID`
     - `22 29 UID 03`

2. تب «ریکاوری کلیدها»
   - مخصوص تگ‌هایی که بلاک‌ها و کلیدهای جدید روی آن‌ها نوشته شده‌اند اما fuse/access protection نهایی روی آن‌ها اعمال نشده است.
   - احراز هویت با کلیدهای جدید:
     - Read: `CF 8B 1B 78`
     - Write: `F1 B3 2A 1D`
   - بازگرداندن هر دو کلید به مقدار پیش‌فرض `00 00 00 00`

## ساخت پروژه

پروژه را در Android Studio باز کنید و Sync بزنید. سپس روی گوشی واقعی که NFC دارد اجرا کنید.

برای build محلی یا CI:

```bash
./gradlew assembleDebug
```

در GitHub Actions، فایل `.github/workflows/android-build.yml` اضافه شده و از actionهای Node 24-compatible استفاده می‌کند.

- Min SDK: 23
- Target/Compile SDK: 35
- زبان: Kotlin
- UI: Native Android View بدون Compose و بدون وابستگی اضافه

## نکات مهم اجرایی

- این اپ از `android.nfc.tech.NfcV` و دستورهای خام `transceive()` استفاده می‌کند.
- ترتیب UID در فرمان‌ها از `tag.id` اندروید استفاده شده است. برای نمایش انسانی، UID در لاگ به صورت reverse هم نمایش داده می‌شود.
- اگر `Present Password` با کلید اشتباه اجرا شود، ICODE SLIX2 ممکن است تا برداشتن تگ از میدان RF و نزدیک‌کردن مجدد آن، دستورهای بعدی را اجرا نکند.
- مقدار پیش‌فرض کلیدهای Read/Write در `IcodeSlix2Config.kt` برابر `00000000` تعریف شده است.
- اگر در محیط شما ترتیب بایت کلید یا UID متفاوت باشد، فقط فایل `Slix2NfcV.kt` و `IcodeSlix2Config.kt` را تغییر دهید.

## فایل‌های اصلی

- `IcodeSlix2Config.kt`: داده بلاک‌ها، کلیدها و شناسه رمزها
- `Slix2NfcV.kt`: اجرای دستورات NFC-V / SLIX2
- `TagWorkflows.kt`: منطق برنامه‌ریزی کامل و ریکاوری
- `MainActivity.kt`: رابط کاربری دو تب و Reader Mode

## هشدار

مرحله نهایی قفل‌کردن برای سناریوی تولیدی طراحی شده و ممکن است برگشت‌پذیر نباشد. فقط روی تگ‌هایی اجرا شود که مالک آن هستید و برای تست ابتدا از چند تگ نمونه استفاده کنید.

## تغییرات نسخه fixed3

- فراخوانی timeout از `NfcV` حذف شد، چون در API فعلی Android برای `NfcV` قابل resolve نیست و باعث خطای compile می‌شد.
- مسیر اجرای دستورهای NFC همچنان با `transceive()` و مدیریت `IOException` انجام می‌شود.
