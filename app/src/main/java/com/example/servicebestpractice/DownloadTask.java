package com.example.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Integer, Integer> {
    String downloadUrl;
    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;


    private DownloadListener listener;

    private boolean isCanceled = false;

    private boolean isPaused = false;

    private int lastProgress;

    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }



    private class DownloadThread extends Thread
    {
        String url;
        int i;
        long block;
        long start=0;
        long end;
        public DownloadThread(String url,int i)
        {
            this.url = url;
            this.i = i;
        }
        @Override
        public void run()
        {
            Log.d("thread", "thread : "+i);
            try {
            File file = null;
            long downloadedLength = 0; // 记录已下载的文件长度
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory + fileName);
                InputStream is = null;
                RandomAccessFile savedFile = null;
            if (file.exists()) {
                downloadedLength = file.length();
            }
            long contentLength = 0;

                contentLength = getContentLength(downloadUrl);

                block = contentLength/3;
                start = block*i;
                end = start+block*(i+1);
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        // 断点下载，指定从哪个字节开始下载
                        .addHeader("RANGE", "bytes=" + start + end)
                        .url(downloadUrl)
                        .build();
                Response response = client.newCall(request).execute();
                if (response != null) {
                    is = response.body().byteStream();
                    savedFile = new RandomAccessFile(file, "rw");
                    savedFile.seek(downloadedLength); // 跳过已下载的字节
                    byte[] b = new byte[1024];
                    int total = 0;
                    int len;
                    while ((len = is.read(b)) != -1) {
                            total += len;
                            savedFile.write(b, 0, len);
                            // 计算已下载的百分比
                            int progress = (int) ((total + downloadedLength) * 100 / contentLength);
                            publishProgress(progress);
                    }

                    response.body().close();

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }
    @Override
    protected Integer doInBackground(String... params) {
        int threadCount=3;
        downloadUrl = params[0];
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] =   new DownloadThread(downloadUrl,i);
            threads[i].start();
        }
      // new DownloadThread("http://iotdown.mayitek.com/1522029924/2372088/0860b830-3a19-4d10-b060-e25aea2d6144.zip").start();
        //response.body().close();
        return TYPE_SUCCESS;
    }




    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress > lastProgress) {
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer status) {
        switch (status) {
          /*  case TYPE_SUCCESS:
                listener.onSuccess();
                break;*/
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
            default:
                break;
        }
    }

    public void pauseDownload() {
        isPaused = true;
    }


    public void cancelDownload() {
        isCanceled = true;
    }

    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }

}