package Renderer;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import javax.media.opengl.GL2;

import Command.LDrawColorT;
import LDraw.Support.ColorLibrary;
import LDraw.Support.GLMatrixMath;

public class LDrawShaderRenderer implements ILDrawRenderer, ILDrawCollector {
	public static final int COLOR_STACK_DEPTH = 64;
	public static final int TEXTURE_STACK_DEPTH = 128;
	public static final int TRANSFORM_STACK_DEPTH = 64;
	public static final int DL_STACK_DEPTH = 64;

	public boolean useWireFrame = false;
	public boolean drawTransparent = false;

	/**
	 * @uml.property name="session"
	 * @uml.associationEnd
	 */
	LDrawDLSession session = null; // DL session - this accumulates draw calls
									// and
	// sorts them.

	/**
	 * @uml.property name="color_now" multiplicity="(0 -1)" dimension="1"
	 */
	float color_now[]; // Color stack.
	/**
	 * @uml.property name="compl_now" multiplicity="(0 -1)" dimension="1"
	 */
	float compl_now[];
	/**
	 * @uml.property name="color_stack" multiplicity="(0 -1)" dimension="1"
	 */
	float color_stack[];
	/**
	 * @uml.property name="color_stack_top"
	 */
	int color_stack_top = 0;

	/**
	 * @uml.property name="wire_frame_count"
	 */
	private boolean isWireFrame = false; // wire frame stack is just a count.

	/**
	 * @uml.property name="tex_stack"
	 * @uml.associationEnd multiplicity="(0 -1)"
	 */
	LDrawTextureSpec tex_stack[]; // Texture stack from push/pop texture.
	/**
	 * @uml.property name="texture_stack_top"
	 */
	int texture_stack_top;
	/**
	 * @uml.property name="tex_now"
	 * @uml.associationEnd
	 */
	LDrawTextureSpec tex_now;

	/**
	 * @uml.property name="transform_stack" multiplicity="(0 -1)" dimension="1"
	 */
	float transform_stack[]; // Transform stack from push/pop matrix.
	/**
	 * @uml.property name="transform_stack_top"
	 */
	int transform_stack_top;
	/**
	 * @uml.property name="transform_now" multiplicity="(0 -1)" dimension="1"
	 */
	float transform_now[];
	/**
	 * @uml.property name="cull_now" multiplicity="(0 -1)" dimension="1"
	 */
	float cull_now[];

	/**
	 * @uml.property name="dl_stack"
	 * @uml.associationEnd multiplicity="(0 -1)"
	 */
	LDrawDLBuilder dl_stack[]; // DL stack from begin/end DL builds.
	/**
	 * @uml.property name="dl_stack_top"
	 */
	int dl_stack_top;
	/**
	 * @uml.property name="dl_now"
	 * @uml.associationEnd
	 */
	LDrawDLBuilder dl_now; // This is the DL being built "right now".

	/**
	 * @uml.property name="mvp" multiplicity="(0 -1)" dimension="1"
	 */
	float mvp[]; // Cached MVP from when shader is built.

	/**
	 * @uml.property name="drag_handles"
	 * @uml.associationEnd
	 */
	LDrawDragHandleInstance drag_handles; // List of drag handles - deferred to
											// draw at the end for perf and
											// correct scaling.
	/**
	 * @uml.property name="scale"
	 */
	float scale; // Needed to code Allen's res-independent drag
					// handles...someday get this from viewport?

	public LDrawShaderRenderer() {
		dl_now = new LDrawDLBuilder();
		color_now = new float[4];
		compl_now = new float[4];
		color_stack = new float[COLOR_STACK_DEPTH * 4];

		tex_stack = new LDrawTextureSpec[TEXTURE_STACK_DEPTH];
		transform_stack = new float[TRANSFORM_STACK_DEPTH * 16];
		transform_now = new float[16];
		cull_now = new float[16];

		dl_stack = new LDrawDLBuilder[DL_STACK_DEPTH];
		mvp = new float[16];

		tex_now = new LDrawTextureSpec();
	}

