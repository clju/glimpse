package org.moldus.glimpse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyRenderer implements GLSurfaceView.Renderer {

    private int shaderProgram = -1;

    private FloatBuffer verticesCoordBuffer;
    private FloatBuffer textureCoordBuffer;
    private int nVertices;
    private int textureDataHandle;

    private Context context;

    private float[] projMat = new float[16];

    private float q0 = 0;
    private float q1 = 0;
    private float q2 = 0;
    private float q3 = 0;

    public MyRenderer(Context context) {

        this.context = context;

        float vertexCoords[] = {
                //Front face
                0f,0f,1f,   1f,0f,1f,   1f,1f,1f,
                0f,0f,1f,   1f,1f,1f,   0f,1f,1f,
                // Back face
                0f,0f,0f,   1f,1f,0f,   1f,0f,0f,
                0f,0f,0f,   0f,1f,0f,   1f,1f,0f,
                // Top face
                0f,1f,1f,   1f,1f,1f,   0f,1f,0f,
                0f,1f,0f,   1f,1f,1f,   1f,1f,0f,
                // Bottom face
                0f,0f,1f,   0f,0f,0f,   1f,0f,1f,
                1f,0f,1f,   0f,0f,0f,   1f,0f,0f,
                // Left face
                0f,0f,1f,   0f,1f,1f,   0f,0f,0f,
                0f,0f,0f,   0f,1f,1f,   0f,1f,0f,
                // Right face
                1f,0f,1f,   1f,0f,0f,   1f,1f,1f,
                1f,0f,0f,   1f,1f,0f,   1f,1f,1f
        };

        float textureCoords[] = {
                // Front face
                0f,0f,   1f,0f,   1f,1f,
                0f,0f,   1f,1f,   0f,1f,
                // Back face
                1f,0f,   0f,1f,   0f,0f,
                1f,0f,   1f,1f,   0f,1f,
                //Top face
                0f,0f,   1f,0f,   0f,1f,
                0f,1f,   1f,0f,   1f,1f,
                // Bottom face
                0f,1f,   0f,0f,   1f,1f,
                1f,1f,   0f,0f,   1f,0f,
                //Left face
                1f,0f,   1f,1f,   0f,0f,
                0f,0f,   1f,1f,   0f,1f,
                // Right face
                0f,0f,   1f,0f,   0f,1f,
                1f,0f,   1f,1f,   0f,1f
        };

        verticesCoordBuffer = ByteBuffer.allocateDirect(vertexCoords.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesCoordBuffer.put(vertexCoords).position(0);

        nVertices = vertexCoords.length;

        textureCoordBuffer = ByteBuffer.allocateDirect(textureCoords.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureCoordBuffer.put(textureCoords).position(0);

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        GLES20.glClearDepthf(1.0f);
        GLES20.glEnable( GLES20.GL_DEPTH_TEST );
        GLES20.glDepthFunc( GLES20.GL_LEQUAL );
        GLES20.glDepthMask( true );

        String vertexShaderString =
                "attribute vec4 a_vWorldCoord;" +
                "attribute vec2 a_vTexCoord;" +
                "uniform mat4 u_mProjection;" +
                "uniform mat4 u_mModelView;" +
                "varying vec2 v_vTexCoord;" +
                "void main()" +
                "{" +
                    "v_vTexCoord = a_vTexCoord;" +
                    "gl_Position = u_mProjection * (u_mModelView * a_vWorldCoord);" +
                "}";
        String fragmentShaderString =
                "uniform sampler2D u_texture;" +
                "varying vec2 v_vTexCoord;" +
                "void main() {" +
                    "gl_FragColor = texture2D(u_texture, v_vTexCoord);" +
                "}";

        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShader, vertexShaderString);
        GLES20.glCompileShader(vertexShader);

        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShader, fragmentShaderString);
        GLES20.glCompileShader(fragmentShader);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);

        GLES20.glBindAttribLocation(shaderProgram, 0, "a_vWorldCoord");
        GLES20.glBindAttribLocation(shaderProgram, 1, "a_vTexCoord");

        GLES20.glLinkProgram(shaderProgram);

        textureDataHandle = loadTexture(R.drawable.bumpy_bricks_public_domain);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(projMat, 0, -ratio, ratio, -1, 1, 1, 20);
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(shaderProgram);

        int vertHandle = GLES20.glGetAttribLocation(shaderProgram, "a_vWorldCoord");
        GLES20.glEnableVertexAttribArray(vertHandle);
        verticesCoordBuffer.position(0);
        GLES20.glVertexAttribPointer(vertHandle, 3, GLES20.GL_FLOAT, false, 12, verticesCoordBuffer);

        int textCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "a_vTexCoord");
        GLES20.glEnableVertexAttribArray(textCoordHandle);
        textureCoordBuffer.position(0);
        GLES20.glVertexAttribPointer(textCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureCoordBuffer);

        int textureUniformHandle = GLES20.glGetUniformLocation(shaderProgram, "u_texture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureDataHandle);
        GLES20.glUniform1i(textureUniformHandle, 0);

        int projHandle = GLES20.glGetUniformLocation(shaderProgram, "u_mProjection");
        GLES20.glUniformMatrix4fv(projHandle, 1, false, projMat ,0);

        int modelViewHandle = GLES20.glGetUniformLocation(shaderProgram, "u_mModelView");
        GLES20.glUniformMatrix4fv(modelViewHandle, 1, false , modelViewMat() ,0);

        //int colorHandle = GLES20.glGetUniformLocation(shaderProgram, "u_vColor");


        //for(int i=0;i<nVertices/6;i++) {
        //int i = 2;
        //    float x = ((float)i+1)/(nVertices/6);
        //    GLES20.glUniform4f(colorHandle, x, 1f-x, 0.0f, 1.0f);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 30, 6);
        //}


        GLES20.glDisableVertexAttribArray(vertHandle);

    }

    public void setRotation(float q0, float q1, float q2, float q3) {
        this.q0 = q0;
        this.q1 = q1;
        this.q2 = q2;
        this.q3 = q3;
    }

    private float[] rotationMatrix() {
        float[] mat = {q0*q0+q1*q1-q2*q2-q3*q3, 2*(q1*q2+q0*q3), 2*(q1*q3-q0*q2), 0.0f,
                           2*(q1*q2-q0*q3), q0*q0-q1*q1+q2*q2-q3*q3, 2*(q0*q1+q2*q3), 0.0f,
                           2*(q0*q2+q1*q3), 2*(q2*q3-q0*q1), q0*q0-q1*q1-q2*q2+q3*q3, 0.0f,
                           0.0f, 0.0f, 0.0f, 1.0f};

        float[] rotMatrix = new float[16];
        Matrix.transposeM(rotMatrix, 0, mat, 0);

        return rotMatrix;
    }

    private float[] modelViewMat() {

        float[] trans = new float[]{10.0f, 0.0f, 0.0f, 0.0f,  0.0f, 10.0f, 0.0f, 0.0f,  0.0f, 0.0f, 10.0f, 0.0f,  -5.0f, -5.0f, -5.0f, 1.0f};

        float[] mat2 = new float[16];
        Matrix.multiplyMM(mat2, 0, rotationMatrix(), 0, trans, 0);

        return mat2;
    }

    // cf. http://www.learnopengles.com/android-lesson-four-introducing-basic-texturing/
    private int loadTexture(int resourceId)
    {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            // Read in the resource
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }
}