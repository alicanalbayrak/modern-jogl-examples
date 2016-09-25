/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut14.basicTexture;

import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.Semantic;

/**
 *
 * @author elect
 */
class ProgramData {

    public int theProgram;

    public int modelToCameraMatrixUnif;
    public int normalModelToCameraMatrixUnif;

    public ProgramData(GL3 gl3, String shaderRoot, String vertSrc, String fragSrc) {

        ShaderProgram shaderProgram = new ShaderProgram();

        ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), shaderRoot, null,
                vertSrc, "vert", null, true);
        ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), shaderRoot, null,
                fragSrc, "frag", null, true);

        shaderProgram.add(vertShaderCode);
        shaderProgram.add(fragShaderCode);

        shaderProgram.link(gl3, System.out);

        theProgram = shaderProgram.program();

        vertShaderCode.destroy(gl3);
        fragShaderCode.destroy(gl3);

        modelToCameraMatrixUnif = gl3.glGetUniformLocation(theProgram, "modelToCameraMatrix");
        normalModelToCameraMatrixUnif = gl3.glGetUniformLocation(theProgram, "normalModelToCameraMatrix");

        gl3.glUniformBlockBinding(theProgram,
                gl3.glGetUniformBlockIndex(theProgram, "Projection"),
                Semantic.Uniform.PROJECTION);
        gl3.glUniformBlockBinding(theProgram,
                gl3.glGetUniformBlockIndex(theProgram, "Material"),
                Semantic.Uniform.MATERIAL);
        gl3.glUniformBlockBinding(theProgram,
                gl3.glGetUniformBlockIndex(theProgram, "Light"),
                Semantic.Uniform.LIGHT);

        gl3.glUseProgram(theProgram);
        gl3.glUniform1i(
                gl3.glGetUniformLocation(theProgram, "gaussianTexture"),
                Semantic.Sampler.GAUSSIAN_TEXTURE);
        gl3.glUseProgram(theProgram);
    }
}
