package com.yinglun.lfrendercoredemo1.Filter;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;

import java.nio.FloatBuffer;

/**
 * @author vojin
 */
public class NormalFilter {

    private FloatBuffer vertexBuffer;
    private FloatBuffer positionBuffer;

    public int inputTexture = -1;
    private int mProgram;
    private final int[] frameBuffer = new int[1];
    private final int[] frameTexture = new int[1];

    private int width = 0;
    private int height = 0;

    private final float[] gVertices = new float[]{
            -1,  1, 0, 1,
             1,  1, 0, 1,
            -1, -1, 0, 1,
             1, -1, 0, 1
    };

    private final float[] gPosition = new float[]{
            0, 1, 0, 1,
            1, 1, 0, 1,
            0, 0, 0, 1,
            1, 0, 0, 1
    };

    private final String gVertexShader =
            "#version 300 es                          \n" +
                    "layout(location = 0) in vec4 vPosition;  \n" +
                    "layout(location = 1) in vec4 a_texcoord; \n" +
                    "out vec2 v_texcoord;                     \n" +
                    "void main() {                            \n" +
                    "  gl_Position = vPosition;               \n" +
                    "  v_texcoord = a_texcoord.xy;            \n" +
                    "}\n";

    private final String gFragmentShader =
            "#version 300 es                          \n" +
                    "precision mediump float;         \n" +
                    "out vec4 fragColor;              \n" +
                    "in vec2 v_texcoord;              \n" +
                    "layout(location = 2) uniform sampler2D tex_sampler_0;\n" +
                    "void main() {\n" +
                    "   fragColor =  texture(tex_sampler_0,v_texcoord);\n" +
                    "}\n";

    public void init() {
        vertexBuffer = OpenGLUtils.createBuffer(gVertices);
        positionBuffer = OpenGLUtils.createBuffer(gPosition);
        mProgram = OpenGLUtils.createProgram(gVertexShader, gFragmentShader);
    }

    public void setFrameSize(int width, int height) {
        this.width = width;
        this.height = height;

        delFrameBufferAndTexture();
        genFrameBufferAndTexture();
    }

    public void drawFrameBuffer() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBuffer[0]);

        GLES30.glViewport(0, 0, width, height);

        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glUseProgram(mProgram);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 4, GLES30.GL_FLOAT, false, 16, vertexBuffer);

        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, positionBuffer);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTexture);
        GLES30.glUniform1i(2, 1);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }

    public int getFrameTexture() {
        return frameTexture[0];
    }

    private void delFrameBufferAndTexture() {
        GLES30.glDeleteFramebuffers(frameBuffer.length, frameBuffer, 0);
        GLES30.glDeleteTextures(frameTexture.length, frameTexture, 0);
    }

    private void genFrameBufferAndTexture() {
        OpenGLUtils.createFrameBuffer(frameBuffer, frameTexture, width, height);
    }
}
