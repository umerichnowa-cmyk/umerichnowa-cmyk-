package com.crashstat.mobile;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.view.WindowManager;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScreenReaderService extends Service {
    static final String ACTION_START = "com.crashstat.mobile.START_SCREEN_READER";
    static final String ACTION_STOP = "com.crashstat.mobile.STOP_SCREEN_READER";
    static final String ACTION_ROUND_CAPTURED = "com.crashstat.mobile.ROUND_CAPTURED";
    static final String ACTION_READER_STATUS = "com.crashstat.mobile.READER_STATUS";
    static final String EXTRA_RESULT_CODE = "result_code";
    static final String EXTRA_RESULT_DATA = "result_data";
    static final String EXTRA_VALUE = "value";
    static final String EXTRA_ACTIVE = "active";

    private static final String CHANNEL_ID = "crashstat_screen_reader";
    private static final int NOTIFICATION_ID = 3107;
    private static final long OCR_INTERVAL_MS = 900L;
    private static final long STABLE_DURATION_MS = 1500L;
    private static final Pattern MULTIPLIER_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d{1,5}(?:[\\.,]\\d{1,2}))\\s*[xX×]");

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread captureThread;
    private Handler captureHandler;
    private TextRecognizer recognizer;
    private final AtomicBoolean processing = new AtomicBoolean(false);

    private long lastFrameAt = 0L;
    private Double lastCandidate = null;
    private int sameCandidateCount = 0;
    private int missingCount = 0;
    private long stableSince = 0L;
    private boolean plateauRecorded = false;
    private int captureWidth;
    private int captureHeight;
    private int densityDpi;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        captureThread = new HandlerThread("CrashStatScreenOCR");
        captureThread.start();
        captureHandler = new Handler(captureThread.getLooper());
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopCapture();
            return START_NOT_STICKY;
        }
        if (!ACTION_START.equals(action) || mediaProjection != null) return START_NOT_STICKY;

        startAsForeground("Initialisation de la lecture de l’écran…");

        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED);
        Intent resultData;
        if (Build.VERSION.SDK_INT >= 33) {
            resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class);
        } else {
            resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
        }

        if (resultCode != Activity.RESULT_OK || resultData == null) {
            stopCapture();
            return START_NOT_STICKY;
        }

        try {
            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = manager.getMediaProjection(resultCode, resultData);
            if (mediaProjection == null) {
                stopCapture();
                return START_NOT_STICKY;
            }
            mediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    stopCapture();
                }
            }, captureHandler);
            startVirtualDisplay();
            RoundStore.setReaderActive(this, true);
            broadcastStatus(true);
            updateNotification("Lecture active • recherche du multiplicateur final");
        } catch (Exception error) {
            stopCapture();
        }
        return START_NOT_STICKY;
    }

    private void startVirtualDisplay() {
        Rect bounds;
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= 30) {
            bounds = windowManager.getMaximumWindowMetrics().getBounds();
            densityDpi = getResources().getConfiguration().densityDpi;
        } else {
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            windowManager.getDefaultDisplay().getRealMetrics(metrics);
            bounds = new Rect(0, 0, metrics.widthPixels, metrics.heightPixels);
            densityDpi = metrics.densityDpi;
        }

        int fullWidth = Math.max(1, bounds.width());
        int fullHeight = Math.max(1, bounds.height());
        double scale = Math.min(1.0, 540.0 / fullWidth);
        captureWidth = Math.max(320, (int) Math.round(fullWidth * scale));
        captureHeight = Math.max(480, (int) Math.round(fullHeight * scale));

        imageReader = ImageReader.newInstance(
                captureWidth, captureHeight, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(this::onImageAvailable, captureHandler);

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "CrashStatScreenReader",
                captureWidth,
                captureHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                captureHandler);
    }

    private void onImageAvailable(ImageReader reader) {
        long now = System.currentTimeMillis();
        if (now - lastFrameAt < OCR_INTERVAL_MS || !processing.compareAndSet(false, true)) {
            Image skipped = reader.acquireLatestImage();
            if (skipped != null) skipped.close();
            return;
        }
        lastFrameAt = now;

        Image image = reader.acquireLatestImage();
        if (image == null) {
            processing.set(false);
            return;
        }

        Bitmap crop = null;
        try {
            crop = cropGameRegion(image);
        } catch (Exception ignored) {
        } finally {
            image.close();
        }

        if (crop == null) {
            processing.set(false);
            return;
        }

        Bitmap bitmapForOcr = crop;
        InputImage inputImage = InputImage.fromBitmap(bitmapForOcr, 0);
        recognizer.process(inputImage)
                .addOnSuccessListener(this::handleRecognizedText)
                .addOnFailureListener(error -> handleMissingCandidate())
                .addOnCompleteListener(task -> {
                    bitmapForOcr.recycle();
                    processing.set(false);
                });
    }

    private Bitmap cropGameRegion(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * captureWidth;
        int paddedWidth = captureWidth + rowPadding / pixelStride;

        Bitmap padded = Bitmap.createBitmap(paddedWidth, captureHeight, Bitmap.Config.ARGB_8888);
        padded.copyPixelsFromBuffer(buffer);
        Bitmap screen = Bitmap.createBitmap(padded, 0, 0, captureWidth, captureHeight);
        padded.recycle();

        int top = Math.max(0, (int) (captureHeight * 0.47));
        int bottom = Math.min(captureHeight, (int) (captureHeight * 0.84));
        Bitmap crop = Bitmap.createBitmap(screen, 0, top, captureWidth, bottom - top);
        screen.recycle();
        return crop;
    }

    private void handleRecognizedText(Text result) {
        Double bestValue = null;
        long bestScore = -1L;

        for (Text.TextBlock block : result.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Matcher matcher = MULTIPLIER_PATTERN.matcher(line.getText());
                while (matcher.find()) {
                    try {
                        double value = Double.parseDouble(matcher.group(1).replace(',', '.'));
                        if (value < 1.0 || value > 100000.0) continue;
                        Rect box = line.getBoundingBox();
                        long score = box == null ? 1L : (long) box.width() * box.height();
                        if (score > bestScore) {
                            bestScore = score;
                            bestValue = value;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        if (bestValue == null) handleMissingCandidate();
        else handleCandidate(bestValue);
    }

    private void handleCandidate(double candidate) {
        long now = System.currentTimeMillis();
        missingCount = 0;

        if (lastCandidate != null && Math.abs(lastCandidate - candidate) < 0.006) {
            sameCandidateCount++;
        } else {
            lastCandidate = candidate;
            sameCandidateCount = 1;
            stableSince = now;
            plateauRecorded = false;
        }

        if (!plateauRecorded
                && sameCandidateCount >= 3
                && now - stableSince >= STABLE_DURATION_MS) {
            plateauRecorded = true;
            int automaticCount = RoundStore.appendAutomatic(this, candidate, now);
            Intent captured = new Intent(ACTION_ROUND_CAPTURED)
                    .setPackage(getPackageName())
                    .putExtra(EXTRA_VALUE, candidate);
            sendBroadcast(captured);
            updateNotification(String.format(Locale.FRANCE,
                    "Dernière manche détectée : %.2fx • %d automatiques",
                    candidate, automaticCount));
        }
    }

    private void handleMissingCandidate() {
        missingCount++;
        if (missingCount >= 2) {
            lastCandidate = null;
            sameCandidateCount = 0;
            stableSince = 0L;
            plateauRecorded = false;
        }
    }

    private void startAsForeground(String message) {
        Notification notification = buildNotification(message);
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateNotification(String message) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, buildNotification(message));
    }

    private Notification buildNotification(String message) {
        Intent openIntent = new Intent(this, ScreenTrainingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(android.R.drawable.presence_online)
                .setContentTitle("CrashStat • lecture d’écran")
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Lecture d’écran CrashStat",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Indique que CrashStat lit la zone du jeu avec votre autorisation.");
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }

    private void broadcastStatus(boolean active) {
        sendBroadcast(new Intent(ACTION_READER_STATUS)
                .setPackage(getPackageName())
                .putExtra(EXTRA_ACTIVE, active));
    }

    private synchronized void stopCapture() {
        RoundStore.setReaderActive(this, false);
        broadcastStatus(false);

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (mediaProjection != null) {
            try {
                mediaProjection.stop();
            } catch (Exception ignored) {
            }
            mediaProjection = null;
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (recognizer != null) recognizer.close();
        if (captureThread != null) captureThread.quitSafely();
        RoundStore.setReaderActive(this, false);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
