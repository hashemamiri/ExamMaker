# آزمون‌ساز اندروید

پروژه اندرویدی فارسی برای ساخت آزمون و قرار دادن سؤال‌ها روی قالب‌های PDF با iText 7.

> توجه: موتور موجود یک نسخه آزمایشی است و هنوز جایگزین اسکنر حرفه‌ای مبتنی بر OpenCV/OCR و پروفایل‌های تأییدشده ۲۲ قالب نشده است.

## ساخت APK بدون Android Studio با GitHub Actions

### ۱. ایجاد مخزن

در GitHub یک Repository خالی بسازید؛ مثلاً `ExamMaker`.

### ۲. ارسال پروژه از WSL

فایل ZIP را باز کنید و داخل پوشه پروژه اجرا کنید:

```bash
git init
git add .
git commit -m "Initial ExamMaker Android project"
git branch -M main
git remote add origin https://github.com/USERNAME/ExamMaker.git
git push -u origin main
```

به‌جای `USERNAME` نام کاربری GitHub خود را قرار دهید.

### ۳. دریافت APK

1. صفحه Repository را باز کنید.
2. وارد تب **Actions** شوید.
3. Workflow با نام **Build Android APK** را انتخاب کنید.
4. روی **Run workflow** بزنید.
5. پس از سبز شدن Build، صفحه اجرای Workflow را باز کنید.
6. از بخش **Artifacts** فایل `ExamMaker-debug-apk` را دانلود کنید.

GitHub پس از هر Push نیز APK را خودکار می‌سازد.

## ساخت در WSL

برای ساخت محلی به JDK 17، Android SDK و Gradle 8.2 نیاز دارید:

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=$HOME/Android/Sdk
gradle --no-daemon assembleDebug
```

خروجی در مسیر زیر ایجاد می‌شود:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## ساختار مهم

- `app/src/main/java/com/exammaker/app/MainActivity.java`: منطق فعلی برنامه
- `app/src/main/assets/templates/`: قالب‌های PDF
- `.github/workflows/android-build.yml`: ساخت خودکار APK
- `app/build.gradle`: وابستگی iText 7 و تنظیمات اندروید

## پیشنهاد برای توسعه اسکنر حرفه‌ای

توسعه باید در شاخه‌ای جدا انجام شود:

```bash
git checkout -b feature/advanced-template-scanner
```

ماژول‌های پیشنهادی:

- `scanner-core`: پردازش تصویر و تشخیص جدول
- `scanner-ocr`: OCR آفلاین فارسی
- `template-profile`: پروفایل و مختصات قالب‌ها
- `visual-editor`: ویرایشگر بصری نواحی
- `dynamic-form`: فرم مشخصات پویا
- `pdf-composer`: تولید PDF با iText 7
- `scanner-tests`: آزمون تصویری ۲۲ قالب
