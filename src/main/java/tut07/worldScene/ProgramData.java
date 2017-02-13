/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut07.worldScene;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;

/**
 * @author elect
 */
class ProgramData {

    public int theProgram, modelToWorldMatrixUnif, worldToCameraMatrixUnif, cameraToClipMatrixUnif,
            baseColorUnif;

    public ProgramData(GL3 gl3, String root, String vert, String frag) {

        ShaderProgram shaderProgram = new ShaderProgram();

        ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), root, null,
                vert, "vert", null, true);
        ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), root, null,
                frag, "frag", null, true);

        shaderProgram.add(vertShaderCode);
        shaderProgram.add(fragShaderCode);

        shaderProgram.link(gl3, System.out);

        theProgram = shaderProgram.program();

        vertShaderCode.destroy(gl3);
        fragShaderCode.destroy(gl3);

        modelToWorldMatrixUnif = gl3.glGetUniformLocation(theProgram, "modelToWorldMatrix");
        worldToCameraMatrixUnif = gl3.glGetUniformLocation(theProgram, "worldToCameraMatrix");
        cameraToClipMatrixUnif = gl3.glGetUniformLocation(theProgram, "cameraToClipMatrix");
        baseColorUnif = gl3.glGetUniformLocation(theProgram, "baseColor");
    }

}
