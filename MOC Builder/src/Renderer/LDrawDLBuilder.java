package Renderer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import javax.media.opengl.GL2;

import LDraw.Support.MatrixMath;

import com.jogamp.common.nio.Buffers;

public class LDrawDLBuilder {

	/**
	 * @uml.property name="flags"
	 * @uml.associationEnd 
	 *                     qualifier="dl_has_meta:Renderer.LDrawDLT java.lang.Boolean"
	 */
	HashMap<LDrawDLT, Boolean> flags;

	/**
	 * @uml.property name="head"
	 * @uml.associationEnd
	 */
	LDrawDLBuilderPerTex head = null;;
	/**
	 * @uml.property name="cur"
	 * @uml.associationEnd
	 */
	LDrawDLBuilderPerTex cur = null;

	// ========== LDrawDLBuilderCreate
	// ================================================
	//
	// Purpose: Create a new builder capable of accumulating DL data.
	//
	// ================================================================================
	public LDrawDLBuilder() {
		flags = new HashMap<LDrawDLT, Boolean>();
		// All allocs for the builder come from one pool.

		// Build one tex now for the untextured set of meshes, which are
		// the default state.
		LDrawDLBuilderPerTex untex = new LDrawDLBuilderPerTex();

		setCur(untex);
		setHead(untex);
	}

	/**
	 * @param head
	 * @uml.property name="head"
	 */
	private void setHead(LDrawDLBuilderPerTex head) {
		this.head = head;
	}

	/**
	 * @param cur
	 * @uml.property name="cur"
	 */
	private void setCur(LDrawDLBuilderPerTex cur) {
		this.cur = cur;

	}

	// ========== LDrawDLBuilderAddQuad
	// ===============================================
	//
	// Purpose: Add one quad to the current DL builder in the current texture.
	//
	// ================================================================================
	public void addQuad(float[] v, float[] n, float[] c) {
		if (MatrixMath.compareFloat(c[3], 0.0f) == 0)
			flags.put(LDrawDLT.dl_has_meta, true);
		else if (MatrixMath.compareFloat(c[3], 1.0f) != 0)
			flags.put(LDrawDLT.dl_has_alpha, true);

		int i;
		LDrawDLBuilderVertexLink nl = new LDrawDLBuilderVertexLink(4,
				LDrawDisplayList.VERT_STRIDE);
		nl.setNext(null);
		nl.setVcount(4);
		float[] data = nl.getData();
		for (i = 0; i < 4; ++i) {
			MatrixMath.copy_vec3(data, LDrawDisplayList.VERT_STRIDE * i, v,
					i * 3);
			MatrixMath.copy_vec3(data, LDrawDisplayList.VERT_STRIDE * i + 3, n,
					0);
			MatrixMath.copy_vec4(data, LDrawDisplayList.VERT_STRIDE * i + 6, c,
					0);
		}

		if (cur.getQuad_tail() != null) {
			cur.getQuad_tail().setNext(nl);
			cur.setQuad_tail(nl);
		} else {
			cur.setQuad_head(nl);
			cur.setQuad_tail(nl);
		}
	}

