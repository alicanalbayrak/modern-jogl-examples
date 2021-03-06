/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut07.worldWithUBO;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import framework.Framework;
import framework.Semantic;
import framework.component.Mesh;
import glm.Glm;
import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import glutil.BufferUtils;
import glutil.MatrixStack;
import one.util.streamex.StreamEx;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL2ES2.GL_STREAM_DRAW;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;

/**
 * @author elect
 */
public class WorldWithUBO extends Framework {

    private final String SHADERS_ROOT = "/tut07/worldWithUBO/shaders", MESHES_ROOT = "/tut07/data/";
    private final String[] MESHES_SOURCE = {"UnitConeTint.xml", "UnitCylinderTint.xml", "UnitCubeTint.xml",
            "UnitCubeColor.xml", "UnitPlane.xml"};
    private ProgramData uniformColor, objectColor, uniformColorTint;
    private Mesh[] meshes = new Mesh[Mesh_.MAX];
    private Vec3 sphereCamRelPos = new Vec3(67.5f, -46.0f, 150.0f), camTarget = new Vec3(0.0f, 0.4f, 0.0f);
    private boolean drawLookAtPoint = false;
    private IntBuffer globalMatricesUBO = GLBuffers.newDirectIntBuffer(1);

    public WorldWithUBO(String title) {
        super(title);
    }

    public static void main(String[] args) {
        new WorldWithUBO("Tutorial 07 - World With UBO");
    }

    @Override
    public void init(GL3 gl3) {

        initializePrograms(gl3);

        for (int i = 0; i < Mesh_.MAX; i++) {
            try {
                meshes[i] = new Mesh(MESHES_ROOT + MESHES_SOURCE[i], gl3);
            } catch (ParserConfigurationException | SAXException | IOException ex) {
                Logger.getLogger(WorldWithUBO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        gl3.glEnable(GL_CULL_FACE);
        gl3.glCullFace(GL_BACK);
        gl3.glFrontFace(GL_CW);

        gl3.glEnable(GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL_LEQUAL);
        gl3.glDepthRangef(0.0f, 1.0f);
        gl3.glEnable(GL_DEPTH_CLAMP);
    }

    private void initializePrograms(GL3 gl3) {

        uniformColor = new ProgramData(gl3, SHADERS_ROOT, "pos-only-world-transform-ubo", "color-uniform");
        objectColor = new ProgramData(gl3, SHADERS_ROOT, "pos-color-world-transform-ubo", "color-passthrough");
        uniformColorTint = new ProgramData(gl3, SHADERS_ROOT, "pos-color-world-transform-ubo", "color-mult-uniform");

        gl3.glGenBuffers(1, globalMatricesUBO);
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, globalMatricesUBO.get(0));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE * 2, null, GL_STREAM_DRAW);
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.GLOBAL_MATRICES, globalMatricesUBO.get(0), 0,
                Mat4.SIZE * 2);
    }

    @Override
    public void display(GL3 gl3) {

        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        final Vec3 camPos = resolveCamPosition();

        MatrixStack camMatrix = new MatrixStack()
                .setMatrix(calcLookAtMatrix(camPos, camTarget, new Vec3(0.0f, 1.0f, 0.0f)));

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, globalMatricesUBO.get(0));
        gl3.glBufferSubData(GL_UNIFORM_BUFFER, Mat4.SIZE, Mat4.SIZE, camMatrix.top().toDfb(matBuffer));
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        MatrixStack modelMatrix = new MatrixStack();

        //  Render the ground plane
        {
            modelMatrix
                    .push()
                    .scale(new Vec3(100.0f, 1.0f, 100.0f))
                    .top().toDfb(matBuffer);

            gl3.glUseProgram(uniformColor.theProgram);
            gl3.glUniformMatrix4fv(uniformColor.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl3.glUniform4f(uniformColor.baseColorUnif, 0.302f, 0.416f, 0.0589f, 1.0f);
            meshes[Mesh_.PLANE].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix.pop();
        }

        //  Draw the trees
        drawForest(gl3, modelMatrix);

        //  Draw the building
        {
            modelMatrix
                    .push()
                    .translate(new Vec3(20.0f, 0.0f, -10.0f));

            drawParthenon(gl3, modelMatrix);

            modelMatrix.pop();
        }

        if (drawLookAtPoint) {

            gl3.glDisable(GL3.GL_DEPTH_TEST);

            modelMatrix
                    .push()
                    .translate(new Vec3(0.0f, 0.0f, -camTarget.sub_(camPos.x).length()))
                    .scale(new Vec3(1.0f))
                    .top().toDfb(matBuffer);

            gl3.glUseProgram(objectColor.theProgram);
            gl3.glUniformMatrix4fv(objectColor.modelToWorldMatrixUnif, 1, false, matBuffer);
            meshes[Mesh_.CUBE_COLOR].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix.pop();
            gl3.glEnable(GL3.GL_DEPTH_TEST);
        }
    }

