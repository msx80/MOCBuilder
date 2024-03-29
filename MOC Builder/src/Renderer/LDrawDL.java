package Renderer;

import java.nio.IntBuffer;
import java.util.HashMap;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import LDraw.Support.LDrawGlobalFlag;
import LDraw.Support.MatrixMath;

public class LDrawDL implements ILDrawDLHandle {
	// This turns on normal smoothing.

	/**
	 * @uml.property name="next_dl"
	 * @uml.associationEnd
	 */
	LDrawDL next_dl; // Session "linked list of active dLs."
	/**
	 * @uml.property name="instance_head"
	 * @uml.associationEnd
	 */
	LDrawDLInstance instance_head; // Linked list of instances to draw.
	/**
	 * @uml.property name="instance_tail"
	 * @uml.associationEnd
	 */
	LDrawDLInstance instance_tail;
	/**
	 * @uml.property name="instance_count"
	 */
	int instance_count;
	/**
	 * @uml.property name="flags"
	 * @uml.associationEnd 
	 *                     qualifier="dl_has_tex:Renderer.LDrawDLT java.lang.Boolean"
	 */
	HashMap<LDrawDLT, Boolean> flags; // See flags defs above.
	/**
	 * @uml.property name="geo_vbo"
	 */
	IntBuffer geo_vbo; // Single VBO containing all geometry in the DL.

	// IntBuffer geo_vao; // Single VAO containing all geometry in the DL.

	// public ByteBuffer byteBufferForVertex;
	/**
	 * @uml.property name="idx_vbo"
	 */
	IntBuffer idx_vbo; // Single VBO containing all mesh indices.

	/**
	 * @uml.property name="tex_count"
	 */
	int tex_count; // Number of per-textures; untex case is always first if
					// present.
	/**
	 * @uml.property name="texes"
	 * @uml.associationEnd multiplicity="(0 -1)"
	 */
	LDrawDLPerTex texes[]; // Variable size array of textures - DL is allocated
							// larger as needed.

	public LDrawDL(int total_texes) {

		next_dl = null;
		instance_head = instance_tail = null;
		instance_count = 0;
		texes = new LDrawDLPerTex[total_texes];
		for (int i = 0; i < total_texes; i++)
			texes[i] = new LDrawDLPerTex();

		geo_vbo = IntBuffer.allocate(1);
		// geo_vao = IntBuffer.allocate(1);
		idx_vbo = IntBuffer.allocate(1);

		flags = new HashMap<LDrawDLT, Boolean>();
		flags.put(LDrawDLT.dl_has_tex, false);

	}

	// ========== LDrawDLDraw
	// =========================================================
	//
	// Purpose: Draw a DL, or save it for later drawing.
	//
	// Notes: This routine takes all of the current 'state' and draws or records
	// an instance.
	//
	// Pass draw_now as true to FORCE immediate drawing and disable all of
	// the instancing/sorting stuff. This is needed if there is extra GL2.GL
	// state like polygon offset that must be used now that isn't recorded
	// by this API.
	//
	// ================================================================================
	public void draw(GL2 gl2, LDrawDLSession session, LDrawTextureSpec spec,
			float cur_color[], float cmp_color[], float transform[],
			boolean draw_now) {
				
		// IMMEDIATE MODE DRAW CASE! If we get here, we are going to draw this
		// DL right now at this
		// position.

		// Push current transform & color into attribute state.
		int i;

		for (i = 0; i < 4; ++i)
			gl2.glVertexAttrib4f(AttributeT.attr_transform_x.getValue() + i,
					transform[i], transform[4 + i], transform[8 + i],
					transform[12 + i]);

		gl2.glVertexAttrib4fv(AttributeT.attr_color_current.getValue(),
				cur_color, 0);
		gl2.glVertexAttrib4fv(AttributeT.attr_color_compliment.getValue(),
				cmp_color, 0);

		assert (tex_count > 0);

		// Bind our DL VBO and set up ptrs.
		gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, geo_vbo.get(0));

