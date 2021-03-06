package org.powerbot.script;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.powerbot.bot.MouseSpline;

/**
 * Input
 * A utility class for generating input to the canvas and retrieving information from the canvas.
 */
public abstract class Input {
	protected final AtomicBoolean blocking;
	private final MouseSpline spline;
	private final AtomicInteger speed;

	protected Input() {
		blocking = new AtomicBoolean(false);
		spline = new MouseSpline();
		speed = new AtomicInteger(100);
	}

	/**
	 * Set the relative speed for mouse movements.
	 * This is a sensitive function and should be used in exceptional circumstances for a short period of time only.
	 *
	 * @param s the new speed as a percentage, i.e. {@code 10} is 10x faster, {@code 25} is 4x as fast
	 *          and {@code 100} is the full speed. Specifying {@code 0} will not change the speed but return the
	 *          current value instead.
	 * @return the speed, which can be different to the value requested
	 */
	public int speed(final int s) {
		speed.set(Math.min(100, Math.max(10, s)));
		return speed.get();
	}

	// TODO: remove boolean return values for input methods

	public final boolean blocking() {
		return blocking.get();
	}

	public void blocking(final boolean b) {
		blocking.set(b);
	}

	public abstract void focus();

	public abstract void defocus();

	public abstract boolean send(final String s);

	public final boolean sendln(final String s) {
		return send(s + "\n");
	}

	public abstract Point getLocation();

	public abstract Point getPressLocation();

	public abstract long getPressWhen();

	public abstract boolean press(final int button);

	public abstract boolean release(final int button);

	protected abstract boolean setLocation(final Point p);

	public final boolean click(final int x, final int y, final int button) {
		return click(new Point(x, y), button);
	}

	public final boolean click(final int x, final int y, final boolean left) {
		return click(new Point(x, y), left);
	}

	public final boolean click(final Point point, final int button) {
		return move(point) && click(button);
	}

	public final boolean click(final Point point, final boolean left) {
		return move(point) && click(left);
	}

	public final boolean click(final boolean left) {
		return click(left ? MouseEvent.BUTTON1 : MouseEvent.BUTTON3);
	}

	public final boolean click(final int button) {
		press(button);
		Condition.sleep(spline.getPressDuration());
		release(button);
		Condition.sleep(spline.getPressDuration());
		return true;
	}

	public final boolean drag(final Point p, final boolean left) {
		return drag(p, left ? MouseEvent.BUTTON1 : MouseEvent.BUTTON3);
	}

	public final boolean drag(final Point p, final int button) {
		press(button);
		final boolean b = move(p);
		release(button);
		return b;
	}

	public final boolean hop(final Point p) {
		return setLocation(p);
	}

	public final boolean hop(final int x, final int y) {
		return hop(new Point(x, y));
	}

	public final boolean move(final int x, final int y) {
		return move(new Point(x, y));
	}

	public final boolean move(final Point p) {
		return apply(
				new Targetable() {
					@Override
					public Point nextPoint() {
						return p;
					}

					@Override
					public boolean contains(final Point point) {
						return p.equals(point);
					}
				},
				new Filter<Point>() {
					@Override
					public boolean accept(final Point point) {
						return p.equals(point);
					}
				}
		);
	}

	public final boolean apply(final Targetable targetable, final Filter<Point> filter) {
		final Point target_point = new Point(-1, -1);
		final int STANDARD_ATTEMPTS = 3;
		for (int i = 0; i < STANDARD_ATTEMPTS; i++) {
			final Point mp = getLocation();
			final Vector3 start = new Vector3(mp.x, mp.y, 255);
			final Point p = targetable.nextPoint();
			if (p.x == -1 || p.y == -1) {
				continue;
			}
			target_point.move(p.x, p.y);
			final Vector3 end = new Vector3(p.x, p.y, 0);
			final Iterable<Vector3> spline = this.spline.getPath(start, end);
			for (final Vector3 v : spline) {
				hop(v.x, v.y);
				Condition.sleep((int) (this.spline.getAbsoluteDelay(v.z) * (speed.get() / 100d) / 1.33e6));
			}
			final Point p2 = getLocation(), ep = end.toPoint2D();
			if (p2.equals(ep) && filter.accept(ep)) {
				return true;
			}
		}
		return false;
	}

	public final boolean scroll() {
		return scroll(true);
	}

	public abstract boolean scroll(final boolean down);

	public abstract Dimension getComponentSize();
}
