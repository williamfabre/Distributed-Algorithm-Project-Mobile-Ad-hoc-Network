package ara.util;

public class Pair<T1,T2> {

	private T1 id;
	private T2 bool;
	
	/**
	 * @param id
	 * @param bool
	 */
	public Pair(T1 id, T2 bool) {
		this.id = id;
		this.bool = bool;
	}

	public T1 getId() {
		return id;
	}

	public void setId(T1 id) {
		this.id = id;
	}

	public T2 getBool() {
		return bool;
	}

	public void setBool(T2 bool) {
		this.bool = bool;
	}
	
	@Override
	public String toString() {
		return "<id : " + id + " bool : " + bool+">";
	}
}
