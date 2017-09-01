package com.simulatedice.mydice;

import android.content.Context;
import android.opengl.GLES20;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import com.simulatedice.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.vecmath.Vector3f;


/**
 * Created by wuchunhui on 17-2-21.
 */

public class Background {

    private  FloatBuffer mVerticesBuffer;
    private  FloatBuffer mVerticesTextureBuffer;

    private int mMVPMatrixHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mTextureUniformHandle;
    private int mTextureCoordinationHandle;

    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    private int mTextureId;

    private int mProgram;
    private Context mContext;

    public Background(Context context,float yOffset,CollisionShape groundShape,DiscreteDynamicsWorld dynamicsWorld){
        mContext = context;

        //创建刚体的初始变换对象
        Transform groundTransform = new Transform();
        groundTransform.setIdentity();
        groundTransform.origin.set(new Vector3f(0.f, yOffset, 0.f));
        Vector3f localInertia = new Vector3f(0, 0, 0);//惯性
        //创建刚体的运动状态对象
        DefaultMotionState myMotionState = new DefaultMotionState(groundTransform);
        //创建刚体信息对象
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(0, myMotionState, groundShape, localInertia);
        //创建刚体
        RigidBody body = new RigidBody(rbInfo);
        //设置反弹系数
        body.setRestitution(0.3f);
        //设置摩擦系数
        body.setFriction(2.0f);
        //将刚体添加进物理世界
        dynamicsWorld.addRigidBody(body);

        initData();
        initShader();
    }

    //初始化数据
    private void initData(){
        final float[] VerticesData = {
                // X, Y, Z,
                -30.0f, 0.0f, -16.0f,

                30.0f,  0.0f,-16.0f,

                30.0f,  0.0f,16.0f,

                -30.0f, 0.0f, -16.0f,

                30.0f,  0.0f,16.0f,

                -30.0f, 0.0f, 16.0f,
        };

        final float[] TextureCoordinateData = {
                0,0,
                1,0,
                1,1,
                0,0,
                1,1,
                0,1
        };

        // Initialize the buffers.
        mVerticesBuffer = ByteBuffer.allocateDirect(VerticesData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVerticesBuffer.put(VerticesData).position(0);

        mVerticesTextureBuffer = ByteBuffer.allocateDirect(TextureCoordinateData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVerticesTextureBuffer.put(TextureCoordinateData).position(0);

    }

    private void initShader(){
        mProgram = MyGLUtils.buildProgram(mContext, R.raw.dice_bg_vertex, R.raw.dice_bg_fragment);

        mTextureId = MyGLUtils.loadTexture(mContext,R.drawable.container_marble,new int[2]);

        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram,"u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
//        mColorHandle = GLES20.glGetAttribLocation(mProgram, "a_Color");
        mTextureCoordinationHandle = GLES20.glGetAttribLocation(mProgram,"a_TexCoordinate");
//        mGlobalTimeHandle = GLES20.glGetUniformLocation(mProgram,"u_GlobalTime");

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
    }

    public void drawSelf() {

        GLES20.glUseProgram(mProgram);

        // Pass in the position information
        mVerticesBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false,
                3*4, mVerticesBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        mVerticesTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinationHandle,2, GLES20.GL_FLOAT,false,2*4,mVerticesTextureBuffer);
        GLES20.glEnableVertexAttribArray(mTextureCoordinationHandle);

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
//        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
//        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, MatrixState.getFinalMatrix(), 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

    }
    
}
