package com.simulatedice.mydice;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.vecmath.Vector3f;

/**
 * Created by wuchunhui on 17-2-21.
 */

public class DiceSurfaceView extends GLSurfaceView {
    private final float TOUCH_SCALE_FACTOR = 180.0f/320;//角度缩放比例
    private DiceRenderer mRenderer;//场景渲染器
    SensorManager mSensorManager;
    SensorEventListener mSensorEventListener;

    Sensor mAccelerSensor;//加速度传感器
    float mAx,mAy,mAz;//三个坐标分量加速度
    float mDx,mDy,mDz;//三个坐标分量速度增量

    private float mPreviousY;//上次的触控位置Y坐标
    private float mPreviousX;//上次的触控位置X坐标
    //关于摄像机的变量
    float cx=0;//摄像机x位置
    float cy=0;//摄像机y位置
    float cz=-50;//摄像机z位置

    float tx=0;//目标点x位置
    float ty=0;//目标点y位置
    float tz=0;//目标点z位置
    public float currSightDis=30;//摄像机和目标的距离
    float angdegElevation=90;//仰角
    public float angdegAzimuth=180;//方位角

    //关于灯光的变量
    float lx=0;//x位置
    float ly=0;//y位置
    float lz=0;//z位置
    float lightDis=100;
    float lightElevation=50;//灯光仰角
    public float lightAzimuth=-30;//灯光的方位角

    CollisionShape boxShape;//共用的立方体
    CollisionShape planeShape;//共用的平面形状
    DiscreteDynamicsWorld dynamicsWorld;//世界对象
    Dice mDice;//骰子

    ArrayList<Dice> mDiceList = new ArrayList<>();


    public DiceSurfaceView(Context context) {
        super(context);
        this.setEGLContextClientVersion(2); //设置使用OPENGL ES2.0
        mRenderer = new DiceRenderer();	//创建场景渲染器
        setRenderer(mRenderer);				//设置渲染器
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);//设置渲染模式为主动渲染

        initWorld();//初始化世界

        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        mAccelerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                float[] value = event.values;

                mDx = Math.abs(mAx-value[0]);
                mDy = Math.abs(mAy-value[1]);
                mDz = Math.abs(mAz-value[2]);