    private Vec3 resolveCamPosition() {

        float phi = (float) Math.toRadians(sphereCamRelPos.x);
        float theta = (float) Math.toRadians(sphereCamRelPos.y + 90.0f);

        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float cosPhi = (float) Math.cos(phi);
        float sinPhi = (float) Math.sin(phi);

        Vec3 dirToCamera = new Vec3(sinTheta * cosPhi, cosTheta, sinTheta * sinPhi);

        return dirToCamera.mul(sphereCamRelPos.z).add(camTarget);
    }

    private Mat4 calcLookAtMatrix(Vec3 cameraPt, Vec3 lookPt, Vec3 upPt) {

        Vec3 lookDir = lookPt.sub_(cameraPt).normalize();
        Vec3 upDir = upPt.normalize();

        Vec3 rightDir = lookDir.cross_(upDir).normalize();
        Vec3 perpUpDir = rightDir.cross_(lookDir);

        Mat4 rotationMat = new Mat4(1.0f);
        rotationMat.c0(rightDir, 0.0f);
        rotationMat.c1(perpUpDir, 0.0f);
        rotationMat.c2(lookDir.negate(), 0.0f);

        rotationMat = rotationMat.transpose();

        Mat4 translationMat = new Mat4(1.0f);
        translationMat.c3(cameraPt.negate(), 1.0f);

        return rotationMat.mul(translationMat);
    }

    private void drawForest(GL3 gl3, MatrixStack modelMatrix_) {

        for (Forest.Tree tree : Forest.trees) {

            modelMatrix_
                    .push()
                    .translate(new Vec3(tree.xPos, 0.0f, tree.zPos));
            drawTree(gl3, modelMatrix_, tree.trunkHeight, tree.coneHeight);
            modelMatrix_.pop();
        }
    }

    private void drawTree(GL3 gl3, MatrixStack modelStack_, float trunkHeight, float coneHeight) {

        //  Draw trunk
        {
            modelStack_.push();

            modelStack_
                    .scale(new Vec3(1.0f, trunkHeight, 1.0f))
                    .translate(new Vec3(0.0f, 0.5f, 0.0f))
                    .top().toDfb(matBuffer);

            gl3.glUseProgram(uniformColorTint.theProgram);
            gl3.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl3.glUniform4f(uniformColorTint.baseColorUnif, 0.694f, 0.4f, 0.106f, 1.0f);
            meshes[Mesh_.CYLINDER].render(gl3);
            gl3.glUseProgram(0);

            modelStack_.pop();
        }

        //  Draw the treetop
        {
            modelStack_.push();

            modelStack_
                    .translate(new Vec3(0.0f, trunkHeight, 0.0f))
                    .scale(new Vec3(3.0f, coneHeight, 3.0f))
                    .top().toDfb(matBuffer);

            gl3.glUseProgram(uniformColorTint.theProgram);
            gl3.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl3.glUniform4f(uniformColorTint.baseColorUnif, 0.0f, 1.0f, 0.0f, 1.0f);
            meshes[Mesh_.CONE].render(gl3);
            gl3.glUseProgram(0);

            modelStack_.pop();
        }
    }

    private void drawParthenon(GL3 gl3, MatrixStack modelMatrix_) {

        final float parthenonWidth = 14.0f;
        final float parthenonLength = 20.0f;
        final float parthenonColumnHeight = 5.0f;
        final float parthenonBaseHeight = 1.0f;
        final float parthenonTopHeight = 2.0f;

        //  Draw base
        {
            modelMatrix_
                    .push()
                    .scale(new Vec3(parthenonWidth, parthenonBaseHeight, parthenonLength))
                    .translate(new Vec3(0.0f, 0.5f, 0.0f))
                    .top().toDfb(matBuffer);
            ;

            gl3.glUseProgram(uniformColorTint.theProgram);
            gl3.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl3.glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f);
            meshes[Mesh_.CUBE_TINT].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix_.pop();
        }

