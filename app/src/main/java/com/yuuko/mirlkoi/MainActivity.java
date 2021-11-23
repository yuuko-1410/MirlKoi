package com.yuuko.mirlkoi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.service.controls.actions.FloatAction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.PathUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import okhttp3.*;
import org.jetbrains.annotations.Nullable;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Permission;
import java.util.Objects;

import static com.bumptech.glide.load.resource.bitmap.TransformationUtils.fitCenter;
import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    FloatingActionButton mButton;
    TextView text001;
    boolean issetu = false;
    static String url;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView= (ImageView) findViewById(R.id.Image);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        submit();
        mButton = (FloatingActionButton) findViewById(R.id.FaButton);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });

    }
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download:
                saveImgToLocal(this,url);
                return true;
            case R.id.setumoshi:
                boolean isChecked = !item.isChecked();
                item.setChecked(isChecked);
                if (item.isChecked()){
                    issetu = true;
                    Toast.makeText(MainActivity.this, "涩图模式开启，咪啪~", Toast.LENGTH_SHORT).show();
                }else{
                    issetu = false;
                }
                return true;
            case R.id.info:
                Toast.makeText(MainActivity.this, "你、你点哪里呢？？？", Toast.LENGTH_SHORT).show();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    private void submit(){
        //创建一个子线程进行逻辑处理
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url;
                    if(issetu){
                        url = "https://iw233.cn/API/Ghs.php";
                    }else{
                        url = "https://iw233.cn/API/Random.php";
                    }
                    //get方式开始
                    OkHttpClient client =new OkHttpClient();
                    Request request=new Request.Builder().url(url).build();
                  //使用newcall()创建一个call对象，并调用
                    Response response= client.newCall(request).execute();
                 //得到返回数据
                    String data= String.valueOf(response.request().url());
                //将返回数据添加到ui页面上
                    showData(data);
                    MainActivity.url = data;
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private void showData(final String data){
        //切换回主线程处理，安卓不允许在子线程设置ui界面，所以需要回到主线程
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Glide.with(MainActivity.this)
                        .load(data)
                        .into(imageView);
            }
        });
    }
    /**
     * 下载到本地
     * @param context 上下文
     * @param url 网络图
     */
    private void saveImgToLocal(Context context, String url) {
        //如果是网络图片，抠图的结果，需要先保存到本地
        Glide.with(context)
                .downloadOnly()
                .load(url)
                .listener(new RequestListener<File>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                        //Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                        //Toast.makeText(context, "下载成功", Toast.LENGTH_SHORT).show();
                        saveToAlbum(context, resource.getAbsolutePath());
                        return false;
                    }
                })
                .preload();
    }
    /**
     * 保存到相册中
     * @param context 上下文
     * @param srcPath 网络图保存到本地的缓存文件路径
     */
    private void saveToAlbum(Context context, String srcPath) {
        File externalFileRootDir = getExternalFilesDir(null);
        do {
            externalFileRootDir = Objects.requireNonNull(externalFileRootDir).getParentFile();
        } while (Objects.requireNonNull(externalFileRootDir).getAbsolutePath().contains("/Android"));

        String saveDir = Objects.requireNonNull(externalFileRootDir).getAbsolutePath();
        String savePath = saveDir + "/" + Environment.DIRECTORY_DCIM + "/" + System.currentTimeMillis() + ".png";

        String dcimPath = String.valueOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        File file = new File(dcimPath, "content_" + System.currentTimeMillis() + ".png");
        FileInputStream fileInputStream;
        FileOutputStream fileOutputStream;
        boolean isCopySuccess = false;

        try{

            fileInputStream = new FileInputStream(srcPath);
            fileOutputStream = new FileOutputStream(savePath);
            byte[] buffer = new byte[1024];
            int byteRead;

            while (-1 != (byteRead = fileInputStream.read(buffer))) {
                fileOutputStream.write(buffer, 0, byteRead);
            }
            fileInputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            isCopySuccess = true;

        }catch (Exception e){
        }finally {
        }
        if (isCopySuccess) {
            //发送广播通知
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getAbsolutePath())));
            Toast.makeText(context, "下载成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "下载失败" , Toast.LENGTH_SHORT).show();
        }
    }

}