                mAx = value[0];
                mAy = value[1];
                mAz = value[2];

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {


            }
        };
        mSensorManager.registerListener(mSensorEventListener,mAccelerSensor,SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        mSensorManager.unregisterListener(mSensorEventListener,mAccelerSensor);

        super.onPause();
    }

    @Override
    public void onResume() {
        mSensorManager.registerListener(mSensorEventListener,mAccelerSensor,SensorManager.SENSOR_DELAY_UI);
        super.onResume();
    }

    //初始化物理世界的方法
    public void initWorld()
    {
        //创建碰撞检测配置信息对象
        CollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
        //创建碰撞检测算法分配者对象，其功能为扫描所有的碰撞检测对，并确定适用的检测策略对应的算法
        CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);
        //设置整个物理世界的边界信息
        Vector3f worldAabbMin = new Vector3f(-1, 0, -1);
        Vector3f worldAabbMax = new Vector3f(1, 1, 1);
        int maxProxies = 1024;
        //创建碰撞检测粗测阶段的加速算法对象
        AxisSweep3 overlappingPairCache =new AxisSweep3(worldAabbMin, worldAabbMax, maxProxies);
        //创建推动约束解决者对象
        SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();
        //创建物理世界对象
        dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, overlappingPairCache, solver,collisionConfiguration);
        //设置重力加速度
        dynamicsWorld.setGravity(new Vector3f(0, -60, 0));
        //创建共用的立方体,包裹体
        boxShape=new BoxShape(new Vector3f(1.1f,1.1f,1.1f));

    }

    //触摸事件回调方法
    @Override
    public boolean onTouchEvent(MotionEvent e)
    {

        float y = e.getY();
        float x = e.getX();
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dy = y - mPreviousY;//计算触控笔Y位移
                float dx = x - mPreviousX;//计算触控笔X位移
                angdegAzimuth += dx * TOUCH_SCALE_FACTOR;//设置沿x轴旋转角度
                angdegElevation += dy * TOUCH_SCALE_FACTOR;//设置沿z轴旋转角度
                //将仰角限制在5～90度范围内
                angdegElevation = Math.max(angdegElevation, 5);
                angdegElevation = Math.min(angdegElevation, 90);
                //设置摄像机的位置
                setCameraPostion();

                case MotionEvent.ACTION_DOWN:

                    for(Dice di:mDiceList){
                        synchronized (di) {
                            RigidBody body = di.body;
                            if (body.isActive() == false) {
                                body.activate();
                            }
                            body.setLinearVelocity(new Vector3f(0, 50.0f, 0.0f));

                            body.setAngularVelocity(new Vector3f(1, 1, 1));
//                        GImpactCollisionAlgorithm.registerAlgorithm(dispatcher);
                        }
                    }

                    //视觉变化
//                    return false;

        }
        mPreviousY = y;//记录触控笔位置
        mPreviousX = x;//记录触控笔位置
        return true;
    }

    // 位置灯光位置的方法
    public void setLightPostion() {
        //计算灯光的位置
        double angradElevation = Math.toRadians(lightElevation);// 仰角（弧度）
        double angradAzimuth = Math.toRadians(lightAzimuth);// 方位角
        lx = (float) (- lightDis * Math.cos(angradElevation)	* Math.sin(angradAzimuth));
        ly = (float) (+ lightDis * Math.sin(angradElevation));
        lz = (float) (- lightDis * Math.cos(angradElevation) * Math.cos(angradAzimuth));
    }

    // 设置摄像机位置的方法
    public void setCameraPostion() {
        //计算摄像机的位置
        double angradElevation = Math.toRadians(angdegElevation);// 仰角（弧度）
        double angradAzimuth = Math.toRadians(angdegAzimuth);// 方位角
        cx = (float) (tx - currSightDis * Math.cos(angradElevation)	* Math.sin(angradAzimuth));
        cy = (float) (ty + currSightDis * Math.sin(angradElevation));
        cz = (float) (tz - currSightDis * Math.cos(angradElevation) * Math.cos(angradAzimuth));
    }

    private class DiceRenderer implements Renderer
    {

        Background mBackground;//地板

        public void onDrawFrame(GL10 gl)
        {
            //清除深度缓冲与颜色缓冲
            GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            //设置camera位置,在上面往下面看
            MatrixState.setCamera
                    (
                            cx,   //人眼位置的X
                            cy, 	//人眼位置的Y
                            cz,   //人眼位置的Z
                            tx, 	//人眼球看的点X
                            ty,   //人眼球看的点Y
                            tz,   //人眼球看的点Z
                            0, 	//up位置
                            1,
                            0
                    );
            //初始化光源位置
            MatrixState.setLightLocation(lx, ly, lz);
            //若加载的物体部位空则绘制物体

//            GLES20.glCullFace(GLES20.GL_FRONT);

            //绘制阴影开启混合
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glCullFace(GLES20.GL_BACK);

            //绘制地板
            MatrixState.pushMatrix();
//            MatrixState.rotate(-90,1,0,0);
            mBackground.drawSelf();
            MatrixState.popMatrix();


            synchronized (mDiceList) {
                for (Dice di : mDiceList) {
                    synchronized (di) {
                        //绘制骰子 中间
                        MatrixState.pushMatrix();
                        MatrixState.scale(1.8f, 1.8f, 1.8f);
                        di.drawSelf(0);
                        di.drawSelf(1);
                        MatrixState.popMatrix();
                    }
                }
            }

        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            //设置视窗大小及位置
            GLES20.glViewport(0, 0, width, height);
            //计算GLSurfaceView的宽高比
            float ratio = (float) width / height;
            //调用此方法计算产生透视投影矩阵
            MatrixState.setProjectFrustum(-ratio, ratio, -1, 1, 2, 100);

            //计算摄像机的位置
            setCameraPostion();
            //计算灯光的位置
            setLightPostion();
            new Thread(){
                @Override
                public void run(){
                    while(true){
                        lightAzimuth +=1;
                        lightAzimuth %= 360;
                        //改变灯光的位置
//                        setLightPostion();
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            //设置屏幕背景色RGBA
            GLES20.glClearColor(0.3f,0.3f,0.3f,1.0f);
            //打开深度检测
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            //关闭背面剪裁
            GLES20.glDisable(GLES20.GL_CULL_FACE);

            //开启混合，阴影可以有透明度
//            GLES20.glEnable(GLES20.GL_BLEND);
//            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            //初始化变换矩阵
            MatrixState.setInitStack();

            for(int i = 0;i<5;i++) {
                mDice = LoadUtil.loadDiceObj("dice_1000.obj", getResources(), getContext());
                mDice.init(boxShape, dynamicsWorld, 1, i*2, 3, 0);
                //使得立方体一开始是不激活的
                mDice.body.forceActivationState(RigidBody.WANTS_DEACTIVATION);
                mDiceList.add(mDice);
            }

            //上下
            CollisionShape planeShape1;
            planeShape1=new StaticPlaneShape(new Vector3f(1, 0, 0), -10);
            Background mBackground1 = new Background(getContext(),0.0f,planeShape1,dynamicsWorld);

            CollisionShape planeShape2;
            planeShape2=new StaticPlaneShape(new Vector3f(-1, 0, 0), -10);
            Background mBackground2 = new Background(getContext(),0.0f,planeShape2,dynamicsWorld);

            //左右
            CollisionShape planeShape3;
            planeShape3=new StaticPlaneShape(new Vector3f(0, 0, 1), -6);
            Background mBackground3 = new Background(getContext(),0.0f,planeShape3,dynamicsWorld);

            CollisionShape planeShape4;
            planeShape4=new StaticPlaneShape(new Vector3f(0, 0, -1), -6);
            Background mBackground4 = new Background(getContext(),0.0f,planeShape4,dynamicsWorld);

            //深浅
            CollisionShape planeShape5;
            planeShape5=new StaticPlaneShape(new Vector3f(0, -1, 0), -10);
            Background mBackground5 = new Background(getContext(),0.0f,planeShape5,dynamicsWorld);

            planeShape=new StaticPlaneShape(new Vector3f(0, 1, 0), 0);
            mBackground = new Background(getContext(),0.0f,planeShape,dynamicsWorld);




            new Thread()
            {
                public void run()
                {
                    while(true)
                    {
                        try
                        {
                            dynamicsWorld.stepSimulation(1.0f/60, 5);
                            for(Dice di:mDiceList){
                                synchronized (di) {
                                    RigidBody body = di.body;
                                    if (body.isActive() == false) {
//                                        body.activate();
                                    }

                                    Vector3f out = new Vector3f();
                                    body.getLinearVelocity(out);

                                    float deltaX,deltaY,deltaZ;

                                    deltaX = mAx;
                                    deltaY = mAy;
                                    deltaZ = mAz;

                                    if(mDx < 2.4f)
                                        deltaX = 0;
                                    if(mDy < 2.4f)
                                        deltaY = 0;
                                    if(mDz < 2.4f )
                                        deltaZ = 0;
                                    Log.i("wch out = ",out.y+ "~~~~~");

                                    out.x += deltaY;
                                    out.z += deltaX;
                                    out.y -= deltaZ;

//                                    if(Math.abs(out.y) < 0.8f) {
//                                        body.setLinearVelocity(new Vector3f(out.x,0,out.y));
//                                    }

                                    body.setLinearVelocity(out);
//                                    body.setAngularVelocity(new Vector3f(0, 0, 0));
//                        GImpactCollisionAlgorithm.registerAlgorithm(dispatcher);
                                }
                            }

                            Thread.sleep(20);	//当前线程睡眠20毫秒

                        } catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
        }

    }
}
