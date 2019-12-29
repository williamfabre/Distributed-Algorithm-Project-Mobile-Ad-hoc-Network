package ara.manet.positioning;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public final class Position {

	private final double x;
	private final double y;

	public Position(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	/**
	 * 
	 * @param other
	 * @return la distance entre la position this et la position other
	 */
	public double distance(Position other) {
		double tmpX = other.x - x;
		double tmpY = other.y - y;
		double distance = Math.sqrt(tmpX * tmpX + tmpY * tmpY);// en metre
		return distance;
	}

	/**
	 * Calcul d'une nouvelle position à partir d'un module et d'un angle depuis la
	 * position courante.
	 * 
	 * @param module,
	 *            distance de puis la position cournate
	 * @param angle,
	 *            en radian, 0 indique le nord, pi/2 indique l'est, pi indique le
	 *            sud, 3pi/2 indique l'ouest
	 * @return une nouvelle position à partir de la position courante
	 */
	public Position getNewPositionWith(double module, double angle) {

		double new_x = Math.sin(angle) * module + x;
		double new_y = Math.cos(angle) * module + y;
		return new Position(new_x, new_y);
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (!(other instanceof Position))
			return false;
		Position pos_other = (Position) other;
		return pos_other.x == x && pos_other.y == y;
	}

	@Override
	public String toString() {
		return "( " + x + " , " + y + " )";
	}

	public Position bound(double minX, double minY, double maxX, double maxY) {
		double x_res = x;
		double y_res = y;
		if (x_res - minX < 0.0) {
			x_res = minX;
		}
		if (y_res - minY < 0.0) {
			y_res = minY;
		}
		if (x_res - maxX > 0.0) {
			x_res = maxX;
		}
		if (y_res - maxY > 0.0) {
			y_res = maxY;
		}
		return new Position(x_res, y_res);
	}

}
