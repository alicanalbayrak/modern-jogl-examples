/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut03.cpuPositionOffset;

import com.jogamp.newt.event.KeyEvent;
import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glutil.BufferUtils;
import framework.Framework;
import framework.Semantic;
import glm.vec._2.Vec2;
import glm.vec._4.Vec4;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author gbarbieri
 */
public class CpuPositionOffset extends Framework {

    private final String SHADERS_ROOT = "tut03/cpuPositionOffset/shaders";
    private final String SHADERS_SOURCE = "standard";

    public static void main(String[] args) {
        new CpuPositionOffset("Tutorial 03 - CPU Position Offset");
    }

    public CpuPositionOffset(String title) {
        super(title);
    }

    private int theProgram;
    private IntBuffer positionBufferObject = GLBuffers.newDirectIntBuffer(1), vao = GLBuffers.newDirectIntBuffer(1);
    private float[] vertexPositions = {
        +0.25f, +0.25f, 0.0f, 1.0f,
        +0.25f, -0.25f, 0.0f, 1.0f,
        -0.25f, -0.25f, 0.0f, 1.0f};
    private long startingTime;

    @Override
    public void init(GL3 gl3) {

        initializeProgram(gl3);
        initializeVertexBuffer(gl3);

        gl3.glGenVertexArrays(1, vao);
        gl3.glBindVertexArray(vao.get(0));

        startingTime = System.currentTimeMillis();
    }

    private void initializeProgram(GL3 gl3) {

        ShaderProgram shaderProgram = new ShaderProgram();

        ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                SHADERS_SOURCE, "vert", null, true);
        ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                SHADERS_SOURCE, "frag", null, true);

        shaderProgram.add(vertShaderCode);
        shaderProgram.add(fragShaderCode);

        shaderProgram.link(gl3, System.out);

        theProgram = shaderProgram.program();

        vertShaderCode.destroy(gl3);
        fragShaderCode.destroy(gl3);
    }

    private void initializeVertexBuffer(GL3 gl3) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexPositions);

        gl3.glGenBuffers(1, positionBufferObject);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject.get(0));
        gl3.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(vertexBuffer);
    }

    @Override
    public void display(GL3 gl3) {

        Vec2 offset = new Vec2(0.0f);

        computePositionOffsets(offset);
        adjustVertexData(gl3, offset);

        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 1.0f));

        gl3.glUseProgram(theProgram);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject.get(0));
        gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 4, GL_FLOAT, false, Vec4.SIZE, 0);

        gl3.glDrawArrays(GL3.GL_TRIANGLES, 0, 3);

        gl3.glDisableVertexAttribArray(Semantic.Attr.POSITION);
        gl3.glUseProgram(0);
    }

    private void computePositionOffsets(Vec2 offset) {

        float loopDuration = 5.0f;
        float scale = (float) (Math.PI * 2.0f / loopDuration);

        float elapsedTime = (System.currentTimeMillis() - startingTime) / 1_000.0f;

        float fCurrTimeThroughLoop = elapsedTime % loopDuration;

        offset.x = (float) (Math.cos(fCurrTimeThroughLoop * scale) * 0.5f);
        offset.y = (float) (Math.sin(fCurrTimeThroughLoop * scale) * 0.5f);
    }

    private void adjustVertexData(GL3 gl3, Vec2 offset) {

        float[] newData = new float[vertexPositions.length];
        System.arraycopy(vertexPositions, 0, newData, 0, vertexPositions.length);

        for (int iVertex = 0; iVertex < vertexPositions.length; iVertex += 4) {

            newData[iVertex + 0] += offset.x;
            newData[iVertex + 1] += offset.y;
        }

        FloatBuffer buffer = GLBuffers.newDirectFloatBuffer(newData);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject.get(0));
        gl3.glBufferSubData(GL_ARRAY_BUFFER, 0, buffer.capacity() * Float.BYTES, buffer);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(buffer);
    }

    @Override
    public void reshape(GL3 gl3, int w, int h) {

        gl3.glViewport(0, 0, w, h);
    }
    
    @Override
    public void end(GL3 gl3) {

        gl3.glDeleteProgram(theProgram);
        gl3.glDeleteBuffers(1, positionBufferObject);
        gl3.glDeleteVertexArrays(1, vao);
        
        BufferUtils.destroyDirectBuffer(positionBufferObject);
        BufferUtils.destroyDirectBuffer(vao);
    }
    
    @Override
    public void keyPressed(KeyEvent keyEvent) {

        switch (keyEvent.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                animator.remove(glWindow);
                glWindow.destroy();
                break;
        }
    }
}
