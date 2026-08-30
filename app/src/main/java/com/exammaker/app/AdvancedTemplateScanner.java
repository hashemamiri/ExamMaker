package com.exammaker.app;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Offline geometry scanner for exam PDF templates.
 * Uses multi-threshold voting, run-length analysis and grid inference.
 * OCR/semantic labels are intentionally handled by template profiles.
 */
public final class AdvancedTemplateScanner {
    private AdvancedTemplateScanner() {}

    public static final class Result {
        public final int[] horizontalLines;
        public final int[] verticalLines;
        public final Rect questionArea;
        public final Rect scoreColumn;
        public final float confidence;
        public final boolean freeFormBody;

        Result(int[] h, int[] v, Rect area, Rect score, float confidence, boolean freeForm) {
            this.horizontalLines = h;
            this.verticalLines = v;
            this.questionArea = area;
            this.scoreColumn = score;
            this.confidence = confidence;
            this.freeFormBody = freeForm;
        }
    }

    public static Result analyze(Bitmap source) {
        final int w = source.getWidth(), h = source.getHeight();
        // Downsample only for classification; coordinates remain in source space.
        int sx = Math.max(1, w / 900), sy = Math.max(1, h / 1300);
        int[][] thresholds = {{85, 3}, {120, 2}, {165, 1}};
        float[] hScore = new float[h];
        float[] vScore = new float[w];

        for (int[] pass : thresholds) {
            int threshold = pass[0], weight = pass[1];
            for (int y = 8; y < h - 8; y += sy) {
                int dark = 0, longest = 0, run = 0, samples = 0;
                for (int x = 8; x < w - 8; x += sx) {
                    samples++;
                    if (gray(source.getPixel(x, y)) < threshold) {
                        dark++; run++; if (run > longest) longest = run;
                    } else run = 0;
                }
                float density = dark / (float)Math.max(1, samples);
                float continuity = longest / (float)Math.max(1, samples);
                if (density > .20f && continuity > .34f)
                    for (int yy = y; yy < Math.min(h, y + sy); yy++) hScore[yy] += weight * (density + continuity);
            }
            for (int x = 8; x < w - 8; x += sx) {
                int dark = 0, longest = 0, run = 0, samples = 0;
                for (int y = 8; y < h - 8; y += sy) {
                    samples++;
                    if (gray(source.getPixel(x, y)) < threshold) {
                        dark++; run++; if (run > longest) longest = run;
                    } else run = 0;
                }
                float density = dark / (float)Math.max(1, samples);
                float continuity = longest / (float)Math.max(1, samples);
                if (density > .16f && continuity > .28f)
                    for (int xx = x; xx < Math.min(w, x + sx); xx++) vScore[xx] += weight * (density + continuity);
            }
        }

        int[] horizontal = groupedPeaks(hScore, 1.0f, Math.max(3, sy * 2));
        int[] vertical = groupedPeaks(vScore, .75f, Math.max(3, sx * 2));
        horizontal = removeNearDuplicates(horizontal, Math.max(5, h / 300));
        vertical = removeNearDuplicates(vertical, Math.max(5, w / 220));

        // Infer the body from the largest bounded vertical gap below the header.
        int bestTop = Math.round(h * .20f), bestBottom = Math.round(h * .92f), bestGap = -1;
        for (int i = 0; i + 1 < horizontal.length; i++) {
            int top = horizontal[i], bottom = horizontal[i + 1], gap = bottom - top;
            if (top > h * .16f && bottom < h * .97f && gap > bestGap) {
                bestTop = top; bestBottom = bottom; bestGap = gap;
            }
        }
        boolean freeForm = bestGap > h * .28f;

        int left = Math.round(w * .04f), right = Math.round(w * .96f);
        List<Integer> bodyVerticals = new ArrayList<>();
        for (int x : vertical) if (x > w * .02f && x < w * .98f) bodyVerticals.add(x);
        if (bodyVerticals.size() >= 2) {
            left = bodyVerticals.get(0);
            right = bodyVerticals.get(bodyVerticals.size() - 1);
        }

        int scoreRight = left + Math.round((right - left) * .09f);
        for (int x : vertical) {
            if (x > left + (right-left)*.035f && x < left + (right-left)*.20f) {
                scoreRight = x; break;
            }
        }
        Rect area = new Rect(scoreRight, bestTop, right, bestBottom);
        Rect score = new Rect(left, bestTop, scoreRight, bestBottom);

        float lineQuality = Math.min(1f, horizontal.length / 10f);
        float columnQuality = Math.min(1f, vertical.length / 5f);
        float bounded = (bestGap > 0 && right > left) ? 1f : 0f;
        float confidence = .45f * lineQuality + .35f * columnQuality + .20f * bounded;
        return new Result(horizontal, vertical, area, score, confidence, freeForm);
    }

    private static int gray(int c) {
        return (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000;
    }

    private static int[] groupedPeaks(float[] score, float minimum, int bridge) {
        ArrayList<Integer> centers = new ArrayList<>();
        int start = -1, last = -1; float weighted = 0, total = 0;
        for (int i = 0; i < score.length; i++) {
            if (score[i] >= minimum) {
                if (start < 0 || i - last > bridge) {
                    if (start >= 0) centers.add(Math.round(weighted / Math.max(.001f, total)));
                    start = i; weighted = total = 0;
                }
                weighted += i * score[i]; total += score[i]; last = i;
            }
        }
        if (start >= 0) centers.add(Math.round(weighted / Math.max(.001f, total)));
        int[] out = new int[centers.size()]; for (int i=0;i<out.length;i++) out[i]=centers.get(i); return out;
    }

    private static int[] removeNearDuplicates(int[] values, int distance) {
        if (values.length < 2) return values;
        ArrayList<Integer> out = new ArrayList<>();
        int sum = values[0], count = 1, last = values[0];
        for (int i=1;i<values.length;i++) {
            if (values[i]-last <= distance) { sum += values[i]; count++; }
            else { out.add(Math.round(sum/(float)count)); sum=values[i]; count=1; }
            last=values[i];
        }
        out.add(Math.round(sum/(float)count));
        int[] r=new int[out.size()];for(int i=0;i<r.length;i++)r[i]=out.get(i);return r;
    }
}
