package com.example.servicebestpractice;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadThread extends Thread
{
    String downloadUrl;
    String url;
    int i;
    long block;
    long start=0;
    long end;
    public DownloadThread(String url,int i)
    {
        this.downloadUrl = url;
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
                }

                response.body().close();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
