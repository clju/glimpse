package org.moldus.glimpse;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.io.IOException;

public class Pic {

    int textureDataHandle;
    double angle;
    float[] transfMatrix = new float[16];

    public Pic(String picFilePath, double angle) {
        this.angle = angle;
        loadTexture(picFilePath);
    }

    // cf. http://www.learnopengles.com/android-lesson-four-introducing-basic-texturing/
    private void loadTexture(String picFilePath)
    {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            try {
                Bitmap bitmap = BitmapFactory.decodeFile(picFilePath, options);

                float ratio = ((float)bitmap.getWidth())/bitmap.getHeight();
                float[] scaleMatrix = new float[]{1f,0f,0f,0f,   0f,ratio,0f,0f,   0f,0f,1f,0f,   0f,0f,0f,1f};

                ExifInterface ei = new ExifInterface(picFilePath);
                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
                double theta = 0;

                switch(orientation) {
                    case(ExifInterface.ORIENTATION_NORMAL):
                        theta = 0;
                        break;
                    case(ExifInterface.ORIENTATION_ROTATE_90):
                        theta = -Math.PI/2;
                        break;
                    case(ExifInterface.ORIENTATION_ROTATE_180):
                        theta = Math.PI;
                        break;
                    case(ExifInterface.ORIENTATION_ROTATE_270):
                        theta = Math.PI/2;
                        break;
                }

                Matrix.multiplyMM(transfMatrix, 0, new float[]{1f,0f,0f,0f,   0f,(float)Math.cos(theta),(float)Math.sin(theta),0f,   0f,-(float)Math.sin(theta),(float)Math.cos(theta),0f,   0f,0f,0f,1f}, 0, scaleMatrix, 0);


                // Bind to the texture in OpenGL
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

                // Set filtering
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

                // Load the bitmap into the bound texture.
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

                // Recycle the bitmap, since its data has been loaded into OpenGL.
                bitmap.recycle();
            }
            catch(IOException e) {
                e.printStackTrace();
            }

        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        textureDataHandle =  textureHandle[0];
    }
}