	// ========== LDrawDLBuilderFinish
	// ================================================
	//
	// Purpose: Take all of the accumulated data in a DL and bake it down to one
	// final form.
	//
	// Notes: The DL is, while being built, a series of linked lists in a BDP
	// for
	// speed. The finished DL is a malloc'd block of memory, pre-sized to
	// fit the DL perfectly, and one VBO. So this routine does the counting,
	// final allocations, and copying.
	//
	// ================================================================================
	public LDrawDL finish(GL2 gl2) {
		int total_texes = 0;
		int total_tris = 0;
		int total_quads = 0;
		int total_lines = 0;

		LDrawDLBuilderVertexLink l;
		LDrawDLBuilderPerTex s;

		// Count up the total vertices we will need, for VBO space, as well
		// as the total distinct non-empty textures.
		for (s = head; s != null; s = s.getNext()) {
			if (s.getTri_head() != null || s.getLine_head() != null
					|| s.getQuad_head() != null)
				++total_texes;
			for (l = s.getTri_head(); l != null; l = l.getNext()) {
				total_tris += l.getVcount();
			}
			for (l = s.getQuad_head(); l != null; l = l.getNext()) {
				total_quads += l.getVcount();
			}
			for (l = s.getLine_head(); l != null; l = l.getNext()) {
				total_lines += l.getVcount();
			}
		}

		// No non-empty textures? Bail out early - nuke our
		// context and get out. Client code knows we get NO DL, rather than
		// an empty one.
		if (total_texes == 0) {
			return null;
		}

		// Malloc DL structure with extra storage for variable-sized tex array.
		LDrawDL dl = new LDrawDL(total_texes);

		// All per-session linked list ptrs start null.
		dl.setNext(null);
		dl.setInstance_head(null);
		dl.setInstance_tail(null);
		dl.setInstance_count(0);

		dl.setTex_count(total_texes);

		LDrawDLPerTex cur_tex = dl.getTexes()[0];
		dl.setFlags(flags);

		total_tris /= 3;
		total_quads /= 4;
		total_lines /= 2;

		// We use one mesh for the entire DL, even if it has multiple textures.
		// We have to
		// do this because we wnat smoothing across triangles that do not share
		// the same
		// texture. (Key use case: minifig faces are part textured, part
		// untextured.)
		//
		// So instead each face gets a texture ID (tid), which is an index that
		// we will tie
		// to our texture list. The mesh smoother remembers this and dumps out
		// the tris in
		// tid order later.

		Mesh M = new Mesh(total_tris, total_quads, total_lines);

		// Now: walk our building textures - for each non-empty one, we will
		// copy it into
		// the tex array and push its vertices.
		int ti = 0;
		for (s = head; s != null; s = s.getNext()) {
			if (s.getTri_head() == null && s.getLine_head() == null
					&& s.getQuad_head() == null)
				continue;
			if (s.getSpec().getTex_obj() != 0)
				dl.flags.put(LDrawDLT.dl_has_tex, true);

			float[] p1 = new float[3];
			float[] p2 = new float[3];
			float[] p3 = new float[3];
			float[] p4 = new float[3];
			float[] color = new float[4];

			for (l = s.getTri_head(); l != null; l = l.getNext()) {

				System.arraycopy(l.data, 0, p1, 0, 3);
				System.arraycopy(l.data, 10, p2, 0, 3);
				System.arraycopy(l.data, 20, p3, 0, 3);
				System.arraycopy(l.data, 6, color, 0, 4);

				M.add_face(p1, p2, p3, null, color, ti);
			}

			for (l = s.getQuad_head(); l != null; l = l.getNext()) {
				System.arraycopy(l.data, 0, p1, 0, 3);
				System.arraycopy(l.data, 10, p2, 0, 3);
				System.arraycopy(l.data, 20, p3, 0, 3);
				System.arraycopy(l.data, 30, p4, 0, 3);
				System.arraycopy(l.data, 6, color, 0, 4);

				M.add_face(p1, p2, p3, p4, color, ti);
			}
			++ti;
		}

		ti = 0;
		for (s = head; s != null; s = s.getNext()) {
			if (s.getTri_head() == null && s.getLine_head() == null
					&& s.getQuad_head() == null)
				continue;
			if (s.getSpec().getTex_obj() != 0)
				dl.getFlags().put(LDrawDLT.dl_has_tex, true);

			float[] p1 = new float[3];
			float[] p2 = new float[3];
			float[] color = new float[4];

			for (l = s.getLine_head(); l != null; l = l.getNext()) {
				System.arraycopy(l.data, 0, p1, 0, 3);
				System.arraycopy(l.data, 10, p2, 0, 3);
				System.arraycopy(l.data, 6, color, 0, 4);

				M.add_face(p1, p2, null, null, color, ti);
			}
			++ti;
		}

		M.finish_faces_and_sort();

		M.add_creases();
		M.find_and_remove_t_junctions();
		M.finish_creases_and_join();

		M.smooth_vertices();
		M.merge_vertices();

		IntBuffer total_vertices, total_indices;
		total_vertices = IntBuffer.allocate(1);
		total_indices = IntBuffer.allocate(1);
		M.get_final_mesh_counts(total_vertices, total_indices);

		ByteBuffer byteBufferForVertex = Buffers
				.newDirectByteBuffer(total_vertices.get(0)
						* LDrawDisplayList.VERT_STRIDE * Float.SIZE / 8);
		FloatBuffer vertex_ptr = byteBufferForVertex.asFloatBuffer();

		ByteBuffer byteBufferForIndex = Buffers
				.newDirectByteBuffer(total_indices.get(0) * Integer.SIZE / 8);
		IntBuffer index_ptr = byteBufferForIndex.asIntBuffer();

		// Grab variable size arrays for the start/offsets of each sub-part of
		// our big pile-o-mesh...
		// the mesher will give us back our tris sorted by texture.

		IntBuffer line_start = IntBuffer.allocate(total_texes);
		IntBuffer line_count = IntBuffer.allocate(total_texes);
		IntBuffer tri_start = IntBuffer.allocate(total_texes);
		IntBuffer tri_count = IntBuffer.allocate(total_texes);
		IntBuffer quad_start = IntBuffer.allocate(total_texes);
		IntBuffer quad_count = IntBuffer.allocate(total_texes);

		M.write_indexed_mesh(total_vertices.get(0), vertex_ptr,
				total_indices.get(0), index_ptr, 0, line_start, line_count,
				tri_start, tri_count, quad_start, quad_count);

		gl2.glGenBuffers(1, dl.geo_vbo);
		gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, dl.geo_vbo.get(0));
		gl2.glBufferData(GL2.GL_ARRAY_BUFFER, byteBufferForVertex.capacity(),
				byteBufferForVertex, GL2.GL_STATIC_DRAW);

		
		gl2.glGenBuffers(1, dl.idx_vbo);
		gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, dl.idx_vbo.get(0));
		gl2.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER,
				byteBufferForIndex.capacity(), byteBufferForIndex,
				GL2.GL_STATIC_DRAW);

		ti = 0;

		for (s = head; s != null; s = s.next) {
			try {
				cur_tex.spec = (LDrawTextureSpec) s.spec.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			cur_tex.quad_off = quad_start.get(ti);
			cur_tex.line_off = line_start.get(ti);
			cur_tex.tri_off = tri_start.get(ti);
			cur_tex.quad_count = quad_count.get(ti);
			cur_tex.line_count = line_count.get(ti);
			cur_tex.tri_count = tri_count.get(ti);

			++ti;
			cur_tex = cur_tex.next();
		}

		M.destroy_mesh();

		gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
		gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);

		return dl;
	}

	
	// ========== LDrawDLBuilderAddLine
	// ===============================================
	//
	// Purpose: Add one line to the current DL builder in the current texture.
	//
	// ================================================================================
	public void addLine(float[] v, float[] n, float[] c) {
		if (MatrixMath.compareFloat(c[3], 0.0f) == 0)
			flags.put(LDrawDLT.dl_has_meta, true);
		else if (MatrixMath.compareFloat(c[3], 1.0f) != 0)
			flags.put(LDrawDLT.dl_has_alpha, true);

		int i;
		LDrawDLBuilderVertexLink nl = new LDrawDLBuilderVertexLink(2,
				LDrawDisplayList.VERT_STRIDE);
		nl.setNext(null);
		nl.setVcount(2);
		
		for (i = 0; i < 2; ++i) {
			MatrixMath.copy_vec3(nl.data, LDrawDisplayList.VERT_STRIDE * i, v,
					i * 3);
			MatrixMath.copy_vec3(nl.data, LDrawDisplayList.VERT_STRIDE * i + 3,
					n, 0);
			MatrixMath.copy_vec4(nl.data, LDrawDisplayList.VERT_STRIDE * i + 6,
					c, 0);
		}

		if (cur.getLine_tail() != null) {
			cur.getLine_tail().setNext(nl);
			cur.setLine_tail(nl);
		} else {
			cur.setLine_head(nl);
			cur.setLine_tail(nl);
		}

	}

	// ========== LDrawDLBuilderAddTri
	// ================================================
	//
	// Purpose: Add one triangle to our DL using the current texture.
	//
	// Notes: This routine 'sniffs' the alpha as it goes by and keeps the DL
	// flags
	// correct - this is how a DL "knows" if it is translucent.
	//
	// We accumulate the tri by allocating a 3-vertex DL link and queueing it
	// onto the triangle list for the current texture.
	//
	// ================================================================================

	public void addTri(float[] v, float[] n, float[] c) {
		// Alpha = 0 means meta color. 0 < Alpha < 1 means translucency.
		if (MatrixMath.compareFloat(c[3], 0.0f) == 0)
			flags.put(LDrawDLT.dl_has_meta, true);
		else if (MatrixMath.compareFloat(c[3], 1.0f) != 0)
			flags.put(LDrawDLT.dl_has_alpha, true);

		int i;
		LDrawDLBuilderVertexLink nl = new LDrawDLBuilderVertexLink(3,
				LDrawDisplayList.VERT_STRIDE);
		nl.setNext(null);
		nl.setVcount(3);
		float data[] = nl.getData();
		for (i = 0; i < 3; ++i) {
			MatrixMath.copy_vec3(data, LDrawDisplayList.VERT_STRIDE * i, v,
					i * 3); // Vertex data is per
			// vertex.
			MatrixMath.copy_vec3(data, LDrawDisplayList.VERT_STRIDE * i + 3, n,
					0);// But color and norm
			// are for the whole
			// tri, for now. So
			// we replicate it
			// out to get
			MatrixMath.copy_vec4(data, LDrawDisplayList.VERT_STRIDE * i + 6, c,
					0);// a uniform DL.
		}

		if (cur.getTri_tail() != null) {
			cur.getTri_tail().setNext(nl);
			cur.setTri_tail(nl);
		} else {
			cur.setTri_head(nl);
			cur.setTri_tail(nl);
		}

	}
}
