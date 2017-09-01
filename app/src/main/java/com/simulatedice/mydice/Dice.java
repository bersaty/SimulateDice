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

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;


/**
 * Created by wuchunhui on 17-2-21.
 */

public class Dice {

    FloatBuffer mVerticesBuffer;//顶点坐标数据缓冲
    FloatBuffer mNormalBuffer;//顶点法向量数据缓冲
    FloatBuffer mVerticesTextureBuffer;

    private int mVertexCount;

    private int mProgram;
    private Context mContext;

    private int mModelMatrixHandle;
    private int mVPMatrixHandle;
    private int mMVPMatrixHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mTextureId;
    private int mTextureCoordinationHandle;
    private int mIsShadowHandle;
    private int mLightPositionHandle;
    private int mCameraPositionHandle;
    private int mNormalHandle;

    public RigidBody body;//对应的刚体对象

    public Dice(Context context,float[] verticesData,float[] verticesNormalData,float[] textureCoordinateData){
        mContext = context;
        initData(verticesData,verticesNormalData,textureCoordinateData);
        initShader();
    }

    public void init(CollisionShape colShape,
                     DiscreteDynamicsWorld dynamicsWorld, float mass, float cx, float cy, float cz){

        boolean isDynamic = (mass != 0f);//物体是否可以运动
        Vector3f localInertia = new Vector3f(0, 0, 0);//惯性向量
        if(isDynamic) //如果物体可以运动
        {
            colShape.calculateLocalInertia(mass, localInertia);//计算惯性
        }
        Transform startTransform = new Transform();//创建刚体的初始变换对象
        startTransform.setIdentity();//变换初始化
        startTransform.origin.set(new Vector3f(cx, cy, cz));//设置初始的位置
        //创建刚体的运动状态对象
        DefaultMotionState myMotionState = new DefaultMotionState(startTransform);
        //创建刚体信息对象
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo
                (mass, myMotionState, colShape, localInertia);
        body = new RigidBody(rbInfo);//创建刚体
        body.setRestitution(0.3f);//设置反弹系数
        body.setFriction(2.0f);//设置摩擦系数

        dynamicsWorld.addRigidBody(body);//将刚体添加进物理世界

    }

    public void initData(float[] verticesData,float[] verticesNormalData,float[] textureCoordinateData){

        mVertexCount = verticesData.length/3;

        // Initialize the buffers.
        mVerticesBuffer = ByteBuffer.allocateDirect(verticesData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVerticesBuffer.put(verticesData).position(0);

        mNormalBuffer = ByteBuffer.allocateDirect(verticesNormalData.length*4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mNormalBuffer.put(verticesNormalData).position(0);

        mVerticesTextureBuffer = ByteBuffer.allocateDirect(textureCoordinateData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVerticesTextureBuffer.put(textureCoordinateData).position(0);
    }

    private void initShader(){
        mProgram = MyGLUtils.buildProgram(mContext, R.raw.dice_scene_vertex, R.raw.dice_scene_frag);

        mTextureId = MyGLUtils.loadTexture(mContext,R.drawable.touzi,new int[2]);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mModelMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMMatrix");
        mVPMatrixHandle = GLES20.glGetUniformLocation(mProgram,"uMProjCameraMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        mCameraPositionHandle = GLES20.glGetUniformLocation(mProgram, "uCamera");
        mLightPositionHandle = GLES20.glGetUniformLocation(mProgram, "uLightLocation");
        mTextureCoordinationHandle = GLES20.glGetAttribLocation(mProgram,"aTextureCoord");
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "aNormal");
        mIsShadowHandle = GLES20.glGetUniformLocation(mProgram,"isShadow");

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
    }

    public void drawSelf(int isShadow) {

        if(isShadow == 0) {
            //获取这个箱子的变换信息对象
            Transform trans = body.getMotionState().getWorldTransform(new Transform());
            //进行移位变换
            MatrixState.translate(trans.origin.x, trans.origin.y, trans.origin.z);
            Quat4f ro = trans.getRotation(new Quat4f());//获取当前变换的旋转信息
            if (ro.x != 0 || ro.y != 0 || ro.z != 0) {
                float[] fa = MyGLUtils.fromSYStoAXYZ(ro);//将四元数转换成AXYZ的形式
                MatrixState.rotate(fa[0], fa[1], fa[2], fa[3]);//执行旋转
            }
        }

        //制定使用某套着色器程序
        GLES20.glUseProgram(mProgram);
        //将最终变换矩阵传入着色器程序
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, MatrixState.getFinalMatrix(), 0);
        //将位置、旋转变换矩阵传入着色器程序
        GLES20.glUniformMatrix4fv(mModelMatrixHandle, 1, false, MatrixState.getMMatrix(), 0);
        //将光源位置传入着色器程序
        GLES20.glUniform3fv(mLightPositionHandle, 1, MatrixState.lightPositionFB);
        //将摄像机位置传入着色器程序
        GLES20.glUniform3fv(mCameraPositionHandle, 1, MatrixState.cameraFB);
        //将是否绘制阴影属性传入着色器程序
        GLES20.glUniform1i(mIsShadowHandle, isShadow);
        //将投影、摄像机组合矩阵传入着色器程序
        GLES20.glUniformMatrix4fv(mVPMatrixHandle, 1, false, MatrixState.getViewProjMatrix(), 0);

        //将顶点位置数据传入渲染管线
        GLES20.glVertexAttribPointer
                (
                        mPositionHandle,
                        3,
                        GLES20.GL_FLOAT,
                        false,
                        3*4,
                        mVerticesBuffer
                );
        //将顶点法向量数据传入渲染管线
        GLES20.glVertexAttribPointer
                (
                        mNormalHandle,
                        3,
                        GLES20.GL_FLOAT,
                        false,
                        3*4,
                        mNormalBuffer
                );
        //纹理数据
        GLES20.glVertexAttribPointer(
                mTextureCoordinationHandle,
                2,
                GLES20.GL_FLOAT,
                false,
                2*4,
                mVerticesTextureBuffer
        );
        //启用顶点位置、法向量、纹理坐标数据
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLES20.glEnableVertexAttribArray(mTextureCoordinationHandle);

        //设置纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
//        GLES20.glUniform1i(mTextureHandle, 0);

        //绘制被加载的物体
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mVertexCount);
    }

}