	// ========== set_color4fv
	// ========================================================
	//
	// Purpose: Copies an RGBA color, but handles the special ptrs 0L and -1L by
	// converting them into the 'magic' colors 0,0,0,0 and 1,1,1,0 that
	// the shader wants.
	//
	// Notes: The shader, when it sees alpha = 0, mixes between the
	// attribute-set
	// current and compliment by blending with the red channel: red = 0 is
	// current, red = 1 is compliment.
	//
	// ================================================================================
	public static void set_color4fv(float c[], float storage[]) {
		if (c.length == 1) {
			if (c[0] == 0) {
				storage[0] = 0;
				storage[1] = 0;
				storage[2] = 0;
				storage[3] = 0;
			} else if (c[0] == -1) {
				storage[0] = 1;
				storage[1] = 1;
				storage[2] = 1;
				storage[3] = 0;
			}
		} else {
			for (int i = 0; i < 4; i++)
				storage[i] = c[i];
		}
	}// end set_color4fv

	// ================================================================================
	// @implementation LDrawShaderRenderer
	// ================================================================================

	// ========== init:
	// ===============================================================
	//
	// Purpose: initialize our renderer, and grab all basic OpenGL state we
	// need.
	//
	// ================================================================================
	public static HashMap<GL2, IntBuffer> progList = new HashMap<GL2, IntBuffer>();

	public LDrawShaderRenderer initWithScale(GL2 gl2, float initial_scale,
			float[] mv_matrix, float[] proj_matrix) {

		// Build our shader if it doesn't exist yet. For now, just stash the GL
		// object statically.
		if (progList.containsKey(gl2) == false) {
			IntBuffer prog = IntBuffer.allocate(1);
			progList.put(gl2, prog);
		}
		IntBuffer prog = progList.get(gl2);
		if (prog.get(0) == 0) {

			// This list of attribute names matches the text of the GLSL
			// attribute
			// declarations -
			// and its order must match the attr_position...array in the .h.
			/**
			 * @uml.property name="attribs" multiplicity="(0 -1)" dimension="1"
			 */
			String attribs[] = { "position", "normal", "color", "transform_x",
					"transform_y", "transform_z", "transform_w",
					"color_current", "color_compliment", "texture_mix" };

			int programID = LDrawShaderLoader.LDrawLoadShaderFromFile(gl2,
					System.getProperty("user.dir")
							+ "/Resource/Shader/test.glsl", attribs);
			prog.put(0, programID);
			int u_tex = gl2.glGetUniformLocation(prog.get(0), "u_tex");
			gl2.glUseProgram(prog.get(0));

			// This matches up texture unit 0 with the sampler in the shader.
			gl2.glUniform1i(u_tex, 0);
		} else {
			gl2.glUseProgram(prog.get(0));

		}

		// todo
		// super.init();

		scale = initial_scale;

		ColorLibrary.sharedColorLibrary()
				.colorForCode(LDrawColorT.LDrawCurrentColor)
				.getColorRGBA(color_now);

		gl2.glVertexAttrib1f(AttributeT.attr_texture_mix.getValue(), 0.0f);

		ColorLibrary.sharedColorLibrary().complimentColor(color_now, compl_now);

		// Set up the basic transform to be identity - our transform is on top
		// of the MVP matrix.
		// memset(transform_now,0,sizeof(transform_now));
		for (int i = 0; i < transform_now.length; i++)
			transform_now[i] = 0;
		transform_now[0] = transform_now[5] = transform_now[10] = transform_now[15] = 1.0f;

		// "Rip" the MVP matrix from OpenGL. (TODO: does LDraw just have this
		// info?)
		// We use this for culling.
		GLMatrixMath.multMatrices(mvp, proj_matrix, mv_matrix);
		for (int i = 0; i < mvp.length; i++)
			cull_now[i] = mvp[i];

		// Create a DL session to match our lifetime.
		session = new LDrawDLSession(mv_matrix);

		// Set up GL state for attribute drawing, not the fixed function drawing
		// we used to do.
		gl2.glEnableVertexAttribArray(AttributeT.attr_position.getValue());
		gl2.glEnableVertexAttribArray(AttributeT.attr_normal.getValue());
		gl2.glEnableVertexAttribArray(AttributeT.attr_color.getValue());
		gl2.glDisableClientState(GL2.GL_COLOR_ARRAY);
		gl2.glDisableClientState(GL2.GL_NORMAL_ARRAY);
		gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);

		drag_handles = null;

