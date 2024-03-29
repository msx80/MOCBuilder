package LDraw.Support.type;

public enum LDrawDomainT {
	LDrawUserOfficial(0), LDrawUserUnofficial(1), LDrawInternalOfficial(2), LDrawInternalUnofficial(
			3);

	/**
	 * @uml.property  name="value"
	 */
	private int value;

	private LDrawDomainT(int value) {
		this.value = value;
	}
}
