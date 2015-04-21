package org.moldus.glimpse;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyRenderer implements GLSurfaceView.Renderer {

    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    private int previewShader = -1;

    private FloatBuffer verticesCoordBuffer;
    private FloatBuffer textureCoordBuffer;

    private float[] projMat = new float[16];

    private ArrayList<Pic> pictures = new ArrayList<Pic>();

    private float q0 = 0;
    private float q1 = 0;
    private float q2 = 0;
    private float q3 = 0;

    public android.hardware.Camera camera;
    private boolean updateTexture = false;
    public boolean changePreview = false;

    public MyRenderer() {

        // Create vertex + texture coord arrays
        float vertexCoords[] = {
                 1f,-1f,0f,    1f, 1f,0f,   -1f, 1f,0f,
                 1f,-1f,0f,   -1f, 1f,0f,   -1f,-1f,0f
        };

        float textureCoords[] = {
                1f,0f,   0f,0f,   0f,1f,
                1f,0f,   0f,1f,   1f,1f
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

        String previewVertexShaderString =
                "#version 100\n" +
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

        String previewFragmentShaderString =
                "#version 100 \n" +
                "#extension GL_OES_EGL_image_external : require\n" +
                "uniform samplerExternalOES u_texture;" +
                "varying vec2 v_vTexCoord;" +
                "void main() {" +
                "gl_FragColor = texture2D(u_texture, v_vTexCoord);" +
                "}";


        int previewVertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(previewVertexShader, previewVertexShaderString);
        GLES20.glCompileShader(previewVertexShader);

        int previewFragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(previewFragmentShader, previewFragmentShaderString);
        GLES20.glCompileShader(previewFragmentShader);

        previewShader = GLES20.glCreateProgram();
        GLES20.glAttachShader(previewShader, previewVertexShader);
        GLES20.glAttachShader(previewShader, previewFragmentShader);

        GLES20.glBindAttribLocation(previewShader, 0, "a_vWorldCoord");
        GLES20.glBindAttribLocation(previewShader, 1, "a_vTexCoord");

        GLES20.glLinkProgram(previewShader);

        // Creates special texture for camera output
        camera = android.hardware.Camera.open();
        newPreview();

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(projMat, 0, -ratio, ratio, -1, 1, 1, 20);

        Camera.Parameters param = camera.getParameters();
        List<Camera.Size> psize = param.getSupportedPreviewSizes();
        if ( psize.size() > 0 ) {
            int i;
            for ( i = 0; i < psize.size(); i++ ) {
                if ( psize.get(i).width < width || psize.get(i).height < height )
                    break;
            }
            if ( i > 0 )
                i--;
            param.setPreviewSize(psize.get(i).width, psize.get(i).height);

        }
        param.set("orientation", "portrait");
        camera.setParameters(param);
        camera.startPreview();
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        synchronized(this) {
            if ( updateTexture ) {
                pictures.get(pictures.size()-1).surfaceTexture.updateTexImage();
                updateTexture = false;
            }

            if(changePreview) {
                newPreview();
                changePreview = false;
            }
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        for(Pic p:pictures) {
            drawPreview(p);
        }

    }

    public void drawPreview(Pic p) {

        // Use shader of pictures
        GLES20.glUseProgram(previewShader);

        // Load vertices into shader
        int vertHandle = GLES20.glGetAttribLocation(previewShader, "a_vWorldCoord");
        GLES20.glEnableVertexAttribArray(vertHandle);
        verticesCoordBuffer.position(0);
        GLES20.glVertexAttribPointer(vertHandle, 3, GLES20.GL_FLOAT, false, 12, verticesCoordBuffer);

        // Load texture coord into shader
        int textCoordHandle = GLES20.glGetAttribLocation(previewShader, "a_vTexCoord");
        GLES20.glEnableVertexAttribArray(textCoordHandle);
        textureCoordBuffer.position(0);
        GLES20.glVertexAttribPointer(textCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureCoordBuffer);

        // Bind texture to Sampler2D of shader
        int textureUniformHandle = GLES20.glGetUniformLocation(previewShader, "u_texture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, p.textureDataHandle);
        GLES20.glUniform1i(textureUniformHandle, 0);

        // Compute model matrix & load it into shader
        float[] trans = new float[]{1f,0f,0f,0f,   0f,1f,0f,0f,   0f,0f,1f,0f,   0f,0f,-3f,1f};
        float[] model = new float[16];

        Matrix.multiplyMM(model, 0, p.rotationMatrix, 0, trans, 0);

        int modelHandle = GLES20.glGetUniformLocation(previewShader, "u_mModel");
        GLES20.glUniformMatrix4fv(modelHandle, 1, false , model ,0);

        // Load view matrix into shader
        float[] view = new float[16];
        Matrix.transposeM(view, 0, deviceOrientation(), 0);
        int viewHandle = GLES20.glGetUniformLocation(previewShader, "u_mView");
        GLES20.glUniformMatrix4fv(viewHandle, 1, false , view ,0);

        // Load projection matrix into shader
        int projHandle = GLES20.glGetUniformLocation(previewShader, "u_mProjection");
        GLES20.glUniformMatrix4fv(projHandle, 1, false, projMat ,0);

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        GLES20.glDisableVertexAttribArray(vertHandle);
    }

    public void setRotation(float q0, float q1, float q2, float q3) {
        // Takes the quaternion corresponding to the opposite angle, so that the rotation given by rotationMatrix() is OK
        // (because rotationMatrix() assumes a clockwise rotation)
        this.q0 = q0;
        this.q1 = q1;
        this.q2 = q2;
        this.q3 = q3;

        pictures.get(pictures.size()-1).rotationMatrix = deviceOrientation();
    }

    private float[] deviceOrientation() {
        return new float[]{q0*q0+q1*q1-q2*q2-q3*q3, 2*(q1*q2+q0*q3), 2*(q1*q3-q0*q2), 0.0f,
                           2*(q1*q2-q0*q3), q0*q0-q1*q1+q2*q2-q3*q3, 2*(q0*q1+q2*q3), 0.0f,
                           2*(q0*q2+q1*q3), 2*(q2*q3-q0*q1), q0*q0-q1*q1-q2*q2+q3*q3, 0.0f,
                           0.0f, 0.0f, 0.0f, 1.0f};
    }

    public synchronized void newPreview() {

        pictures.add(new Pic(deviceOrientation()));
        pictures.get(pictures.size()-1).surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                updateTexture = true;
            }
        });


        try {
            camera.setPreviewTexture(pictures.get(pictures.size()-1).surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}