		return this;
	}// end init:

	// ========== dealloc:
	// ============================================================
	//
	// Purpose: Clean up our state. Note that this "triggers" the draw from
	// our
	// display list session that has stored up some of our draw calls.
	//
	// ================================================================================
	public void dealloc(GL2 gl2) {
		LDrawDragHandleInstance dh;
		// todo
		// LDrawDLSessionDrawAndDestroy(session);
		// session = nil;

		// Go through and draw the drag handles...

		// for(dh = drag_handles; dh != null; dh = dh->next)
		// {
		// float s = dh->size / scale;
		// float m[16] = { s, 0, 0, 0, 0, s, 0, 0, 0, 0, s, 0, dh->xyz[0],
		// dh->xyz[1],dh->xyz[2], 1.0 };
		//
		// [this pushMatrix:m];
		// [this drawDragHandleImm:dh->xyz withSize:dh->size];
		// [this popMatrix];
		// }

		// Put back OGL state to what LDraw usually has.
		gl2.glUseProgram(0);

		int a;
		for (a = 0; a < AttributeT.attr_count.getValue(); ++a)
			gl2.glDisableVertexAttribArray(a);
		gl2.glEnableClientState(GL2.GL_COLOR_ARRAY);
		gl2.glEnableClientState(GL2.GL_NORMAL_ARRAY);
		gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);

		// LDrawBDPDestroy(pool);

		// [super dealloc];

	}// end dealloc:

	// ========== pushMatrix:
	// =========================================================
	//
	// Purpose: accumulate a transform temporarily. The transform will be
	// 'grabbed'
	// later if a DL is made.
	//
	// Notes: our current texture is mapped in _object_ coordinates. So if we
	// are
	// going to transform our coordinate system AND we have textures active
	// we produce a new texture whose planar projection matches our new
	// coordinates.
	//
	// IF we used eye-space texturing this would not be necessary. But
	// eye space texturing was actually more complex than this case in the
	// shader.
	//
	// ================================================================================
	public void pushMatrix(float matrix[]) {
		assert (transform_stack_top < TRANSFORM_STACK_DEPTH);
		for (int i = 0; i < transform_now.length; i++)
			transform_stack[16 * transform_stack_top + i] = transform_now[i];

		float[] temp = new float[16];
		for (int i = 0; i < 16; i++)
			temp[i] = transform_stack[16 * transform_stack_top + i];

		GLMatrixMath.multMatrices(transform_now, temp, matrix);
		++transform_stack_top;

		pushTexture(tex_now);
		if (tex_now.getTex_obj() != 0) {
			// If we have a current texture, transform the tetxure by "matrix".
			// TODO: doc _why_ this works mathematically.
			float s[], t[];
			s = new float[4];
			t = new float[4];

			GLMatrixMath.applyMatrixTranspose(s, matrix, tex_now.plane_s);
			GLMatrixMath.applyMatrixTranspose(t, matrix, tex_now.plane_t);
			for (int i = 0; i < 4; i++) {
				tex_now.plane_s[i] = s[i];
				tex_now.plane_t[i] = t[i];
			}
			// memcpy(tex_now.plane_s,s,sizeof(s));
			// memcpy(tex_now.plane_t,t,sizeof(t));
		}
		GLMatrixMath.multMatrices(cull_now, mvp, transform_now);
	}// end pushMatrix:

	// ========== checkCull:to:
	// =======================================================
	//
	// Purpose: cull out bounding boxes that are off-screen. We transform to
	// clip
	// coordinates and see if the AABB (in screen space) of the original
	// bounding cube (in MV coordinates) is now entirely out of clip bounds.
	//
	// Notes: we also look at the screen-space size of the box to decide if
	// we can
	// cull it because it's tiny or replace it with a box.
	//
	// TODO: change hard-coded values to be compensated for aspect ratio,
	// etc.
	//
	// ================================================================================
	public CullingT checkCull(float[] minXYZ, float[] maxXYZ) {
		if (minXYZ[0] > maxXYZ[0] || minXYZ[1] > maxXYZ[1]
				|| minXYZ[2] > maxXYZ[2])
			return CullingT.cull_skip;

		float aabb_model[] = { minXYZ[0], minXYZ[1], minXYZ[2], maxXYZ[0],
				maxXYZ[1], maxXYZ[2] };
		float aabb_ndc[] = new float[6];

		GLMatrixMath.aabbToClipbox(aabb_model, cull_now, aabb_ndc);

		if (aabb_ndc[3] < -1.0f || aabb_ndc[4] < -1.0f || aabb_ndc[0] > 1.0f
				|| aabb_ndc[1] > 1.0f) {
			return CullingT.cull_skip;
		}

		int x_pix = (int) ((aabb_ndc[3] - aabb_ndc[0]) * 512.0);
		int y_pix = (int) ((aabb_ndc[4] - aabb_ndc[1]) * 384.0);
		int dim = Math.max(x_pix, y_pix);

		if (dim < 1)
			return CullingT.cull_skip;
		if (dim < 10)
			return CullingT.cull_box;

		return CullingT.cull_draw;
	}// end pushMatrix:to:

	//
	// ========== drawBoxFrom:to:
	// =====================================================
	//
	// Purpose: draw an axis-aligned cube of a given size.
	//
	// Notes: this routine retains a single unit-cube display list that can
	// be
	// drawn multiple times; the DL system will end up instancing it for us.
	// Because BrickSmith ensures GL resources are never lost, we can just
	// keep the cube statically.
	//
	// ================================================================================
	static LDrawDL unit_cube = null;

	public void drawBoxFrom(GL2 gl2, float[] minXyz, float[] maxXyz) {
		if (unit_cube == null) {
			LDrawDLBuilder builder = new LDrawDLBuilder();

			float top[] = { 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0 };
			float bot[] = { 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1 };
			float lft[] = { 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0 };
			float rgt[] = { 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1 };
			float frt[] = { 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1 };
			float bak[] = { 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0 };

			float c[] = new float[4];
			float n[] = { 0, 1, 0 };

			builder.addQuad(top, n, c);
			builder.addQuad(bot, n, c);
			builder.addQuad(lft, n, c);
			builder.addQuad(rgt, n, c);
			builder.addQuad(frt, n, c);
			builder.addQuad(bak, n, c);

			unit_cube = builder.finish(gl2);

		}

		float dim[] = { maxXyz[0] - minXyz[0], maxXyz[1] - minXyz[1],
				maxXyz[2] - minXyz[2] };

		float rescale[] = { dim[0], 0, 0, 0, 0, dim[1], 0, 0, 0, 0, dim[2], 0,
				minXyz[0], minXyz[1], minXyz[2], 1 };

		pushMatrix(rescale);
		drawDL(gl2, unit_cube);
		popMatrix();

	}// end drawBoxFrom:to:
		//
		//

	// ========== popMatrix:
	// ==========================================================
	//
	// Purpose: reset one level of the matrix stack.
	//
	// ================================================================================
	public void popMatrix() {
		// We always push a texture frame with every matrix frame for now, so
		// that
		// we can re-transform the tex projection. We simply have 2x the slots
		// in our stacks.
		popTexture();

		assert (transform_stack_top > 0);
		--transform_stack_top;
		// memcpy(transform_now, transform_stack + 16 * transform_stack_top,
		// sizeof(transform_now));
		for (int i = 0; i < 16; i++)
			transform_now[i] = transform_stack[16 * transform_stack_top + i];
		GLMatrixMath.multMatrices(cull_now, mvp, transform_now);
	}// end popMatrix:

	// ========== pushColor:
	// ==========================================================
	//
	// Purpose: push a color change onto the stack. This sets the RGBA for
	// the
	// current and compliment color for DLs that use the current color.
	//
	// ================================================================================
	public void pushColor(float color[]) {
		assert (color_stack_top < COLOR_STACK_DEPTH);

		color_stack[0 + color_stack_top * 4] = color_now[0];
		color_stack[1 + color_stack_top * 4] = color_now[1];
		color_stack[2 + color_stack_top * 4] = color_now[2];
		color_stack[3 + color_stack_top * 4] = color_now[3];
		++color_stack_top;

		if (color.length != 0) {
			if (color.length == 1)
				color = compl_now;

			color_now[0] = color[0];
			color_now[1] = color[1];
			color_now[2] = color[2];
			color_now[3] = color[3];
			ColorLibrary.sharedColorLibrary().complimentColor(color_now,
					compl_now);
		}
	}// end pushColor:

	// ========== popColor:
	// ===========================================================
	//
	// Purpose: pop the stack of current colors that has previously been
	// pushed.
	//
	// ================================================================================
	public void popColor() {
		assert (color_stack_top > 0);
		--color_stack_top;
		color_now[0] = color_stack[0 + color_stack_top * 4];
		color_now[1] = color_stack[1 + color_stack_top * 4];
		color_now[2] = color_stack[2 + color_stack_top * 4];
		color_now[3] = color_stack[3 + color_stack_top * 4];
		ColorLibrary.sharedColorLibrary().complimentColor(color_now, compl_now);
	}// end popColor:

	// ========== pushTexture:
	// ========================================================
	//
	// Purpose: change the current texture to a new one, specified by a spec
	// with
	// textures and projection.
	//
	// ================================================================================
	public void pushTexture(LDrawTextureSpec spec) {
		assert (texture_stack_top < TEXTURE_STACK_DEPTH);

		try {
			tex_stack[texture_stack_top] = (LDrawTextureSpec) tex_now.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// memcpy(tex_stack+texture_stack_top,&tex_now,sizeof(tex_now));
		++texture_stack_top;
		// memcpy(&tex_now,spec,sizeof(tex_now));
		try {
			tex_now = (LDrawTextureSpec) spec.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// if (dl_stack_top != 0)
		// dl_now.setTex(tex_now);

	}// end pushTexture:

	// ========== popTexture:
	// =========================================================
	//
	// Purpose: pop a texture off the stack that was previously pushed.
	// When
	// the
	// last texture is popped, we go back to being untextured.
	//
	// ================================================================================
	public void popTexture() {
		assert (texture_stack_top > 0);
		--texture_stack_top;
		try {
			tex_now = (LDrawTextureSpec) (tex_stack[texture_stack_top].clone());
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// if (dl_stack_top != 0)
		// dl_now.setTex(tex_now);

	}// end popTexture:

	// ========== pushWireFrame:
	// ======================================================
	//
	// Purpose: push a change to wire frame mode. This is nested - when
	// the
	// last
	// "wire frame" is popped, we are no longer wire frame.
	//
	// ================================================================================
	public void pushWireFrame(GL2 gl2) {
		synchronized (gl2) {
			if (isWireFrame == false && useWireFrame) {
				isWireFrame = true;
				gl2.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
			}
		}

	}// end pushWireFrame:

	// ========== popWireFrame:
	// =======================================================
	//
	// Purpose: undo a previous wire frame command - the push and pops
	// must
	// be
	// balanced.
	//
	// ================================================================================
	public void popWireFrame(GL2 gl2) {
		synchronized (gl2) {
			if (isWireFrame == true) {
				isWireFrame = false;
				gl2.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
			}
		}

	}// end popWireFrame:

	// ========== drawQuad:normal:color:
	// ==============================================
	//
	// Purpose: Adds one quad to the current display list.
	//
	// Notes: This should only be called after a dlBegin has been called;
	// client
	// code only gets a protocol interface to this API by calling beginDL
	// first.
	//
	// ================================================================================
	public void drawQuad(float[] vertices, float[] normal, float[] color) {
		assert (dl_stack_top == 0);
		float c[] = new float[4];

		set_color4fv(color, c);

		dl_now.addQuad(vertices, normal, c);

	}// end drawQuad:normal:color:

	// ========== drawTri:normal:color:
	// ===============================================
	//
	// Purpose: Adds one triangle to the current display list.
	//
	// ================================================================================
	public void drawTri(float[] vertices, float[] normal, float[] color) {
		assert (dl_stack_top == 0);

		float c[] = new float[4];
		;

		set_color4fv(color, c);

		dl_now.addTri(vertices, normal, c);

	}// end drawTri:normal:color:

	// ========== drawLine:normal:color:
	// ==============================================
	//
	// Purpose: Adds one line to the current display list.
	//
	// ================================================================================
	public void drawLine(float[] vertices, float[] normal, float[] color) {
		assert (dl_stack_top == 0);

		float c[] = new float[4];

		set_color4fv(color, c);
		
		dl_now.addLine(vertices, normal, c);
	}// end drawLine:normal:color:

	// ========== drawDragHandle:withSize:
	// ============================================
	//
	// Purpose: This draws one drag handle using the current transform.
	//
	// Notes: We don't draw anything - we just grab a list link and stash
	// the
	// drag handle in "global model space" - that is, the space that the
	// root of all drawing happens, without the local part transform.
	// We do that so that when we pop out all local transforms and draw
	// later we will be in the right place, but we'll have no local
	// scaling
	// that could deform our handle.
	//
	// ================================================================================
	public void drawDragHandle(float[] xyz, float size) {
		LDrawDragHandleInstance dh = new LDrawDragHandleInstance();

		dh.setNext(drag_handles);
		drag_handles = dh;
		dh.setSize(7.0f);

		float handle_local[] = { xyz[0], xyz[1], xyz[2], 1.0f };
		float handle_world[] = new float[4];

		GLMatrixMath.applyMatrix(handle_world, transform_now, handle_local);

		float[] xyz_dh = dh.getXyz();
		xyz_dh[0] = handle_world[0];
		xyz_dh[1] = handle_world[1];
		xyz_dh[2] = handle_world[2];
		dh.setSize(size);

	}// end drawDragHandle:withSize:

	// ========== drawDragHandle:withSize:
	// ============================================
	//
	// Purpose: Draw a drag handle - for realzies this time
	//
	// Notes: This routine builds a one-off sphere VBO as needed.
	// BrickSmith
	// guarantees that we never lose our shared group of GL contexts, so
	// we
	// don't have to worry about the last context containing the VBO
	// going
	// away.
	//
	// The vertex format for the sphere handle is just pure vertices -
	// since
	// the draw routine sets up its own VAO with its own internal format,
	// there's no need to depend on or conform to vertex formats for the
	// rest
	// of the drawing system.
	//
	// ================================================================================
	public static IntBuffer vaoTag = IntBuffer.allocate(1);
	public static IntBuffer vboTag = IntBuffer.allocate(1);
	public static IntBuffer vboVertexCount = IntBuffer.allocate(1);

	public void drawDragHandleImm(GL2 gl2, float xyz, float size) {
		if (vaoTag.get() == 0) {
			// Bail if we've already done it.

			int latitudeSections = 8;
			int longitudeSections = 8;

			float latitudeRadians = (float) (Math.PI / latitudeSections); // lat.
																			// wraps
			// halfway
			// around sphere
			float longitudeRadians = (float) (2 * Math.PI / longitudeSections); // long.
																				// wraps
			// all
			// the way
			IntBuffer vertexCount = IntBuffer.allocate(1);
			ByteBuffer vertexes;
			int latitudeCount = 0;
			int longitudeCount = 0;
			float latitude = 0;
			float longitude = 0;

			// ---------- Generate Sphere
			// -----------------------------------------------

			// Each latitude strip begins with two vertexes at the prime
			// meridian,
			// then
			// has two more vertexes per segment thereafter.
			vertexCount.put((2 + longitudeSections * 2) * latitudeSections);

			gl2.glGenBuffers(1, vboTag);
			gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboTag.get());

			gl2.glBufferData(GL2.GL_ARRAY_BUFFER, vertexCount.get() * 3, null,
					GL2.GL_STATIC_DRAW);
			vertexes = gl2.glMapBuffer(GL2.GL_ARRAY_BUFFER, GL2.GL_WRITE_ONLY);

			// Calculate vertexes for each strip of latitude.
			for (latitudeCount = 0; latitudeCount < latitudeSections; latitudeCount += 1) {
				latitude = (latitudeCount * latitudeRadians);

				// Include the prime meridian twice; once to start the strip and
				// once
				// to
				// complete the last triangle of the -1 meridian.
				for (longitudeCount = 0; longitudeCount <= longitudeSections; longitudeCount += 1) {
					longitude = longitudeCount * longitudeRadians;

					// Ben says: when we are "pushing" vertices into a
					// GL_WRITE_ONLY
					// mapped
					// buffer, we should really
					// never read back from the vertices that we read to - the
					// memory we
					// are
					// writing to often has funky
					// properties like being uncached which make it expensive to
					// do
					// anything
					// other than what we said we'd
					// do (and we said: we are only going to write to them).
					//
					// Mind you it's moot in this case since we only need to
					// write
					// vertices.

					// Top vertex
					vertexes.put(0,
							(byte) (Math.cos(longitude) * Math.sin(latitude)));
					vertexes.put(1,
							(byte) (Math.sin(longitude) * Math.sin(latitude)));
					vertexes.put(2, (byte) (Math.cos(latitude)));
					// *vertexes++ =cos(longitude)*sin(latitude);
					// *vertexes++ =sin(longitude)*sin(latitude);
					// *vertexes++ =cos(latitude);

					// Bottom vertex
					vertexes.put(
							3,
							(byte) (Math.cos(longitude) * Math.sin(latitude
									+ latitudeRadians)));
					vertexes.put(
							4,
							(byte) (Math.sin(longitude) * Math.sin(latitude
									+ latitudeRadians)));
					vertexes.put(5,
							(byte) (Math.cos(latitude + latitudeRadians)));
					// *vertexes++ = cos(longitude)*sin(latitude +
					// latitudeRadians);
					// *vertexes++ = sin(longitude)*sin(latitude +
					// latitudeRadians);
					// *vertexes++ = cos(latitude + latitudeRadians);
				}
			}

			gl2.glUnmapBuffer(GL2.GL_ARRAY_BUFFER);
			gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

			// ---------- Optimize
			// ------------------------------------------------------

			vboVertexCount = vertexCount;

			// Encapsulate in a VAO
			gl2.glGenVertexArrays(1, vaoTag);
			gl2.glBindVertexArray(vaoTag.get());
			gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboTag.get());
			gl2.glEnableVertexAttribArray(AttributeT.attr_position.getValue());
			gl2.glEnableVertexAttribArray(AttributeT.attr_normal.getValue());
			// Normal and vertex use the same data - in a unit sphere the
			// normals
			// are
			// the vertices!
			gl2.glVertexAttribPointer(AttributeT.attr_position.getValue(), 3,
					GL2.GL_FLOAT, false, 3, (Buffer) (IntBuffer.allocate(0)));
			gl2.glVertexAttribPointer(AttributeT.attr_normal.getValue(), 3,
					GL2.GL_FLOAT, false, 3, (Buffer) (IntBuffer.allocate(0)));
			// The sphere color is constant - no need to get it from per-vertex
			// data.
			gl2.glBindVertexArray(0);
			gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

		}

		gl2.glDisable(GL2.GL_TEXTURE_2D);

		int i;
		for (i = 0; i < 4; ++i)
			gl2.glVertexAttrib4f(AttributeT.attr_transform_x.getValue() + i,
					transform_now[i], transform_now[4 + i],
					transform_now[8 + i], transform_now[12 + i]);

		gl2.glVertexAttrib4f(AttributeT.attr_color.getValue(), 0.50f, 0.53f,
				1.00f, 1.00f); // Nice lavendar
		// color
		// for the whole sphere.

		gl2.glBindVertexArray(vaoTag.get());
		gl2.glDrawArrays(GL2.GL_TRIANGLE_STRIP, 0, vboVertexCount.get());
		gl2.glBindVertexArray(0); // Failing to unbind can cause bizarre
		// crashes
		// if other VAOs are in display lists

		gl2.glEnable(GL2.GL_TEXTURE_2D);

	}// end drawDragHandleImm:

	// ========== beginDL:
	// ============================================================
	//
	// Purpose: This begins accumulating a display list.
	//
	// ================================================================================
	public ILDrawCollector beginDL() {
		assert (dl_stack_top < DL_STACK_DEPTH);

		dl_stack[dl_stack_top] = dl_now;
		++dl_stack_top;
		dl_now = new LDrawDLBuilder();

		return this;

	}// end beginDL:

	// ========== endDL:cleanupFunc:
	// ==================================================
	//
	// Purpose: close off a DL, returning the display list if there is
	// one.
	//
	// ================================================================================
	public ILDrawDLHandle endDL(GL2 gl2, LDrawDLCleanup_f func) {
		assert (dl_stack_top > 0);
		LDrawDL dl = dl_now != null ? dl_now.finish(gl2) : null;
		--dl_stack_top;
		dl_now = dl_stack[dl_stack_top];

		ILDrawDLHandle outHandle = (ILDrawDLHandle) dl;
		return outHandle;

	}// end endDL:cleanupFunc:

	// ========== drawDL:
	// =============================================================
	//
	// Purpose: draw a DL using the current state. We pass this to our DL
	// session
	// that sorts out how to actually do tihs.
	//
	// ================================================================================
	public void drawDL(GL2 gl2, ILDrawDLHandle dl) {
		if (drawTransparent == false && color_now[3] != 1.0)
			return;
		if (drawTransparent && color_now[3] == 1.0)
			return;
		
		synchronized (this) {
			dl.draw(gl2, session, tex_now, color_now, compl_now, transform_now,
					false);
		}
	}// end drawDL:

	@Deprecated
	public LDrawDLBuilder getDLBuilderNow() {
		return dl_now;
	}
}
