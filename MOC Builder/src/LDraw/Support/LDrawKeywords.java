package LDraw.Support;

public class LDrawKeywords {
	// MPD
	public static final String LDRAW_MPD_SUBMODEL_START = "FILE";
	public static final String LDRAW_MPD_SUBMODEL_END = "NOFILE";
	//Comment markers
	public static final String LDRAW_COMMENT_WRITE="WRITE";
	public static final String LDRAW_COMMENT_PRINT="PRINT";
	public static final String LDRAW_COMMENT_SLASH="//";

	// Color definition
	public static final String LDRAW_COLOR_DEFINITION="!COLOUR";
	public static final String LDRAW_COLOR_DEF_CODE="CODE";
	public static final String LDRAW_COLOR_DEF_VALUE="VALUE";
	public static final String LDRAW_COLOR_DEF_EDGE="EDGE";
	public static final String LDRAW_COLOR_DEF_ALPHA="ALPHA";
	public static final String LDRAW_COLOR_DEF_LUMINANCE="LUMINANCE";
	public static final String LDRAW_COLOR_DEF_MATERIAL_CHROME="CHROME";
	public static final String LDRAW_COLOR_DEF_MATERIAL_PEARLESCENT="PEARLESCENT";
	public static final String LDRAW_COLOR_DEF_MATERIAL_RUBBER="RUBBER";
	public static final String LDRAW_COLOR_DEF_MATERIAL_MATTE_METALLIC="MATTE_METALLIC";
	public static final String LDRAW_COLOR_DEF_MATERIAL_METAL="METAL";
	public static final String LDRAW_COLOR_DEF_MATERIAL_CUSTOM="MATERIAL";
	// Model header
	public static final String LDRAW_HEADER_NAME = "Name:";
	public static final String LDRAW_HEADER_AUTHOR = "Author:";
	public static final String LDRAW_CATEGORY = "!CATEGORY";
	public static final String LDRAW_KEYWORDS = "!KEYWORDS";
	public static final String LDRAW_ORG = "!LDRAW_ORG";
	// Steps and Rotation Steps
	public static final String LDRAW_STEP_TERMINATOR="STEP";
	public static final String LDRAW_ROTATION_STEP_TERMINATOR="ROTSTEP";
	public static final String LDRAW_ROTATION_END="END";
	public static final String LDRAW_ROTATION_RELATIVE="REL";
	public static final String LDRAW_ROTATION_ABSOLUTE="ABS";
	public static final String LDRAW_ROTATION_ADDITIVE="ADD";
	
	//BFC
	public static final String LDRAW_BFC="BFC";
	
	//MOC Builder Specific
	public static final String MOCBUILDER_CUSTOM_META="!MOCBUILDER";
	public static final String MOCBUILDER_STEP_NAME="STEPNAME";

	// Textures
	public static final String LDRAW_TEXTURE	="!TEXMAP";
	public static final String LDRAW_TEXTURE_GEOMETRY="!:";
	public static final String LDRAW_TEXTURE_METHOD_PLANAR="PLANAR";
	public static final String LDRAW_TEXTURE_START="START";
	public static final String LDRAW_TEXTURE_NEXT="NEXT";
	public static final String LDRAW_TEXTURE_FALLBACK="FALLBACK";
	public static final String LDRAW_TEXTURE_END="END";
	public static final String LDRAW_TEXTURE_GLOSSMAP="GLOSSMAP";

	// Important Categories
	public static final String LDRAW_MOVED_CATEGORY="Moved";
	public static final String LDRAW_MOVED_DESCRIPTION_PREFIX="~Moved to";

	// LSynth
	public static final String LSYNTH_COMMAND="SYNTH";
	public static final String LSYNTH_SHOW   ="SHOW";
	public static final String LSYNTH_BEGIN  ="BEGIN";
	public static final String LSYNTH_END    ="END";
	public static final String LSYNTH_SYNTHESIZED="SYNTHESIZED";

}
