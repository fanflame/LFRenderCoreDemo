package com.yinglun.lfrendercoredemo1.Filter;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Locale;

/**
 * @author vojin
 */
public class OpenGLUtils {

    private static final String TAG = "OpenGLUtils";

    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShader == 0) {
            return 0;
        }
        int program = GLES30.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        GLES30.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES30.glAttachShader(program, fragmentShader);
        checkGlError("glAttachShader");
        GLES30.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES30.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES30.glGetProgramInfoLog(program));
            GLES30.glDeleteProgram(program);
            program = 0;
        }
        if (vertexShader > 0) {
            GLES30.glDetachShader(program, vertexShader);
            GLES30.glDeleteShader(vertexShader);
        }
        if (fragmentShader > 0) {
            GLES30.glDetachShader(program, fragmentShader);
            GLES30.glDeleteShader(fragmentShader);
        }
        return program;
    }

    public static int loadShader(int shaderType, String source) {
        int shader = GLES30.glCreateShader(shaderType);
        checkGlError(String.format(Locale.getDefault(), "glCreateShader type=%d", shaderType));
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader $shaderType:");
            Log.e(TAG, " " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    static void createFrameBuffer(int[] frameBuffer, int[] frameBufferTexture, int width, int height) {
        GLES30.glGenFramebuffers(frameBuffer.length, frameBuffer, 0);
        GLES30.glGenTextures(frameBufferTexture.length, frameBufferTexture, 0);
        for (int i = 0; i < frameBufferTexture.length; i++) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, frameBufferTexture[i]);
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBuffer[i]);
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, frameBufferTexture[i], 0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        }
        checkGlError("createFrameBuffer");
    }

    public static int createExternalTexture() {
        int[] texId = new int[1];
        GLES30.glGenTextures(1, IntBuffer.wrap(texId));
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId[0]);
        GLES30.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        checkGlError("createExternalTexture");
        return texId[0];
    }

    public static int createTexture() {
        int[] texId = new int[1];
        GLES30.glGenTextures(1, IntBuffer.wrap(texId));
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId[0]);
        GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        checkGlError("createExternalTexture");
        return texId[0];
    }

    public static FloatBuffer createBuffer(float[] data) {
        return (FloatBuffer) createBuffer(data.length).put(data).flip();
    }

    public static FloatBuffer createBuffer(int capacity) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity * 4);
        return byteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
    }


    public static void checkGlError(String op) {
        int error = GLES30.glGetError();
        if (error != GLES30.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
            //            throw new RuntimeException(msg);
        }
    }
}
