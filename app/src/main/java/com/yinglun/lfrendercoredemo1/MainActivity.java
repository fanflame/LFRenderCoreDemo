package com.yinglun.lfrendercoredemo1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private ImageGLSurfaceView playView;
    boolean lr = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playView = new ImageGLSurfaceView(this);

        FrameLayout placeHolder = findViewById(R.id.placeholder);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        placeHolder.addView(playView, params);
        updatePhoto();

        playView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updatePhoto();
            }
        });
    }


    private void updatePhoto() {
        if(!lr) {
            final Bitmap sourceBitmap = getImageFromAssetsFile(this, "Pic/" + "example.jpg");
            Matrix matrix = new Matrix();
            matrix.postScale(1, -1);
            final Bitmap mBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, true);

            playView.setBitmap(mBitmap);
            playView.setFormat("2dz_m");
            playView.setDepth(0.03f,0.5f);
            playView.requestRender();
            lr = true;
        }else{
            final Bitmap sourceBitmap = getImageFromAssetsFile(this, "Pic/" + "lr.png");
            Matrix matrix = new Matrix();
            matrix.postScale(1, -1);
            final Bitmap mBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, true);

            playView.setBitmap(mBitmap);
            playView.setFormat("sbs");
            playView.requestRender();
            lr = false;
        }
    }

    public Bitmap getImageFromAssetsFile(Context context, String fileName)
    {
        Bitmap image = null;
        AssetManager am = context.getResources().getAssets();
        try
        {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return image;

    }
}