		if (LDrawGlobalFlag.WANT_SMOOTH != false) {
			gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, idx_vbo.get(0));
		}
		gl2.glVertexAttribPointer(AttributeT.attr_position.getValue(), 3,
				GL2.GL_FLOAT, false, LDrawDisplayList.VERT_STRIDE * Float.SIZE
						/ 8, 0);
		gl2.glVertexAttribPointer(AttributeT.attr_normal.getValue(), 3,
				GL2.GL_FLOAT, false, LDrawDisplayList.VERT_STRIDE * Float.SIZE
						/ 8, 3 * Float.SIZE / 8);
		gl2.glVertexAttribPointer(AttributeT.attr_color.getValue(), 4,
				GL2.GL_FLOAT, false, LDrawDisplayList.VERT_STRIDE * Float.SIZE
						/ 8, 6 * Float.SIZE / 8);

		LDrawDLPerTex tptr = texes[0];
		int tptr_index = 0;

		if (tex_count == 1 && tptr.spec.tex_obj == 0
				&& (spec == null || spec.tex_obj == 0)) {			
			// Special case: one untextured mesh - just draw.
			if (LDrawGlobalFlag.WANT_SMOOTH != false) {
				if (tptr.tri_count != 0)
					gl2.glDrawElements(GL2.GL_TRIANGLES, tptr.tri_count,
							GL2.GL_UNSIGNED_INT, tptr.tri_off * Float.SIZE / 8);
				if (tptr.quad_count != 0)
					gl2.glDrawElements(GL2.GL_QUADS, tptr.quad_count,
							GL2.GL_UNSIGNED_INT, tptr.quad_off * Float.SIZE / 8);

				if (tptr.line_count != 0)
					gl2.glDrawElements(GL2.GL_LINES, tptr.line_count,
							GL2.GL_UNSIGNED_INT, tptr.line_off * Float.SIZE / 8);
			}
		} else {
			// Textured case - for each texture set up the DL texture (or
			// current
			// texture if none), then draw.
			int t;
			for (t = 0; t < tex_count; ++t, tptr = texes[++tptr_index]) {
				if (tptr.spec.tex_obj != 0) {
					LDrawDisplayList.setup_tex_spec(gl2, tptr.spec);
				} else
					LDrawDisplayList.setup_tex_spec(gl2, spec);

				if (LDrawGlobalFlag.WANT_SMOOTH != false) {
					if (tptr.tri_count != 0)
						gl2.glDrawElements(GL2.GL_TRIANGLES, tptr.tri_count,
								GL2.GL_UNSIGNED_INT, tptr.tri_off);
					if (tptr.quad_count != 0)
						gl2.glDrawElements(GL2.GL_QUADS, tptr.quad_count,
								GL2.GL_UNSIGNED_INT, tptr.quad_off);
					if (tptr.line_count != 0)
						gl2.glDrawElements(GL2.GL_LINES, tptr.line_count,
								GL2.GL_UNSIGNED_INT, tptr.line_off);
				}
			}

			LDrawDisplayList.setup_tex_spec(gl2, spec);
		}
	}// end LDrawDLDraw

	/**
	 * @return
	 * @uml.property name="next_dl"
	 */
	public LDrawDL getNext() {
		return next_dl;
	}

	/**
	 * @param next_dl
	 * @uml.property name="next_dl"
	 */
	public void setNext(LDrawDL next_dl) {
		this.next_dl = next_dl;
	}

	/**
	 * @return
	 * @uml.property name="instance_head"
	 */
	public LDrawDLInstance getInstance_head() {
		return instance_head;
	}

	/**
	 * @param instance_head
	 * @uml.property name="instance_head"
	 */
	public void setInstance_head(LDrawDLInstance instance_head) {
		this.instance_head = instance_head;
	}

	/**
	 * @return
	 * @uml.property name="instance_tail"
	 */
	public LDrawDLInstance getInstance_tail() {
		return instance_tail;
	}

	/**
	 * @param instance_tail
	 * @uml.property name="instance_tail"
	 */
	public void setInstance_tail(LDrawDLInstance instance_tail) {
		this.instance_tail = instance_tail;
	}

	/**
	 * @return
	 * @uml.property name="instance_count"
	 */
	public int getInstance_count() {
		return instance_count;
	}

	/**
	 * @param instance_count
	 * @uml.property name="instance_count"
	 */
	public void setInstance_count(int instance_count) {
		this.instance_count = instance_count;
	}

	public HashMap<LDrawDLT, Boolean> getFlags() {
		return flags;
	}

	public void setFlags(HashMap<LDrawDLT, Boolean> flags) {
		this.flags.putAll(flags);
	}

	/**
	 * @return
	 * @uml.property name="geo_vbo"
	 */
	public IntBuffer getGeo_vbo() {
		return geo_vbo;
	}

	/**
	 * @param geo_vbo
	 * @uml.property name="geo_vbo"
	 */
	public void setGeo_vbo(IntBuffer geo_vbo) {
		this.geo_vbo = geo_vbo;
	}

	/**
	 * @return
	 * @uml.property name="idx_vbo"
	 */
	public IntBuffer getIdx_vbo() {
		return idx_vbo;
	}

	/**
	 * @param idx_vbo
	 * @uml.property name="idx_vbo"
	 */
	public void setIdx_vbo(IntBuffer idx_vbo) {
		this.idx_vbo = idx_vbo;
	}

	/**
	 * @return
	 * @uml.property name="tex_count"
	 */
	public int getTex_count() {
		return tex_count;
	}

	/**
	 * @param tex_count
	 * @uml.property name="tex_count"
	 */
	public void setTex_count(int tex_count) {
		this.tex_count = tex_count;
	}

	/**
	 * @return
	 * @uml.property name="texes"
	 */
	public LDrawDLPerTex[] getTexes() {
		return texes;
	}

	/**
	 * @param texes
	 * @uml.property name="texes"
	 */
	public void setTexes(LDrawDLPerTex[] texes) {
		this.texes = texes;
	}

	// ========== LDrawDLDestroy
	// ======================================================
	//
	// Purpose: free a display list - release GL2.GL and system memory.
	//
	// ================================================================================
	public void destroy(GL2 gl2) {
		if (instance_head != null) {
			// Special case: if our DL is destroyed WHILE a session is using it
			// for
			// deferred drawing, we do NOT destroy it - we mark it for
			// destruction
			// later and the session nukes it. This is needed for the case where
			// client code creates a DL, draws it, and immediately destroys it,
			// as
			// a silly way to get 'immediate' drawing. In this case, the session
			// may have intentionally deferred the DL.
			flags.put(LDrawDLT.dl_needs_destroy, true);
			;
			return;
		}
		// Make sure that no instances from a session are queued to this list;
		// if we
		// are in Q and run now, we'll cause seg faults later. This assert hits
		// when: (1) we build a temp DL and don't mark it as temp or (2) we for
		// some
		// reason inval a DL mid-draw, which is usually a sign of coding error.
		assert (instance_head == null);
		if (LDrawGlobalFlag.WANT_SMOOTH != false) {
			gl2.glDeleteBuffers(1, idx_vbo);
		}
		gl2.glDeleteBuffers(1, geo_vbo);
		// gl2.glDeleteVertexArrays(1, geo_vao);

	}// end LDrawDLDestroy

}
