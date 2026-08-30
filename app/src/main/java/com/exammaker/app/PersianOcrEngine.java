package com.exammaker.app;

import android.content.Context;
import android.graphics.Bitmap;
import com.googlecode.tesseract.android.TessBaseAPI;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Offline Persian OCR backed by Tesseract LSTM. */
public final class PersianOcrEngine implements Closeable {
    private final Context context;
    private TessBaseAPI api;

    public PersianOcrEngine(Context context) { this.context = context.getApplicationContext(); }

    public synchronized void initialize() throws IOException {
        if (api != null) return;
        File base = new File(context.getFilesDir(), "tesseract");
        File data = new File(base, "tessdata");
        if (!data.exists() && !data.mkdirs()) throw new IOException("Cannot create OCR data directory");
        File model = new File(data, "fas.traineddata");
        if (!model.exists() || model.length() < 100_000) copyAsset("tessdata/fas.traineddata", model);
        api = new TessBaseAPI();
        if (!api.init(base.getAbsolutePath(), "fas", TessBaseAPI.OEM_LSTM_ONLY)) {
            api.recycle(); api = null; throw new IOException("Persian OCR initialization failed");
        }
        api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
        api.setVariable("preserve_interword_spaces", "1");
    }

    public synchronized OcrResult recognize(Bitmap bitmap) throws IOException {
        initialize();
        long started = System.currentTimeMillis();
        api.setImage(bitmap);
        String text = api.getUTF8Text();
        int confidence = api.meanConfidence();
        api.clear();
        return new OcrResult(text == null ? "" : normalize(text), confidence,
                extractFields(text == null ? "" : text), System.currentTimeMillis() - started);
    }

    private Map<String,String> extractFields(String text) {
        LinkedHashMap<String,String> fields = new LinkedHashMap<>();
        match(fields, "courseName", text, "(?:نام\\s*درس|درس)\\s*[:：]?\\s*([^\\n]{2,60})");
        match(fields, "teacherName", text, "(?:نام\\s*(?:استاد|دبیر|مدرس)|استاد|دبیر)\\s*[:：]?\\s*([^\\n]{2,60})");
        match(fields, "examDate", text, "(?:تاریخ(?:\\s*امتحان)?)\\s*[:：]?\\s*([۰-۹0-9/\\-]{4,20})");
        match(fields, "examTime", text, "(?:ساعت(?:\\s*(?:برگزاری|امتحان))?)\\s*[:：]?\\s*([۰-۹0-9:]{2,12})");
        match(fields, "duration", text, "(?:مدت(?:\\s*(?:زمان|امتحان))?)\\s*[:：]?\\s*([۰-۹0-9]{1,4})");
        match(fields, "grade", text, "(?:پایه|رشته|گروه\\s*آموزشی)\\s*[:：]?\\s*([^\\n]{2,60})");
        return fields;
    }

    private void match(Map<String,String> out, String key, String text, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
        if (m.find()) out.put(key, m.group(1).trim());
    }

    private String normalize(String s) {
        return s.replace('ي','ی').replace('ك','ک').replaceAll("[ \\t]+", " ").trim();
    }

    private void copyAsset(String asset, File target) throws IOException {
        try (InputStream in=context.getAssets().open(asset); OutputStream out=new FileOutputStream(target)) {
            byte[] b=new byte[16*1024]; int n; while((n=in.read(b))!=-1) out.write(b,0,n);
        }
    }

    @Override public synchronized void close() { if(api!=null){ api.recycle(); api=null; } }

    public static final class OcrResult {
        public final String text; public final int confidence; public final Map<String,String> fields; public final long elapsedMs;
        OcrResult(String text,int confidence,Map<String,String> fields,long elapsedMs){this.text=text;this.confidence=confidence;this.fields=fields;this.elapsedMs=elapsedMs;}
    }
}
