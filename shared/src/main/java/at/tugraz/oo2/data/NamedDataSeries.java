package at.tugraz.oo2.data;

import lombok.Data;

@Data
public class NamedDataSeries {
	private final String name;
	private final DataSeries dataSeries;
}
