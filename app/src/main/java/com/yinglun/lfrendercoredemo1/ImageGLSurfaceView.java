/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yinglun.lfrendercoredemo1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;

import com.yinglun.lf3drendercoreforandroid.ILF3DInterface;
import com.yinglun.lf3drendercoreforandroid.LFRenderCore;
import com.yinglun.lfrendercoredemo1.Filter.NormalFilter;
import com.yinglun.lfrendercoredemo1.Filter.OpenGLUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class ImageGLSurfaceView extends GLSurfaceView {
    private static final String TAG = "ImageGLSurfaceView";

    private final ImageRenderer renderer;

    private Context mContext;

    public ImageGLSurfaceView(
            Context context) {
        super(context);

        mContext = context;

        setEGLContextClientVersion(3);
        renderer = new ImageRenderer();

        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setBitmap(Bitmap bitmap) {
        renderer.setBitmap(bitmap);
    }

    public void setFormat(String format) {
        renderer.setFormat(format);
    }

    public void setDepth(float fDPt, float d3) {
        renderer.setDepth(fDPt, d3);
    }

    private class ImageRenderer implements Renderer {
        private final NormalFilter oesFilter;
        private int textId = -1;
        private int mWidth;
        private int mHeight;

        private String format = "sbs";
        private boolean formatChanged;

        private Bitmap mBitmap;

        float fDpt = 0.03f;
        float d3 = 0.5f;

        public ImageRenderer() {
            Log.d(TAG, "VideoRenderer()");

            oesFilter = new NormalFilter();
        }


        @Override
        public synchronized void onSurfaceCreated(GL10 gl, EGLConfig config) {
            textId = OpenGLUtils.createTexture();

            oesFilter.init();
            oesFilter.inputTexture = textId;

//            ILF3DInterface ilf3DInterface = new ILF3DInterface();
//            ilf3DInterface.deviceType = 0;
//            ilf3DInterface.graphicsType = 11;
//            LFRenderCore.initNativeGraphic(ilf3DInterface);
            LFRenderCore.setGLVersion(2);

            /// <summary>
            /// 设置3D格式
            /// </summary>
            /// <param name="format">
            /// 左右格式："sbs","3D_L_R"；
            /// 上下格式："ubd","3D_U_D"；
            /// 2d+深度："2dz","2D_Z"；
            ///2dz+深度转多视点："2dz_m","2D_Z_M"；
            /// 多宫格："mxn",例2x1,3x3</param>
            LFRenderCore.set3DType(format);
            Log.d(TAG, "onSurfaceCreated");
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            mWidth = width;
            mHeight = height;
            //preview filter surfaceSize changed
            oesFilter.setFrameSize(width, height);
            LFRenderCore.setViewSize(width, height);
            LFRenderCore.setTexture(oesFilter.getFrameTexture(), width, height);
            Log.d(TAG, "onSurfaceChanged");
        }

        @SuppressLint("LongLogTag")
        @Override
        public void onDrawFrame(GL10 gl) {
            if (formatChanged) {
                LFRenderCore.set3DType(format);
                formatChanged = false;
            }
            if (mBitmap != null && !mBitmap.isRecycled()) {
                Bitmap newBitmap = Bitmap.createBitmap(mBitmap);
                if (format.equals("2dz")) {
                    LFRenderCore.setDepth(fDpt, d3);
                    Bitmap blurBitmap = DepthGaussianBlur.GaussBlur(mContext, newBitmap, 25f, 1f);
                    loadBitmap(blurBitmap);
                }else{
                    loadBitmap(newBitmap);
                }


                //rend to fbo
                oesFilter.drawFrameBuffer();
                //rend to screen
                GLES30.glViewport(0, 0, mWidth, mHeight);

                LFRenderCore.draw();
            }

        }

        private void loadBitmap(Bitmap bmp) {
            if (textId == -1) {
                return;
            }
            if (bmp != null && !bmp.isRecycled()) {
                //绑定纹理
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textId);
                //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
                GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
                //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
                GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
                //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
                GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
                //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
                GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
                //根据以上指定的参数，生成一个2D纹理
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);
            }
        }

        public void setBitmap(Bitmap bitmap) {

            if (this.mBitmap != null && !this.mBitmap.isRecycled()) {
                this.mBitmap.recycle();
            }
            this.mBitmap = bitmap;
        }

        public void setFormat(String format) {
            this.format = format;
            formatChanged = true;
        }

        public void setDepth(float fDpt, float dD3) {
            this.fDpt = fDpt;
            this.d3 = d3;
        }
    }
}
