package luke.sky.wunderground;

import java.util.List;


public interface DbAdapter
{
	public void sendMeasurement(
	 final String category,
	 final String name,
	 final Long value,
	 final List<Dimension> dim
	);
}