        //  Draw top
        {
            modelMatrix_.push();

            modelMatrix_
                    .translate(new Vec3(0.0f, parthenonColumnHeight + parthenonBaseHeight, 0.0f))
                    .scale(new Vec3(parthenonWidth, parthenonTopHeight, parthenonLength))
                    .translate(new Vec3(0.0f, 0.5f, 0.0f))
                    .top().toDfb(matBuffer);

            gl3.glUseProgram(uniformColorTint.theProgram);
            gl3.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl3.glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f);
            meshes[Mesh_.CUBE_TINT].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix_.pop();
        }

        //  Draw columns
        final float frontZval = parthenonLength / 2.0f - 1.0f;
        final float rightXval = parthenonWidth / 2.0f - 1.0f;

        for (int iColumnNum = 0; iColumnNum < ((int) parthenonWidth / 2.0f); iColumnNum++) {
            {
                modelMatrix_
                        .push()
                        .translate(new Vec3(2.0f * iColumnNum - parthenonWidth / 2 + 1.0f, parthenonBaseHeight, frontZval));

                drawColumn(gl3, modelMatrix_, parthenonColumnHeight);

                modelMatrix_.pop();
            }
            {
                modelMatrix_
                        .push()
                        .translate(new Vec3(2.0f * iColumnNum - parthenonWidth / 2.0f + 1.0f, parthenonBaseHeight, -frontZval));

                drawColumn(gl3, modelMatrix_, parthenonColumnHeight);

                modelMatrix_.pop();
            }
        }
        //Don't draw the first or last columns, since they've been drawn already.
        for (int iColumnNum = 1; iColumnNum < ((int) ((parthenonLength - 2.0f) / 2.0f)); iColumnNum++) {
            {
                modelMatrix_
                        .push()
                        .translate(new Vec3(rightXval, parthenonBaseHeight, 2.0f * iColumnNum - parthenonLength / 2.0f + 1.0f));

                drawColumn(gl3, modelMatrix_, parthenonColumnHeight);

                modelMatrix_.pop();
            }
            {
                modelMatrix_
                        .push()
                        .translate(new Vec3(-rightXval, parthenonBaseHeight, 2.0f * iColumnNum - parthenonLength / 2.0f + 1.0f));

                drawColumn(gl3, modelMatrix_, parthenonColumnHeight);

                modelMatrix_.pop();
            }
        }

        //  Draw interior
        {
            modelMatrix_
                    .push()
                    .translate(new Vec3(0.0f, 1.0f, 0.0f))
                    .scale(new Vec3(parthenonWidth - 6.0f, parthenonColumnHeight, parthenonLength - 6.0f))
                    .translate(new Vec3(0.0f, 0.5f, 0.0f))
                    .top().toDfb(matBuffer);

            gl3.glUseProgram(objectColor.theProgram);
            gl3.glUniformMatrix4fv(objectColor.modelToWorldMatrixUnif, 1, false, matBuffer);
            meshes[Mesh_.CUBE_COLOR].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix_.pop();
        }

        //  Draw headpiece
        {
            modelMatrix_
                    .push()
                    .translate(new Vec3(
                            0.0f,
                            parthenonColumnHeight + parthenonBaseHeight + parthenonTopHeight / 2.0f,
                            parthenonLength / 2.0f))
                    .rotateX(-135.0f)
                    .rotateY(45.0f)
                    .top().toDfb(matBuffer);

            gl3.glUseProgram(objectColor.theProgram);
            gl3.glUniformMatrix4fv(objectColor.modelToWorldMatrixUnif, 1, false, matBuffer);
            meshes[Mesh_.CUBE_COLOR].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix_.pop();
        }
    }

    //Columns are 1x1 in the X/Z, and fHieght units in the Y.
    private void drawColumn(GL3 gl3, MatrixStack modelMatrix, float parthenonColumnHeight) {

        final float columnBaseHeight = 0.25f;

        //Draw the bottom of the column.
        {
            modelMatrix
                    .push()
                    .scale(new Vec3(1.0f, columnBaseHeight, 1.0f))
                    .translate(new Vec3(0.0f, 0.5f, 0.0f))
                    .top().toDfb(matBuffer);

            gl3.glUseProgram(uniformColorTint.theProgram);
            gl3.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl3.glUniform4f(uniformColorTint.baseColorUnif, 1.0f, 1.0f, 1.0f, 1.0f);
            meshes[Mesh_.CUBE_TINT].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix.pop();
        }

        //Draw the top of the column.
        {
            modelMatrix
                    .push()
                    .translate(new Vec3(0.0f, parthenonColumnHeight - columnBaseHeight, 0.0f))
                    .scale(new Vec3(1.0f, columnBaseHeight, 1.0f))
                    .translate(new Vec3(0.0f, 0.5f, 0.0f))
                    .top().toDfb(matBuffer);

            gl3.glUseProgram(uniformColorTint.theProgram);
            gl3.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl3.glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f);
            meshes[Mesh_.CUBE_TINT].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix.pop();
        }

        //Draw the main column.
        {
            modelMatrix
                    .push()
                    .translate(new Vec3(0.0f, columnBaseHeight, 0.0f))
                    .scale(new Vec3(0.8f, parthenonColumnHeight - columnBaseHeight * 2.0f, 0.8f))
                    .translate(new Vec3(0.0f, 0.5f, 0.0f))
                    .top().toDfb(matBuffer);

            gl3.glUseProgram(uniformColorTint.theProgram);
            gl3.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl3.glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f);
            meshes[Mesh_.CYLINDER].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix.pop();
        }
    }

    @Override
    public void reshape(GL3 gl3, int w, int h) {

        float zNear = 1.0f, zFar = 1000.0f;

        MatrixStack perspMatrix = new MatrixStack()
                .setMatrix(Glm.perspective_(45.0f, w / (float) h, zNear, zFar));

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, globalMatricesUBO.get(0));
        gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, perspMatrix.top().toDfb(matBuffer));
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl3.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl3) {

        gl3.glDeleteProgram(uniformColor.theProgram);
        gl3.glDeleteProgram(objectColor.theProgram);
        gl3.glDeleteProgram(uniformColorTint.theProgram);

        gl3.glDeleteBuffers(1, globalMatricesUBO);

        StreamEx.of(meshes).forEach(mesh -> mesh.dispose(gl3));

        BufferUtils.destroyDirectBuffer(globalMatricesUBO);
    }

    @Override
    public void keyPressed(KeyEvent e) {

        float factor;

        factor = e.isShiftDown() ? 10 : 1;

        switch (e.getKeyCode()) {

            case KeyEvent.VK_W:
                camTarget.z -= 4.0f / factor;
                break;

            case KeyEvent.VK_S:
                camTarget.z += 4.0f / factor;
                break;

            case KeyEvent.VK_D:
                camTarget.x += 4.0f / factor;
                break;

            case KeyEvent.VK_A:
                camTarget.x -= 4.0f / factor;
                break;

            case KeyEvent.VK_E:
                camTarget.y -= 4.0f / factor;
                break;

            case KeyEvent.VK_Q:
                camTarget.y += 4.0f / factor;
                break;

            case KeyEvent.VK_I:
                sphereCamRelPos.y -= 11.25f / factor;
                break;

            case KeyEvent.VK_K:
                sphereCamRelPos.y += 11.25f / factor;
                break;

            case KeyEvent.VK_J:
                sphereCamRelPos.x -= 11.25f / factor;
                break;

            case KeyEvent.VK_L:
                sphereCamRelPos.x += 11.25f / factor;
                break;

            case KeyEvent.VK_O:
                sphereCamRelPos.z -= 5.0f / factor;
                break;

            case KeyEvent.VK_U:
                sphereCamRelPos.z += 5.0f / factor;
                break;

            case KeyEvent.VK_SPACE:
                drawLookAtPoint = !drawLookAtPoint;
                camTarget.print("Target");
                sphereCamRelPos.print("Position");
                break;

            case KeyEvent.VK_ESCAPE:
                animator.remove(glWindow);
                glWindow.destroy();
                break;
        }

        sphereCamRelPos.y = Glm.clamp(sphereCamRelPos.y, -78.75f, -1.0f);
        camTarget.y = Glm.clamp(camTarget.y, 0.0f, camTarget.y);
        sphereCamRelPos.z = Glm.clamp(sphereCamRelPos.z, 5.0f, sphereCamRelPos.z);
    }

    private interface Mesh_ {

        public final static int CONE = 0;
        public final static int CYLINDER = 1;
        public final static int CUBE_TINT = 2;
        public final static int CUBE_COLOR = 3;
        public final static int PLANE = 4;
        public final static int MAX = 5;
    }
}
