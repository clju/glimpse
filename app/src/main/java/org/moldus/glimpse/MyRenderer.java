package org.moldus.glimpse;

import android.content.Context;
import android.database.Cursor;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.provider.MediaStore;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyRenderer implements GLSurfaceView.Renderer {

    private int pictureShader = -1;
    private Context context;

    private FloatBuffer verticesCoordBuffer;
    private FloatBuffer textureCoordBuffer;

    private float[] projMat = new float[16];

    private ArrayList<Pic> pictures = new ArrayList<Pic>();

    private float q0 = 0;
    private float q1 = 0;
    private float q2 = 0;
    private float q3 = 0;

    public MyRenderer(Context context) {

        this.context = context;

        // Create vertex + texture coord arrays
        float vertexCoords[] = {
                0f,-0.5f,-0.5f,   0f,0.5f,-0.5f,   0f,0.5f,0.5f,
                0f,-0.5f,-0.5f,   0f,0.5f, 0.5f,   0f,-0.5f,0.5f
        };

        float textureCoords[] = {
                0f,1f,   1f,1f,   1f,0f,
                0f,1f,   1f,0f,   0f,0f
        };

        // Create buffers
        verticesCoordBuffer = ByteBuffer.allocateDirect(vertexCoords.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesCoordBuffer.put(vertexCoords).position(0);

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
                "uniform mat4 u_mModel;" +
                "uniform mat4 u_mView;" +
                "varying vec2 v_vTexCoord;" +
                "void main()" +
                "{" +
                    "v_vTexCoord = a_vTexCoord;" +
                    "gl_Position = u_mProjection * (u_mView * (u_mModel * a_vWorldCoord));" +
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

        pictureShader = GLES20.glCreateProgram();
        GLES20.glAttachShader(pictureShader, vertexShader);
        GLES20.glAttachShader(pictureShader, fragmentShader);

        GLES20.glBindAttribLocation(pictureShader, 0, "a_vWorldCoord");
        GLES20.glBindAttribLocation(pictureShader, 1, "a_vTexCoord");

        GLES20.glLinkProgram(pictureShader);


        // Load pictures
        String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN};
        Cursor recentPicturesCursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI ,projection, null, null, MediaStore.Images.Media.DATE_TAKEN+" desc limit 10");
        if(recentPicturesCursor != null) {
            recentPicturesCursor.moveToFirst();
            int i = 0;
            do {
                String photoFilePath = recentPicturesCursor.getString(recentPicturesCursor.getColumnIndex(MediaStore.Images.Media.DATA) );
                pictures.add(new Pic(photoFilePath, 2*i*Math.PI/10));
                recentPicturesCursor.moveToNext();
                i++;
            } while(! recentPicturesCursor.isAfterLast());
        }

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

        for(Pic p:pictures) {
            drawPicture(p.textureDataHandle, p.transfMatrix, p.angle);
        }

    }

    public void drawPicture(int textureDataHandle, float[] transfMatrix, double angle) {

        // Use shader of pictures
        GLES20.glUseProgram(pictureShader);

        // Load vertices into shader
        int vertHandle = GLES20.glGetAttribLocation(pictureShader, "a_vWorldCoord");
        GLES20.glEnableVertexAttribArray(vertHandle);
        verticesCoordBuffer.position(0);
        GLES20.glVertexAttribPointer(vertHandle, 3, GLES20.GL_FLOAT, false, 12, verticesCoordBuffer);

        // Lod texture coord into shader
        int textCoordHandle = GLES20.glGetAttribLocation(pictureShader, "a_vTexCoord");
        GLES20.glEnableVertexAttribArray(textCoordHandle);
        textureCoordBuffer.position(0);
        GLES20.glVertexAttribPointer(textCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureCoordBuffer);

        // Bind texture to Sampler2D of shader
        int textureUniformHandle = GLES20.glGetUniformLocation(pictureShader, "u_texture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureDataHandle);
        GLES20.glUniform1i(textureUniformHandle, 0);

        // Compute model matrix & load it into shader
        float[] mModel = new float[16];
        float[] m = new float[16];

        float[] trans = new float[]{1f,0f,0f,0f,   0f,1f,0f,0f,   0f,0f,1f,0f,   -3f,0f,0f,1f};
        Matrix.multiplyMM(m, 0, trans, 0, transfMatrix, 0);

        float[] rot = new float[]{(float)Math.cos(angle),(float)Math.sin(angle),0f,0f,   -(float)Math.sin(angle),(float)Math.cos(angle),0f,0f,   0f,0f,1f,0f,   0f,0f,0f,1f};
        Matrix.multiplyMM(mModel,0,rot,0,m,0);

        int modelHandle = GLES20.glGetUniformLocation(pictureShader, "u_mModel");
        GLES20.glUniformMatrix4fv(modelHandle, 1, false , mModel ,0);

        // Load view matrix into shader
        int viewHandle = GLES20.glGetUniformLocation(pictureShader, "u_mView");
        GLES20.glUniformMatrix4fv(viewHandle, 1, false , viewMat() ,0);

        // Load projection matrix into shader
        int projHandle = GLES20.glGetUniformLocation(pictureShader, "u_mProjection");
        GLES20.glUniformMatrix4fv(projHandle, 1, false, projMat ,0);

        // Draw the picture
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        GLES20.glDisableVertexAttribArray(vertHandle);
    }

    public void setRotation(float q0, float q1, float q2, float q3) {
        // Takes the quaternion corresponding to the opposite angle, so that the rotation given by rotationMatrix() is OK
        // (because rotationMatrix() assumes a clockwise rotation)
        this.q0 = q0;
        this.q1 = -q1;
        this.q2 = -q2;
        this.q3 = -q3;
    }

    private float[] viewMat() {
        float[] mat = {q0*q0+q1*q1-q2*q2-q3*q3, 2*(q1*q2+q0*q3), 2*(q1*q3-q0*q2), 0.0f,
                           2*(q1*q2-q0*q3), q0*q0-q1*q1+q2*q2-q3*q3, 2*(q0*q1+q2*q3), 0.0f,
                           2*(q0*q2+q1*q3), 2*(q2*q3-q0*q1), q0*q0-q1*q1-q2*q2+q3*q3, 0.0f,
                           0.0f, 0.0f, 0.0f, 1.0f};

        float[] rotMatrix = new float[16];
        Matrix.transposeM(rotMatrix, 0, mat, 0);

        return mat;
    }


}