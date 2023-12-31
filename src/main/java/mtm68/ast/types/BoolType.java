package mtm68.ast.types;

public class BoolType implements Type {

	@Override
	public TypeType getTypeType() {
		return TypeType.BOOL;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof BoolType;
	}
	
	@Override
	public String toString() {
		return "bool";
	}

	@Override
	public String getPP() {
		return this.toString();
	}
}
