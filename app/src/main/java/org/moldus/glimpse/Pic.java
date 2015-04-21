package org.moldus.glimpse;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;

public class Pic {

    public int textureDataHandle;
    public float[] rotationMatrix;
    public SurfaceTexture surfaceTexture;

    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    public Pic(float[] rotationMatrix) {

        int[] hTex = new int[1];
        GLES20.glGenTextures ( 1, hTex, 0 );
        textureDataHandle = hTex[0];

        this.rotationMatrix = rotationMatrix;

        // Binds texture to openGL, changes parameters, and creates surfaceTexture
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureDataHandle);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        surfaceTexture = new SurfaceTexture(textureDataHandle);

    }


}
