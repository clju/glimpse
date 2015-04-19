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
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyRenderer implements GLSurfaceView.Renderer {

    private int shaderProgram = -1;

    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private short[] drawOrder;

    private Context context;

    private float[] projMat = new float[16];

    private float q0 = 0;
    private float q1 = 0;
    private float q2 = 0;
    private float q3 = 0;

    public MyRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        GLES20.glClearDepthf(1.0f);
        GLES20.glEnable( GLES20.GL_DEPTH_TEST );
        GLES20.glDepthFunc( GLES20.GL_LEQUAL );
        GLES20.glDepthMask( true );

        String vertexShaderString =
                "attribute vec4 vWorldCoord;" +
                "uniform mat4 mProjection;" +
                "uniform mat4 mModelView;" +
                "void main()" +
                "{" +
                    "gl_Position = mProjection * (mModelView * vWorldCoord);" +
                "}";
        String fragmentShaderString =
                "uniform vec4 vColor;" +
                "void main() {" +
                    "gl_FragColor = vColor;" +
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
        GLES20.glLinkProgram(shaderProgram);

        float squareCoords[] = {
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 1.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f,
                1.0f, 1.0f, 1.0f};

        float textureCoords[] = {0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 1.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f,
                1.0f, 1.0f, 1.0f};

        drawOrder = new short[]{0, 1, 2, 2, 1, 3,
                5, 1, 7, 7, 1, 3,
                6, 7, 2, 2, 7, 3,
                0, 4, 2, 2, 4, 6,
                4, 5, 6, 6, 5, 7,
                0, 4, 1, 1, 4, 5};

        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length*4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        ByteBuffer dbb = ByteBuffer.allocateDirect(drawOrder.length*2);
        dbb.order(ByteOrder.nativeOrder());
        drawListBuffer = dbb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);
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

        int vertHandle = GLES20.glGetAttribLocation(shaderProgram, "vWorldCoord");
        GLES20.glEnableVertexAttribArray(vertHandle);
        GLES20.glVertexAttribPointer(vertHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

        int projHandle = GLES20.glGetUniformLocation(shaderProgram, "mProjection");
        GLES20.glUniformMatrix4fv(projHandle, 1, false, projMat ,0);

        int modelViewHandle = GLES20.glGetUniformLocation(shaderProgram, "mModelView");
        GLES20.glUniformMatrix4fv(modelViewHandle, 1, false , modelViewMat() ,0);

        int colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor");

        for(int i=0;i<drawOrder.length/3;i++) {
            drawListBuffer.position(3*i);
            float x = ((float)i+1)/(drawOrder.length/3);
            GLES20.glUniform4f(colorHandle, x, 1-x, 0.0f, 1.0f);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, 3, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        }


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
        //float[] rot1 = {(float)Math.cos(aZ), -(float)Math.sin(aZ), 0.0f,0.0f,  (float)Math.sin(aZ), (float)Math.cos(aZ), 0.0f, 0.0f,  0.0f, 0.0f, 1.0f, 0.0f,  0.0f, 0.0f, 0.0f, 1.0f};
        //float[] rot2 = {(float)Math.cos(aY), 0.0f, (float)Math.sin(aY), 0.0f,  0.0f, 1.0f, 0.0f, 0.0f,  -(float)Math.sin(aY), 0.0f, (float)Math.cos(aY), 0.0f,  0.0f, 0.0f, 0.0f, 1.0f};
        //float[] rot3 = {1.0f, 0.0f, 0.0f, 0.0f,  0.0f, (float) Math.cos(aX), -(float) Math.sin(aX), 0.0f, 0.0f, (float) Math.sin(aX), (float) Math.cos(aX), 0.0f,  0.0f, 0.0f, 0.0f, 1.0f};


        //float[] rot12 = new float[16];
        //float[] rot = quaternionToRotation();

        //Matrix.multiplyMM(rot12, 0, rot1, 0, rot2, 0);
        //Matrix.multiplyMM(rot, 0, rot12, 0, rot3, 0);


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