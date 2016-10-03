package com.editor.com.images2video.controller;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.editor.com.images2video.controller.callback.IConvertCallback;
import com.editor.com.images2video.controller.callback.ILoadCallback;
import com.editor.com.images2video.controller.model.AudioFormat;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Umair_Adil on 03/10/2016.
 */

public class VideoResizer {


    private static String TAG = VideoResizer.class.getSimpleName();

    private static boolean loaded;

    private Context context;
    private File video;
    private IConvertCallback callback;

    private VideoResizer(Context context) {
        this.context = context;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void load(Context context, final ILoadCallback callback) {
        try {
            FFmpeg.getInstance(context).loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onStart() {

                }

                @Override
                public void onSuccess() {
                    loaded = true;
                    callback.onSuccess();
                }

                @Override
                public void onFailure() {
                    loaded = false;
                    callback.onFailure(new Exception("Failed to loaded FFmpeg lib"));
                }

                @Override
                public void onFinish() {

                }
            });
        } catch (FFmpegNotSupportedException e) {
            loaded = false;
            callback.onFailure(e);
        } catch (Exception e) {
            loaded = false;
            callback.onFailure(e);
        }
    }

    public static VideoResizer with(Context context) {
        return new VideoResizer(context);
    }

    public VideoResizer setFile(File originalFiles) {
        this.video = originalFiles;
        return this;
    }

    public VideoResizer setCallback(IConvertCallback callback) {
        this.callback = callback;
        return this;
    }

    public void resize() {
        if (!isLoaded()) {
            callback.onFailure(new Exception("FFmpeg not loaded"));
            return;
        }

        if (video == null || !video.exists()) {
            callback.onFailure(new IOException("File not exists"));
            return;
        }
        if (!video.canRead()) {
            callback.onFailure(new IOException("Can't read the file. Missing permission?"));
            return;
        }

        final File outputLocation = getConvertedFile();


        //final String[] cmd = new String[]{"-i", video.getPath(), "-vf", "scale=320:240",outputLocation.getPath(),"-hide_banner"};
        final String[] cmd = new String[]{"-i", video.getPath()
                , "-vf"
                ,"scale=1920:1080"
                , outputLocation.getPath()
                , "-hide_banner"};

        try {
            FFmpeg.getInstance(context).execute(cmd, new ExecuteBinaryResponseHandler() {
                @Override
                public void onStart() {
                }

                @Override
                public void onProgress(String message) {
                    Log.i(TAG, "onProgress: " + message);
                }

                @Override
                public void onSuccess(String message) {
                    refreshGallery(outputLocation.getPath());
                    callback.onSuccess(outputLocation);

                }

                @Override
                public void onFailure(String message) {
                    if (outputLocation.exists()) {
                        outputLocation.delete();
                    }
                    callback.onFailure(new IOException(message));
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    private static File getConvertedFile() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String timeStamp = simpleDateFormat.format(new Date());

        /*String[] f = imageName.split("\\.");
        String filePath = timeStamp + imageName.replace(f[f.length - 1], "mp4");*/

        String path = Environment.getExternalStorageDirectory() + File.separator + "MyImages" + File.separator + timeStamp + "_resized.mp4";
        Log.i(TAG, "Output Path: " + path);
        return new File(path);
    }

    private void refreshGallery(String path) {

        File file = getConvertedFile();
        Log.i(TAG, "Path: " + file.getAbsolutePath());

        try {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            context.sendBroadcast(mediaScanIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }

       /* try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(), file.getName(), null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));*/
    }
}