package com.autoarchery.app.vision;

import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Method;
import java.util.Arrays;

public class TargetVisionAnalyzerTest {
    private final TargetVisionAnalyzer analyzer = new TargetVisionAnalyzer();
    private Object invoke(String name, Class<?>[] types, Object... args) throws Exception {
        Method m = TargetVisionAnalyzer.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(analyzer, args);
    }
    private float[] flat(float brightness) { float[] f = new float[96*96]; Arrays.fill(f, brightness); return f; }
    private DetectedShotResult detect(float[] f) throws Exception {
        return (DetectedShotResult) invoke("detectIncrementalChange", new Class<?>[]{float[].class,float[].class}, flat(150), f);
    }
    @Test public void uniformExposureDropIsNotAnArrow() throws Exception { assertNull(detect(flat(120))); }
    @Test public void broadShadowIsNotAnArrow() throws Exception {
        float[] f=flat(150); for(int y=10;y<60;y++) for(int x=10;x<60;x++) f[y*96+x]=60;
        assertNull(detect(f));
    }
    @Test public void shortCentralShaftIsNotDiscardedByOuterRadiusRule() throws Exception {
        float[] f=flat(150); for(int y=46;y<=49;y++) for(int x=48;x<=70;x++) f[y*96+x]=40;
        DetectedShotResult shot=detect(f); assertNotNull(shot); assertTrue(shot.getConfidence()<.7f);
        assertTrue(Math.abs(shot.getXNormalized())<.04f || shot.getXNormalized()>.4f);
    }
    @Test public void symmetricShaftDoesNotInventBullseye() throws Exception {
        float[] f=flat(150); for(int y=46;y<=49;y++) for(int x=8;x<=87;x++) f[y*96+x]=40;
        assertNull(detect(f));
    }
    @Test public void bilinearSampleClampsCoordinatesBeforeWeights() throws Exception {
        float value=(Float)invoke("bilinearSample",new Class<?>[]{float[].class,int.class,int.class,float.class,float.class},new float[]{0,255,0,255},2,2,-1f,0f);
        assertEquals(0,value,.001f);
    }
    @Test public void concentricFrontFacingTargetCanCalibrate() throws Exception {
        float[] f=new float[160*120];
        for(int y=0;y<120;y++) for(int x=0;x<160;x++) {
            double radius=Math.hypot(x-80,y-60);
            f[y*160+x]= radius>=50 ? 230 : ((int)(radius/5)%2==0 ? 50 : 160);
        }
        TargetCalibrationResult result=(TargetCalibrationResult)invoke("detectCalibration",new Class<?>[]{float[].class,int.class,int.class},f,160,120);
        assertNotNull(result); assertEquals(50,result.getRadiusXPx(),3); assertEquals(0,result.getRotationDeg(),.001f);
    }
    @Test public void singleStrongCircleIsNotEnoughToCalibrate() throws Exception {
        float[] f=new float[160*120];
        for(int y=0;y<120;y++) for(int x=0;x<160;x++) f[y*160+x]=Math.hypot(x-80,y-60)<35 ? 40 : 180;
        assertNull(invoke("detectCalibration",new Class<?>[]{float[].class,int.class,int.class},f,160,120));
    }
}
