package com.yinglun.lfrendercoredemo1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

/**
 * author:vojin
 * 创建时间：2022/7/20
 * Describe:
 */
public class DepthGaussianBlur {
private final static String TAG = "DepthGaussianBlur";

    public static Bitmap GaussBlur(Context context,Bitmap src,float blurRadius,float scale){
        Bitmap depthBitmap = getDepth(src);
        Bitmap blurBitmap = blurBitmap(context,depthBitmap,blurRadius,scale);
        depthBitmap.recycle();
        Bitmap newBitmap = mergeBitmap(src,blurBitmap);
        src.recycle();
        return newBitmap;
    }

    /**
     * @param image         需要模糊的图片
     * @param blurRadius    模糊的半径（1-25之间）
     * @return 模糊处理后的Bitmap
     */
    public static Bitmap blurBitmap(Context context, Bitmap image, float blurRadius,float scale ) {
        int outWidth = (int)(image.getWidth()*scale);
        int outHeight = (int) (image.getHeight()*scale);
        // 将缩小后的图片做为预渲染的图片
        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, outWidth, outHeight, false);
        // 创建一张渲染后的输出图片
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);
        // 创建RenderScript内核对象
        RenderScript rs = RenderScript.create(context);
        // 创建一个模糊效果的RenderScript的工具对象
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        // 由于RenderScript并没有使用VM来分配内存,所以需要使用Allocation类来创建和分配内存空间
        // 创建Allocation对象的时候其实内存是空的,需要使用copyTo()将数据填充进去
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
        // 设置渲染的模糊程度, 25f是最大模糊度
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            blurScript.setRadius(blurRadius);
        }
        // 设置blurScript对象的输入内存
        blurScript.setInput(tmpIn);
        // 将输出数据保存到输出内存中
        blurScript.forEach(tmpOut);
        // 将数据填充到Allocation中
        tmpOut.copyTo(outputBitmap);
        inputBitmap.recycle();
        rs.destroy();
        Bitmap result = Bitmap.createScaledBitmap(outputBitmap,image.getWidth(),image.getHeight(),false);
        outputBitmap.recycle();
        return result;
    }

    /**
     * 把两个位图覆盖合成为一个位图，以底层位图的长宽为基准
     * @param srcBitmap 原图在底面
     * @param depthBitmap 深度图覆盖在上面
     * @return
     */
    public static Bitmap mergeBitmap(Bitmap srcBitmap, Bitmap depthBitmap) {

        if (srcBitmap == null || srcBitmap.isRecycled()
                || depthBitmap == null || depthBitmap.isRecycled()) {
            Log.e(TAG, "backBitmap=" + srcBitmap + ";frontBitmap=" + depthBitmap);
            return null;
        }
        Bitmap bitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Rect baseRect  = new Rect(srcBitmap.getWidth()/2, 0, srcBitmap.getWidth(), srcBitmap.getHeight());
        //从原图右半部分覆盖
        Rect frontRect = new Rect(0, 0, depthBitmap.getWidth(), depthBitmap.getHeight());
        canvas.drawBitmap(depthBitmap, frontRect, baseRect, null);
        return bitmap;
    }

    /**
     *  获取深度图
     * @param source 原图
     * @return 返回深度图
     */
    public static Bitmap getDepth(Bitmap source) {
        return Bitmap.createBitmap(source, (int)(source.getWidth()*0.5), 0, (int)(source.getWidth()*0.5), source.getHeight());
    }

    public  static Bitmap scaleBitmap(Bitmap bm,float scale){
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        // 得到新的图片
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix,true);
    }
}
