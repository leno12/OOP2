package at.tugraz.oo2.client.ui.component;

import com.sun.javafx.scene.control.DatePickerContent;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.AccessibleRole;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.skin.DatePickerSkin;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.controlsfx.control.PopOver;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.TimeZone;

public class DateTimePicker extends ComboBox<LocalDateTime> {


	private LocalDateTime localDateTime;
	private DatePicker fakeDatePicker;
	private PopOver popOver;
	private Label hour;
	private Slider hourSlider;
	private Label minute;
	private Slider minuteSlider;
	private Label second;
	private Slider secondSlider;
	private DatePickerContent content;

	public DateTimePicker() {
		this("DateTimePicker");
	}

	public DateTimePicker(String name) {
		this(FXCollections.observableArrayList(), name);
	}

	public DateTimePicker(ObservableList<LocalDateTime> items, String name) {
		super(items);
		this.setEditable(true);
		setAccessibleRole(AccessibleRole.DATE_PICKER);
		getStyleClass().add("date-picker");

		hour = new Label("00");
		hour.setPadding(new Insets(0, 0, 0, 10));
		hourSlider = new Slider(0, 23, 0);
		hourSlider.setPadding(new Insets(5, 0, 5, 0));
		minute = new Label("00");
		minute.setPadding(new Insets(0, 0, 0, 10));
		minuteSlider = new Slider(0, 59, 0);
		minuteSlider.setPadding(new Insets(0, 0, 5, 0));
		second = new Label("00");
		second.setPadding(new Insets(0, 0, 0, 10));
		secondSlider = new Slider(0, 59, 0);
		secondSlider.setPadding(new Insets(0, 0, 5, 0));
		localDateTime = LocalDateTime.now();


		this.setConverter(new StringConverter<>() {
			@Override
			public String toString(LocalDateTime localDateTime) {
				if (localDateTime == null) {
					return null;
				}
				DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM);
				return localDateTime.format(formatter);
			}

			@Override
			public LocalDateTime fromString(String s) {
				try {
					DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM);
					return LocalDateTime.parse(s, formatter);
				} catch (DateTimeParseException exception) {
					try {
						return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(s) * 1000), TimeZone.getDefault().toZoneId());
					} catch (NumberFormatException ex) {
						return LocalDateTime.now();
					}
				}
			}
		});


		fakeDatePicker = new DatePicker();
		fakeDatePicker.valueProperty().setValue(localDateTime.toLocalDate());
		fakeDatePicker.valueProperty().addListener(this::setValue);
		DatePickerSkin datePickerSkin = new DatePickerSkin(fakeDatePicker);
		fakeDatePicker.setSkin(datePickerSkin);
		content = (DatePickerContent) datePickerSkin.getPopupContent();


		GridPane gridPane = new GridPane();
		ColumnConstraints columnConstraints1 = new ColumnConstraints();
		columnConstraints1.setMinWidth(30);
		ColumnConstraints columnConstraints2 = new ColumnConstraints();
		gridPane.getColumnConstraints().addAll(columnConstraints1, columnConstraints2);
		gridPane.add(content, 0, 0, 2, 1);


		hour.textProperty().bind(hourSlider.valueProperty().asString("%.0f"));
		hourSlider.valueProperty().addListener(this::setValue);

		gridPane.add(hour, 0, 1);
		gridPane.add(hourSlider, 1, 1);


		minute.textProperty().bind(minuteSlider.valueProperty().asString("%.0f"));
		minuteSlider.valueProperty().addListener(this::setValue);
		gridPane.add(minute, 0, 2);
		gridPane.add(minuteSlider, 1, 2);

		second.textProperty().bind(secondSlider.valueProperty().asString("%.0f"));
		secondSlider.valueProperty().addListener(this::setValue);
		gridPane.add(second, 0, 3);
		gridPane.add(secondSlider, 1, 3);

		popOver = new PopOver(gridPane);
		popOver.setTitle(name);
	}

	public final String getName() {
		return popOver.getTitle();
	}

	public final void setName(String name) {
		popOver.setTitle(name);
	}

	private void setValue() {
		int hours = (int) Math.round(hourSlider.getValue());
		int minutes = (int) Math.round(minuteSlider.getValue());
		int seconds = (int) Math.round(secondSlider.getValue());
		localDateTime = LocalDateTime.of(fakeDatePicker.getValue(), LocalTime.of(hours, minutes, seconds));
		content.goToDate(localDateTime.toLocalDate(), true);

		DateTimePicker.this.valueProperty().setValue(localDateTime);
	}

	public void setDate(LocalDateTime localDateTime) {
		this.localDateTime = localDateTime;
		hourSlider.setValue(localDateTime.getHour());
		minuteSlider.setValue(localDateTime.getMinute());
		secondSlider.setValue(localDateTime.getSecond());
		fakeDatePicker.setValue(localDateTime.toLocalDate());
		content.goToDate(localDateTime.toLocalDate(), true);
		DateTimePicker.this.valueProperty().setValue(localDateTime);
	}

	public void setDate(LocalDate localDate) {
		this.localDateTime = localDate.atTime(0, 0);
		setDate(localDateTime);
	}

	@Override
	public void show() {
		LocalDateTime current = this.getConverter().fromString(this.getEditor().getText());
		if (current == null) {
			current = LocalDateTime.now();
		}
		setDate(current);
		popOver.show(this);
	}

	@Override
	public void hide() {
	}

	private void setValue(Observable observable) {
		setValue();
	}

	public long getSecondsTimestamp() {
		return Timestamp.valueOf(this.getValue()).getTime() / 1000;
	}
}
